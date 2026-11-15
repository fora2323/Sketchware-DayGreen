package org.sketchware.daygreen.ai;

import android.graphics.Color;

import com.besome.sketch.beans.BlockBean;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import a.a.a.eC;
import a.a.a.jC;
import a.a.a.wq;

/**
 * Menulis block ke event yang sudah punya block (sebagai ekor rantai).
 * v1: hanya block linear (bukan C-block / if), dan event harus sudah ada blocknya.
 */
public final class AiBlockWriter {

    public static final class Step {
        public final String opCode;
        public final List<String> parameters;

        public Step(String opCode, List<String> parameters) {
            this.opCode = opCode;
            this.parameters = parameters;
        }
    }

    public static final class Result {
        public final boolean success;
        public final String message;

        Result(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    private AiBlockWriter() {
    }

    /**
     * Daftar event key yang sudah punya block di file Java tertentu.
     * AI hanya boleh memilih dari daftar ini.
     */
    public static List<String> listEventKeys(String scId, String javaName) {
        return new ArrayList<>(jC.a(scId).b(javaName).keySet());
    }

    public static Result appendBlocks(String scId, String javaName, String eventKey, List<Step> steps) {
        if (steps == null || steps.isEmpty()) {
            return new Result(false, "Tidak ada block yang mau ditulis.");
        }

        Map<String, AiBlockCatalog.Entry> catalog = AiBlockCatalog.getAll();

        // 1. Validasi setiap block sebelum menyentuh data project.
        for (int i = 0; i < steps.size(); i++) {
            Step s = steps.get(i);
            AiBlockCatalog.Entry entry = catalog.get(s.opCode);
            if (entry == null) {
                return new Result(false, "Langkah " + (i + 1) + ": opcode \"" + s.opCode + "\" tidak ada di katalog.");
            }
            if ("c".equals(entry.type) || "e".equals(entry.type)) {
                return new Result(false, "Langkah " + (i + 1) + ": block C/if belum didukung AI.");
            }
            if (s.parameters == null || s.parameters.size() != entry.paramTypes.size()) {
                int expected = entry.paramTypes.size();
                return new Result(false, "Langkah " + (i + 1) + " (" + s.opCode + "): butuh " + expected + " parameter.");
            }
        }

        eC data = jC.a(scId);
        Map<String, ArrayList<BlockBean>> fileBlocks = data.b(javaName);
        if (!fileBlocks.containsKey(eventKey)) {
            return new Result(false, "Event \"" + eventKey + "\" belum ada di file ini. Buat dulu di editor.");
        }

        ArrayList<BlockBean> current = new ArrayList<>(fileBlocks.get(eventKey));
        if (current.isEmpty()) {
            return new Result(false, "Event \"" + eventKey + "\" belum punya block. Buat dulu blocknya di editor.");
        }

        BlockBean tail = findTail(current);
        if (tail == null) {
            return new Result(false, "Rantai block di event ini tidak valid.");
        }

        File logic = new File(wq.a(scId), "logic");
        File backup = backup(logic);

        try {
            int nextId = maxId(fileBlocks) + 1;
            for (Step s : steps) {
                AiBlockCatalog.Entry entry = catalog.get(s.opCode);
                BlockBean block = new BlockBean(String.valueOf(nextId), entry.spec, entry.type, entry.typeName, entry.opCode);
                block.color = parseColor(entry.color);
                block.parameters.clear();
                block.parameters.addAll(s.parameters);
                block.nextBlock = -1;

                tail.nextBlock = nextId;
                current.add(block);
                tail = block;
                nextId++;
            }

            data.a(javaName, eventKey, current);
            data.k();
            if (backup != null) {
                Files.deleteIfExists(backup.toPath());
            }
            return new Result(true, "Berhasil menambah " + steps.size() + " block ke \"" + eventKey + "\".");
        } catch (Exception e) {
            restore(backup, logic);
            return new Result(false, "Gagal menyimpan, perubahan dikembalikan: " + e.getMessage());
        }
    }

    private static BlockBean findTail(List<BlockBean> blocks) {
        for (int i = blocks.size() - 1; i >= 0; i--) {
            BlockBean b = blocks.get(i);
            if (b.nextBlock == -1) {
                return b;
            }
        }
        return null;
    }

    private static int maxId(Map<String, ArrayList<BlockBean>> fileBlocks) {
        int max = 0;
        for (ArrayList<BlockBean> list : fileBlocks.values()) {
            for (BlockBean b : list) {
                try {
                    max = Math.max(max, Integer.parseInt(b.id));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return max;
    }

    private static int parseColor(String hex) {
        try {
            return Color.parseColor(hex);
        } catch (Exception e) {
            return 0xFF000000;
        }
    }

    private static File backup(File logic) {
        if (!logic.exists()) {
            return null;
        }
        File bak = new File(logic.getParentFile(), "logic.ai.bak");
        try {
            Files.copy(logic.toPath(), bak.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return bak;
        } catch (IOException e) {
            return null;
        }
    }

    private static void restore(File backup, File logic) {
        if (backup == null || !backup.exists()) {
            return;
        }
        try {
            Files.copy(backup.toPath(), logic.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
        }
    }
}
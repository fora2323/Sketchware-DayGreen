package org.sketchware.daygreen.ai;

import android.graphics.Color;

import com.besome.sketch.beans.BlockBean;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import a.a.a.eC;
import a.a.a.jC;
import a.a.a.wq;

/**
 * Writes blocks to the end of an event that already has blocks.
 * v1: linear blocks only (no C-blocks or if), and the event must already exist.
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
     * Event keys that already have blocks in the given Java file.
     * The agent may only pick from this list.
     */
    public static List<String> listEventKeys(String scId, String javaName) {
        List<String> keys = new ArrayList<>();
        eC data = jC.a(scId);
        if (data == null) {
            return keys;
        }
        Map<String, ArrayList<BlockBean>> blocks = data.b(javaName);
        if (blocks != null) {
            keys.addAll(blocks.keySet());
        }
        return keys;
    }
    
    public static Result appendBlocks(String scId, String javaName, String eventKey, List<Step> steps) {
        if (steps == null || steps.isEmpty()) {
            return new Result(false, "There are no blocks to write.");
        }

        Map<String, AiBlockCatalog.Entry> catalog = AiBlockCatalog.getAll();

        // 1. Validate every step before touching project data.
        for (int i = 0; i < steps.size(); i++) {
            Step s = steps.get(i);
            AiBlockCatalog.Entry entry = catalog.get(s.opCode);
            if (entry == null) {
                return new Result(false, "Step " + (i + 1) + ": opcode \"" + s.opCode + "\" is not in the catalog.");
            }
            if ("c".equals(entry.type) || "e".equals(entry.type)) {
                return new Result(false, "Step " + (i + 1) + ": C-blocks and if-blocks are not supported by the agent yet.");
            }
            if (s.parameters == null || s.parameters.size() != entry.paramTypes.size()) {
                int expected = entry.paramTypes.size();
                return new Result(false, "Step " + (i + 1) + " (" + s.opCode + "): expects " + expected + " parameter(s).");
            }
        }

        eC data = jC.a(scId);
        Map<String, ArrayList<BlockBean>> fileBlocks = data.b(javaName);
        if (!fileBlocks.containsKey(eventKey)) {
            return new Result(false, "Event \"" + eventKey + "\" does not exist in this file. Create it in the editor first.");
        }

        ArrayList<BlockBean> current = new ArrayList<>(fileBlocks.get(eventKey));
        if (current.isEmpty()) {
            return new Result(false, "Event \"" + eventKey + "\" has no blocks yet. Add its first block in the editor.");
        }

        BlockBean tail = findTail(current);
        if (tail == null) {
            return new Result(false, "The block chain in this event is invalid.");
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
            return new Result(true, "Added " + steps.size() + " block(s) to \"" + eventKey + "\".");
        } catch (Exception e) {
            restore(backup, logic);
            return new Result(false, "Save failed, changes were reverted: " + e.getMessage());
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
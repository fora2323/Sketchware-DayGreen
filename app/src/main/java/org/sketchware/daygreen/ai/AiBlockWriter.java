package org.sketchware.daygreen.ai;

import android.graphics.Color;
import android.util.Pair;

import com.besome.sketch.beans.BlockBean;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import a.a.a.eC;
import a.a.a.jC;
import a.a.a.wq;

/**
 * Writes blocks to Activity events, and creates More Blocks.
 * v1: linear blocks only (no C-blocks or if-blocks).
 */
public final class AiBlockWriter {

    private static final Pattern MORE_BLOCK_NAME = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

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

    /**
     * Readable dump of one event's blocks, in the order they are in the chain,
     * so the agent can see what is already there before it adds more.
     */
    public static String readEventBlocks(String scId, String javaName, String eventKey) {
        eC data = jC.a(scId);
        if (data == null) {
            return "Project data is not loaded. Close the project and open it again.";
        }

        Map<String, ArrayList<BlockBean>> fileBlocks = data.b(javaName);
        ArrayList<BlockBean> blocks = fileBlocks == null ? null : fileBlocks.get(eventKey);
        if (blocks == null || blocks.isEmpty()) {
            return "Event \"" + eventKey + "\" has no blocks yet.";
        }

        StringBuilder sb = new StringBuilder();
        for (BlockBean b : blocks) {
            sb.append("id=").append(b.id)
                    .append(" op=").append(b.opCode)
                    .append(" spec=").append(b.spec)
                    .append(" next=").append(b.nextBlock)
                    .append(" params=").append(b.parameters)
                    .append('\n');
        }
        return sb.toString().trim();
    }

    /**
     * Appends blocks to an event. The event can be an existing one, a built-in Activity event
     * that has no blocks yet, or a More Block (name_moreBlock).
     */
    public static Result appendBlocks(String scId, String javaName, String eventKey, List<Step> steps) {
        if (steps == null || steps.isEmpty()) {
            return fail("There are no blocks to write.");
        }

        Map<String, AiBlockCatalog.Entry> catalog = AiBlockCatalog.getAll();

        // 1. Validate every step before touching project data.
        for (int i = 0; i < steps.size(); i++) {
            Step s = steps.get(i);
            AiBlockCatalog.Entry entry = catalog.get(s.opCode);
            if (entry == null) {
                return fail("Step " + (i + 1) + ": opcode \"" + s.opCode + "\" is not in the catalog.");
            }
            if ("c".equals(entry.type) || "e".equals(entry.type)) {
                return fail("Step " + (i + 1) + ": C-blocks and if-blocks are not supported by the agent yet.");
            }
            if (s.parameters == null || s.parameters.size() != entry.paramTypes.size()) {
                return fail("Step " + (i + 1) + " (" + s.opCode + "): expects "
                        + entry.paramTypes.size() + " parameter(s).");
            }
        }

        eC data = jC.a(scId);
        if (data == null) {
            return fail("Project data is not loaded. Close the project and open it again.");
        }

        if (!isAllowedEventKey(scId, javaName, eventKey)) {
            return fail("Event \"" + eventKey + "\" is not a valid event for this file.");
        }

        Map<String, ArrayList<BlockBean>> fileBlocks = data.b(javaName);
        ArrayList<BlockBean> current = new ArrayList<>();
        if (fileBlocks != null && fileBlocks.get(eventKey) != null) {
            current.addAll(fileBlocks.get(eventKey));
        }

        // An empty event is fine: the first new block becomes the head of the event.
        BlockBean tail = current.isEmpty() ? null : findTail(current);
        if (!current.isEmpty() && tail == null) {
            return fail("The block chain in this event is invalid.");
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

                if (tail != null) {
                    tail.nextBlock = nextId;
                }
                current.add(block);
                tail = block;
                nextId++;
            }

            data.a(javaName, eventKey, current);
            data.k();
            deleteBackup(backup);
            return ok("Added " + steps.size() + " block(s) to \"" + eventKey + "\".");
        } catch (Exception e) {
            restore(backup, logic);
            return fail("Save failed, changes were reverted: " + e.getMessage());
        }
    }

    /**
     * Creates a More Block. Same write path that LogicEditorActivity uses after MakeBlockActivity.
     */
    public static Result createMoreBlock(String scId, String javaName, String name, String spec) {
        if (name == null || !MORE_BLOCK_NAME.matcher(name).matches()) {
            return fail("More block name must start with a letter and contain only letters, digits, or underscores.");
        }
        if (spec == null || spec.trim().isEmpty()) {
            return fail("More block spec cannot be empty.");
        }

        eC data = jC.a(scId);
        if (data == null) {
            return fail("Project data is not loaded. Close the project and open it again.");
        }

        for (Pair<String, String> more : data.i(javaName)) {
            if (name.equals(more.first)) {
                return fail("A more block named \"" + name + "\" already exists.");
            }
        }

        File logic = new File(wq.a(scId), "logic");
        File backup = backup(logic);
        try {
            data.a(javaName, name, spec.trim());
            data.k();
            deleteBackup(backup);
            return ok("Created more block \"" + name + "\".");
        } catch (Exception e) {
            restore(backup, logic);
            return fail("Failed to create more block, changes were reverted: " + e.getMessage());
        }
    }

    private static boolean isAllowedEventKey(String scId, String javaName, String eventKey) {
        if (eventKey == null || eventKey.isEmpty()) {
            return false;
        }

        Map<String, ArrayList<BlockBean>> blocks = jC.a(scId).b(javaName);
        if (blocks != null && blocks.containsKey(eventKey)) {
            return true;
        }
        if (AiEventCatalog.isKnownActivityKey(eventKey)) {
            return true;
        }

        for (Pair<String, String> more : jC.a(scId).i(javaName)) {
            if (eventKey.equals(more.first + "_moreBlock")) {
                return true;
            }
        }
        return false;
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
        if (fileBlocks == null) {
            return max;
        }
        for (ArrayList<BlockBean> list : fileBlocks.values()) {
            if (list == null) continue;
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

    private static void deleteBackup(File backup) {
        if (backup == null) return;
        try {
            Files.deleteIfExists(backup.toPath());
        } catch (IOException ignored) {
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

    private static Result ok(String message) {
        return new Result(true, message);
    }

    private static Result fail(String message) {
        return new Result(false, message);
    }
}
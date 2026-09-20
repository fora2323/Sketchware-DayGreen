package pro.sketchware.code2block;

import android.os.Environment;

import com.besome.sketch.beans.BlockBean;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.aldi.sayuti.block.ExtraBlockFile;
import io.edward.blockcraft.core.BlocksLoader;
import mod.hey.studios.editor.manage.block.ExtraBlockInfo;
import mod.hey.studios.editor.manage.block.v2.BlockLoader;
import pro.sketchware.utility.FileUtil;

public class Code2BlockConverter {

    public static class ConversionResult {
        public final boolean isSuccess;
        public final ArrayList<BlockBean> blocks;
        public final String errorMessage;

        public ConversionResult(ArrayList<BlockBean> blocks) {
            this.isSuccess = true;
            this.blocks = blocks != null ? blocks : new ArrayList<>();
            this.errorMessage = null;
        }

        public ConversionResult(String errorMessage) {
            this.isSuccess = false;
            this.blocks = new ArrayList<>();
            this.errorMessage = errorMessage;
        }
    }

    private static final String CUSTOM_BLOCKS_PATH =
            Environment.getExternalStorageDirectory() + "/.sketchware/resources/block/My Block/block.json";
    private static final String ASD_BLOCKS_PATH =
            Environment.getExternalStorageDirectory() + "/.sketchware/resources/block/asd/block.json";

    private static boolean customBlocksLoaded = false;

    private static void ensureCustomBlocksLoaded() {
        if (customBlocksLoaded) return;
        customBlocksLoaded = true;
        try {
            File customFile = new File(CUSTOM_BLOCKS_PATH);
            if (customFile.exists()) {
                ExtraBlockFile.getExtraBlockData();
                return;
            }
            File asdFile = new File(ASD_BLOCKS_PATH);
            if (asdFile.exists()) {
                try {
                    String content = FileUtil.readFile(asdFile.getAbsolutePath());
                    if (content != null && !content.trim().isEmpty()) {
                        new Gson().fromJson(content, new TypeToken<List<Map<String, Object>>>() {}.getType());
                    }
                } catch (Exception ignored) {}
            }
            ExtraBlockFile.getExtraBlockData();
        } catch (Exception ignored) {}
    }

    public static ConversionResult convertJavaToBlocks(String javaCode) {
        return convertJavaToBlocks(javaCode, null, null);
    }

    public static ConversionResult convertJavaToBlocks(String currentCode, String initialCode, ArrayList<BlockBean> originalBlocks) {
        if (currentCode == null || currentCode.trim().isEmpty()) {
            return new ConversionResult(new ArrayList<>());
        }

        // If code is unchanged and original blocks exist, preserve original blocks directly
        if (initialCode != null && currentCode.trim().equals(initialCode.trim()) && originalBlocks != null && !originalBlocks.isEmpty()) {
            return new ConversionResult(originalBlocks);
        }

        if (!BlocksLoader.isLibraryLoaded()) {
            return new ConversionResult("Rust native library (librust_language.so) is not loaded.\n" +
                    "Details: " + BlocksLoader.getLoadError());
        }

        try {
            String jsonResult = BlocksLoader.generateBlocksFromCode(currentCode);
            ConversionResult converted = parseJsonToConversionResult(jsonResult);
            if (!converted.isSuccess || originalBlocks == null || originalBlocks.isEmpty()) {
                return converted;
            }

            // Differential / statement-level merging
            ArrayList<BlockBean> merged = mergeOriginalAndConverted(originalBlocks, converted.blocks);
            return new ConversionResult(merged);
        } catch (Exception e) {
            return new ConversionResult("Conversion failed: " + e.getMessage());
        }
    }

    public static ConversionResult convertFileToBlocks(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return new ConversionResult(new ArrayList<>());
        }

        if (!BlocksLoader.isLibraryLoaded()) {
            return new ConversionResult("Rust native library (librust_language.so) is not loaded.\n" +
                    "Details: " + BlocksLoader.getLoadError());
        }

        try {
            String jsonResult = BlocksLoader.BlockBeansGenerator(filePath);
            return parseJsonToConversionResult(jsonResult);
        } catch (Exception e) {
            return new ConversionResult("Conversion failed: " + e.getMessage());
        }
    }

    private static ConversionResult parseJsonToConversionResult(String jsonResult) {
        if (jsonResult == null || jsonResult.trim().isEmpty()) {
            return new ConversionResult("Generator returned empty result.");
        }

        JsonElement parsedElement = JsonParser.parseString(jsonResult);
        if (parsedElement.isJsonObject()) {
            JsonObject obj = parsedElement.getAsJsonObject();
            if (obj.has("Error")) {
                return new ConversionResult("Parse Error: " + obj.get("Error").getAsString());
            }
        }

        if (!parsedElement.isJsonArray()) {
            return new ConversionResult("Unexpected output format from generator: " + jsonResult);
        }

        JsonArray array = parsedElement.getAsJsonArray();
        ArrayList<BlockBean> blocks = new ArrayList<>();

        ensureCustomBlocksLoaded();

        // Pass 1: Map original IDs from Rust to new unique sequential IDs
        HashMap<Integer, Integer> originalToNewId = new HashMap<>();
        HashSet<Integer> usedIds = new HashSet<>();
        int nextAvailableId = 10;

        for (int i = 0; i < array.size(); i++) {
            JsonObject blockObj = array.get(i).getAsJsonObject();
            int originalId = blockObj.has("id") ? blockObj.get("id").getAsInt() : (10 + i);

            int newId = originalId;
            if (usedIds.contains(newId)) {
                newId = nextAvailableId++;
                while (usedIds.contains(newId)) {
                    newId = nextAvailableId++;
                }
            }
            usedIds.add(newId);
            originalToNewId.put(originalId, newId);
        }

        // Pass 2: Create BlockBeans and remap connections and parameter block references
        for (int i = 0; i < array.size(); i++) {
            JsonObject blockObj = array.get(i).getAsJsonObject();
            BlockBean bean = new BlockBean();

            int originalId = blockObj.has("id") ? blockObj.get("id").getAsInt() : (10 + i);
            Integer mappedNewId = originalToNewId.get(originalId);
            int newId = mappedNewId != null ? mappedNewId : originalId;
            bean.id = String.valueOf(newId);

            if (blockObj.has("op_code")) {
                bean.opCode = blockObj.get("op_code").getAsString();
            }

            if (blockObj.has("spec")) {
                bean.spec = blockObj.get("spec").getAsString();
            }

            if (blockObj.has("block_type")) {
                bean.type = blockObj.get("block_type").getAsString();
            }

            if (blockObj.has("code")) {
                bean.code = blockObj.get("code").getAsString();
            }

            if (blockObj.has("spec2")) {
                bean.spec2 = blockObj.get("spec2").getAsString();
            }

            if (blockObj.has("color") && !blockObj.get("color").isJsonNull()) {
                bean.color = (int) blockObj.get("color").getAsLong();
            }

            if (blockObj.has("next_block")) {
                int origNext = blockObj.get("next_block").getAsInt();
                if (origNext >= 0) {
                    Integer mappedNext = originalToNewId.get(origNext);
                    bean.nextBlock = mappedNext != null ? mappedNext : origNext;
                } else {
                    bean.nextBlock = -1;
                }
            } else {
                bean.nextBlock = -1;
            }

            if (blockObj.has("sub_stack1")) {
                int origSub1 = blockObj.get("sub_stack1").getAsInt();
                if (origSub1 >= 0) {
                    Integer mappedSub1 = originalToNewId.get(origSub1);
                    bean.subStack1 = mappedSub1 != null ? mappedSub1 : origSub1;
                } else {
                    bean.subStack1 = -1;
                }
            } else {
                bean.subStack1 = -1;
            }

            if (blockObj.has("sub_stack2")) {
                int origSub2 = blockObj.get("sub_stack2").getAsInt();
                if (origSub2 >= 0) {
                    Integer mappedSub2 = originalToNewId.get(origSub2);
                    bean.subStack2 = mappedSub2 != null ? mappedSub2 : origSub2;
                } else {
                    bean.subStack2 = -1;
                }
            } else {
                bean.subStack2 = -1;
            }

            bean.parameters = new ArrayList<>();
            if (blockObj.has("parameters") && blockObj.get("parameters").isJsonArray()) {
                JsonArray params = blockObj.get("parameters").getAsJsonArray();
                for (int p = 0; p < params.size(); p++) {
                    JsonElement pElem = params.get(p);
                    String rawParam = pElem.isJsonNull() ? "" : pElem.getAsString();
                    String processedParam = rawParam;
                    if (rawParam.startsWith("@")) {
                        try {
                            int refOrigId = Integer.parseInt(rawParam.substring(1));
                            Integer mappedRef = originalToNewId.get(refOrigId);
                            int refNewId = mappedRef != null ? mappedRef : refOrigId;
                            processedParam = "@" + refNewId;
                        } catch (NumberFormatException ignored) {}
                    }
                    bean.parameters.add(processedParam);
                }
            }

            normalizeBlock(bean);
            blocks.add(bean);
        }

        return new ConversionResult(blocks);
    }

    private static void normalizeBlock(BlockBean bean) {
        if (bean.opCode == null) {
            bean.opCode = "";
        }

        switch (bean.opCode) {
            case "addSourceDirectlyString":
            case "addSourceDirectlyObject":
                bean.opCode = "asdString";
                bean.type = "s";
                bean.spec = "string %s.inputOnly";
                bean.code = "%s";
                bean.color = 0xff5cb722;
                break;

            case "addSourceDirectlyDouble":
                bean.opCode = "asdNumber";
                bean.type = "d";
                bean.spec = "number %s.inputOnly";
                bean.code = "%s";
                bean.color = 0xff5cb722;
                break;

            case "addSourceDirectlyBool":
                bean.opCode = "asdBoolean";
                bean.type = "b";
                bean.spec = "boolean %s.inputOnly";
                bean.code = "%s";
                bean.color = 0xff5cb722;
                break;

            case "addSourceDirectly":
                bean.type = " ";
                bean.spec = "add source directly %s.inputOnly";
                bean.code = "%s";
                bean.color = 0xff5cb722;
                break;

            case "asdString":
                bean.type = "s";
                bean.spec = "string %s.inputOnly";
                bean.code = "%s";
                bean.color = 0xff5cb722;
                break;

            case "asdNumber":
                bean.type = "d";
                bean.spec = "number %s.inputOnly";
                bean.code = "%s";
                bean.color = 0xff5cb722;
                break;

            case "asdBoolean":
                bean.type = "b";
                bean.spec = "boolean %s.inputOnly";
                bean.code = "%s";
                bean.color = 0xff5cb722;
                break;

            case "addSourceDirectlyIf":
                bean.type = "c";
                bean.spec = "add source directly %s.inputOnly";
                bean.code = "if (%1$s) {\n%2$s\n}";
                bean.color = 0xff5cb722;
                break;

            default:
                ma.swblockeditor.core.BlockBean indexed = BlocksLoader.ALL_INDEXED_BLOCKS.get(bean.opCode);
                if (indexed != null) {
                    if (bean.spec == null || bean.spec.isEmpty()) bean.spec = indexed.getSpec();
                    if (bean.spec2 == null || bean.spec2.isEmpty()) bean.spec2 = indexed.getSpec2();
                    if (bean.code == null || bean.code.isEmpty()) bean.code = indexed.getCode();
                    if (bean.type == null || bean.type.isEmpty()) bean.type = indexed.getType();
                    if (bean.color == 0) bean.color = indexed.getColor();
                } else {
                    ExtraBlockInfo extraInfo = BlockLoader.getBlockInfo(bean.opCode);
                    if (extraInfo != null && !extraInfo.isMissing) {
                        if (bean.spec == null || bean.spec.isEmpty()) bean.spec = extraInfo.getSpec();
                        if (bean.spec2 == null || bean.spec2.isEmpty()) bean.spec2 = extraInfo.getSpec2();
                        if (bean.code == null || bean.code.isEmpty()) bean.code = extraInfo.getCode();
                        if (bean.color == 0) bean.color = extraInfo.getColor();
                    } else {
                        // Fallback to Operator ASD block
                        if ("s".equals(bean.type)) {
                            bean.opCode = "asdString";
                            bean.spec = "string %s.inputOnly";
                            bean.code = "%s";
                            bean.color = 0xff5cb722;
                        } else if ("d".equals(bean.type)) {
                            bean.opCode = "asdNumber";
                            bean.spec = "number %s.inputOnly";
                            bean.code = "%s";
                            bean.color = 0xff5cb722;
                        } else if ("b".equals(bean.type)) {
                            bean.opCode = "asdBoolean";
                            bean.spec = "boolean %s.inputOnly";
                            bean.code = "%s";
                            bean.color = 0xff5cb722;
                        } else {
                            bean.opCode = "addSourceDirectly";
                            bean.type = " ";
                            bean.spec = "add source directly %s.inputOnly";
                            bean.code = "%s";
                            bean.color = 0xff5cb722;
                        }
                    }
                }
                break;
        }
    }

    private static class BlockSubtree {
        final BlockBean root;
        final ArrayList<BlockBean> allNodes;
        final String signature;

        BlockSubtree(BlockBean root, ArrayList<BlockBean> allNodes, String signature) {
            this.root = root;
            this.allNodes = allNodes;
            this.signature = signature;
        }
    }

    private static ArrayList<BlockBean> mergeOriginalAndConverted(ArrayList<BlockBean> originalBlocks, ArrayList<BlockBean> convertedBlocks) {
        if (convertedBlocks == null || convertedBlocks.isEmpty()) {
            return new ArrayList<>();
        }
        if (originalBlocks == null || originalBlocks.isEmpty()) {
            return convertedBlocks;
        }

        List<BlockSubtree> origSubtrees = extractTopLevelSubtrees(originalBlocks);
        List<BlockSubtree> convSubtrees = extractTopLevelSubtrees(convertedBlocks);

        if (origSubtrees.isEmpty()) return convertedBlocks;
        if (convSubtrees.isEmpty()) return new ArrayList<>();

        boolean[] origUsed = new boolean[origSubtrees.size()];
        List<BlockSubtree> chosenSubtrees = new ArrayList<>();

        for (BlockSubtree conv : convSubtrees) {
            int matchIdx = -1;
            for (int i = 0; i < origSubtrees.size(); i++) {
                if (!origUsed[i] && signaturesMatch(conv.signature, origSubtrees.get(i).signature)) {
                    matchIdx = i;
                    break;
                }
            }

            if (matchIdx >= 0) {
                // Keep the exact original subtree without modification
                origUsed[matchIdx] = true;
                chosenSubtrees.add(origSubtrees.get(matchIdx));
            } else {
                // Use the newly converted subtree
                chosenSubtrees.add(conv);
            }
        }

        return assembleFinalBlocks(chosenSubtrees);
    }

    private static List<BlockSubtree> extractTopLevelSubtrees(ArrayList<BlockBean> blocks) {
        HashMap<String, BlockBean> map = new HashMap<>();
        Set<Integer> nonRootIds = new HashSet<>();

        for (BlockBean b : blocks) {
            map.put(b.id, b);
            if (b.nextBlock >= 0) nonRootIds.add(b.nextBlock);
            if (b.subStack1 >= 0) nonRootIds.add(b.subStack1);
            if (b.subStack2 >= 0) nonRootIds.add(b.subStack2);
            for (String p : b.parameters) {
                if (p != null && p.startsWith("@")) {
                    try {
                        nonRootIds.add(Integer.parseInt(p.substring(1)));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        // Find root of top-level chain
        BlockBean root = null;
        for (BlockBean b : blocks) {
            try {
                int idInt = Integer.parseInt(b.id);
                if (!nonRootIds.contains(idInt)) {
                    root = b;
                    break;
                }
            } catch (NumberFormatException ignored) {}
        }
        if (root == null && !blocks.isEmpty()) {
            root = blocks.get(0);
        }

        List<BlockSubtree> subtrees = new ArrayList<>();
        BlockBean curr = root;
        Set<String> visited = new HashSet<>();

        while (curr != null && visited.add(curr.id)) {
            ArrayList<BlockBean> subtreeNodes = new ArrayList<>();
            collectSubtree(curr, map, subtreeNodes, false);
            String sig = computeSubtreeSignature(curr, map);
            subtrees.add(new BlockSubtree(curr, subtreeNodes, sig));

            if (curr.nextBlock >= 0) {
                curr = map.get(String.valueOf(curr.nextBlock));
            } else {
                break;
            }
        }

        return subtrees;
    }

    private static void collectSubtree(BlockBean node, HashMap<String, BlockBean> map, ArrayList<BlockBean> nodes, boolean isSubnode) {
        if (node == null) return;
        nodes.add(node);

        for (String p : node.parameters) {
            if (p != null && p.startsWith("@")) {
                try {
                    BlockBean paramChild = map.get(p.substring(1));
                    if (paramChild != null) {
                        collectSubtree(paramChild, map, nodes, true);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        if (node.subStack1 >= 0) {
            BlockBean sub1 = map.get(String.valueOf(node.subStack1));
            collectChain(sub1, map, nodes);
        }
        if (node.subStack2 >= 0) {
            BlockBean sub2 = map.get(String.valueOf(node.subStack2));
            collectChain(sub2, map, nodes);
        }
    }

    private static void collectChain(BlockBean start, HashMap<String, BlockBean> map, ArrayList<BlockBean> nodes) {
        BlockBean curr = start;
        Set<String> visited = new HashSet<>();
        while (curr != null && visited.add(curr.id)) {
            collectSubtree(curr, map, nodes, true);
            if (curr.nextBlock >= 0) {
                curr = map.get(String.valueOf(curr.nextBlock));
            } else {
                break;
            }
        }
    }

    private static String computeSubtreeSignature(BlockBean node, HashMap<String, BlockBean> map) {
        if (node == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append(node.opCode).append("(");
        for (int i = 0; i < node.parameters.size(); i++) {
            if (i > 0) sb.append(",");
            String p = node.parameters.get(i);
            if (p != null && p.startsWith("@")) {
                BlockBean child = map.get(p.substring(1));
                sb.append("@").append(computeSubtreeSignature(child, map));
            } else {
                sb.append(normalizeCodeSnippet(p));
            }
        }
        sb.append(")");
        if (node.subStack1 >= 0) {
            sb.append("{s1:");
            BlockBean s1 = map.get(String.valueOf(node.subStack1));
            sb.append(computeChainSignature(s1, map));
            sb.append("}");
        }
        if (node.subStack2 >= 0) {
            sb.append("{s2:");
            BlockBean s2 = map.get(String.valueOf(node.subStack2));
            sb.append(computeChainSignature(s2, map));
            sb.append("}");
        }
        return sb.toString();
    }

    private static String computeChainSignature(BlockBean start, HashMap<String, BlockBean> map) {
        StringBuilder sb = new StringBuilder();
        BlockBean curr = start;
        Set<String> visited = new HashSet<>();
        while (curr != null && visited.add(curr.id)) {
            sb.append(computeSubtreeSignature(curr, map)).append(";");
            if (curr.nextBlock >= 0) {
                curr = map.get(String.valueOf(curr.nextBlock));
            } else {
                break;
            }
        }
        return sb.toString();
    }

    private static String normalizeCodeSnippet(String s) {
        if (s == null) return "";
        return s.replaceAll("\\s+", "").trim();
    }

    private static boolean signaturesMatch(String sig1, String sig2) {
        if (sig1 == null || sig2 == null) return false;
        return normalizeCodeSnippet(sig1).equals(normalizeCodeSnippet(sig2));
    }

    private static ArrayList<BlockBean> assembleFinalBlocks(List<BlockSubtree> chosenSubtrees) {
        Set<Integer> usedIds = new HashSet<>();
        int maxId = 0;

        // Collect existing IDs from preserved subtrees
        for (BlockSubtree subtree : chosenSubtrees) {
            for (BlockBean node : subtree.allNodes) {
                try {
                    int idInt = Integer.parseInt(node.id);
                    usedIds.add(idInt);
                    maxId = Math.max(maxId, idInt);
                } catch (NumberFormatException ignored) {}
            }
        }

        ArrayList<BlockBean> finalBlocks = new ArrayList<>();
        int nextAvailableId = Math.max(10, maxId + 1);

        for (int t = 0; t < chosenSubtrees.size(); t++) {
            BlockSubtree subtree = chosenSubtrees.get(t);
            BlockSubtree nextSubtree = (t + 1 < chosenSubtrees.size()) ? chosenSubtrees.get(t + 1) : null;

            // Check if this subtree has ID collisions or is a newly converted subtree that needs re-indexing
            HashMap<Integer, Integer> remappedIds = new HashMap<>();
            Set<Integer> subtreeIds = new HashSet<>();
            boolean needsReindexing = false;

            for (BlockBean node : subtree.allNodes) {
                try {
                    int idInt = Integer.parseInt(node.id);
                    if (!subtreeIds.add(idInt)) {
                        needsReindexing = true;
                        break;
                    }
                } catch (NumberFormatException e) {
                    needsReindexing = true;
                    break;
                }
            }

            if (needsReindexing) {
                for (BlockBean node : subtree.allNodes) {
                    int oldId;
                    try {
                        oldId = Integer.parseInt(node.id);
                    } catch (NumberFormatException e) {
                        oldId = nextAvailableId++;
                    }
                    while (usedIds.contains(nextAvailableId)) {
                        nextAvailableId++;
                    }
                    int newId = nextAvailableId++;
                    usedIds.add(newId);
                    remappedIds.put(oldId, newId);
                }

                // Apply remapping to clone nodes
                for (BlockBean node : subtree.allNodes) {
                    BlockBean clone = node.clone();
                    int oldId = Integer.parseInt(node.id);
                    Integer mappedId = remappedIds.get(oldId);
                    clone.id = String.valueOf(mappedId != null ? mappedId : oldId);

                    if (clone.nextBlock >= 0 && remappedIds.containsKey(clone.nextBlock)) {
                        clone.nextBlock = remappedIds.get(clone.nextBlock);
                    }
                    if (clone.subStack1 >= 0 && remappedIds.containsKey(clone.subStack1)) {
                        clone.subStack1 = remappedIds.get(clone.subStack1);
                    }
                    if (clone.subStack2 >= 0 && remappedIds.containsKey(clone.subStack2)) {
                        clone.subStack2 = remappedIds.get(clone.subStack2);
                    }

                    ArrayList<String> newParams = new ArrayList<>();
                    for (String p : clone.parameters) {
                        if (p != null && p.startsWith("@")) {
                            try {
                                int refId = Integer.parseInt(p.substring(1));
                                Integer mappedRef = remappedIds.get(refId);
                                newParams.add("@" + (mappedRef != null ? mappedRef : refId));
                            } catch (NumberFormatException e) {
                                newParams.add(p);
                            }
                        } else {
                            newParams.add(p);
                        }
                    }
                    clone.parameters = newParams;
                    finalBlocks.add(clone);
                }
            } else {
                for (BlockBean node : subtree.allNodes) {
                    finalBlocks.add(node.clone());
                }
            }
        }

        // Now link top-level blocks in chain
        HashMap<String, BlockBean> finalMap = new HashMap<>();
        for (BlockBean b : finalBlocks) {
            finalMap.put(b.id, b);
        }

        for (int t = 0; t < chosenSubtrees.size(); t++) {
            BlockSubtree subtree = chosenSubtrees.get(t);
            BlockSubtree nextSubtree = (t + 1 < chosenSubtrees.size()) ? chosenSubtrees.get(t + 1) : null;

            BlockBean currentRoot = finalBlocks.stream().filter(b -> b.id.equals(subtree.root.id)).findFirst().orElse(null);
            if (currentRoot != null) {
                if (nextSubtree != null) {
                    try {
                        currentRoot.nextBlock = Integer.parseInt(nextSubtree.root.id);
                    } catch (NumberFormatException ignored) {}
                } else {
                    currentRoot.nextBlock = -1;
                }
            }
        }

        return finalBlocks;
    }
}

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

import dev.aldi.sayuti.block.ExtraBlockFile;
import io.edward.blockcraft.core.BlocksLoader;
import pro.sketchware.utility.FileUtil;

public class Code2BlockConverter {

    public static class ConversionResult {
        public final boolean isSuccess;
        public final ArrayList<BlockBean> blocks;
        public final String errorMessage;

        public ConversionResult(ArrayList<BlockBean> blocks) {
            this.isSuccess = true;
            this.blocks = blocks;
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
                    List<Map<String, Object>> loaded = new Gson().fromJson(content,
                            new TypeToken<List<Map<String, Object>>>() {}.getType());
                }
            } catch (Exception ignored) {}
        }
        ExtraBlockFile.getExtraBlockData();
    }

    public static ConversionResult convertJavaToBlocks(String javaCode) {
        if (javaCode == null || javaCode.trim().isEmpty()) {
            return new ConversionResult(new ArrayList<>());
        }

        if (!BlocksLoader.isLibraryLoaded()) {
            return new ConversionResult("Rust native library (librust_language.so) is not loaded.\n" +
                    "Please compile rust_language and place librust_language.so in app/src/main/jniLibs/arm64-v8a/\n" +
                    "Details: " + BlocksLoader.getLoadError());
        }

        try {
            String jsonResult = BlocksLoader.generateBlocksFromCode(javaCode);
            return parseJsonToConversionResult(jsonResult);
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
                    "Please compile rust_language and place librust_language.so in app/src/main/jniLibs/arm64-v8a/\n" +
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

            if (bean.opCode != null && !bean.opCode.isEmpty()) {
                ma.swblockeditor.core.BlockBean indexed = BlocksLoader.ALL_INDEXED_BLOCKS.get(bean.opCode);
                if (indexed != null) {
                    if (bean.spec == null || bean.spec.isEmpty()) {
                        bean.spec = indexed.getSpec();
                    }
                    if (bean.spec2 == null || bean.spec2.isEmpty()) {
                        bean.spec2 = indexed.getSpec2();
                    }
                    if (bean.code == null || bean.code.isEmpty()) {
                        bean.code = indexed.getCode();
                    }
                    if (bean.type == null || bean.type.isEmpty()) {
                        bean.type = indexed.getType();
                    }
                    if (bean.color == 0) {
                        bean.color = indexed.getColor();
                    }
                    blocks.add(bean);
                }
                // If not in Sketchware's built-in block catalog, skip/discard it entirely.
            }
        }

        return new ConversionResult(blocks);
    }
}

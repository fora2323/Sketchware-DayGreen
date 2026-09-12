package pro.sketchware.code2block;

import com.besome.sketch.beans.BlockBean;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;

import io.edward.blockcraft.core.BlocksLoader;
import mod.hey.studios.editor.manage.block.v2.BlockLoader;

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

            for (int i = 0; i < array.size(); i++) {
                JsonObject blockObj = array.get(i).getAsJsonObject();
                BlockBean bean = new BlockBean();

                if (blockObj.has("id")) {
                    bean.id = String.valueOf(blockObj.get("id").getAsInt());
                } else {
                    bean.id = String.valueOf(10 + i);
                }

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
                    bean.nextBlock = blockObj.get("next_block").getAsInt();
                }

                if (blockObj.has("sub_stack1")) {
                    bean.subStack1 = blockObj.get("sub_stack1").getAsInt();
                }

                if (blockObj.has("sub_stack2")) {
                    bean.subStack2 = blockObj.get("sub_stack2").getAsInt();
                }

                bean.parameters = new ArrayList<>();
                if (blockObj.has("parameters") && blockObj.get("parameters").isJsonArray()) {
                    JsonArray params = blockObj.get("parameters").getAsJsonArray();
                    for (int p = 0; p < params.size(); p++) {
                        JsonElement pElem = params.get(p);
                        bean.parameters.add(pElem.isJsonNull() ? "" : pElem.getAsString());
                    }
                }

                if (bean.opCode != null && !bean.opCode.isEmpty()) {
                    ma.swblockeditor.core.BlockBean indexed = BlocksLoader.ALL_INDEXED_BLOCKS.get(bean.opCode);
                    if (indexed != null) {
                        if ((bean.spec == null || bean.spec.isEmpty()) && indexed.getSpec() != null && !indexed.getSpec().isEmpty()) {
                            bean.spec = indexed.getSpec();
                        }
                        if ((bean.spec2 == null || bean.spec2.isEmpty()) && indexed.getSpec2() != null && !indexed.getSpec2().isEmpty()) {
                            bean.spec2 = indexed.getSpec2();
                        }
                        if ((bean.code == null || bean.code.isEmpty()) && indexed.getCode() != null && !indexed.getCode().isEmpty()) {
                            bean.code = indexed.getCode();
                        }
                        if ((bean.type == null || bean.type.isEmpty()) && indexed.getType() != null && !indexed.getType().isEmpty()) {
                            bean.type = indexed.getType();
                        }
                        if (bean.color == 0 && indexed.getColor() != 0) {
                            bean.color = indexed.getColor();
                        }
                    }

                    if (bean.spec != null && !bean.spec.isEmpty()) {
                        BlockLoader.registerRuntimeBlock(bean.opCode, bean.spec, bean.spec2, bean.code, bean.color);
                    }
                }

                blocks.add(bean);
            }

            return new ConversionResult(blocks);
        } catch (Exception e) {
            return new ConversionResult("Conversion failed: " + e.getMessage());
        }
    }
}

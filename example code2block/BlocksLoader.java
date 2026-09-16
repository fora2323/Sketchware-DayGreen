package io.edward.blockcraft.core;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import io.edward.blockcraft.blocks.records.BlockPalette;
import ma.swblockeditor.core.BlockBean;

public class BlocksLoader {

    private native String loadBlocksCollection();
    private static native String[] nativeBlockTokenizer(byte[] input);
    public static native String BlockBeansGenerator(String xmlCode);

    public static ArrayList<String> blockTokenizer(String input) {
        String[] arr = nativeBlockTokenizer(input.getBytes(StandardCharsets.UTF_8));
        return new ArrayList<>(Arrays.asList(arr));
    }

    public static final ArrayList<BlockPalette> JAVA_PALETTES = new ArrayList<>();
    public static final ArrayList<BlockPalette> XML_PALETTES = new ArrayList<>();
    public static final HashMap<String, BlockBean> ALL_INDEXED_BLOCKS = new HashMap<>();

    public void init() {
        System.loadLibrary("rust_language");
        try {
            JSONObject root = new JSONObject(loadBlocksCollection());
            String[] langs = {"java", "xml"};

            for (String langKey : langs) {
                JSONArray palettes = root.getJSONArray(langKey);
                for (int i = 0; i < palettes.length(); i++) {
                    JSONObject p = palettes.getJSONObject(i);
                    BlockPalette palette = new BlockPalette(
                            p.getString("name"),
                            p.getInt("color")
                    );

                    JSONArray beans = p.getJSONArray("beans");
                    for (int j = 0; j < beans.length(); j++) {
                        JSONObject b = beans.getJSONObject(j);
                        BlockBean blockBean = new BlockBean()
                                .setOpCode(b.getString("op_code"))
                                .setHeaderText(b.getString("header_text"))
                                .setColor(b.getInt("color"))
                                .setType(b.getString("block_type"))
                                .setCode(b.getString("code"))
                                .setSpec(b.getString("spec"))
                                .setSpec2(b.getString("spec2"))
                                .setTokenizedSpec(toStringList(b.getJSONArray("tokenized_spec")))
                                .setParameters(toStringList(b.getJSONArray("parameters")));

                        // TODO: needs to support back addButton feat from @PaletteBlock

                        palette.blocks().add(blockBean);
                        ALL_INDEXED_BLOCKS.put(blockBean.getOpCode(), blockBean);
                    }

                    if (langKey.equals("java")) {
                        JAVA_PALETTES.add(palette);
                    } else {
                        XML_PALETTES.add(palette);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static ArrayList<BlockPalette> getPalettes(String lang) {
        return switch (lang) {
            case "java" -> JAVA_PALETTES;
            case "xml" -> XML_PALETTES;
            default -> new ArrayList<>();
        };
    }

    public static ArrayList<String> toStringList(JSONArray arr) {
        ArrayList<String> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            list.add(arr.optString(i));
        }
        return list;
    }


}

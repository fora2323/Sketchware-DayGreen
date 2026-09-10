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

    private static native String loadBlocksCollection();
    private static native String[] nativeBlockTokenizer(byte[] input);
    public static native String BlockBeansGenerator(String filePath);
    public static native String generateBlocksFromCode(String code);

    private static boolean isLoaded = false;
    private static String loadError = null;

    public static final ArrayList<BlockPalette> JAVA_PALETTES = new ArrayList<>();
    public static final ArrayList<BlockPalette> XML_PALETTES = new ArrayList<>();
    public static final HashMap<String, BlockBean> ALL_INDEXED_BLOCKS = new HashMap<>();

    public static synchronized void init() {
        if (isLoaded) return;
        try {
            System.loadLibrary("rust_language");
            String collection = loadBlocksCollection();
            if (collection == null || collection.isEmpty()) return;

            JSONObject root = new JSONObject(collection);
            String[] langs = {"java", "xml"};

            JAVA_PALETTES.clear();
            XML_PALETTES.clear();
            ALL_INDEXED_BLOCKS.clear();

            for (String langKey : langs) {
                if (!root.has(langKey)) continue;
                JSONArray palettes = root.getJSONArray(langKey);
                for (int i = 0; i < palettes.length(); i++) {
                    JSONObject p = palettes.getJSONObject(i);
                    BlockPalette palette = new BlockPalette(
                            p.optString("name", "Palette"),
                            p.optInt("color", 0xFF000000)
                    );

                    JSONArray beans = p.optJSONArray("beans");
                    if (beans != null) {
                        for (int j = 0; j < beans.length(); j++) {
                            JSONObject b = beans.getJSONObject(j);
                            BlockBean blockBean = new BlockBean()
                                    .setOpCode(b.optString("op_code"))
                                    .setHeaderText(b.optString("header_text"))
                                    .setColor(b.optInt("color"))
                                    .setType(b.optString("block_type"))
                                    .setCode(b.optString("code"))
                                    .setSpec(b.optString("spec"))
                                    .setSpec2(b.optString("spec2"))
                                    .setTokenizedSpec(toStringList(b.optJSONArray("tokenized_spec")))
                                    .setParameters(toStringList(b.optJSONArray("parameters")));

                            palette.blocks().add(blockBean);
                            ALL_INDEXED_BLOCKS.put(blockBean.getOpCode(), blockBean);
                        }
                    }

                    if (langKey.equals("java")) {
                        JAVA_PALETTES.add(palette);
                    } else {
                        XML_PALETTES.add(palette);
                    }
                }
            }
            isLoaded = true;
        } catch (Throwable e) {
            loadError = e.getMessage();
            e.printStackTrace();
        }
    }

    public static boolean isLibraryLoaded() {
        if (!isLoaded) {
            init();
        }
        return isLoaded;
    }

    public static String getLoadError() {
        return loadError != null ? loadError : "Unknown load error";
    }

    public static ArrayList<BlockPalette> getPalettes(String lang) {
        if (!isLoaded) {
            init();
        }
        return switch (lang.toLowerCase()) {
            case "java" -> JAVA_PALETTES;
            case "xml" -> XML_PALETTES;
            default -> new ArrayList<>();
        };
    }

    public static ArrayList<String> blockTokenizer(String input) {
        if (!isLoaded) {
            init();
        }
        if (input == null) return new ArrayList<>();
        try {
            String[] arr = nativeBlockTokenizer(input.getBytes(StandardCharsets.UTF_8));
            if (arr == null) return new ArrayList<>();
            return new ArrayList<>(Arrays.asList(arr));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static ArrayList<String> toStringList(JSONArray arr) {
        ArrayList<String> list = new ArrayList<>();
        if (arr == null) return list;
        for (int i = 0; i < arr.length(); i++) {
            list.add(arr.optString(i, ""));
        }
        return list;
    }
}

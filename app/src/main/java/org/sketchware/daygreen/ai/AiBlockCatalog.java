package org.sketchware.daygreen.ai;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import a.a.a.lq;
import mod.hilal.saif.blocks.BlocksHandler;
import pro.sketchware.blocks.ExtraBlocks;

public final class AiBlockCatalog {

    private static final String TAG = "AiBlockCatalog";

    public static final class Entry {
        public final String opCode;
        public final String type;
        public final String typeName;
        public final String spec;
        public final String color;
        public final List<String> paramTypes;

        Entry(String opCode, String type, String typeName, String spec, String color, List<String> paramTypes) {
            this.opCode = opCode;
            this.type = type;
            this.typeName = typeName;
            this.spec = spec;
            this.color = color;
            this.paramTypes = Collections.unmodifiableList(paramTypes);
        }
    }

    private static Map<String, Entry> cache;

    private AiBlockCatalog() {
    }

    public static synchronized Map<String, Entry> getAll() {
        if (cache != null) {
            return cache;
        }

        ArrayList<HashMap<String, Object>> raw = new ArrayList<>();
        try {
            BlocksHandler.builtInBlocks(raw);
        } catch (Throwable t) {
            Log.e(TAG, "BlocksHandler.builtInBlocks failed, falling back to ExtraBlocks only", t);
            raw.clear();
            try {
                ExtraBlocks.extraBlocks(raw);
            } catch (Throwable t2) {
                Log.e(TAG, "ExtraBlocks.extraBlocks failed too", t2);
            }
        }

        Map<String, Entry> map = new LinkedHashMap<>();
        for (HashMap<String, Object> b : raw) {
            try {
                String name = str(b.get("name"));
                if (name.isEmpty() || map.containsKey(name)) {
                    continue;
                }
                map.put(name, new Entry(
                        name,
                        str(b.get("type")),
                        str(b.get("typeName")),
                        str(b.get("spec")),
                        str(b.get("color")),
                        safeParamTypes(name)
                ));
            } catch (Throwable t) {
                Log.e(TAG, "Skipping a block that could not be read", t);
            }
        }

        if (!map.isEmpty()) {
            cache = map;
        }
        return map;
    }

    public static Entry get(String opCode) {
        return getAll().get(opCode);
    }

    /**
     * Searches blocks by opcode or spec text. Used by the agent before it picks a block.
     */
    public static List<Entry> search(String query, int limit) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Entry> result = new ArrayList<>();
        for (Entry e : getAll().values()) {
            if (q.isEmpty()
                    || e.opCode.toLowerCase(Locale.ROOT).contains(q)
                    || e.spec.toLowerCase(Locale.ROOT).contains(q)) {
                result.add(e);
                if (result.size() >= limit) {
                    break;
                }
            }
        }
        return result;
    }

    private static List<String> safeParamTypes(String opCode) {
        try {
            List<String> types = lq.a(opCode);
            return types == null ? new ArrayList<>() : new ArrayList<>(types);
        } catch (Throwable t) {
            Log.w(TAG, "No param info for opcode " + opCode, t);
            return new ArrayList<>();
        }
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}
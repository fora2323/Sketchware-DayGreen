package org.sketchware.daygreen.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import a.a.a.lq;
import mod.hilal.saif.blocks.BlocksHandler;

public final class AiBlockCatalog {

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
        if (cache == null) {
            ArrayList<HashMap<String, Object>> raw = new ArrayList<>();
            BlocksHandler.builtInBlocks(raw);

            Map<String, Entry> map = new LinkedHashMap<>();
            for (HashMap<String, Object> b : raw) {
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
                        lq.a(name)
                ));
            }
            cache = map;
        }
        return cache;
    }

    public static Entry get(String opCode) {
        return getAll().get(opCode);
    }

    /**
     * Cari block berdasarkan opcode atau teks spec. Dipakai AI sebelum memilih block.
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

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}
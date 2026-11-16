package org.sketchware.daygreen.ai;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import a.a.a.lq;
import mod.hilal.saif.blocks.BlocksHandler;
import pro.sketchware.blocks.ExtraBlocks;

public final class AiBlockCatalog {

    private static final String TAG = "AiBlockCatalog";

    /** Matches Sketchware spec placeholders such as %s, %d, %m.color. */
    private static final Pattern PLACEHOLDER = Pattern.compile("%[a-zA-Z]");

    public static final class Entry {
        public final String opCode;
        public final String type;
        public final String typeName;
        public final String spec;
        public final String color;
        /** Empty when paramsKnown is false. Never treat empty as "no parameters" unless paramsKnown is true. */
        public final List<String> paramTypes;
        /** False when Sketchware could not report the parameter types for this block. */
        public final boolean paramsKnown;

        Entry(String opCode, String type, String typeName, String spec, String color,
              List<String> paramTypes, boolean paramsKnown) {
            this.opCode = opCode;
            this.type = type;
            this.typeName = typeName;
            this.spec = spec;
            this.color = color;
            this.paramTypes = Collections.unmodifiableList(paramTypes);
            this.paramsKnown = paramsKnown;
        }
    }

    private static final class Scored {
        final Entry entry;
        final int score;

        Scored(Entry entry, int score) {
            this.entry = entry;
            this.score = score;
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
                List<String> types = safeParamTypes(name);
                map.put(name, new Entry(
                        name,
                        str(b.get("type")),
                        str(b.get("typeName")),
                        str(b.get("spec")),
                        str(b.get("color")),
                        types == null ? new ArrayList<>() : types,
                        types != null
                ));
            } catch (Throwable t) {
                Log.e(TAG, "Skipping a block that could not be read", t);
            }
        }

        Log.i(TAG, "Loaded " + map.size() + " blocks into the agent catalog.");
        if (!map.isEmpty()) {
            cache = map;
        }
        return map;
    }

    public static Entry get(String opCode) {
        return getAll().get(opCode);
    }

    /**
     * Searches blocks by words. Every word in the query is matched against the opcode and spec,
     * in any order. Results are sorted by how many words match, best first.
     * An empty query returns the first blocks in the catalog.
     */
    public static List<Entry> search(String query, int limit) {
        List<String> tokens = tokenize(query);
        List<Entry> all = new ArrayList<>(getAll().values());

        if (tokens.isEmpty()) {
            return all.subList(0, Math.min(limit, all.size()));
        }

        List<Scored> scored = new ArrayList<>();
        for (Entry e : all) {
            String opcode = e.opCode.toLowerCase(Locale.ROOT);
            String spec = e.spec.toLowerCase(Locale.ROOT);
            int score = 0;
            for (String token : tokens) {
                if (opcode.equals(token)) {
                    score += 5;
                } else if (opcode.contains(token)) {
                    score += 2;
                } else if (spec.contains(token)) {
                    score += 2;
                }
            }
            if (score > 0) {
                scored.add(new Scored(e, score));
            }
        }

        scored.sort(Comparator
                .comparingInt((Scored s) -> s.score).reversed()
                .thenComparing(s -> s.entry.opCode));

        List<Entry> result = new ArrayList<>();
        for (Scored s : scored) {
            if (result.size() >= limit) break;
            result.add(s.entry);
        }
        return result;
    }

    /**
     * Number of parameter placeholders in a spec, e.g. "setText %s to %d" gives 2.
     * Used to check that the parameter count from the catalog matches the spec.
     */
    public static int countPlaceholders(String spec) {
        if (spec == null) return 0;
        Matcher m = PLACEHOLDER.matcher(spec);
        int count = 0;
        while (m.find()) count++;
        return count;
    }

    /**
     * Lowercase words of the query. Punctuation is removed, so "if-else" and "if else" give the same words.
     */
    private static List<String> tokenize(String query) {
        List<String> tokens = new ArrayList<>();
        if (query == null) return tokens;
        for (String part : query.toLowerCase(Locale.ROOT).split("[^a-z0-9_]+")) {
            if (!part.isEmpty()) tokens.add(part);
        }
        return tokens;
    }

    /**
     * Returns null when the parameter info cannot be read. Callers must not treat null as "no parameters".
     */
    private static List<String> safeParamTypes(String opCode) {
        try {
            List<String> types = lq.a(opCode);
            return types == null ? null : new ArrayList<>(types);
        } catch (Throwable t) {
            Log.w(TAG, "No param info for opcode " + opCode, t);
            return null;
        }
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}
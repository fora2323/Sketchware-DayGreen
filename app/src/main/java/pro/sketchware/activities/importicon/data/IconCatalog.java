package pro.sketchware.activities.importicon.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import pro.sketchware.utility.SketchwareUtil;

public class IconCatalog {

    private static final String TAG = "IconCatalog";
    private static final String METADATA_URL = "https://fonts.google.com/metadata/icons";
    private static final String CACHE_FILE_NAME = "material_symbols_catalog.json";
    private static final long CACHE_MAX_AGE_MS = 7L * 24L * 60L * 60L * 1000L; // 7 days

    public static class IconItem {
        public final String name;
        public final List<String> categories;
        public final List<String> tags;

        public IconItem(@NonNull String name, @NonNull List<String> categories, @NonNull List<String> tags) {
            this.name = name;
            this.categories = Collections.unmodifiableList(categories);
            this.tags = Collections.unmodifiableList(tags);
        }

        public boolean matches(@NonNull String lowerQuery) {
            if (name.toLowerCase(Locale.ROOT).contains(lowerQuery)) {
                return true;
            }
            for (String tag : tags) {
                if (tag.toLowerCase(Locale.ROOT).contains(lowerQuery)) {
                    return true;
                }
            }
            return false;
        }
    }

    public interface CatalogCallback {
        void onCatalogLoaded(@NonNull List<IconItem> items, boolean fromNetwork);

        void onCatalogError(@NonNull String errorMessage);
    }

    private final File cacheFile;
    private final OkHttpClient httpClient;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private volatile boolean isShutdown = false;

    public IconCatalog(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        this.cacheFile = new File(appContext.getCacheDir(), CACHE_FILE_NAME);
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .build();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public boolean hasCache() {
        return cacheFile.exists() && cacheFile.length() > 0;
    }

    public void loadCatalog(@NonNull CatalogCallback callback) {
        if (isShutdown) return;

        executor.execute(() -> {
            if (isShutdown) return;

            List<IconItem> cachedItems = readFromCache();
            boolean hasValidCache = !cachedItems.isEmpty();

            if (hasValidCache) {
                mainHandler.post(() -> {
                    if (!isShutdown) {
                        callback.onCatalogLoaded(cachedItems, false);
                    }
                });

                boolean isExpired = (System.currentTimeMillis() - cacheFile.lastModified()) > CACHE_MAX_AGE_MS;
                if (isExpired && SketchwareUtil.isConnected() && !isShutdown) {
                    try {
                        List<IconItem> freshItems = fetchAndCacheCatalog();
                        if (!freshItems.isEmpty() && !isShutdown) {
                            mainHandler.post(() -> {
                                if (!isShutdown) {
                                    callback.onCatalogLoaded(freshItems, true);
                                }
                            });
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Background catalog refresh failed, keeping cached data", e);
                    }
                }
                return;
            }

            if (!SketchwareUtil.isConnected()) {
                String msg = "No internet connection. Connect to the internet to load Material Symbols.";
                mainHandler.post(() -> {
                    if (!isShutdown) {
                        callback.onCatalogError(msg);
                    }
                });
                return;
            }

            try {
                List<IconItem> freshItems = fetchAndCacheCatalog();
                if (isShutdown) return;

                if (freshItems.isEmpty()) {
                    String msg = "Icon catalog is empty.";
                    mainHandler.post(() -> {
                        if (!isShutdown) {
                            callback.onCatalogError(msg);
                        }
                    });
                } else {
                    mainHandler.post(() -> {
                        if (!isShutdown) {
                            callback.onCatalogLoaded(freshItems, true);
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to load icon catalog", e);
                String msg = "Failed to load icon catalog: " + (e.getMessage() != null ? e.getMessage() : "Unknown error");
                mainHandler.post(() -> {
                    if (!isShutdown) {
                        callback.onCatalogError(msg);
                    }
                });
            }
        });
    }

    @NonNull
    private List<IconItem> readFromCache() {
        if (!cacheFile.exists() || cacheFile.length() == 0) {
            return Collections.emptyList();
        }
        try {
            String jsonText = Files.readString(cacheFile.toPath(), StandardCharsets.UTF_8);
            return parseCatalogJson(jsonText);
        } catch (Exception e) {
            Log.w(TAG, "Failed to read cached icon catalog, ignoring cache", e);
            return Collections.emptyList();
        }
    }

    @NonNull
    private List<IconItem> fetchAndCacheCatalog() throws IOException, JSONException {
        Request request = new Request.Builder()
                .url(METADATA_URL)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code());
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("Empty response from metadata server");
            }
            String rawResponse = body.string();
            String cleanedJson = stripXssiPrefix(rawResponse);
            List<IconItem> items = parseCatalogJson(cleanedJson);
            if (!items.isEmpty()) {
                saveToCache(items);
            }
            return items;
        }
    }

    @NonNull
    private String stripXssiPrefix(@NonNull String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith(")]}'")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline != -1) {
                return trimmed.substring(firstNewline + 1).trim();
            }
            return trimmed.substring(4).trim();
        }
        return trimmed;
    }

    private void saveToCache(@NonNull List<IconItem> items) {
        try {
            JSONObject root = new JSONObject();
            JSONArray iconsArray = new JSONArray();
            for (IconItem item : items) {
                JSONObject obj = new JSONObject();
                obj.put("name", item.name);
                obj.put("categories", new JSONArray(item.categories));
                obj.put("tags", new JSONArray(item.tags));
                iconsArray.put(obj);
            }
            root.put("icons", iconsArray);
            File parent = cacheFile.getParentFile();
            if (parent != null && !parent.exists()) {
                //noinspection ResultOfMethodCallIgnored
                parent.mkdirs();
            }
            Files.writeString(cacheFile.toPath(), root.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.w(TAG, "Failed to write icon catalog cache", e);
        }
    }

    @NonNull
    private List<IconItem> parseCatalogJson(@NonNull String jsonText) throws JSONException {
        JSONObject root = new JSONObject(jsonText);
        JSONArray iconsArray = root.optJSONArray("icons");
        if (iconsArray == null) {
            return Collections.emptyList();
        }

        List<IconItem> result = new ArrayList<>(iconsArray.length());
        for (int i = 0; i < iconsArray.length(); i++) {
            JSONObject iconObj = iconsArray.optJSONObject(i);
            if (iconObj == null) continue;

            String name = iconObj.optString("name", "").trim();
            if (name.isEmpty()) continue;

            List<String> categories = parseStringArray(iconObj.optJSONArray("categories"));
            List<String> tags = parseStringArray(iconObj.optJSONArray("tags"));
            result.add(new IconItem(name, categories, tags));
        }
        return result;
    }

    @NonNull
    private List<String> parseStringArray(JSONArray array) {
        if (array == null || array.length() == 0) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) {
            String val = array.optString(i, "").trim();
            if (!val.isEmpty()) {
                list.add(val);
            }
        }
        return list;
    }

    public void shutdown() {
        isShutdown = true;
        executor.shutdownNow();
    }
}
package pro.sketchware.activities.importicon.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import pro.sketchware.utility.FilePathUtil;
import pro.sketchware.utility.SketchwareUtil;

public class IconDownloader {

    private static final String TAG = "IconDownloader";
    private static final String SVG_URL_TEMPLATE = "https://cdn.jsdelivr.net/gh/google/material-design-icons@master/symbols/web/%s/materialsymbolsoutlined/%s_24px.svg";
    private static final String CACHE_DIR_NAME = "material_symbols_outlined";

    public interface DownloadCallback {
        void onSuccess(@NonNull File svgFile);

        void onError(@NonNull String errorMessage);
    }

    private final File cacheDir;
    private final OkHttpClient httpClient;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final Map<String, List<DownloadCallback>> inFlightCallbacks = new HashMap<>();
    private volatile boolean isShutdown = false;

    public IconDownloader(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        this.cacheDir = new File(appContext.getCacheDir(), CACHE_DIR_NAME);
        if (!this.cacheDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            this.cacheDir.mkdirs();
        }
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();
        this.executor = Executors.newFixedThreadPool(4);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    @NonNull
    public File getCachedSvgFile(@NonNull String iconName) {
        String safeName = sanitizeFileName(iconName);
        return new File(cacheDir, safeName + ".svg");
    }

    public boolean isCached(@NonNull String iconName) {
        File file = getCachedSvgFile(iconName);
        return file.exists() && file.length() > 0;
    }

    public void downloadSvgAsync(@NonNull String iconName, @NonNull DownloadCallback callback) {
        if (isShutdown) return;

        File cachedFile = getCachedSvgFile(iconName);
        if (cachedFile.exists() && cachedFile.length() > 0) {
            mainHandler.post(() -> callback.onSuccess(cachedFile));
            return;
        }

        boolean shouldStartTask = false;
        synchronized (inFlightCallbacks) {
            List<DownloadCallback> waiters = inFlightCallbacks.get(iconName);
            if (waiters != null) {
                waiters.add(callback);
            } else {
                waiters = new ArrayList<>();
                waiters.add(callback);
                inFlightCallbacks.put(iconName, waiters);
                shouldStartTask = true;
            }
        }

        if (!shouldStartTask || isShutdown) {
            return;
        }

        executor.execute(() -> {
            try {
                File downloaded = ensureCachedSvgSync(iconName);
                notifySuccess(iconName, downloaded);
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "Failed to download icon";
                notifyError(iconName, msg);
            }
        });
    }

    @NonNull
    public File ensureCachedSvgSync(@NonNull String iconName) throws IOException {
        File targetFile = getCachedSvgFile(iconName);
        if (targetFile.exists() && targetFile.length() > 0) {
            return targetFile;
        }

        if (!SketchwareUtil.isConnected()) {
            throw new IOException("No internet connection and icon is not cached.");
        }

        if (!cacheDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            cacheDir.mkdirs();
        }

        String url = String.format(Locale.ROOT, SVG_URL_TEMPLATE, iconName, iconName);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        File tempFile = new File(cacheDir, sanitizeFileName(iconName) + "_" + Thread.currentThread().getId() + ".tmp");
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + " while downloading " + iconName);
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("Empty SVG response for " + iconName);
            }

            try (InputStream in = body.byteStream();
                 FileOutputStream out = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            }

            if (tempFile.length() == 0) {
                throw new IOException("Downloaded SVG is empty for " + iconName);
            }

            Files.move(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return targetFile;
        } finally {
            if (tempFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                tempFile.delete();
            }
        }
    }

    public void saveResizedSvgToProjectAsync(
            @NonNull String sourceIconName,
            @NonNull String scId,
            @NonNull String outputResourceName,
            int sizePx,
            @NonNull DownloadCallback callback
    ) {
        if (isShutdown) return;

        executor.execute(() -> {
            try {
                File savedFile = saveResizedSvgToProject(sourceIconName, scId, outputResourceName, sizePx);
                mainHandler.post(() -> callback.onSuccess(savedFile));
            } catch (Exception e) {
                Log.e(TAG, "Failed to save SVG icon to project", e);
                String msg = e.getMessage() != null ? e.getMessage() : "Failed to save icon";
                mainHandler.post(() -> callback.onError(msg));
            }
        });
    }

    @NonNull
    public File saveResizedSvgToProject(
            @NonNull String sourceIconName,
            @NonNull String scId,
            @NonNull String outputResourceName,
            int sizePx
    ) throws Exception {
        File cachedSvg = ensureCachedSvgSync(sourceIconName);
        String rawSvg = Files.readString(cachedSvg.toPath(), StandardCharsets.UTF_8);
        String resizedSvg = resizeSvgDimensionsOnly(rawSvg, sizePx);

        FilePathUtil filePathUtil = new FilePathUtil();
        String outputPath = filePathUtil.getSvgFullPath(scId, outputResourceName);
        File outputFile = new File(outputPath);
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            parentDir.mkdirs();
        }

        Files.writeString(outputFile.toPath(), resizedSvg, StandardCharsets.UTF_8);
        return outputFile;
    }

    @NonNull
    private String resizeSvgDimensionsOnly(@NonNull String svgContent, int sizePx) throws Exception {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);

        XmlPullParser parser = factory.newPullParser();
        parser.setInput(new StringReader(svgContent));

        StringWriter writer = new StringWriter();
        XmlSerializer serializer = factory.newSerializer();
        serializer.setOutput(writer);
        serializer.startDocument("UTF-8", true);

        String sizeValue = sizePx + "px";
        boolean rootSvgHandled = false;

        while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
            int eventType = parser.getEventType();
            if (eventType == XmlPullParser.START_TAG) {
                String tagName = parser.getName();
                String namespace = parser.getNamespace();

                if ("svg".equals(tagName) && !rootSvgHandled) {
                    if (namespace != null && !namespace.isEmpty()) {
                        serializer.setPrefix("", namespace);
                    }
                    serializer.startTag(namespace, tagName);

                    boolean hasWidth = false;
                    boolean hasHeight = false;
                    for (int i = 0; i < parser.getAttributeCount(); i++) {
                        String attrNs = parser.getAttributeNamespace(i);
                        String attrName = parser.getAttributeName(i);
                        String attrVal = parser.getAttributeValue(i);
                        String targetNs = (attrNs == null || attrNs.isEmpty()) ? null : attrNs;

                        if ("width".equals(attrName)) {
                            serializer.attribute(targetNs, attrName, sizeValue);
                            hasWidth = true;
                        } else if ("height".equals(attrName)) {
                            serializer.attribute(targetNs, attrName, sizeValue);
                            hasHeight = true;
                        } else {
                            serializer.attribute(targetNs, attrName, attrVal);
                        }
                    }

                    if (!hasWidth) {
                        serializer.attribute(null, "width", sizeValue);
                    }
                    if (!hasHeight) {
                        serializer.attribute(null, "height", sizeValue);
                    }
                    rootSvgHandled = true;
                } else {
                    serializer.startTag(namespace, tagName);
                    for (int i = 0; i < parser.getAttributeCount(); i++) {
                        String attrNs = parser.getAttributeNamespace(i);
                        String attrName = parser.getAttributeName(i);
                        String attrVal = parser.getAttributeValue(i);
                        String targetNs = (attrNs == null || attrNs.isEmpty()) ? null : attrNs;
                        serializer.attribute(targetNs, attrName, attrVal);
                    }
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                serializer.endTag(parser.getNamespace(), parser.getName());
            } else if (eventType == XmlPullParser.TEXT) {
                serializer.text(parser.getText());
            }
            parser.next();
        }

        serializer.endDocument();
        return writer.toString();
    }

    private void notifySuccess(@NonNull String iconName, @NonNull File file) {
        List<DownloadCallback> waiters;
        synchronized (inFlightCallbacks) {
            waiters = inFlightCallbacks.remove(iconName);
        }
        if (waiters == null || waiters.isEmpty()) return;

        mainHandler.post(() -> {
            for (DownloadCallback cb : waiters) {
                try {
                    cb.onSuccess(file);
                } catch (Exception e) {
                    Log.w(TAG, "Error delivering icon download success callback", e);
                }
            }
        });
    }

    private void notifyError(@NonNull String iconName, @NonNull String errorMessage) {
        List<DownloadCallback> waiters;
        synchronized (inFlightCallbacks) {
            waiters = inFlightCallbacks.remove(iconName);
        }
        if (waiters == null || waiters.isEmpty()) return;

        mainHandler.post(() -> {
            for (DownloadCallback cb : waiters) {
                try {
                    cb.onError(errorMessage);
                } catch (Exception e) {
                    Log.w(TAG, "Error delivering icon download error callback", e);
                }
            }
        });
    }

    @NonNull
    private String sanitizeFileName(@Nullable String name) {
        if (name == null || name.isEmpty()) {
            return "icon";
        }
        return name.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    public void shutdown() {
        isShutdown = true;
        synchronized (inFlightCallbacks) {
            inFlightCallbacks.clear();
        }
        executor.shutdownNow();
    }
}
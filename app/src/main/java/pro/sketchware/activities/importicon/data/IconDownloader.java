package pro.sketchware.activities.importicon.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import pro.sketchware.utility.FilePathUtil;
import pro.sketchware.utility.SketchwareUtil;

public class IconDownloader {

    private static final String TAG = "IconDownloader";

    public static final String STYLE_OUTLINED = "outlined";
    public static final String STYLE_ROUNDED = "rounded";
    public static final String STYLE_SHARP = "sharp";

    public static final String FILL_DEFAULT = "default";
    public static final String FILL_FILLED = "fill1";

    public static final String DEFAULT_STYLE = STYLE_OUTLINED;
    public static final String DEFAULT_FILL_TYPE = FILL_DEFAULT;
    public static final int DEFAULT_OPTICAL_SIZE = 40;

    private static final String SVG_URL_TEMPLATE =
            "https://fonts.gstatic.com/s/i/short-term/release/materialsymbols%s/%s/%s/%dpx.svg";
    private static final String CACHE_DIR_NAME = "material_symbols";
    private static final String PREVIEW_CACHE_DIR_NAME = "material_symbols_preview";

    private static final Pattern SVG_OPEN_TAG_PATTERN =
            Pattern.compile("<svg\\b([^>]*?)(/?)>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern WIDTH_ATTR_PATTERN =
            Pattern.compile("(?i)((?:^|\\s)width\\s*=\\s*)(\"[^\"]*\"|'[^']*')");
    private static final Pattern HEIGHT_ATTR_PATTERN =
            Pattern.compile("(?i)((?:^|\\s)height\\s*=\\s*)(\"[^\"]*\"|'[^']*')");

    public interface DownloadCallback {
        void onSuccess(@NonNull File svgFile);

        void onError(@NonNull String errorMessage);
    }

    private final File baseCacheDir;
    private final File previewCacheDir;
    private final OkHttpClient httpClient;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final Map<String, List<DownloadCallback>> inFlightCallbacks = new HashMap<>();
    private volatile boolean isShutdown = false;

    public IconDownloader(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        this.baseCacheDir = new File(appContext.getCacheDir(), CACHE_DIR_NAME);
        if (!this.baseCacheDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            this.baseCacheDir.mkdirs();
        }
        this.previewCacheDir = new File(appContext.getCacheDir(), PREVIEW_CACHE_DIR_NAME);
        if (!this.previewCacheDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            this.previewCacheDir.mkdirs();
        }
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();
        this.executor = Executors.newFixedThreadPool(4);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    @NonNull
    public static String normalizeStyle(@Nullable String style) {
        if (STYLE_ROUNDED.equalsIgnoreCase(style)) {
            return STYLE_ROUNDED;
        }
        if (STYLE_SHARP.equalsIgnoreCase(style)) {
            return STYLE_SHARP;
        }
        return STYLE_OUTLINED;
    }

    @NonNull
    public static String normalizeFillType(@Nullable String fillType) {
        if (FILL_FILLED.equalsIgnoreCase(fillType)) {
            return FILL_FILLED;
        }
        return FILL_DEFAULT;
    }

    public static int normalizeOpticalSize(int size) {
        if (size == 20 || size == 24 || size == 40 || size == 48) {
            return size;
        }
        return DEFAULT_OPTICAL_SIZE;
    }

    @NonNull
    public File getCachedSvgFile(
            @NonNull String iconName,
            @NonNull String style,
            @NonNull String fillType,
            int opticalSize
    ) {
        String safeStyle = normalizeStyle(style);
        String safeFill = normalizeFillType(fillType);
        int safeOpsz = normalizeOpticalSize(opticalSize);
        String safeName = sanitizeFileName(iconName);

        File variantDir = new File(
                new File(new File(baseCacheDir, safeStyle), safeFill),
                String.valueOf(safeOpsz)
        );
        return new File(variantDir, safeName + ".svg");
    }

    public boolean isCached(
            @NonNull String iconName,
            @NonNull String style,
            @NonNull String fillType,
            int opticalSize
    ) {
        File file = getCachedSvgFile(iconName, style, fillType, opticalSize);
        return file.exists() && file.length() > 0;
    }

    @NonNull
    private String buildFlightKey(
            @NonNull String iconName,
            @NonNull String style,
            @NonNull String fillType,
            int opticalSize
    ) {
        return normalizeStyle(style)
                + "/" + normalizeFillType(fillType)
                + "/" + normalizeOpticalSize(opticalSize)
                + "/" + iconName;
    }

    public void downloadSvgAsync(
            @NonNull String iconName,
            @NonNull String style,
            @NonNull String fillType,
            int opticalSize,
            @NonNull DownloadCallback callback
    ) {
        if (isShutdown) return;

        String safeStyle = normalizeStyle(style);
        String safeFill = normalizeFillType(fillType);
        int safeOpsz = normalizeOpticalSize(opticalSize);

        File cachedFile = getCachedSvgFile(iconName, safeStyle, safeFill, safeOpsz);
        if (cachedFile.exists() && cachedFile.length() > 0) {
            mainHandler.post(() -> {
                if (!isShutdown) {
                    callback.onSuccess(cachedFile);
                }
            });
            return;
        }

        String flightKey = buildFlightKey(iconName, safeStyle, safeFill, safeOpsz);
        boolean shouldStartTask = false;
        synchronized (inFlightCallbacks) {
            List<DownloadCallback> waiters = inFlightCallbacks.get(flightKey);
            if (waiters != null) {
                waiters.add(callback);
            } else {
                waiters = new ArrayList<>();
                waiters.add(callback);
                inFlightCallbacks.put(flightKey, waiters);
                shouldStartTask = true;
            }
        }

        if (!shouldStartTask || isShutdown) {
            return;
        }

        executor.execute(() -> {
            try {
                File downloaded = ensureCachedSvgSync(iconName, safeStyle, safeFill, safeOpsz);
                notifySuccess(flightKey, downloaded);
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "Failed to download icon";
                notifyError(flightKey, msg);
            }
        });
    }

    @NonNull
    public File ensureCachedSvgSync(
            @NonNull String iconName,
            @NonNull String style,
            @NonNull String fillType,
            int opticalSize
    ) throws IOException {
        String safeStyle = normalizeStyle(style);
        String safeFill = normalizeFillType(fillType);
        int safeOpsz = normalizeOpticalSize(opticalSize);

        File targetFile = getCachedSvgFile(iconName, safeStyle, safeFill, safeOpsz);
        if (targetFile.exists() && targetFile.length() > 0) {
            return targetFile;
        }

        if (!SketchwareUtil.isConnected()) {
            throw new IOException("No internet connection and icon is not cached.");
        }

        File parentDir = targetFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            parentDir.mkdirs();
        }

        String url = String.format(Locale.ROOT, SVG_URL_TEMPLATE, safeStyle, iconName, safeFill, safeOpsz);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        File tempFile = new File(parentDir, sanitizeFileName(iconName) + "_" + Thread.currentThread().getId() + ".tmp");
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

    public void prepareResizedSvgPreviewAsync(
            @NonNull String sourceIconName,
            @NonNull String style,
            @NonNull String fillType,
            int opticalSize,
            int outputSizePx,
            @NonNull DownloadCallback callback
    ) {
        if (isShutdown) return;

        String safeStyle = normalizeStyle(style);
        String safeFill = normalizeFillType(fillType);
        int safeOpsz = normalizeOpticalSize(opticalSize);

        executor.execute(() -> {
            try {
                File cachedSvg = ensureCachedSvgSync(sourceIconName, safeStyle, safeFill, safeOpsz);
                String rawSvg = new String(Files.readAllBytes(cachedSvg.toPath()), StandardCharsets.UTF_8);
                String resizedSvg = resizeSvgDimensionsOnly(rawSvg, outputSizePx);

                if (!previewCacheDir.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    previewCacheDir.mkdirs();
                }

                String previewFileName = safeStyle + "_" + safeFill + "_" + safeOpsz
                        + "_" + sanitizeFileName(sourceIconName) + "_" + outputSizePx + "px.svg";
                File previewFile = new File(previewCacheDir, previewFileName);
                Files.write(previewFile.toPath(), resizedSvg.getBytes(StandardCharsets.UTF_8));

                mainHandler.post(() -> {
                    if (!isShutdown) {
                        callback.onSuccess(previewFile);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to prepare resized SVG preview", e);
                String msg = e.getMessage() != null ? e.getMessage() : "Failed to load icon preview";
                mainHandler.post(() -> {
                    if (!isShutdown) {
                        callback.onError(msg);
                    }
                });
            }
        });
    }

    public void saveResizedSvgToProjectAsync(
            @NonNull String sourceIconName,
            @NonNull String style,
            @NonNull String fillType,
            int opticalSize,
            @NonNull String scId,
            @NonNull String outputResourceName,
            int outputSizePx,
            @NonNull DownloadCallback callback
    ) {
        if (isShutdown) return;

        executor.execute(() -> {
            try {
                File savedFile = saveResizedSvgToProject(
                        sourceIconName,
                        style,
                        fillType,
                        opticalSize,
                        scId,
                        outputResourceName,
                        outputSizePx
                );
                mainHandler.post(() -> {
                    if (!isShutdown) {
                        callback.onSuccess(savedFile);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to save SVG icon to project", e);
                String msg = e.getMessage() != null ? e.getMessage() : "Failed to save icon";
                mainHandler.post(() -> {
                    if (!isShutdown) {
                        callback.onError(msg);
                    }
                });
            }
        });
    }

    @NonNull
    public File saveResizedSvgToProject(
            @NonNull String sourceIconName,
            @NonNull String style,
            @NonNull String fillType,
            int opticalSize,
            @NonNull String scId,
            @NonNull String outputResourceName,
            int outputSizePx
    ) throws IOException {
        File cachedSvg = ensureCachedSvgSync(sourceIconName, style, fillType, opticalSize);
        String rawSvg = new String(Files.readAllBytes(cachedSvg.toPath()), StandardCharsets.UTF_8);
        String resizedSvg = resizeSvgDimensionsOnly(rawSvg, outputSizePx);

        FilePathUtil filePathUtil = new FilePathUtil();
        String outputPath = filePathUtil.getSvgFullPath(scId, outputResourceName);
        File outputFile = new File(outputPath);
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            parentDir.mkdirs();
        }

        Files.write(outputFile.toPath(), resizedSvg.getBytes(StandardCharsets.UTF_8));
        return outputFile;
    }

    @NonNull
    public static String resizeSvgDimensionsOnly(@NonNull String svgContent, int sizePx) {
        Matcher svgMatcher = SVG_OPEN_TAG_PATTERN.matcher(svgContent);
        if (!svgMatcher.find()) {
            return svgContent;
        }

        String attrs = svgMatcher.group(1) != null ? svgMatcher.group(1) : "";
        String selfClose = svgMatcher.group(2) != null ? svgMatcher.group(2) : "";
        String sizeValue = sizePx + "px";

        Matcher widthMatcher = WIDTH_ATTR_PATTERN.matcher(attrs);
        if (widthMatcher.find()) {
            attrs = widthMatcher.replaceFirst(Matcher.quoteReplacement(widthMatcher.group(1) + "\"" + sizeValue + "\""));
        } else {
            attrs = attrs + " width=\"" + sizeValue + "\"";
        }

        Matcher heightMatcher = HEIGHT_ATTR_PATTERN.matcher(attrs);
        if (heightMatcher.find()) {
            attrs = heightMatcher.replaceFirst(Matcher.quoteReplacement(heightMatcher.group(1) + "\"" + sizeValue + "\""));
        } else {
            attrs = attrs + " height=\"" + sizeValue + "\"";
        }

        String replacementTag = "<svg" + attrs + selfClose + ">";
        StringBuffer sb = new StringBuffer();
        svgMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacementTag));
        svgMatcher.appendTail(sb);
        return sb.toString();
    }

    private void notifySuccess(@NonNull String flightKey, @NonNull File file) {
        List<DownloadCallback> waiters;
        synchronized (inFlightCallbacks) {
            waiters = inFlightCallbacks.remove(flightKey);
        }
        if (waiters == null || waiters.isEmpty()) return;

        mainHandler.post(() -> {
            if (isShutdown) return;
            for (DownloadCallback cb : waiters) {
                try {
                    cb.onSuccess(file);
                } catch (Exception e) {
                    Log.w(TAG, "Error delivering icon download success callback", e);
                }
            }
        });
    }

    private void notifyError(@NonNull String flightKey, @NonNull String errorMessage) {
        List<DownloadCallback> waiters;
        synchronized (inFlightCallbacks) {
            waiters = inFlightCallbacks.remove(flightKey);
        }
        if (waiters == null || waiters.isEmpty()) return;

        mainHandler.post(() -> {
            if (isShutdown) return;
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
    private static String sanitizeFileName(@Nullable String name) {
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
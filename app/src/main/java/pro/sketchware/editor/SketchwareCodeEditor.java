package pro.sketchware.editor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import com.besome.sketch.design.DesignActivity;
import com.bobur.androidsvg.SVG;

import pro.sketchware.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import a.a.a.wq;
import io.github.rosemoe.sora.lang.analysis.StyleUpdateRange;
import io.github.rosemoe.sora.lang.styling.Styles;
import io.github.rosemoe.sora.lang.styling.line.LineSideIcon;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.widget.CodeEditor;
import mod.bobur.VectorDrawableParser;
import pro.sketchware.activities.resourceseditor.components.utils.ColorsEditorManager;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.ThemeUtils;
import org.sketchware.daygreen.git.GitConfiguration;
import org.sketchware.daygreen.git.GitManager;

import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.HistogramDiff;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import java.nio.charset.StandardCharsets;
import android.util.Log;

public class SketchwareCodeEditor extends CodeEditor {

    private static final Pattern RESOURCE_PATTERN = Pattern.compile(
            "#([0-9a-fA-F]{3,8})"
                    + "|@(?:android:)?color/([a-zA-Z0-9_]+)"
                    + "|R\\.color\\.([a-zA-Z0-9_]+)"
                    + "|0x([0-9a-fA-F]{6,8})"
                    + "|\\b(?:android\\.graphics\\.)?Color\\.(BLACK|BLUE|CYAN|DKGRAY|GRAY|GREEN|LTGRAY|MAGENTA|RED|TRANSPARENT|WHITE|YELLOW)\\b"
                    + "|@(?:android:)?drawable/([a-zA-Z0-9_]+)"
                    + "|R\\.drawable\\.([a-zA-Z0-9_]+)"
                    + "|@(?:android:)?mipmap/([a-zA-Z0-9_]+)"
                    + "|R\\.mipmap\\.([a-zA-Z0-9_]+)"
    );

    private ColorsEditorManager colorsManager;
    private String currentScId;
    private final Map<String, Drawable> iconCache = new HashMap<>();
    private boolean cachedGutterActive;
    private boolean cachedConflictEnabled;

    public SketchwareCodeEditor(Context context) {
        super(context);
    }

    public SketchwareCodeEditor(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SketchwareCodeEditor(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setScId(String scId) {
        if (scId != null && !scId.equals(currentScId)) {
            this.currentScId = scId;
            DesignActivity.sc_id = scId;
            colorsManager = new ColorsEditorManager();
            rerunAnalysis();
        }
    }

    private pro.sketchware.editor.search.EditorSearchPanel searchPanel;

    public void setSearchPanel(pro.sketchware.editor.search.EditorSearchPanel searchPanel) {
        this.searchPanel = searchPanel;
        if (searchPanel != null) {
            searchPanel.attachToEditor(this);
        }
    }

    public pro.sketchware.editor.search.EditorSearchPanel getSearchPanel() {
        return searchPanel;
    }

    @Override
    public void beginSearchMode() {
        if (searchPanel != null) {
            searchPanel.show(true);
        } else {
            super.beginSearchMode();
        }
    }

    public String getScId() {
        return this.currentScId != null ? this.currentScId : DesignActivity.sc_id;
    }

    public boolean isGitRepoActive() {
        String scId = getScId();
        return scId != null && !scId.isEmpty() && GitManager.hasGitRepo(scId);
    }

    public boolean isGutterIndicationActive() {
        return GitConfiguration.isGutterIndicationEnabled(getContext()) && isGitRepoActive();
    }

    @Override
    public void setStyles(@Nullable Styles styles) {
        if (styles != null) {
            processStyles(styles, null);
        }
        super.setStyles(styles);
    }

    @Override
    public void updateStyles(@Nullable Styles styles, @Nullable StyleUpdateRange range) {
        if (styles != null) {
            processStyles(styles, range);
        }
        super.updateStyles(styles, range);
    }

    public enum LineChangeType {
        NONE, ADDED, MODIFIED, DELETED
    }

    private String baseText = null;
    private final Map<Integer, LineChangeType> currentLineChanges = new HashMap<>();

    public void setBaseText(String text) {
        this.baseText = text;
        recomputeLineChanges();
    }

    @Override
    public void setText(CharSequence text) {
        super.setText(text);
        if (text != null && (baseText == null || baseText.isEmpty())) {
            this.baseText = text.toString();
        }
    }

    private void recomputeLineChanges() {
        currentLineChanges.clear();
        if (baseText == null || baseText.isEmpty()) return;
        String currentText = getText().toString();

        try {
            byte[] baseBytes = baseText.getBytes(StandardCharsets.UTF_8);
            byte[] currentBytes = currentText.getBytes(StandardCharsets.UTF_8);

            RawText baseRaw = new RawText(baseBytes);
            RawText currentRaw = new RawText(currentBytes);

            HistogramDiff diff = new HistogramDiff();
            EditList edits = diff.diff(RawTextComparator.DEFAULT, baseRaw, currentRaw);

            for (Edit edit : edits) {
                switch (edit.getType()) {
                    case INSERT:
                        for (int line = edit.getBeginB(); line < edit.getEndB(); line++) {
                            currentLineChanges.put(line, LineChangeType.ADDED);
                        }
                        break;
                    case REPLACE:
                        for (int line = edit.getBeginB(); line < edit.getEndB(); line++) {
                            currentLineChanges.put(line, LineChangeType.MODIFIED);
                        }
                        break;
                    case DELETE:
                        int delLine = edit.getBeginB();
                        if (delLine < currentRaw.size()) {
                            if (!currentLineChanges.containsKey(delLine)) {
                                currentLineChanges.put(delLine, LineChangeType.DELETED);
                            }
                        } else if (delLine - 1 >= 0) {
                            if (!currentLineChanges.containsKey(delLine - 1)) {
                                currentLineChanges.put(delLine - 1, LineChangeType.DELETED);
                            }
                        }
                        break;
                }
            }
        } catch (Exception e) {
            Log.w("SketchwareCodeEditor", "Failed to compute diff for gutter indication", e);
        }
    }

    private static class GutterIndicatorDrawable extends Drawable {
        private final LineChangeType type;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path trianglePath = new Path();
        private final float density;

        public GutterIndicatorDrawable(LineChangeType type, int color, float density) {
            this.type = type;
            this.density = density;
            this.paint.setColor(color);
            this.paint.setStyle(Paint.Style.FILL);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            Rect bounds = getBounds();
            if (bounds.isEmpty()) return;

            float width = bounds.width();
            float height = bounds.height();
            float left = bounds.left;
            float top = bounds.top;

            if (type == LineChangeType.DELETED) {
                // Android Studio deleted marker: right-pointing triangle / wedge (▶)
                // located in the gutter pointing towards the editor content.
                trianglePath.reset();
                float arrowWidth = Math.max(5f * density, width * 0.5f);
                float arrowHeight = Math.max(7f * density, height * 0.5f);
                float centerY = top + height / 2f;
                float halfH = arrowHeight / 2f;

                trianglePath.moveTo(left, centerY - halfH);
                trianglePath.lineTo(left + arrowWidth, centerY);
                trianglePath.lineTo(left, centerY + halfH);
                trianglePath.close();

                canvas.drawPath(trianglePath, paint);
            } else {
                // Android Studio Added (Green) / Modified (Blue) marker:
                // Sleek vertical strip along the gutter line with rounded corners.
                float barWidth = Math.max(3.5f * density, width * 0.3f);
                float cornerRadius = 1.5f * density;
                RectF barRect = new RectF(left, top, left + barWidth, top + height);
                canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, paint);
            }
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

        @Override
        public int getIntrinsicWidth() {
            return (int) (12 * density);
        }

        @Override
        public int getIntrinsicHeight() {
            return (int) (20 * density);
        }
    }

    private Drawable getGutterIndicator(LineChangeType type) {
        boolean isDark = ThemeUtils.isDarkThemeEnabled(getContext());
        String key = "gutter_line_" + type.name() + (isDark ? "_dark" : "_light");
        Drawable d = iconCache.get(key);
        if (d == null) {
            float density = getResources().getDisplayMetrics().density;
            int color;
            switch (type) {
                case ADDED:
                    // Android Studio Green: #62B543 in Dark / #499C54 in Light
                    color = isDark ? Color.parseColor("#62B543") : Color.parseColor("#499C54");
                    break;
                case MODIFIED:
                    // Android Studio Blue: #389FD6 in Dark / #1D84C6 in Light
                    color = isDark ? Color.parseColor("#389FD6") : Color.parseColor("#1D84C6");
                    break;
                case DELETED:
                    // Android Studio Red: #ED5565 in Dark / #E53935 in Light
                    color = isDark ? Color.parseColor("#ED5565") : Color.parseColor("#E53935");
                    break;
                default:
                    color = Color.TRANSPARENT;
                    break;
            }

            d = new GutterIndicatorDrawable(type, color, density);
            iconCache.put(key, d);
        }
        return d;
    }

    private void processStyles(Styles styles, @Nullable StyleUpdateRange range) {
        getProps().sideIconSizeFactor = 0.9f;

        cachedGutterActive = isGutterIndicationActive();
        cachedConflictEnabled = GitConfiguration.isConflictDetectionEnabled(getContext());

        if (cachedGutterActive) {
            recomputeLineChanges();
        } else {
            currentLineChanges.clear();
        }

        // The Styles object here can be concurrently touched by sora-editor's own
        // background analyzer thread while we're adding/removing our custom
        // color/drawable preview icons on it. When that race happens, the internal
        // list sora-editor iterates over throws ConcurrentModificationException.
        // We don't own that internal list, so the safe fix is to skip this pass
        // gracefully instead of crashing — the next style update (fired on every
        // keystroke/re-analysis) will simply redraw the icons correctly.
        try {
            Content textContent = getText();
            int lineCount = textContent.getLineCount();

            if (range == null) {
                styles.eraseAllLineStyles();
                for (int i = 0; i < lineCount; i++) {
                    processLine(i, textContent, styles);
                }
            } else {
                var iterator = range.lineIndexIterator(lineCount);
                while (iterator.hasNext()) {
                    int lineIndex = iterator.nextInt();
                    if (lineIndex >= 0 && lineIndex < textContent.getLineCount()) {
                        styles.eraseLineStyle(lineIndex, LineSideIcon.class);
                        processLine(lineIndex, textContent, styles);
                    }
                }
            }

            styles.finishBuilding();
        } catch (java.util.ConcurrentModificationException ignored) {
            // Styles was mutated concurrently by the analyzer thread — safe to skip.
        }
    }

    private boolean processLine(int line, Content text, Styles styles) {
        if (line < 0 || line >= text.getLineCount()) return false;

        String lineText = text.getLineString(line);

        boolean iconAdded = false;

        if (cachedConflictEnabled) {
            if (lineText.startsWith("<<<<<<<") || lineText.startsWith("=======") || lineText.startsWith(">>>>>>>")) {
                styles.addLineStyle(new LineSideIcon(line, getIconForConflict(lineText)));
                iconAdded = true;
            }
        }

        if (!iconAdded && cachedGutterActive && !currentLineChanges.isEmpty()) {
            LineChangeType changeType = currentLineChanges.get(line);
            if (changeType != null && changeType != LineChangeType.NONE) {
                styles.addLineStyle(new LineSideIcon(line, getGutterIndicator(changeType)));
                iconAdded = true;
            }
        }

        Matcher matcher = RESOURCE_PATTERN.matcher(lineText);

        while (matcher.find()) {
            String match = matcher.group();

            int color = resolveColor(match);
            if (color != 0) {
                if (!iconAdded) {
                    styles.addLineStyle(new LineSideIcon(line, getIconForColor(color)));
                    iconAdded = true;
                }
            } else {
                Drawable drawable = resolveDrawable(match);
                if (drawable != null && !iconAdded) {
                    styles.addLineStyle(new LineSideIcon(line, drawable));
                    iconAdded = true;
                }
            }
        }

        return iconAdded;
    }

    private Drawable getIconForConflict(String lineText) {
        String key = "conflict_" + (lineText.length() >= 7 ? lineText.substring(0, 7) : lineText);
        Drawable d = iconCache.get(key);
        if (d == null) {
            float density = getResources().getDisplayMetrics().density;
            int size = (int) (18 * density);
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

            if (lineText.startsWith("<<<<<<<")) {
                p.setColor(Color.RED);
            } else if (lineText.startsWith("=======")) {
                p.setColor(Color.YELLOW);
            } else {
                p.setColor(Color.GREEN);
            }
            float radius = 4 * density;
            canvas.drawRoundRect(new RectF(0, 0, size, size), radius, radius, p);

            d = new BitmapDrawable(getResources(), bitmap);
            iconCache.put(key, d);
        }
        return d;
    }

    private Drawable getIconForColor(int color) {
        String key = "color_" + color;
        Drawable d = iconCache.get(key);
        if (d == null) {
            float density = getResources().getDisplayMetrics().density;
            int size = (int) (18 * density);
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

            p.setColor(color);
            float radius = 4 * density;
            canvas.drawRoundRect(new RectF(0, 0, size, size), radius, radius, p);

            // Calculate darkness manually since ColorPreviewRenderer is deleted
            double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
            if (darkness < 0.5) {
                p.setStyle(Paint.Style.STROKE);
                p.setColor(0x33000000);
                p.setStrokeWidth(1 * density);
                canvas.drawRoundRect(
                        new RectF(0.5f * density, 0.5f * density,
                                size - 0.5f * density, size - 0.5f * density),
                        radius, radius, p);
            }

            d = new BitmapDrawable(getResources(), bitmap);
            iconCache.put(key, d);
        }
        return d;
    }

    private Drawable resolveDrawable(String match) {
        String name = null;
        boolean isSystem = false;
        boolean isMipmap = false;

        if (match.startsWith("@drawable/")) {
            name = match.substring(10);
        } else if (match.startsWith("@android:drawable/")) {
            name = match.substring(18);
            isSystem = true;
        } else if (match.startsWith("R.drawable.")) {
            name = match.substring(11);
        } else if (match.startsWith("@mipmap/")) {
            name = match.substring(8);
            isMipmap = true;
        } else if (match.startsWith("@android:mipmap/")) {
            name = match.substring(16);
            isSystem = true;
            isMipmap = true;
        } else if (match.startsWith("R.mipmap.")) {
            name = match.substring(9);
            isMipmap = true;
        }

        if (name == null) return null;

        String cacheKey = (isSystem ? "sys_" : (isMipmap ? "mip_" : "res_")) + name;
        Drawable cached = iconCache.get(cacheKey);
        if (cached != null) return cached;

        if (currentScId != null && !isSystem) {
            try {
                List<String> vectorPaths = new ArrayList<>();
                vectorPaths.add(wq.b(currentScId) + "/files/resource/drawable/" + name + ".xml");
                vectorPaths.add(wq.b(currentScId) + "/converted-vectors/" + name + ".xml");
                vectorPaths.add(wq.b(currentScId) + "/converted-vectors/" + name + ".svg");

                for (String vectorPath : vectorPaths) {
                    if (FileUtil.isExistFile(vectorPath)) {
                        try {
                            String content = FileUtil.readFile(vectorPath);
                            SVG svgObj;

                            if (content.contains("<vector")) {
                                VectorDrawableParser parser = new VectorDrawableParser(content);
                                svgObj = SVG.getFromString(parser.toSvg());
                            } else if (content.contains("<svg") || content.contains(":svg")) {
                                svgObj = SVG.getFromString(content);
                            } else {
                                continue;
                            }

                            float density = getResources().getDisplayMetrics().density;
                            int size = (int) (48 * density);
                            svgObj.setDocumentWidth(size);
                            svgObj.setDocumentHeight(size);

                            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                            Canvas canvas = new Canvas(bitmap);

                            svgObj.renderToCanvas(canvas);

                            Drawable d = new BitmapDrawable(getResources(), bitmap);
                            iconCache.put(cacheKey, d);
                            return d;
                        } catch (Exception ignored) {}
                    }
                }

                List<String> rasterPaths = new ArrayList<>();
                String[] densities = {"xhdpi", "hdpi", "mdpi", "xxhdpi", "xxxhdpi", ""};

                if (isMipmap) {
                    rasterPaths.add(FileUtil.getExternalStorageDir()
                            + "/.sketchware/resources/icons/" + currentScId
                            + "/mipmaps/mipmap-xhdpi/" + name + ".png");
                    for (String dStr : densities) {
                        String suffix = dStr.isEmpty() ? "" : "-" + dStr;
                        rasterPaths.add(wq.b(currentScId)
                                + "/files/resource/mipmap" + suffix + "/" + name + ".png");
                    }
                } else {
                    for (String dStr : densities) {
                        String suffix = dStr.isEmpty() ? "" : "-" + dStr;
                        rasterPaths.add(wq.b(currentScId)
                                + "/files/resource/drawable" + suffix + "/" + name + ".png");
                    }
                }

                for (String pathStr : rasterPaths) {
                    String finalPath = pathStr;
                    if (!FileUtil.isExistFile(finalPath)) {
                        finalPath = finalPath.replace(".png", ".jpg");
                    }

                    if (FileUtil.isExistFile(finalPath)) {
                        Bitmap b = BitmapFactory.decodeFile(finalPath);
                        if (b != null) {
                            Drawable d = new BitmapDrawable(getResources(), b);
                            iconCache.put(cacheKey, d);
                            return d;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        try {
            String type = isMipmap ? "mipmap" : "drawable";
            int resId = getContext().getResources().getIdentifier(
                    name, type, isSystem ? "android" : getContext().getPackageName());
            if (resId != 0) {
                Drawable d = AppCompatResources.getDrawable(getContext(), resId);
                if (d != null) {
                    iconCache.put(cacheKey, d);
                    return d;
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    private int resolveColor(String colorStr) {
        try {
            if (colorStr.startsWith("#")) {
                String hex = colorStr;
                if (hex.length() == 4) {
                    hex = "#" + hex.charAt(1) + hex.charAt(1)
                            + hex.charAt(2) + hex.charAt(2)
                            + hex.charAt(3) + hex.charAt(3);
                }
                return Color.parseColor(hex);
            }

            if (colorStr.startsWith("0x")) {
                return (int) Long.parseLong(colorStr.substring(2), 16);
            }

            if (colorStr.startsWith("R.color.")) {
                return resolveXmlColor("@color/" + colorStr.substring(8));
            }

            if (colorStr.contains("Color.")) {
                String constant = colorStr.substring(colorStr.lastIndexOf(".") + 1);
                return resolveColorConstant(constant);
            }

            return resolveXmlColor(colorStr);
        } catch (Exception ignored) {}
        return 0;
    }

    private int resolveXmlColor(String colorStr) {
        if (colorStr.startsWith("#")) {
            try {
                String hex = colorStr;
                if (hex.length() == 4) {
                    hex = "#" + hex.charAt(1) + hex.charAt(1)
                            + hex.charAt(2) + hex.charAt(2)
                            + hex.charAt(3) + hex.charAt(3);
                }
                return Color.parseColor(hex);
            } catch (Exception ignored) {}
        }

        if (colorsManager != null && colorStr.startsWith("@color/")) {
            boolean isNight = ThemeUtils.isDarkThemeEnabled(getContext());
            String hex = colorsManager.getColorValue(getContext(), colorStr, 4, isNight);
            if (hex != null && !hex.equals("#00000000")) {
                return Color.parseColor(hex);
            }
        }
        return 0;
    }

    private int resolveColorConstant(String constant) {
        return switch (constant) {
            case "BLACK" -> Color.BLACK;
            case "BLUE" -> Color.BLUE;
            case "CYAN" -> Color.CYAN;
            case "DKGRAY" -> Color.DKGRAY;
            case "GRAY" -> Color.GRAY;
            case "GREEN" -> Color.GREEN;
            case "LTGRAY" -> Color.LTGRAY;
            case "MAGENTA" -> Color.MAGENTA;
            case "RED" -> Color.RED;
            case "TRANSPARENT" -> Color.TRANSPARENT;
            case "WHITE" -> Color.WHITE;
            case "YELLOW" -> Color.YELLOW;
            default -> 0;
        };
    }
}
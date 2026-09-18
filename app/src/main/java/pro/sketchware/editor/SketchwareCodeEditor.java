package pro.sketchware.editor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import com.besome.sketch.design.DesignActivity;
import com.bobur.androidsvg.SVG;

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

    public String getScId() {
        return this.currentScId;
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

    private void processStyles(Styles styles, @Nullable StyleUpdateRange range) {
        getProps().sideIconSizeFactor = 0.9f;

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
        Matcher matcher = RESOURCE_PATTERN.matcher(lineText);

        boolean iconAdded = false;

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
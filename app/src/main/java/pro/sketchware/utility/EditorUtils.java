package pro.sketchware.utility;

import static pro.sketchware.utility.ThemeUtils.isDarkThemeEnabled;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;

import com.google.android.material.color.MaterialColors;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.rosemoe.sora.lang.Language;
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme;
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;
import io.github.rosemoe.sora.widget.schemes.SchemeDarcula;
import io.github.rosemoe.sora.widget.schemes.SchemeGitHub;

import mod.jbk.code.CodeEditorColorSchemes;
import mod.jbk.code.CodeEditorLanguages;
import pro.sketchware.R;
import pro.sketchware.editor.SketchwareCodeEditor;

public class EditorUtils {
    EditorUtils() {
    }

    @NonNull
    public static EditorColorScheme getMaterialStyledScheme(CodeEditor editor) {
        return getMaterialStyledScheme(editor, true);
    }

    @NonNull
    public static EditorColorScheme getMaterialStyledScheme(CodeEditor editor, boolean fullOverride) {
        var scheme = editor.getColorScheme();
        var primary = MaterialColors.getColor(editor, R.attr.colorPrimary);
        var surface = MaterialColors.getColor(editor, R.attr.colorSurface);
        var surfaceContainer = MaterialColors.getColor(editor, R.attr.colorSurfaceContainer);
        var surfaceContainerLow = MaterialColors.getColor(editor, R.attr.colorSurfaceContainerLow);
        var surfaceContainerHighest = MaterialColors.getColor(editor, R.attr.colorSurfaceContainerHighest);
        var onSurface = MaterialColors.getColor(editor, R.attr.colorOnSurface);
        var onSurfaceVariant = MaterialColors.getColor(editor, R.attr.colorOnSurfaceVariant);

        if (fullOverride) {
            scheme.setColor(EditorColorScheme.WHOLE_BACKGROUND, surface);
            if (!(scheme instanceof TextMateColorScheme)) {
                scheme.setColor(EditorColorScheme.TEXT_NORMAL, onSurface);
            }
            scheme.setColor(EditorColorScheme.CURRENT_LINE, surfaceContainerLow);
            scheme.setColor(EditorColorScheme.LINE_NUMBER_PANEL, surfaceContainer);
            scheme.setColor(EditorColorScheme.LINE_NUMBER_BACKGROUND, surfaceContainer);
        }

        scheme.setColor(EditorColorScheme.LINE_DIVIDER, surfaceContainerHighest);
        scheme.setColor(EditorColorScheme.SELECTION_INSERT, onSurfaceVariant);
        scheme.setColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_FOREGROUND, onSurface);
        scheme.setColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_BACKGROUND, surfaceContainerHighest);

        scheme.setColor(EditorColorScheme.COMPLETION_WND_BACKGROUND, surfaceContainer);
        scheme.setColor(EditorColorScheme.COMPLETION_WND_TEXT_PRIMARY, onSurface);
        scheme.setColor(EditorColorScheme.COMPLETION_WND_TEXT_SECONDARY, onSurfaceVariant);
        scheme.setColor(EditorColorScheme.COMPLETION_WND_CORNER, primary);

        scheme.setColor(EditorColorScheme.TEXT_ACTION_WINDOW_BACKGROUND, surfaceContainerHighest);
        scheme.setColor(EditorColorScheme.TEXT_ACTION_WINDOW_ICON_COLOR, onSurface);
        scheme.setColor(EditorColorScheme.SELECTION_HANDLE, primary);
        scheme.setColor(EditorColorScheme.SELECTED_TEXT_BACKGROUND, ColorUtils.setAlphaComponent(primary, 0x55));

        return scheme;
    }

    @NonNull
    public static Typeface getTypeface(Context context) {
        return Typeface.createFromAsset(context.getAssets(), "fonts/jetbrainsmono-regular.ttf");
    }

    public static void loadJavaConfig(CodeEditor editor) {
        Language language = CodeEditorLanguages.loadTextMateLanguage(CodeEditorLanguages.SCOPE_NAME_JAVA);
        if (language instanceof TextMateLanguage tm) {
            ArrayList<String> keywords = new ArrayList<>();
            if (editor instanceof SketchwareCodeEditor) {
                String scId = ((SketchwareCodeEditor) editor).getScId();
                keywords.addAll(getDynamicKeywords(scId, false));
            }
            keywords.addAll(extractDocumentWords(editor));
            tm.setCompleterKeywords(keywords.toArray(new String[0]));
        }
        loadConfigByLanguage(editor, language, true);
    }

    public static void loadKotlinConfig(CodeEditor editor) {
        Language language = CodeEditorLanguages.loadTextMateLanguage(CodeEditorLanguages.SCOPE_NAME_KOTLIN);
        if (language instanceof TextMateLanguage tm) {
            ArrayList<String> keywords = new ArrayList<>();
            if (editor instanceof SketchwareCodeEditor) {
                String scId = ((SketchwareCodeEditor) editor).getScId();
                keywords.addAll(getDynamicKeywords(scId, false));
            }
            keywords.addAll(extractDocumentWords(editor));
            tm.setCompleterKeywords(keywords.toArray(new String[0]));
        }
        loadConfigByLanguage(editor, language, true);
    }

    public static void loadXmlConfig(CodeEditor editor) {
        loadXmlConfig(editor, null);
    }

    public static void loadXmlConfig(CodeEditor editor, String pathOrFileName) {
        Language language = CodeEditorLanguages.loadTextMateLanguage(CodeEditorLanguages.SCOPE_NAME_XML);
        if (language instanceof TextMateLanguage tm) {
            ArrayList<String> keywords = new ArrayList<>();
            if (editor instanceof SketchwareCodeEditor) {
                String scId = ((SketchwareCodeEditor) editor).getScId();
                keywords.addAll(getDynamicKeywords(scId, true));
            }
            keywords.addAll(extractDocumentWords(editor));
            tm.setCompleterKeywords(keywords.toArray(new String[0]));
        }
        loadConfigByLanguage(editor, language, true);
    }

    public static void loadHtmlConfig(CodeEditor editor) {
        Language language = CodeEditorLanguages.loadTextMateLanguage(CodeEditorLanguages.SCOPE_NAME_HTML);
        if (language instanceof TextMateLanguage tm) {
            tm.setCompleterKeywords(extractDocumentWords(editor).toArray(new String[0]));
        }
        loadConfigByLanguage(editor, language, true);
    }

    public static void loadCssConfig(CodeEditor editor) {
        Language language = CodeEditorLanguages.loadTextMateLanguage(CodeEditorLanguages.SCOPE_NAME_CSS);
        if (language instanceof TextMateLanguage tm) {
            tm.setCompleterKeywords(extractDocumentWords(editor).toArray(new String[0]));
        }
        loadConfigByLanguage(editor, language, true);
    }

    public static void loadJsConfig(CodeEditor editor) {
        Language language = CodeEditorLanguages.loadTextMateLanguage(CodeEditorLanguages.SCOPE_NAME_JS);
        if (language instanceof TextMateLanguage tm) {
            tm.setCompleterKeywords(extractDocumentWords(editor).toArray(new String[0]));
        }
        loadConfigByLanguage(editor, language, true);
    }

    public static void loadCppConfig(CodeEditor editor) {
        Language language = CodeEditorLanguages.loadTextMateLanguage(CodeEditorLanguages.SCOPE_NAME_CPP);
        if (language instanceof TextMateLanguage tm) {
            tm.setCompleterKeywords(extractDocumentWords(editor).toArray(new String[0]));
        }
        loadConfigByLanguage(editor, language, true);
    }

    public static void loadCConfig(CodeEditor editor) {
        Language language = CodeEditorLanguages.loadTextMateLanguage(CodeEditorLanguages.SCOPE_NAME_C);
        if (language instanceof TextMateLanguage tm) {
            tm.setCompleterKeywords(extractDocumentWords(editor).toArray(new String[0]));
        }
        loadConfigByLanguage(editor, language, true);
    }

    private static ArrayList<String> extractDocumentWords(CodeEditor editor) {
        ArrayList<String> result = new ArrayList<>();
        try {
            if (editor != null && editor.getText() != null) {
                String content = editor.getText().toString();
                Matcher m = Pattern.compile("\\b[a-zA-Z_][a-zA-Z0-9_]{2,}\\b").matcher(content);
                while (m.find()) {
                    String word = m.group();
                    if (!result.contains(word)) {
                        result.add(word);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("EditorUtils", "Failed to extract document words", e);
        }
        return result;
    }

    private static ArrayList<String> getDynamicKeywords(String scId, boolean isXml) {
        ArrayList<String> keywords = new ArrayList<>();
        if (scId == null || scId.isEmpty()) return keywords;

        String basePath = FileUtil.getExternalStorageDir() + "/.sketchware/data/" + scId + "/files/resource";

        processResourceNames(keywords, basePath + "/values/colors.xml", "color", isXml);
        processResourceNames(keywords, basePath + "/values/strings.xml", "string", isXml);
        processResourceNames(keywords, basePath + "/values/dimens.xml", "dimen", isXml);
        processResourceNames(keywords, basePath + "/values/styles.xml", "style", isXml);

        processFileNames(keywords, basePath + "/drawable/", "drawable", isXml);
        processFileNames(keywords, basePath + "/layout/", "layout", isXml);

        String layoutDir = basePath + "/layout/";
        ArrayList<String> layoutFiles = new ArrayList<>();
        FileUtil.listDir(layoutDir, layoutFiles);
        for (String lf : layoutFiles) {
            if (lf.endsWith(".xml")) {
                try {
                    String fileContent = FileUtil.readFile(lf);
                    Matcher m = Pattern.compile("android:id\\s*=\\s*\"@\\+id/([^\"]+)\"").matcher(fileContent);
                    while (m.find()) {
                        String id = m.group(1);
                        if (isXml) {
                            String format1 = "@id/" + id;
                            String format2 = "@+id/" + id;
                            if (!keywords.contains(format1)) keywords.add(format1);
                            if (!keywords.contains(format2)) keywords.add(format2);
                        } else {
                            addJavaRes(keywords, "id", id);
                        }
                    }
                } catch (Exception e) {
                    Log.e("EditorUtils", "Failed to parse layout ids from " + lf, e);
                }
            }
        }
        return keywords;
    }

    private static void addJavaRes(ArrayList<String> list, String type, String name) {
        String key1 = "R." + type + "." + name;
        String key2 = type + "." + name;
        if (!list.contains(key1)) list.add(key1);
        if (!list.contains(key2)) list.add(key2);
        if (!list.contains(name)) list.add(name);
    }

    private static void processResourceNames(ArrayList<String> list, String filePath, String type, boolean isXml) {
        if (!FileUtil.isExistFile(filePath)) return;
        try {
            String fileContent = FileUtil.readFile(filePath);
            Matcher m = Pattern.compile("<" + type + "[^>]+name\\s*=\\s*\"([^\"]+)\"").matcher(fileContent);
            while (m.find()) {
                String name = m.group(1);
                if (isXml) {
                    String format = "@" + type + "/" + name;
                    if (!list.contains(format)) list.add(format);
                } else {
                    addJavaRes(list, type, name);
                }
            }
        } catch (Exception e) {
            Log.e("EditorUtils", "Failed to parse " + filePath, e);
        }
    }

    private static void processFileNames(ArrayList<String> list, String dir, String type, boolean isXml) {
        ArrayList<String> files = new ArrayList<>();
        FileUtil.listDir(dir, files);
        for (String f : files) {
            String name = new File(f).getName();
            int dot = name.lastIndexOf('.');
            if (dot > 0) name = name.substring(0, dot);
            if (isXml) {
                String format = "@" + type + "/" + name;
                if (!list.contains(format)) list.add(format);
            } else {
                addJavaRes(list, type, name);
            }
        }
    }

    private static void loadConfigByLanguage(CodeEditor editor, Language language, boolean isTextMate) {
        editor.setEditorLanguage(language);
        boolean isDark = isDarkThemeEnabled(editor.getContext());

        if (isTextMate) {
            String theme = isDark ? CodeEditorColorSchemes.THEME_GITHUB_DARK : CodeEditorColorSchemes.THEME_GITHUB_LIGHT;
            editor.setColorScheme(CodeEditorColorSchemes.loadTextMateColorScheme(theme));
        } else {
            editor.setColorScheme(isDark ? new SchemeDarcula() : new SchemeGitHub());
        }

        getMaterialStyledScheme(editor, true);
        editor.setPinLineNumber(true);
        editor.rerunAnalysis();
    }
}
package pro.sketchware.utility;

import static pro.sketchware.utility.ThemeUtils.isDarkThemeEnabled;

import android.content.Context;
import android.graphics.Typeface;

import androidx.annotation.NonNull;

import com.google.android.material.color.MaterialColors;

import extensions.fora2323.daygreen.keyword.IDCssKeyword;
import extensions.fora2323.daygreen.keyword.IDHtmlKeyword;
import extensions.fora2323.daygreen.keyword.IDJavaKeyword;
import extensions.fora2323.daygreen.keyword.IDJsKeyword;
import extensions.fora2323.daygreen.keyword.IDKotlinKeyword;
import extensions.fora2323.daygreen.keyword.IDXmlKeyword;

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
            // Only override normal text color for non-TextMate schemes
            // to avoid breaking syntax highlighting colors
            if (!(scheme instanceof TextMateColorScheme)) {
                scheme.setColor(EditorColorScheme.TEXT_NORMAL, onSurface);
            }
            scheme.setColor(EditorColorScheme.CURRENT_LINE, surfaceContainerLow);
            scheme.setColor(EditorColorScheme.LINE_NUMBER_PANEL, surfaceContainer);
            scheme.setColor(EditorColorScheme.LINE_NUMBER_BACKGROUND, surfaceContainer);
        }

        // Always style these to maintain IDE feel
        scheme.setColor(EditorColorScheme.LINE_DIVIDER, surfaceContainerHighest);
        scheme.setColor(EditorColorScheme.SELECTION_INSERT, onSurfaceVariant);
        scheme.setColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_FOREGROUND, onSurface);
        scheme.setColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_BACKGROUND, surfaceContainerHighest);

        // Completion window styling - matches IDE panels
        scheme.setColor(EditorColorScheme.COMPLETION_WND_BACKGROUND, surfaceContainer);
        scheme.setColor(EditorColorScheme.COMPLETION_WND_TEXT_PRIMARY, onSurface);
        scheme.setColor(EditorColorScheme.COMPLETION_WND_TEXT_SECONDARY, onSurfaceVariant);
        scheme.setColor(EditorColorScheme.COMPLETION_WND_CORNER, primary);

        return scheme;
    }

    @NonNull
    public static Typeface getTypeface(Context context) {
        return Typeface.createFromAsset(context.getAssets(), "fonts/jetbrainsmono-regular.ttf");
    }

    public static void loadJavaConfig(CodeEditor editor) {
        Language language = CodeEditorLanguages.loadTextMateLanguage(CodeEditorLanguages.SCOPE_NAME_JAVA);
        if (language instanceof TextMateLanguage tm) {
            tm.setCompleterKeywords(IDJavaKeyword.KEYWORDS);
        }
        loadConfigByLanguage(editor, language, true);
    }

    public static void loadKotlinConfig(CodeEditor editor) {
        Language language = CodeEditorLanguages.loadTextMateLanguage(CodeEditorLanguages.SCOPE_NAME_KOTLIN);
        if (language instanceof TextMateLanguage) {
            ((TextMateLanguage) language).setCompleterKeywords(IDKotlinKeyword.KEYWORDS);
        }
        loadConfigByLanguage(editor, language, true);
    }

    /** Kompatibilitas lama: pakai keyword gabungan (semua kategori). */
    public static void loadXmlConfig(CodeEditor editor) {
        loadXmlConfig(editor, null);
    }

    /**
     * @param pathOrFileName path lengkap atau nama file yang lagi dibuka,
     *                        dipakai buat milih keyword autocomplete yang sesuai
     *                        (layout / values-colors / values-strings / styles / attrs / manifest / drawable).
     *                        Boleh null kalau gak tahu jenis filenya (bakal pakai keyword gabungan).
     */
    public static void loadXmlConfig(CodeEditor editor, String pathOrFileName) {
        Language language = CodeEditorLanguages.loadTextMateLanguage(CodeEditorLanguages.SCOPE_NAME_XML);
        if (language instanceof TextMateLanguage tm) {
            tm.setCompleterKeywords(IDXmlKeyword.forFile(pathOrFileName));
        }
        loadConfigByLanguage(editor, language, true);
    }

    public static void loadHtmlConfig(CodeEditor editor) {
        Language language = CodeEditorLanguages.loadTextMateLanguage(CodeEditorLanguages.SCOPE_NAME_HTML);
        if (language instanceof TextMateLanguage tm) {
            tm.setCompleterKeywords(IDHtmlKeyword.KEYWORDS);
        }
        loadConfigByLanguage(editor, language, true);
    }

    public static void loadCssConfig(CodeEditor editor) {
        Language language = CodeEditorLanguages.loadTextMateLanguage(CodeEditorLanguages.SCOPE_NAME_CSS);
        if (language instanceof TextMateLanguage tm) {
            tm.setCompleterKeywords(IDCssKeyword.KEYWORDS);
        }
        loadConfigByLanguage(editor, language, true);
    }

    public static void loadJsConfig(CodeEditor editor) {
        Language language = CodeEditorLanguages.loadTextMateLanguage(CodeEditorLanguages.SCOPE_NAME_JS);
        if (language instanceof TextMateLanguage tm) {
            tm.setCompleterKeywords(IDJsKeyword.KEYWORDS);
        }
        loadConfigByLanguage(editor, language, true);
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
package mod.hey.studios.code;

import static pro.sketchware.utility.GsonUtils.getGson;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.content.res.AppCompatResources;

import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import a.a.a.Lx;
import io.github.rosemoe.sora.lang.Language;
import io.github.rosemoe.sora.langs.java.JavaLanguage;
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme;
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;
import io.github.rosemoe.sora.widget.schemes.SchemeDarcula;
import io.github.rosemoe.sora.widget.schemes.SchemeEclipse;
import io.github.rosemoe.sora.widget.schemes.SchemeGitHub;
import io.github.rosemoe.sora.widget.schemes.SchemeNotepadXX;
import io.github.rosemoe.sora.widget.schemes.SchemeVS2019;
import mod.hey.studios.util.Helper;
import mod.jbk.code.CodeEditorColorSchemes;
import mod.jbk.code.CodeEditorLanguages;
import pro.sketchware.R;
import pro.sketchware.activities.preview.LayoutPreviewActivity;
import pro.sketchware.databinding.CodeEditorHsBinding;
import pro.sketchware.utility.EditorUtils;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;
import pro.sketchware.utility.ThemeUtils;
import pro.sketchware.utility.UI;

public class SrcCodeEditor extends BaseAppCompatActivity {
    public static final String FLAG_FROM_ANDROID_MANIFEST = "from_android_manifest";
    public static final List<Pair<String, Class<? extends EditorColorScheme>>> KNOWN_COLOR_SCHEMES = List.of(
            new Pair<>("Default", EditorColorScheme.class),
            new Pair<>("GitHub", SchemeGitHub.class),
            new Pair<>("Eclipse", SchemeEclipse.class),
            new Pair<>("Darcula", SchemeDarcula.class),
            new Pair<>("VS2019", SchemeVS2019.class),
            new Pair<>("NotepadXX", SchemeNotepadXX.class)
    );
    public static SharedPreferences pref;
    public static int languageId;
    private String beforeContent = "";
    private CodeEditorHsBinding binding;
    private boolean fromAndroidManifest;
    private String scId;
    private String activityName;

    public static void loadCESettings(Context c, CodeEditor ed, String prefix) {
        loadCESettings(c, ed, prefix, false);
    }

    public static void loadCESettings(Context c, CodeEditor ed, String prefix, boolean loadTheme) {
        pref = c.getSharedPreferences("hsce", Activity.MODE_PRIVATE);

        int text_size = pref.getInt(prefix + "_ts", 12);
        int theme = pref.getInt(prefix + "_theme", 3);
        boolean word_wrap = pref.getBoolean(prefix + "_ww", false);
        boolean auto_c = pref.getBoolean(prefix + "_ac", true);
        boolean auto_complete_symbol_pairs = pref.getBoolean(prefix + "_acsp", true);

        if (loadTheme) selectTheme(ed, theme);
        ed.setTextSize(text_size);
        ed.setWordwrap(word_wrap);
        ed.getProps().symbolPairAutoCompletion = auto_complete_symbol_pairs;
        ed.getComponent(EditorAutoCompletion.class).setEnabled(auto_c);
    }

    public static void selectTheme(CodeEditor ed, int which) {
        boolean isTextMate = ed.getColorScheme() instanceof TextMateColorScheme;
        boolean isDark = ThemeUtils.isDarkThemeEnabled(ed.getContext());

        if (isTextMate) {
            if (which == 0) {
                // Default Dynamic logic
                Language language = ed.getEditorLanguage();
                String scopeName = (language instanceof TextMateLanguage tm && tm.getAutoCompleter().getKeywords() != null) ?
                        CodeEditorLanguages.SCOPE_NAME_XML : CodeEditorLanguages.SCOPE_NAME_KOTLIN;

                String theme;
                if (scopeName.equals(CodeEditorLanguages.SCOPE_NAME_XML)) {
                    theme = isDark ? CodeEditorColorSchemes.THEME_GITHUB_DARK : CodeEditorColorSchemes.THEME_GITHUB;
                } else {
                    theme = isDark ? CodeEditorColorSchemes.THEME_DRACULA : CodeEditorColorSchemes.THEME_GITHUB;
                }
                ed.setColorScheme(CodeEditorColorSchemes.loadTextMateColorScheme(theme));
                EditorUtils.getMaterialStyledScheme(ed, true);
            } else {
                String themeName = switch (which) {
                    case 1 -> CodeEditorColorSchemes.THEME_DRACULA;
                    case 2 -> CodeEditorColorSchemes.THEME_GITHUB;
                    case 3 -> CodeEditorColorSchemes.THEME_GITHUB_DARK;
                    case 4 -> CodeEditorColorSchemes.THEME_ECLIPSE;
                    case 5 -> CodeEditorColorSchemes.THEME_VS2019;
                    case 6 -> CodeEditorColorSchemes.THEME_NOTEPADXX;
                    default -> CodeEditorColorSchemes.THEME_DRACULA;
                };
                ed.setColorScheme(CodeEditorColorSchemes.loadTextMateColorScheme(themeName));
                EditorUtils.getMaterialStyledScheme(ed, false);
            }
        } else {
            EditorColorScheme scheme = switch (which) {
                case 1 -> new SchemeGitHub();
                case 2 -> new SchemeEclipse();
                case 3 -> new SchemeDarcula();
                case 4 -> new SchemeVS2019();
                case 5 -> new SchemeNotepadXX();
                default -> isDark ? new SchemeDarcula() : new EditorColorScheme();
            };
            ed.setColorScheme(scheme);
            EditorUtils.getMaterialStyledScheme(ed, which == 0);
        }
        ed.rerunAnalysis();
    }

    public static void selectLanguage(CodeEditor ed, int which) {
        switch (which) {
            default:
            case 0:
                ed.setEditorLanguage(new JavaLanguage());
                languageId = 0;
                break;

            case 1:
                ed.setEditorLanguage(CodeEditorLanguages.loadTextMateLanguage(CodeEditorLanguages.SCOPE_NAME_KOTLIN));
                languageId = 1;
                break;

            case 2:
                ed.setEditorLanguage(CodeEditorLanguages.loadTextMateLanguage(CodeEditorLanguages.SCOPE_NAME_XML));
                languageId = 2;
                break;
        }

    }

    public static String prettifyXml(String xml, int indentAmount, Intent extras) {
        if (xml == null || xml.trim().isEmpty()) return xml;

        try {
            boolean omitXmlDecl = extras != null && extras.hasExtra("disableHeader");
            String indentUnit = " ".repeat(Math.max(1, indentAmount));

            StringBuilder formatted = new StringBuilder();
            int depth = 0;

            // We'll use a simple state to track if we just opened a tag and might
            // want to keep the content on the same line.
            boolean justOpenedTag = false;

            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("<[^>]+>", java.util.regex.Pattern.DOTALL)
                    .matcher(xml);

            int lastEnd = 0;
            while (m.find()) {
                String between = xml.substring(lastEnd, m.start()).trim();
                String rawTag = m.group();
                lastEnd = m.end();

                String tag = rawTag.replaceAll("\\s+", " ")
                        .replace("< ", "<")
                        .replace(" >", ">")
                        .replace(" />", " />")
                        .trim();

                boolean isDeclaration = tag.startsWith("<?");
                boolean isComment = tag.startsWith("<!--");
                boolean isClosing = tag.startsWith("</");
                boolean isSelfClosing = !isDeclaration && tag.endsWith("/>");

                if (isClosing) depth = Math.max(0, depth - 1);

                if (!between.isEmpty()) {
                    if (justOpenedTag) {
                        // Content immediately following an opening tag.
                        // For simple text values (no newlines in 'between'),
                        // don't add a newline/indent yet.
                        if (!between.contains("\n")) {
                            formatted.append(between);
                        } else {
                            formatted.append("\n").append(indentUnit.repeat(depth + 1)).append(between).append("\n");
                        }
                    } else {
                        formatted.append(indentUnit.repeat(depth)).append(between).append("\n");
                    }
                }

                if (isDeclaration) {
                    if (!omitXmlDecl) {
                        formatted.append(tag).append("\n");
                    }
                    justOpenedTag = false;
                } else if (isComment) {
                    formatted.append(indentUnit.repeat(depth)).append(tag).append("\n");
                    justOpenedTag = false;
                } else if (isClosing) {
                    if (justOpenedTag && !between.contains("\n")) {
                        // If we just opened this tag and the content was simple,
                        // append the closing tag on the same line.
                        formatted.append(tag).append("\n");
                    } else {
                        formatted.append(indentUnit.repeat(depth)).append(tag).append("\n");
                    }
                    justOpenedTag = false;
                } else {
                    // Opening tag
                    if (justOpenedTag) {
                        formatted.append("\n");
                    }
                    appendTagWithWrapping(formatted, tag, isSelfClosing, depth, indentUnit, indentAmount);
                    
                    if (isSelfClosing) {
                        justOpenedTag = false;
                    } else {
                        justOpenedTag = true;
                        depth++;
                    }
                }
            }

            return formatted.toString().trim();

        } catch (Exception e) {
            return null;
        }
    }

    // Max characters allowed on one line before an opening tag's attributes get
    // wrapped onto separate lines, matching Android Studio's default right margin.
    private static final int PRETTIFY_MAX_LINE_LENGTH = 100;

    private static void appendTagWithWrapping(StringBuilder formatted, String tag,
                                                boolean isSelfClosing, int depth,
                                                String indentUnit, int indentAmount) {
        String inner = isSelfClosing
                ? tag.substring(1, tag.length() - 2).trim()
                : tag.substring(1, tag.length() - 1).trim();

        int firstSpace = inner.indexOf(' ');
        String tagName = firstSpace == -1 ? inner : inner.substring(0, firstSpace);
        String attrPart = firstSpace == -1 ? "" : inner.substring(firstSpace + 1).trim();

        java.util.List<String> attrs = new java.util.ArrayList<>();
        if (!attrPart.isEmpty()) {
            java.util.regex.Matcher am = java.util.regex.Pattern
                    .compile("[^\\s=]+=\"[^\"]*\"")
                    .matcher(attrPart);
            while (am.find()) attrs.add(am.group());
        }

        int singleLineLength = indentAmount * depth + tag.length();

        formatted.append(indentUnit.repeat(depth));
        if (attrs.size() <= 1 || singleLineLength <= PRETTIFY_MAX_LINE_LENGTH) {
            formatted.append(tag);
            // If it's self-closing or a declaration/comment, it gets a newline later.
            // Opening tags MIGHT get a newline depending on 'justOpenedTag' logic in prettifyXml.
            if (isSelfClosing) formatted.append("\n");
            return;
        }

        formatted.append("<").append(tagName).append("\n");
        for (int i = 0; i < attrs.size(); i++) {
            formatted.append(indentUnit.repeat(depth + 1)).append(attrs.get(i));
            if (i == attrs.size() - 1) {
                formatted.append(isSelfClosing ? " />\n" : ">");
            } else {
                formatted.append("\n");
            }
        }
    }

    /**
     * Adds a specified amount of tabs.
     */
    public static void a(StringBuilder code, int tabAmount) {
        for (int i = 0; i < tabAmount; ++i) {
            code.append('\t');
        }
    }

    public static void showSwitchThemeDialog(Activity activity, CodeEditor codeEditor, DialogInterface.OnClickListener listener) {
        String[] themeItems;
        int selectedThemeIndex = -1;

        if (codeEditor.getColorScheme() instanceof TextMateColorScheme) {
            themeItems = new String[]{"Default (Dynamic)", "Dracula", "GitHub Light", "GitHub Dark", "Eclipse", "VS2019", "NotepadXX"};
        } else {
            EditorColorScheme currentScheme = codeEditor.getColorScheme();
            var knownColorSchemesProperlyOrdered = new ArrayList<>(KNOWN_COLOR_SCHEMES);
            Collections.reverse(knownColorSchemesProperlyOrdered);
            selectedThemeIndex = knownColorSchemesProperlyOrdered.stream()
                    .filter(pair -> pair.second.equals(currentScheme.getClass()))
                    .map(KNOWN_COLOR_SCHEMES::indexOf)
                    .findFirst()
                    .orElse(-1);
            
            // Add Default to items if not there
            List<String> items = new ArrayList<>();
            items.add("Default (Dynamic)");
            items.addAll(KNOWN_COLOR_SCHEMES.stream().map(pair -> pair.first).toList());
            themeItems = items.toArray(new String[0]);
            
            if (selectedThemeIndex != -1) selectedThemeIndex++; // Shift for Default
        }

        new MaterialAlertDialogBuilder(activity)
                .setTitle("Select Theme")
                .setSingleChoiceItems(themeItems, selectedThemeIndex, listener)
                .setNegativeButton(R.string.common_word_cancel, null)
                .show();
    }

    public static void showSwitchLanguageDialog(Activity activity, CodeEditor codeEditor, DialogInterface.OnClickListener listener) {
        CharSequence[] languagesList = {
                "Java",
                "Kotlin",
                "XML"
        };

        new MaterialAlertDialogBuilder(activity)
                .setTitle("Select Language")
                .setSingleChoiceItems(languagesList, languageId, listener)
                .setNegativeButton(R.string.common_word_cancel, null)
                .show();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        enableEdgeToEdgeNoContrast();
        super.onCreate(savedInstanceState);

        binding = CodeEditorHsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        fromAndroidManifest = getIntent().getBooleanExtra(FLAG_FROM_ANDROID_MANIFEST, false);
        String title = getIntent().getStringExtra("title");
        scId = getIntent().getStringExtra("sc_id");
        activityName = getIntent().getStringExtra("activity_name");

        binding.editor.setScId(scId);
        binding.editor.setTypefaceText(EditorUtils.getTypeface(this));
        binding.editor.setTextSize(16);

        if (fromAndroidManifest) {
            String filePath = FileUtil.getExternalStorageDir() + "/.sketchware/data/" + scId + "/Injection/androidmanifest/activities_components.json";
            if (FileUtil.isExistFile(filePath)) {
                ArrayList<HashMap<String, Object>> arrayList = getGson()
                        .fromJson(FileUtil.readFile(filePath), Helper.TYPE_MAP_LIST);
                for (int i = 0; i < arrayList.size(); i++) {
                    if (arrayList.get(i).get("name").equals(activityName)) {
                        beforeContent = (String) arrayList.get(i).get("value");
                    }
                }
            }
        }

        if (!fromAndroidManifest)
            beforeContent = FileUtil.readFile(getIntent().getStringExtra("content"));
        binding.editor.setText(beforeContent);

        if (title.endsWith(".java")) {
            binding.editor.setEditorLanguage(new JavaLanguage());
            languageId = 0;
        } else if (title.endsWith(".kt")) {
            binding.editor.setEditorLanguage(CodeEditorLanguages.loadTextMateLanguage(CodeEditorLanguages.SCOPE_NAME_KOTLIN));
            if (ThemeUtils.isDarkThemeEnabled(getApplicationContext())) {
                binding.editor.setColorScheme(CodeEditorColorSchemes.loadTextMateColorScheme(CodeEditorColorSchemes.THEME_GITHUB_DARK));
            } else {
                binding.editor.setColorScheme(CodeEditorColorSchemes.loadTextMateColorScheme(CodeEditorColorSchemes.THEME_GITHUB));
            }
            languageId = 1;
        } else if (title.endsWith(".xml")) {
            EditorUtils.loadXmlConfig(binding.editor);
            languageId = 2;
        }

        loadCESettings(this, binding.editor, "act", true);
        loadToolbar();

        UI.addSystemWindowInsetToPadding(binding.appBarLayout, true, true, true, false);
        UI.addSystemWindowInsetToMargin(binding.editor, true, false, true, true);
    }

    public void save() {
        beforeContent = binding.editor.getText().toString();

        if (fromAndroidManifest) {
            String filePath = FileUtil.getExternalStorageDir() + "/.sketchware/data/" + scId + "/Injection/androidmanifest/activities_components.json";
            if (FileUtil.isExistFile(filePath)) {
                ArrayList<HashMap<String, Object>> activitiesComponents = getGson()
                        .fromJson(FileUtil.readFile(filePath), Helper.TYPE_MAP_LIST);
                for (int i = 0; i < activitiesComponents.size(); i++) {
                    if (activitiesComponents.get(i).get("name").equals(activityName)) {
                        activitiesComponents.get(i).put("value", beforeContent);
                        FileUtil.writeFile(filePath, getGson().toJson(activitiesComponents));
                        SketchwareUtil.toast("Saved");
                        return;
                    }
                }
                HashMap<String, Object> map = new HashMap<>();
                map.put("name", activityName);
                map.put("value", beforeContent);
                activitiesComponents.add(map);
                FileUtil.writeFile(filePath, getGson().toJson(activitiesComponents));
            } else {
                ArrayList<HashMap<String, Object>> arrayList = new ArrayList<>();
                HashMap<String, Object> map = new HashMap<>();
                map.put("name", activityName);
                map.put("value", beforeContent);
                arrayList.add(map);
                FileUtil.writeFile(filePath, getGson().toJson(arrayList));
            }
        } else FileUtil.writeFile(getIntent().getStringExtra("content"), beforeContent);

        SketchwareUtil.toast("Saved");
    }

    @Override
    public void onBackPressed() {
        if (beforeContent.equals(binding.editor.getText().toString())) {
            super.onBackPressed();
        } else {
            MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
            dialog.setIcon(R.drawable.ic_warning_96dp);
            dialog.setTitle(Helper.getResString(R.string.common_word_warning));
            dialog.setMessage(Helper.getResString(R.string.src_code_editor_unsaved_changes_dialog_warning_message));

            dialog.setPositiveButton(Helper.getResString(R.string.common_word_exit), (v, which) -> {
                v.dismiss();
                finish();
            });
            dialog.setNegativeButton(Helper.getResString(R.string.common_word_cancel), null);
            dialog.show();
        }
    }

    private void loadToolbar() {
        {
            String title = getIntent().getStringExtra("title");
            binding.toolbar.setTitle(title);
            SharedPreferences local_pref = getSharedPreferences("hsce", Activity.MODE_PRIVATE);
            Menu toolbarMenu = binding.toolbar.getMenu();
            toolbarMenu.clear();
            toolbarMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, "Undo").setIcon(AppCompatResources.getDrawable(this, R.drawable.ic_mtrl_undo)).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            toolbarMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, "Redo").setIcon(AppCompatResources.getDrawable(this, R.drawable.ic_mtrl_redo)).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            toolbarMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, "Save").setIcon(AppCompatResources.getDrawable(this, R.drawable.ic_mtrl_save)).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            if (isFileInLayoutFolder() && getIntent().hasExtra("sc_id")) {
                toolbarMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, "Layout Preview");
            }
            toolbarMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, "Find & Replace");
            toolbarMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, "Word wrap").setCheckable(true).setChecked(local_pref.getBoolean("act_ww", false));
            toolbarMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, "Pretty print");
            toolbarMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, "Select language");
            toolbarMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, "Select theme");
            toolbarMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, "Auto complete").setCheckable(true).setChecked(local_pref.getBoolean("act_ac", true));
            toolbarMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, "Auto complete symbol pair").setCheckable(true).setChecked(local_pref.getBoolean("act_acsp", true));

            binding.toolbar.setOnMenuItemClickListener(item -> {
                String title1 = item.getTitle().toString();
                switch (title1) {
                    case "Undo":
                        binding.editor.undo();
                        break;

                    case "Redo":
                        binding.editor.redo();
                        break;

                    case "Save":
                        save();
                        break;

                    case "Pretty print":
                        if (getIntent().hasExtra("java")) {
                            StringBuilder b = new StringBuilder();

                            for (String line : binding.editor.getText().toString().split("\n")) {
                                String trims = (line + "X").trim();
                                trims = trims.substring(0, trims.length() - 1);

                                b.append(trims);
                                b.append("\n");
                            }

                            boolean err = false;
                            String ss = b.toString();

                            try {
                                ss = Lx.j(ss, true);
                            } catch (Exception e) {
                                err = true;
                                SketchwareUtil.toastError("Your code contains incorrectly nested parentheses");
                            }

                            if (!err) binding.editor.setText(ss);

                        } else if (getIntent().hasExtra("xml")) {
                            String format = prettifyXml(binding.editor.getText().toString(), 4, getIntent());

                            if (format != null) {
                                binding.editor.setText(format);
                            } else {
                                SketchwareUtil.toastError("Failed to format XML file", Toast.LENGTH_LONG);
                            }
                        } else {
                            SketchwareUtil.toast("Only Java and XML files can be formatted");
                        }
                        break;

                    case "Select language":
                        showSwitchLanguageDialog(this, binding.editor, (dialog, which) -> {
                            selectLanguage(binding.editor, which);
                            dialog.dismiss();
                        });
                        break;

                    case "Find & Replace":
                        binding.editor.getSearcher().stopSearch();
                        binding.editor.beginSearchMode();
                        break;

                    case "Select theme":
                        showSwitchThemeDialog(this, binding.editor, (dialog, which) -> {
                            selectTheme(binding.editor, which);
                            pref.edit().putInt("act_theme", which).apply();
                            dialog.dismiss();
                        });
                        break;

                    case "Word wrap":
                        item.setChecked(!item.isChecked());
                        binding.editor.setWordwrap(item.isChecked());

                        pref.edit().putBoolean("act_ww", item.isChecked()).apply();
                        break;

                    case "Auto complete symbol pair":
                        item.setChecked(!item.isChecked());
                        binding.editor.getProps().symbolPairAutoCompletion = item.isChecked();

                        pref.edit().putBoolean("act_acsp", item.isChecked()).apply();
                        break;

                    case "Auto complete":
                        item.setChecked(!item.isChecked());

                        binding.editor.getComponent(EditorAutoCompletion.class).setEnabled(item.isChecked());
                        pref.edit().putBoolean("act_ac", item.isChecked()).apply();
                        break;

                    case "Layout Preview":
                        toLayoutPreview();
                        break;

                    default:
                        return false;
                }
                return true;
            });
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        float scaledDensity = getResources().getDisplayMetrics().scaledDensity;
        pref.edit().putInt("act_ts", (int) (binding.editor.getTextSizePx() / scaledDensity)).apply();
    }

    private boolean isFileInLayoutFolder() {
        String content = getIntent().getStringExtra("content");
        if (content != null) {
            File file = new File(content);
            if (content.contains("/resource/layout/")) {
                String layoutFolder = file.getParent();
                return layoutFolder != null && layoutFolder.endsWith("/resource/layout");
            }
        }
        return false;
    }

    private void toLayoutPreview() {
        Intent intent = new Intent(getApplicationContext(), LayoutPreviewActivity.class);
        intent.putExtras(getIntent());
        intent.putExtra("xml", binding.editor.getText().toString());
        startActivity(intent);
    }
}
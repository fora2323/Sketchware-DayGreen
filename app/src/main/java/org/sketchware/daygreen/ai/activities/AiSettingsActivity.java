package org.sketchware.daygreen.ai.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;

import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.besome.sketch.editor.manage.library.LibraryCategoryView;
import com.besome.sketch.editor.manage.library.LibraryItemView;
import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.sketchware.daygreen.ai.AiConnectionTester;
import org.sketchware.daygreen.ai.AiSettingsStore;
import pro.sketchware.databinding.ActivityAiSettingsBinding;
import pro.sketchware.utility.SketchwareUtil;

public class AiSettingsActivity extends BaseAppCompatActivity {

    private static final class Provider {
        final String name;
        final String[] models;

        Provider(String name, String... models) {
            this.name = name;
            this.models = models;
        }
    }

    private static final class Tool {
        final String name;
        final String category;
        final String policy;

        Tool(String name, String category, String policy) {
            this.name = name;
            this.category = category;
            this.policy = policy;
        }
    }

    private static final Provider[] PROVIDERS = {
            new Provider("OpenAI", "gpt-4o", "gpt-4o-mini", "gpt-4.1", "gpt-4.1-mini"),
            new Provider("Anthropic (Claude)", "claude-sonnet-4-5", "claude-haiku-4-5", "claude-opus-4-1"),
            new Provider("Google (Gemini)", "gemini-2.5-pro", "gemini-2.5-flash"),
            new Provider("DeepSeek", "deepseek-chat", "deepseek-reasoner"),
            new Provider("OpenRouter", "openrouter/auto"),
            new Provider("Moonshot (Kimi)", "kimi-k2-0711-preview")
    };

    private static final Tool[] TOOLS = {
            new Tool("file_info", "Read", "runs freely"),
            new Tool("read_file", "Read", "runs freely"),
            new Tool("write_file", "Write", "asks first"),
            new Tool("edit_file", "Write", "asks first"),
            new Tool("list_dir", "Read", "runs freely"),
            new Tool("search", "Read", "runs freely"),
            new Tool("make_dir", "Write", "asks first"),
            new Tool("copy_path", "Write", "asks first"),
            new Tool("move_path", "Write", "asks first"),
            new Tool("delete_path", "Write", "asks first"),
            new Tool("find_files", "Read", "runs freely"),
            new Tool("run_android_shell", "Shell", "asks first"),
            new Tool("run_ubuntu", "Shell", "asks first"),
            new Tool("http_get", "Network", "runs freely"),
            new Tool("http_request", "Network", "asks first"),
            new Tool("http_download", "Write", "asks first"),
            new Tool("http_upload", "Network", "asks first"),
            new Tool("save_memory", "Write", "runs freely"),
            new Tool("forget_memory", "Write", "runs freely"),
            new Tool("scratchpad", "Write", "runs freely"),
            new Tool("spawn_agent", "Read", "runs freely"),
            new Tool("ask_user", "Read", "runs freely"),
            new Tool("set_goal", "Read", "runs freely"),
            new Tool("write_todos", "Read", "runs freely")
    };

    private static final String DEFAULT_SYSTEM_PROMPT =
            "You are an AI assistant for Sketchware DayGreen. Be concise and precise.";

    private static final int COLOR_OK = 0xFF4CAF50;
    private static final int COLOR_ERROR = 0xFFE53935;
    private static final int COLOR_NEUTRAL = 0xFF9E9E9E;

    private ActivityAiSettingsBinding binding;
    private AiSettingsStore store;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private int providerIndex;
    private int modelIndex;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        enableEdgeToEdgeNoContrast();
        super.onCreate(savedInstanceState);
        binding = ActivityAiSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        {
            View view = binding.main;
            int left = view.getPaddingLeft();
            int top = view.getPaddingTop();
            int right = view.getPaddingRight();
            int bottom = view.getPaddingBottom();

            ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
                Insets systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
                v.setPadding(left + systemInsets.left, top, right + systemInsets.right, bottom);
                return insets;
            });
        }

        {
            View view = binding.contentScroll;
            int left = view.getPaddingLeft();
            int top = view.getPaddingTop();
            int right = view.getPaddingRight();
            int bottom = view.getPaddingBottom();

            ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
                Insets navBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(left, top, right, bottom + navBarInsets.bottom);
                return insets;
            });
        }

        store = new AiSettingsStore(this);
        providerIndex = store.getProviderIndex();
        modelIndex = store.getModelIndex();

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.rowProvider.setOnClickListener(v -> showProviderDialog());
        binding.rowApiKey.setOnClickListener(v -> showApiKeyDialog());
        binding.rowModel.setOnClickListener(v -> showModelDialog());
        binding.rowSystemPrompt.setOnClickListener(v -> showSystemPromptDialog());
        binding.rowTest.setOnClickListener(v -> runTest());

        binding.radioPermission.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == binding.rbAsk.getId()) store.setPermission(0);
            else if (checkedId == binding.rbAutoEdit.getId()) store.setPermission(1);
            else if (checkedId == binding.rbPlan.getId()) store.setPermission(2);
            else if (checkedId == binding.rbAutonomous.getId()) store.setPermission(3);
        });
        selectPermission(store.getPermission());

        buildToolRows();
        refreshValues();
    }

    private void selectPermission(int index) {
        RadioButton[] buttons = {binding.rbAsk, binding.rbAutoEdit, binding.rbPlan, binding.rbAutonomous};
        buttons[Math.max(0, Math.min(index, buttons.length - 1))].setChecked(true);
    }

    private void refreshValues() {
        Provider provider = PROVIDERS[providerIndex];
        String apiKey = store.getApiKey(providerIndex);

        binding.tvProviderValue.setText(provider.name);
        binding.tvApiKeyValue.setText(apiKey.isEmpty() ? "Not set" : maskKey(apiKey));
        binding.tvModelValue.setText(provider.models[modelIndex]);
        binding.tvSystemPromptValue.setText(store.getSystemPrompt(DEFAULT_SYSTEM_PROMPT));

        if (apiKey.isEmpty()) {
            setTestStatus("Enter an API key first", COLOR_NEUTRAL);
        } else if (store.isVerified(providerIndex)) {
            setTestStatus("Connected", COLOR_OK);
        } else {
            setTestStatus("Not tested", COLOR_NEUTRAL);
        }
    }

    private void setTestStatus(String text, int color) {
        binding.tvTestStatus.setText(text);
        binding.tvTestStatus.setTextColor(color);
    }

    private String maskKey(String key) {
        if (key.length() <= 8) {
            return "••••••••";
        }
        return key.substring(0, 3) + "••••••••" + key.substring(key.length() - 4);
    }

    private void buildToolRows() {
        LibraryCategoryView toolsCategory = new LibraryCategoryView(this);
        toolsCategory.setTitle("Tools (" + TOOLS.length + ")");

        for (int i = 0; i < TOOLS.length; i++) {
            Tool tool = TOOLS[i];

            LibraryItemView item = new LibraryItemView(this);
            item.setHideIcon(true);
            item.setHideEnabled();
            item.title.setText(tool.name);
            item.description.setText(tool.category + " · " + tool.policy);
            item.sw_enable.setVisibility(View.VISIBLE);
            item.sw_enable.setChecked(store.isToolEnabled(tool.name));
            item.sw_enable.setOnCheckedChangeListener((button, isChecked) ->
                    store.setToolEnabled(tool.name, isChecked));
            item.setOnClickListener(v -> item.sw_enable.toggle());

            toolsCategory.addLibraryItem(item, i < TOOLS.length - 1);
        }

        binding.toolsContainer.addView(toolsCategory);
    }

    private void showProviderDialog() {
        String[] names = new String[PROVIDERS.length];
        for (int i = 0; i < PROVIDERS.length; i++) {
            names[i] = PROVIDERS[i].name;
        }
        final int[] selected = {providerIndex};

        new MaterialAlertDialogBuilder(this)
                .setTitle("Provider")
                .setSingleChoiceItems(names, providerIndex, (d, which) -> selected[0] = which)
                .setPositiveButton("Apply", (d, w) -> {
                    if (providerIndex != selected[0]) {
                        providerIndex = selected[0];
                        modelIndex = 0;
                        store.setProviderIndex(providerIndex);
                        store.setModelIndex(modelIndex);
                    }
                    refreshValues();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showApiKeyDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("API key");
        input.setText(store.getApiKey(providerIndex));
        int pad = SketchwareUtil.dpToPx(16);
        input.setPadding(pad, pad, pad, pad);

        new MaterialAlertDialogBuilder(this)
                .setTitle("API key")
                .setView(input)
                .setPositiveButton("Apply", (d, w) -> {
                    String key = input.getText().toString().trim();
                    store.setApiKey(providerIndex, key);
                    store.setVerified(providerIndex, false);
                    refreshValues();
                    if (!key.isEmpty()) {
                        runTest();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showModelDialog() {
        Provider provider = PROVIDERS[providerIndex];
        final int[] selected = {modelIndex};

        new MaterialAlertDialogBuilder(this)
                .setTitle("Model")
                .setSingleChoiceItems(provider.models, modelIndex, (d, which) -> selected[0] = which)
                .setPositiveButton("Apply", (d, w) -> {
                    modelIndex = selected[0];
                    store.setModelIndex(modelIndex);
                    refreshValues();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSystemPromptDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMinLines(4);
        input.setGravity(Gravity.TOP);
        input.setText(store.getSystemPrompt(DEFAULT_SYSTEM_PROMPT));
        int pad = SketchwareUtil.dpToPx(16);
        input.setPadding(pad, pad, pad, pad);

        new MaterialAlertDialogBuilder(this)
                .setTitle("System prompt")
                .setView(input)
                .setPositiveButton("Apply", (d, w) -> {
                    store.setSystemPrompt(input.getText().toString().trim());
                    refreshValues();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void runTest() {
        final int testProvider = providerIndex;
        String key = store.getApiKey(testProvider);
        if (key.isEmpty()) {
            setTestStatus("Enter an API key first", COLOR_NEUTRAL);
            return;
        }

        setTestStatus("Testing...", COLOR_NEUTRAL);
        AiConnectionTester.test(testProvider, key, (success, message) -> mainHandler.post(() -> {
            if (isDestroyed()) return;
            store.setVerified(testProvider, success);
            if (testProvider == providerIndex) {
                setTestStatus(message, success ? COLOR_OK : COLOR_ERROR);
            }
        }));
    }
}
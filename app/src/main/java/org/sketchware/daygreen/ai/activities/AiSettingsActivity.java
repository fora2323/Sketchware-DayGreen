package org.sketchware.daygreen.ai.activities;

import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

import com.besome.sketch.lib.base.BaseAppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import pro.sketchware.databinding.ActivityAiSettingsBinding;

public class AiSettingsActivity extends BaseAppCompatActivity {

    private static final String[] PROVIDERS = {"OpenAI"};
    private static final String[] MODELS = {"gpt-4o", "gpt-4o-mini", "gpt-4.1", "gpt-4.1-mini"};
    private static final String DEFAULT_SYSTEM_PROMPT =
            "You are an AI assistant for Sketchware DayGreen. Be concise and precise.";

    private ActivityAiSettingsBinding binding;
    private String provider = PROVIDERS[0];
    private String apiKey = "";
    private String model = MODELS[0];
    private String systemPrompt = DEFAULT_SYSTEM_PROMPT;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAiSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        binding.rowProvider.setOnClickListener(v -> showProviderDialog());
        binding.rowApiKey.setOnClickListener(v -> showApiKeyDialog());
        binding.rowModel.setOnClickListener(v -> showModelDialog());
        binding.rowSystemPrompt.setOnClickListener(v -> showSystemPromptDialog());

        refreshValues();
    }

    private void refreshValues() {
        binding.tvProviderValue.setText(provider);
        binding.tvApiKeyValue.setText(apiKey.isEmpty() ? "Not set" : maskKey(apiKey));
        binding.tvModelValue.setText(model);
        binding.tvSystemPromptValue.setText(systemPrompt);
    }

    private String maskKey(String key) {
        if (key.length() <= 8) {
            return "••••••••";
        }
        return key.substring(0, 3) + "••••••••" + key.substring(key.length() - 4);
    }

    private void showProviderDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Provider")
                .setSingleChoiceItems(PROVIDERS, 0, (d, which) -> provider = PROVIDERS[which])
                .setPositiveButton("Apply", (d, w) -> refreshValues())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showApiKeyDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("API key");
        input.setText(apiKey);

        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        new MaterialAlertDialogBuilder(this)
                .setTitle("API key")
                .setView(input)
                .setPositiveButton("Apply", (d, w) -> {
                    apiKey = input.getText().toString().trim();
                    refreshValues();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showModelDialog() {
        final int[] selected = {indexOf(MODELS, model)};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Model")
                .setSingleChoiceItems(MODELS, selected[0], (d, which) -> selected[0] = which)
                .setPositiveButton("Apply", (d, w) -> {
                    model = MODELS[selected[0]];
                    refreshValues();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSystemPromptDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMinLines(4);
        input.setGravity(android.view.Gravity.TOP);
        input.setText(systemPrompt);

        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        new MaterialAlertDialogBuilder(this)
                .setTitle("System prompt")
                .setView(input)
                .setPositiveButton("Apply", (d, w) -> {
                    systemPrompt = input.getText().toString().trim();
                    refreshValues();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private int indexOf(String[] items, String value) {
        for (int i = 0; i < items.length; i++) {
            if (items[i].equals(value)) return i;
        }
        return 0;
    }
}
package pro.sketchware.activities.editor.logic;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.rosemoe.sora.widget.component.EditorAutoCompletion;
import mod.hey.studios.code.SrcCodeEditor;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.code2block.Code2BlockConverter;
import pro.sketchware.databinding.ActivityCode2blockEditorBinding;
import pro.sketchware.utility.EditorUtils;
import pro.sketchware.utility.UI;

public class Code2BlockEditorActivity extends BaseAppCompatActivity {

    public static final String EXTRA_CODE = "code";
    public static final String EXTRA_SC_ID = "sc_id";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_SUBTITLE = "subtitle";
    public static final String RESULT_EXTRA_BLOCKS = "blocks";

    private ActivityCode2blockEditorBinding binding;
    private String initialCode = "";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        enableEdgeToEdgeNoContrast();
        super.onCreate(savedInstanceState);

        binding = ActivityCode2blockEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String scId = getIntent().getStringExtra(EXTRA_SC_ID);
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        String subtitle = getIntent().getStringExtra(EXTRA_SUBTITLE);
        initialCode = getIntent().getStringExtra(EXTRA_CODE);
        if (initialCode == null) {
            initialCode = "";
        }

        setupToolbar(title, subtitle);
        setupEditor(scId);

        binding.fabConvert.setOnClickListener(v -> performConversion());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                checkAndExit();
            }
        });

        UI.addSystemWindowInsetToPadding(binding.appBarLayout, true, true, true, false);
        UI.addSystemWindowInsetToMargin(binding.editor, true, false, true, true);
    }

    private void setupToolbar(String title, String subtitle) {
        if (title != null && !title.isEmpty()) {
            binding.toolbar.setTitle(title);
        } else {
            binding.toolbar.setTitle("Code to Blocks");
        }

        if (subtitle != null && !subtitle.isEmpty()) {
            binding.toolbar.setSubtitle(subtitle);
        }

        binding.toolbar.setNavigationOnClickListener(v -> checkAndExit());
        binding.toolbar.inflateMenu(R.menu.menu_code2block_editor);
        binding.toolbar.setOnMenuItemClickListener(this::onMenuItemClicked);

        MenuItem wordWrapItem = binding.toolbar.getMenu().findItem(R.id.action_word_wrap);
        if (wordWrapItem != null) {
            wordWrapItem.setChecked(binding.editor.isWordwrap());
        }

        MenuItem autoCItem = binding.toolbar.getMenu().findItem(R.id.action_autocomplete);
        if (autoCItem != null) {
            autoCItem.setChecked(binding.editor.getComponent(EditorAutoCompletion.class).isEnabled());
        }
    }

    private void setupEditor(String scId) {
        binding.editor.setScId(scId);
        binding.editor.setTypefaceText(EditorUtils.getTypeface(this));
        binding.editor.setTextSize(14);
        binding.editor.setText(initialCode);
        binding.editor.setEditable(true);

        EditorUtils.loadJavaConfig(binding.editor);
        SrcCodeEditor.loadCESettings(this, binding.editor, "act", true);
    }

    private boolean onMenuItemClicked(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_undo) {
            if (binding.editor.canUndo()) {
                binding.editor.undo();
            }
            return true;
        } else if (id == R.id.action_redo) {
            if (binding.editor.canRedo()) {
                binding.editor.redo();
            }
            return true;
        } else if (id == R.id.action_format) {
            binding.editor.formatCodeAsync();
            return true;
        } else if (id == R.id.action_word_wrap) {
            boolean nextState = !binding.editor.isWordwrap();
            binding.editor.setWordwrap(nextState);
            item.setChecked(nextState);
            return true;
        } else if (id == R.id.action_autocomplete) {
            boolean nextState = !binding.editor.getComponent(EditorAutoCompletion.class).isEnabled();
            binding.editor.getComponent(EditorAutoCompletion.class).setEnabled(nextState);
            item.setChecked(nextState);
            return true;
        }
        return false;
    }

    private void performConversion() {
        String currentCode = binding.editor.getText().toString();
        binding.progressIndicator.setVisibility(View.VISIBLE);
        binding.fabConvert.setEnabled(false);

        executor.execute(() -> {
            Code2BlockConverter.ConversionResult result = Code2BlockConverter.convertJavaToBlocks(currentCode);
            mainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) return;

                binding.progressIndicator.setVisibility(View.GONE);
                binding.fabConvert.setEnabled(true);

                if (result.isSuccess) {
                    Intent data = new Intent();
                    data.putParcelableArrayListExtra(RESULT_EXTRA_BLOCKS, result.blocks);
                    setResult(RESULT_OK, data);
                    Toast.makeText(Code2BlockEditorActivity.this,
                            "Converted " + result.blocks.size() + " blocks successfully!",
                            Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    showErrorDialog(result.errorMessage);
                }
            });
        });
    }

    private void showErrorDialog(String errorMessage) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Conversion Error")
                .setMessage(errorMessage != null ? errorMessage : "Unknown conversion error")
                .setPositiveButton(R.string.common_word_ok, null)
                .setNeutralButton("Copy Error", (dialog, which) -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Conversion Error", errorMessage);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(this, "Error copied to clipboard", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void checkAndExit() {
        String currentCode = binding.editor.getText().toString();
        if (!currentCode.equals(initialCode)) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Discard Changes?")
                    .setMessage("You have unsaved changes. Do you want to discard them?")
                    .setPositiveButton("Discard", (dialog, which) -> finish())
                    .setNegativeButton("Keep Editing", null)
                    .show();
        } else {
            finish();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}

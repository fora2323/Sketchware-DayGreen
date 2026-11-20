package org.sketchware.daygreen.ai;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.editor.LogicEditorActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.sketchware.daygreen.ai.activities.AiSettingsActivity;

import java.util.List;

import a.a.a.jC;
import pro.sketchware.databinding.DialogAiInputBinding;
import pro.sketchware.databinding.FragmentAiChatBinding;
import pro.sketchware.utility.SketchwareUtil;

public class AiChatFragment extends Fragment {

    private static final String ARG_SC_ID = "sc_id";
    private static final String ARG_JAVA_NAME = "java_name";

    private FragmentAiChatBinding binding;
    private AiChatClient client;
    private AiSettingsStore store;
    private ChatAdapter adapter;
    private ChatTextFormatter formatter;
    private final Handler main = new Handler(Looper.getMainLooper());
    private AiChatClient.Proposal pending;

    public static AiChatFragment newInstance(String scId, String javaName) {
        AiChatFragment fragment = new AiChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SC_ID, scId);
        args.putString(ARG_JAVA_NAME, javaName);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAiChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        store = new AiSettingsStore(requireContext());
        String scId = getScId();
        String javaName = getJavaName();
        String xmlName = getXmlName();

        client = new AiChatClient(requireContext(), scId, javaName, xmlName);

        formatter = new ChatTextFormatter(requireContext());
        adapter = new ChatAdapter(formatter, this::openEvent);

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(false);
        binding.rvChat.setLayoutManager(layoutManager);
        binding.rvChat.setAdapter(adapter);

        // Adjust input box with keyboard WindowInsets
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
            Insets navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            int bottomPadding = Math.max(imeInsets.bottom, navInsets.bottom);
            binding.cvInput.setPadding(
                    binding.cvInput.getPaddingLeft(),
                    binding.cvInput.getPaddingTop(),
                    binding.cvInput.getPaddingRight(),
                    bottomPadding > 0 ? bottomPadding : (int) (8 * getResources().getDisplayMetrics().density)
            );
            if (insets.isVisible(WindowInsetsCompat.Type.ime())) {
                scrollToBottom();
            }
            return insets;
        });

        // Display active context pill (e.g. MainActivity.java | main.xml)
        if (binding.tvActiveContext != null) {
            binding.tvActiveContext.setText(javaName + " | " + xmlName);
        }

        if (scId.isEmpty()) {
            adapter.add(ChatAdapter.ChatMessage.agent(
                    "Project ID is missing. Close the project and open it again."));
            binding.btnSend.setEnabled(false);
            return;
        }

        binding.btnPrompt1.setOnClickListener(v -> sendPrompt("Analisis dan perbaiki error compile terakhir di project ini"));
        binding.btnPrompt2.setOnClickListener(v -> sendPrompt("Tolong buatkan layout form login dengan input email, password, dan tombol masuk di " + xmlName));
        binding.btnPrompt3.setOnClickListener(v -> sendPrompt("Tolong pasang block logic di onCreate untuk tombol login dan tampilkan Toast"));
        binding.btnPrompt4.setOnClickListener(v -> sendPrompt("Tolong analisis kode dan potensi bug di file " + javaName + " dan layout " + xmlName));

        binding.btnSend.setOnClickListener(v -> send());
        binding.btnApply.setOnClickListener(v -> confirmApply());
        binding.btnLink.setOnClickListener(v -> showAttachmentOptions());
        if (binding.btnSettings != null) {
            binding.btnSettings.setOnClickListener(v -> openSettings());
        }
        binding.btnModel.setOnClickListener(v ->
                AiModelPicker.show(requireContext(), store, store.getProviderIndex(), this::refreshModelLabel));

        updateEmptyState();
        adapter.registerAdapterDataObserver(new androidx.recyclerview.widget.RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                updateEmptyState();
            }
        });
    }

    private void sendPrompt(String text) {
        binding.etInput.setText(text);
        send();
    }

    private void updateEmptyState() {
        if (binding == null) return;
        boolean empty = adapter.getItemCount() == 0;
        binding.llEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshModelLabel();
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

    private void openSettings() {
        startActivity(new Intent(requireContext(), AiSettingsActivity.class));
    }

    private void refreshModelLabel() {
        if (binding == null) return;
        int providerIndex = store.getProviderIndex();
        if (providerIndex < 0 || providerIndex >= AiProviders.ALL.length) return;

        AiProviders.Provider provider = AiProviders.ALL[providerIndex];
        String modelId = store.getModelId("");
        binding.btnModel.setText(modelId.isEmpty() ? provider.models[0] : modelId);
    }

    /**
     * Shows a bottom sheet to attach error logs, layouts, blocks, or links.
     */
    private void showAttachmentOptions() {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet =
                new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());

        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        android.widget.TextView title = new android.widget.TextView(requireContext());
        title.setText("Quick Assistant Actions");
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, 0, 0, pad);
        layout.addView(title);

        String[] options = {
                "🔍 Analisis Error Compile",
                "🎨 Periksa / Bikin Layout (" + getXmlName() + ")",
                "🧩 Pasang Block Logic (" + getJavaName() + ")",
                "🔗 Tambah URL Link"
        };

        for (int i = 0; i < options.length; i++) {
            android.widget.TextView item = new android.widget.TextView(requireContext());
            item.setText(options[i]);
            item.setTextSize(15);
            item.setPadding(0, pad / 2, 0, pad / 2);
            android.util.TypedValue outValue = new android.util.TypedValue();
            requireContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            item.setBackgroundResource(outValue.resourceId);
            int finalI = i;
            item.setOnClickListener(v -> {
                bottomSheet.dismiss();
                switch (finalI) {
                    case 0:
                        attachCompileError();
                        break;
                    case 1:
                        binding.etInput.setText("Tolong periksa layout " + getXmlName() + " dan buat tampilan baru yang bagus.");
                        binding.etInput.setSelection(binding.etInput.getText().length());
                        break;
                    case 2:
                        binding.etInput.setText("Tolong pasang block logic di " + getJavaName() + " untuk event ");
                        binding.etInput.setSelection(binding.etInput.getText().length());
                        break;
                    case 3:
                        showLinkDialog();
                        break;
                }
            });
            layout.addView(item);
        }

        bottomSheet.setContentView(layout);
        bottomSheet.show();
    }

    private void attachCompileError() {
        String error = AiDiagnosticHelper.getCompileError(getScId());
        binding.etInput.setText("Tolong analisis error compile berikut dan beri solusinya:\n" + error);
        binding.etInput.setSelection(binding.etInput.getText().length());
    }

    /**
     * Opens a dialog to paste a URL. The URL is appended to the input field.
     */
    private void showLinkDialog() {
        DialogAiInputBinding dialog = DialogAiInputBinding.inflate(getLayoutInflater());
        dialog.tilInput.setHint("URL");
        dialog.etInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add link")
                .setView(dialog.getRoot())
                .setPositiveButton("Add", (d, w) -> {
                    String url = dialog.etInput.getText() == null
                            ? ""
                            : dialog.etInput.getText().toString().trim();
                    if (url.isEmpty()) return;

                    String current = binding.etInput.getText() == null
                            ? ""
                            : binding.etInput.getText().toString();
                    String separator = current.isEmpty() || current.endsWith(" ") ? "" : " ";
                    binding.etInput.append(separator + url + " ");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openEvent(String eventName) {
        if (binding == null) return;

        String scId = getScId();
        String javaName = getJavaName();

        ProjectFileBean file = findProjectFile(scId, javaName);
        if (file == null) {
            SketchwareUtil.toast("File Java tidak ditemukan di project.");
            return;
        }

        boolean onCreateGroup = "onCreate".equals(eventName) || "initializeLogic".equals(eventName);
        String id = onCreateGroup ? "onCreate" : eventName;
        String event = "onCreate".equals(eventName) ? "initializeLogic" : eventName;

        Intent intent = new Intent(requireContext(), LogicEditorActivity.class);
        intent.putExtra("sc_id", scId);
        intent.putExtra("id", id);
        intent.putExtra("event", event);
        intent.putExtra("project_file", file);
        startActivity(intent);
    }

    private static ProjectFileBean findProjectFile(String scId, String javaName) {
        try {
            for (ProjectFileBean projectFile : jC.b(scId).b()) {
                if (javaName.equals(projectFile.getJavaName())) {
                    return projectFile;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void send() {
        String text = binding.etInput.getText() == null
                ? ""
                : binding.etInput.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }
        binding.etInput.setText("");
        adapter.add(ChatAdapter.ChatMessage.user(text));
        adapter.updateStatus("Thinking...");
        scrollToBottom();
        setBusy(true);

        client.ask(text, new AiChatClient.Callback() {
            @Override
            public void onResult(String reply, List<AiChatClient.Proposal> proposals) {
                main.post(() -> {
                    if (binding == null) return;
                    setBusy(false);
                    adapter.removeStatus();
                    adapter.add(ChatAdapter.ChatMessage.agent(reply));

                    if (proposals.isEmpty()) {
                        pending = null;
                        binding.btnApply.setVisibility(View.GONE);
                    } else {
                        pending = proposals.get(proposals.size() - 1);
                        binding.btnApply.setVisibility(View.VISIBLE);

                        String buttonLabel;
                        String proposalLabel;
                        if (pending.isLayout()) {
                            buttonLabel = "Apply layout to " + pending.xmlName;
                            proposalLabel = "Proposed layout XML (not applied yet):\n";
                        } else if (pending.isWidget()) {
                            buttonLabel = "Add widget to " + pending.xmlName;
                            proposalLabel = "Proposed widget (not applied yet):\n";
                        } else if (pending.isSource()) {
                            buttonLabel = "Write source code to " + pending.eventKey;
                            proposalLabel = "Proposed source code (not written yet):\n";
                        } else {
                            buttonLabel = "Write blocks to " + pending.eventKey;
                            proposalLabel = "Proposed blocks (not written yet):\n";
                        }

                        binding.btnApply.setText(buttonLabel);
                        adapter.add(ChatAdapter.ChatMessage.agent(proposalLabel + pending.summary));
                    }
                    scrollToBottom();
                });
            }

            @Override
            public void onError(String error) {
                main.post(() -> {
                    if (binding == null) return;
                    setBusy(false);
                    adapter.removeStatus();
                    adapter.add(ChatAdapter.ChatMessage.agent("Error: " + error));
                    scrollToBottom();
                });
            }

            @Override
            public void onStatus(String status) {
                main.post(() -> {
                    if (binding == null) return;
                    adapter.updateStatus(status);
                    scrollToBottom();
                });
            }

            @Override
            public void onThinking(String thinking) {
                main.post(() -> {
                    if (binding == null) return;
                    adapter.removeStatus();
                    adapter.add(ChatAdapter.ChatMessage.thinking(thinking));
                    scrollToBottom();
                });
            }
        });
    }

    private void confirmApply() {
        if (pending == null) return;
        AiChatClient.Proposal p = pending;

        if (p.isLayout()) {
            confirmLayoutCode(p);
        } else if (p.isSource()) {
            confirmSourceCode(p);
        } else if (p.isWidget()) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Add widget?")
                    .setMessage("Add " + p.widgetType + " (" + p.widgetId + ") to " + p.xmlName + "?\n\n" + p.summary)
                    .setPositiveButton("Add", (d, w) -> applyPending(p))
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Write blocks?")
                    .setMessage("Event: " + p.eventKey + "\n\n" + p.summary)
                    .setPositiveButton("Write", (d, w) -> applyPending(p))
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    private void confirmLayoutCode(AiChatClient.Proposal p) {
        TextView code = new TextView(requireContext());
        code.setText(p.layoutXml);
        code.setTypeface(Typeface.MONOSPACE);
        code.setTextIsSelectable(true);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        code.setPadding(pad, pad, pad, pad);

        ScrollView scroll = new ScrollView(requireContext());
        scroll.addView(code);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Apply layout XML to " + p.xmlName + "?")
                .setMessage("This will update the project layout with the following XML:")
                .setView(scroll)
                .setPositiveButton("Apply", (d, w) -> applyPending(p))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmSourceCode(AiChatClient.Proposal p) {
        TextView code = new TextView(requireContext());
        code.setText(p.sourceCode);
        code.setTypeface(Typeface.MONOSPACE);
        code.setTextIsSelectable(true);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        code.setPadding(pad, pad, pad, pad);

        ScrollView scroll = new ScrollView(requireContext());
        scroll.addView(code);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Write this source code?")
                .setMessage("Event: " + p.eventKey + "\nInserted using addSourceDirectly block.")
                .setView(scroll)
                .setPositiveButton("Write", (d, w) -> applyPending(p))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void applyPending(AiChatClient.Proposal p) {
        String scId = getScId();
        new Thread(() -> {
            String message;
            boolean success;

            if (p.isLayout()) {
                AiLayoutHelper.Result r = AiLayoutHelper.applyLayoutXml(scId, p.xmlName, p.layoutXml);
                message = r.message;
                success = r.success;
            } else if (p.isWidget()) {
                AiLayoutHelper.Result r = AiLayoutHelper.addWidget(scId, p.xmlName, p.widgetType,
                        p.widgetId, p.widgetParent, p.widgetText, p.widgetWidth, p.widgetHeight, p.widgetOrientation);
                message = r.message;
                success = r.success;
            } else if (p.isSource()) {
                AiBlockWriter.Result r = AiBlockWriter.writeSourceCode(scId, p.javaName, p.eventKey, p.sourceCode);
                message = r.message;
                success = r.success;
            } else {
                AiBlockWriter.Result r = AiBlockWriter.appendBlocks(scId, p.javaName, p.eventKey, p.steps);
                message = r.message;
                success = r.success;
            }

            main.post(() -> {
                if (binding == null) return;
                adapter.add(ChatAdapter.ChatMessage.agent(message));
                if (success) {
                    pending = null;
                    binding.btnApply.setVisibility(View.GONE);
                    SketchwareUtil.toast("Applied successfully.");
                }
                scrollToBottom();
            });
        }).start();
    }

    private void setBusy(boolean busy) {
        binding.btnSend.setEnabled(!busy);
        binding.btnSend.setAlpha(busy ? 0.45f : 1f);
    }

    private void scrollToBottom() {
        if (binding == null || adapter.getItemCount() == 0) return;
        binding.rvChat.post(() -> binding.rvChat.scrollToPosition(adapter.getItemCount() - 1));
    }

    private String getScId() {
        Bundle args = getArguments();
        return args != null ? args.getString(ARG_SC_ID, "") : "";
    }

    private String getJavaName() {
        Bundle args = getArguments();
        return args != null ? args.getString(ARG_JAVA_NAME, "MainActivity") : "MainActivity";
    }

    private String getXmlName() {
        ProjectFileBean file = findProjectFile(getScId(), getJavaName());
        return file != null ? file.getXmlName() : "main.xml";
    }
}

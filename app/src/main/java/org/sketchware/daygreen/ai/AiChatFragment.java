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
        client = new AiChatClient(requireContext(), getScId(), getJavaName());

        formatter = new ChatTextFormatter(requireContext());
        adapter = new ChatAdapter(formatter, this::openEvent);

        // Messages start at the top and grow downward, like a normal chat list.
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(false);
        binding.rvChat.setLayoutManager(layoutManager);
        binding.rvChat.setAdapter(adapter);

        if (getScId().isEmpty()) {
            adapter.add(ChatAdapter.ChatMessage.agent(
                    "Project id is missing. Close the project and open it again."));
            binding.btnSend.setEnabled(false);
            return;
        }
        
        binding.btnPrompt1.setOnClickListener(v -> sendPrompt("Fix the warnings and errors in the current file"));
        binding.btnPrompt2.setOnClickListener(v -> sendPrompt("Add documentation to my current file"));
        binding.btnPrompt3.setOnClickListener(v -> sendPrompt("Explain this code to me"));

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
     * Shows a bottom sheet to attach a photo, file, or analyze an error.
     */
    private void showAttachmentOptions() {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet = 
                new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        
        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);
        
        android.widget.TextView title = new android.widget.TextView(requireContext());
        title.setText("Add attachment");
        title.setTextSize(18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, pad);
        layout.addView(title);
        
        String[] options = {"Photo", "File", "Analyze Error", "URL"};
        for (int i = 0; i < options.length; i++) {
            android.widget.TextView item = new android.widget.TextView(requireContext());
            item.setText(options[i]);
            item.setTextSize(16);
            item.setPadding(0, pad/2, 0, pad/2);
            android.util.TypedValue outValue = new android.util.TypedValue();
            requireContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            item.setBackgroundResource(outValue.resourceId);
            int finalI = i;
            item.setOnClickListener(v -> {
                bottomSheet.dismiss();
                switch (finalI) {
                    case 0:
                        pro.sketchware.utility.SketchwareUtil.toast("Photo attachment coming soon");
                        break;
                    case 1:
                        pro.sketchware.utility.SketchwareUtil.toast("File attachment coming soon");
                        break;
                    case 2:
                        pro.sketchware.utility.SketchwareUtil.toast("Error analysis coming soon");
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

    /**
     * Tapping a highlighted event name (onCreate, onResume, initializeLogic...)
     * opens that event in the Logic editor.
     */
    private void openEvent(String eventName) {
        if (binding == null) return;

        String scId = getScId();
        String javaName = getJavaName();

        ProjectFileBean file = findProjectFile(scId, javaName);
        if (file == null) {
            SketchwareUtil.toast("File Java tidak ditemukan di project.");
            return;
        }

        // Same key rule as AiEventCatalog: id + "_" + event.
        // initializeLogic (and onCreate) live under the onCreate target.
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
        for (ProjectFileBean projectFile : jC.b(scId).b()) {
            if (javaName.equals(projectFile.getJavaName())) {
                return projectFile;
            }
        }
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
                        // A new answer without a proposal clears the old proposal.
                        pending = null;
                        binding.btnApply.setVisibility(View.GONE);
                    } else {
                        pending = proposals.get(proposals.size() - 1);
                        binding.btnApply.setVisibility(View.VISIBLE);
                        String label = pending.isSource()
                                ? "Proposed source code (not written yet):\n"
                                : "Proposed blocks (not written yet):\n";
                        adapter.add(ChatAdapter.ChatMessage.agent(label + pending.summary));
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

        if (p.isSource()) {
            confirmSourceCode(p);
        } else {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Write blocks?")
                    .setMessage("Event: " + p.eventKey + "\n\n" + p.summary)
                    .setPositiveButton("Write", (d, w) -> applyPending(p))
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    /**
     * Shows the whole code before anything is written. The user can scroll the full text.
     */
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
                .setMessage("Event: " + p.eventKey + "\nNo block fits, so this is raw code.")
                .setView(scroll)
                .setPositiveButton("Write", (d, w) -> applyPending(p))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void applyPending(AiChatClient.Proposal p) {
        String scId = getScId();
        new Thread(() -> {
            AiBlockWriter.Result r = p.isSource()
                    ? AiBlockWriter.writeSourceCode(scId, p.javaName, p.eventKey, p.sourceCode)
                    : AiBlockWriter.appendBlocks(scId, p.javaName, p.eventKey, p.steps);

            main.post(() -> {
                if (binding == null) return;
                adapter.add(ChatAdapter.ChatMessage.agent(r.message));
                if (r.success) {
                    pending = null;
                    binding.btnApply.setVisibility(View.GONE);
                    SketchwareUtil.toast(p.isSource() ? "Source code written." : "Blocks written.");
                }
                scrollToBottom();
            });
        }).start();
    }

    private void setBusy(boolean busy) {
        binding.btnSend.setEnabled(!busy);
        // Keep the disabled look clearly different, so it does not look frozen.
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
        return args != null ? args.getString(ARG_JAVA_NAME, "") : "";
    }
}
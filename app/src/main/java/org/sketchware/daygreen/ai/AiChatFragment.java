package org.sketchware.daygreen.ai;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.sketchware.daygreen.ai.activities.AiSettingsActivity;

import java.util.List;

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

        adapter = new ChatAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        binding.rvChat.setLayoutManager(layoutManager);
        binding.rvChat.setAdapter(adapter);

        binding.btnSend.setOnClickListener(v -> send());
        binding.btnApply.setOnClickListener(v -> confirmApply());
        binding.btnLink.setOnClickListener(v -> showLinkDialog());
        binding.btnSettings.setOnClickListener(v -> openSettings());
        binding.btnModel.setOnClickListener(v ->
                AiModelPicker.show(requireContext(), store, store.getProviderIndex(), this::refreshModelLabel));
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

    private void send() {
        String text = binding.etInput.getText() == null
                ? ""
                : binding.etInput.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }
        binding.etInput.setText("");
        adapter.add(ChatAdapter.ChatMessage.user(text));
        adapter.add(ChatAdapter.ChatMessage.loading());
        scrollToBottom();
        setBusy(true);

        client.ask(text, new AiChatClient.Callback() {
            @Override
            public void onResult(String reply, List<AiChatClient.Proposal> proposals) {
                main.post(() -> {
                    if (binding == null) return;
                    setBusy(false);
                    adapter.removeLoading();
                    adapter.add(ChatAdapter.ChatMessage.agent(reply));
                    if (!proposals.isEmpty()) {
                        pending = proposals.get(proposals.size() - 1);
                        binding.btnApply.setVisibility(View.VISIBLE);
                        adapter.add(ChatAdapter.ChatMessage.agent(
                                "Proposed blocks (not written yet):\n" + pending.summary));
                    }
                    scrollToBottom();
                });
            }

            @Override
            public void onError(String error) {
                main.post(() -> {
                    if (binding == null) return;
                    setBusy(false);
                    adapter.removeLoading();
                    adapter.add(ChatAdapter.ChatMessage.agent("Error: " + error));
                    scrollToBottom();
                });
            }
        });
    }

    private void confirmApply() {
        if (pending == null) return;
        AiChatClient.Proposal p = pending;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Write blocks?")
                .setMessage("Event: " + p.eventKey + "\n\n" + p.summary)
                .setPositiveButton("Write", (d, w) -> applyPending(p))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void applyPending(AiChatClient.Proposal p) {
        String scId = getScId();
        new Thread(() -> {
            AiBlockWriter.Result r = AiBlockWriter.appendBlocks(scId, p.javaName, p.eventKey, p.steps);
            main.post(() -> {
                if (binding == null) return;
                adapter.add(ChatAdapter.ChatMessage.agent(r.message));
                if (r.success) {
                    pending = null;
                    binding.btnApply.setVisibility(View.GONE);
                    SketchwareUtil.toast("Blocks written.");
                }
                scrollToBottom();
            });
        }).start();
    }

    private void setBusy(boolean busy) {
        binding.btnSend.setEnabled(!busy);
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
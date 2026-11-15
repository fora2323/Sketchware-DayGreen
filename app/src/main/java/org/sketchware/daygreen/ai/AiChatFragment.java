package org.sketchware.daygreen.ai;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import pro.sketchware.databinding.FragmentAiChatBinding;
import pro.sketchware.utility.SketchwareUtil;

public class AiChatFragment extends Fragment {

    private static final String ARG_SC_ID = "sc_id";
    private static final String ARG_JAVA_NAME = "java_name";

    private FragmentAiChatBinding binding;
    private AiChatClient client;
    private final Handler main = new Handler(Looper.getMainLooper());
    private AiChatClient.Proposal pending;

    public static AiChatFragment newInstance(String scId, String javaName) {
        AiChatFragment f = new AiChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SC_ID, scId);
        args.putString(ARG_JAVA_NAME, javaName);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAiChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        String scId = args != null ? args.getString(ARG_SC_ID, "") : "";
        String javaName = args != null ? args.getString(ARG_JAVA_NAME, "") : "";
        client = new AiChatClient(requireContext(), scId, javaName);

        binding.btnSend.setOnClickListener(v -> send());
        binding.btnApply.setOnClickListener(v -> confirmApply());
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

    private void send() {
        String text = binding.etInput.getText() == null ? "" : binding.etInput.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }
        binding.etInput.setText("");
        appendLog("Kamu: " + text);
        setBusy(true);

        client.ask(text, new AiChatClient.Callback() {
            @Override
            public void onResult(String reply, List<AiChatClient.Proposal> proposals) {
                main.post(() -> {
                    if (binding == null) return;
                    setBusy(false);
                    appendLog("AI: " + reply);
                    if (!proposals.isEmpty()) {
                        pending = proposals.get(proposals.size() - 1);
                        binding.btnApply.setVisibility(View.VISIBLE);
                        appendLog("Usulan block (belum ditulis):\n" + pending.summary);
                    }
                });
            }

            @Override
            public void onError(String error) {
                main.post(() -> {
                    if (binding == null) return;
                    setBusy(false);
                    appendLog("Error: " + error);
                });
            }
        });
    }

    private void confirmApply() {
        if (pending == null) return;
        AiChatClient.Proposal p = pending;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Tulis block?")
                .setMessage("Event: " + p.eventKey + "\n\n" + p.summary)
                .setPositiveButton("Tulis", (d, w) -> applyPending(p))
                .setNegativeButton("Batal", null)
                .show();
    }

    private void applyPending(AiChatClient.Proposal p) {
        String scId = getArguments() != null ? getArguments().getString(ARG_SC_ID, "") : "";
        new Thread(() -> {
            AiBlockWriter.Result r = AiBlockWriter.appendBlocks(scId, p.javaName, p.eventKey, p.steps);
            main.post(() -> {
                if (binding == null) return;
                appendLog(r.message);
                if (r.success) {
                    pending = null;
                    binding.btnApply.setVisibility(View.GONE);
                    SketchwareUtil.toast("Block ditulis.");
                }
            });
        }).start();
    }

    private void setBusy(boolean busy) {
        binding.btnSend.setEnabled(!busy);
        binding.progress.setVisibility(busy ? View.VISIBLE : View.GONE);
    }

    private void appendLog(String text) {
        binding.tvLog.append(text + "\n\n");
        binding.scrollLog.post(() -> binding.scrollLog.fullScroll(View.FOCUS_DOWN));
    }
}
package org.sketchware.daygreen.ai;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class AiModelPicker {

    private AiModelPicker() {
    }

    /**
     * Fetches the model list for the given provider and shows it in a dropdown.
     * Falls back to the built-in list if the fetch fails.
     */
    public static void show(Context context, AiSettingsStore store, int providerIndex, Runnable onPicked) {
        AiProviders.Provider provider = AiProviders.ALL[providerIndex];
        Handler main = new Handler(Looper.getMainLooper());

        ProgressBar bar = new ProgressBar(context);
        int pad = dp(context, 24);
        bar.setPadding(pad, pad, pad, pad);

        AlertDialog loading = new MaterialAlertDialogBuilder(context)
                .setTitle("Loading models...")
                .setView(bar)
                .show();

        AiModelFetcher.fetch(provider, store.getApiKey(providerIndex), new AiModelFetcher.Callback() {
            @Override
            public void onResult(List<String> models) {
                main.post(() -> {
                    loading.dismiss();
                    showList(context, store, models, provider.models, onPicked, null);
                });
            }

            @Override
            public void onError(String error) {
                main.post(() -> {
                    loading.dismiss();
                    showList(context, store, new ArrayList<>(), provider.models, onPicked, error);
                });
            }
        });
    }

    private static void showList(Context context, AiSettingsStore store, List<String> fetched,
                                 String[] defaults, Runnable onPicked, @Nullable String warning) {
        List<String> models = fetched.isEmpty() ? Arrays.asList(defaults) : fetched;
        if (warning != null) {
            Toast.makeText(context, "Using default list: " + warning, Toast.LENGTH_LONG).show();
        }

        String current = store.getModelId("");
        if (current.isEmpty()) {
            current = defaults[0];
        }
        String[] items = models.toArray(new String[0]);
        int checked = Math.max(0, models.indexOf(current));
        final int[] selected = {checked};

        new MaterialAlertDialogBuilder(context)
                .setTitle("Model")
                .setSingleChoiceItems(items, checked, (d, which) -> selected[0] = which)
                .setPositiveButton("OK", (d, w) -> {
                    store.setModelId(items[selected[0]]);
                    if (onPicked != null) {
                        onPicked.run();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density);
    }
}
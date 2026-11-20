package org.sketchware.daygreen.ai

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.ProgressBar
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object AiModelPicker {

    @JvmStatic
    fun show(context: Context, store: AiSettingsStore, providerIndex: Int, onPicked: Runnable?) {
        val provider = AiProviders.ALL[providerIndex]
        val main = Handler(Looper.getMainLooper())

        val bar = ProgressBar(context)
        val pad = dp(context, 24)
        bar.setPadding(pad, pad, pad, pad)

        val loading = MaterialAlertDialogBuilder(context)
            .setTitle("Memuat model...")
            .setView(bar)
            .show()

        AiModelFetcher.fetch(provider, store.getApiKey(providerIndex), object : AiModelFetcher.Callback {
            override fun onResult(models: List<String>) {
                main.post {
                    loading.dismiss()
                    showList(context, store, models, provider.models, onPicked, null)
                }
            }

            override fun onError(error: String) {
                main.post {
                    loading.dismiss()
                    showList(context, store, emptyList(), provider.models, onPicked, error)
                }
            }
        })
    }

    private fun showList(
        context: Context,
        store: AiSettingsStore,
        fetched: List<String>,
        defaults: Array<out String>,
        onPicked: Runnable?,
        warning: String?
    ) {
        val models = if (fetched.isEmpty()) defaults.toList() else fetched
        if (warning != null) {
            Toast.makeText(context, "Menggunakan daftar bawaan: $warning", Toast.LENGTH_LONG).show()
        }

        var current = store.getModelId("")
        if (current.isEmpty()) {
            current = defaults[0]
        }
        val items = models.toTypedArray()
        val checked = Math.max(0, models.indexOf(current))
        val selected = intArrayOf(checked)

        MaterialAlertDialogBuilder(context)
            .setTitle("Pilih Model")
            .setSingleChoiceItems(items, checked) { _, which -> selected[0] = which }
            .setPositiveButton("OK") { _, _ ->
                store.setModelId(items[selected[0]])
                onPicked?.run()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun dp(context: Context, value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }
}

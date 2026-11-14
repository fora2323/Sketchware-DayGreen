package org.sketchware.daygreen.donation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import pro.sketchware.databinding.ActivityDonationBinding
import pro.sketchware.databinding.ItemDonationOptionBinding

class DonationActivity : AppCompatActivity() {

    private data class Option(val title: String, val url: String)

    private val options = listOf(
        Option("GitHub Sponsors", "https://github.com/sponsors/fora2323"),
        Option("Buy Me a Coffee", "https://buymeacoffee.com/fora2323"),
        Option("Ko-fi", "https://ko-fi.com/fora2323")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityDonationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.collapsingToolbar.title = "Support"
        binding.toolbar.setNavigationOnClickListener { finish() }

        options.forEachIndexed { index, option ->
            val item = ItemDonationOptionBinding.inflate(layoutInflater, binding.optionsContainer, false)
            item.tvTitle.text = option.title
            item.divider.visibility = if (index == options.lastIndex) View.GONE else View.VISIBLE
            item.root.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(option.url)))
            }
            binding.optionsContainer.addView(item.root)
        }
    }
}
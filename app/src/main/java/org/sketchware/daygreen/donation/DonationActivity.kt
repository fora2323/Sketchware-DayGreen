package org.sketchware.daygreen.donation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import pro.sketchware.R
import pro.sketchware.databinding.ActivityDonationBinding
import pro.sketchware.databinding.ItemDonationOptionBinding

class DonationActivity : AppCompatActivity() {

    private data class Option(val title: String, val url: String)

    private val options by lazy {
        listOf(
            Option(getString(R.string.donation_kofi_title), getString(R.string.donation_kofi_url)),
            Option(getString(R.string.donation_saweria_title), getString(R.string.donation_saweria_url))
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityDonationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.collapsingToolbar.title = getString(R.string.donation_title)
        binding.collapsingToolbar.subtitle = getString(R.string.donation_subtitle)
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
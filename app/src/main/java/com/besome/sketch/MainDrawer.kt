package com.besome.sketch

import a.a.a.mB
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout

import com.besome.sketch.help.ProgramInfoActivity
import com.besome.sketch.tools.NewKeyStoreActivity
import com.google.android.material.navigation.NavigationView
import com.google.android.material.theme.overlay.MaterialThemeOverlay

import dev.chrisbanes.insetter.Insetter
import dev.chrisbanes.insetter.Side

import extensions.anbui.daydream.activity.DayDreamCleanUp
import extensions.anbui.daydream.activity.project.settings.DayDreamUniversalSettingsActivity

import mod.hilal.saif.activities.tools.AppSettings

import pro.sketchware.R
import pro.sketchware.activities.about.AboutActivity
import pro.sketchware.utility.UI

class MainDrawer @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = R.attr.navigationViewStyle) : NavigationView(MaterialThemeOverlay.wrap(context, attrs, defStyleAttr, DEF_STYLE_RES), attrs, defStyleAttr) {

    companion object {
        private val DEF_STYLE_RES = R.style.Widget_SketchwarePro_NavigationView_Main
    }

    init {
        val ctx = getContext()
        val layoutDirection = ctx.resources.configuration.layoutDirection

        Insetter.builder().margin(WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.navigationBars(), Side.create(layoutDirection == LAYOUT_DIRECTION_LTR, false, layoutDirection == LAYOUT_DIRECTION_RTL, false)).applyToView(this)

        val headerView = LayoutInflater.from(ctx).inflate(R.layout.main_drawer_header, null) as ViewGroup
        headerView.findViewById<View>(R.id.status_bar_overlapper).minimumHeight = UI.getStatusBarHeight(ctx)

        addHeaderView(headerView)
        inflateMenu(R.menu.main_drawer_menu)
        setNavigationItemSelectedListener { item ->
            initializeSocialLinks(item.itemId)
            initializeDrawerItems(item.itemId)
            close()
            false
        }
    }

    private fun initializeSocialLinks(@IdRes id: Int) {
        if (!mB.a()) {
            @StringRes val url = when (id) {
                R.id.social_discord -> R.string.link_discord_invite
                R.id.social_github -> R.string.link_github_url
                R.id.social_telegram -> R.string.link_telegram_invite
                else -> -1
            }

            if (url != -1) {
                openUrl(context.getString(url))
            }
        }
    }

    private fun initializeDrawerItems(@IdRes id: Int) {
        val activity = unwrap(context) ?: return
        when (id) {
            R.id.program_info -> {
                val intent = Intent(activity, ProgramInfoActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                activity.startActivityForResult(intent, 105)
            }
            R.id.daydream_cleanup -> {
                activity.startActivity(Intent(activity, DayDreamCleanUp::class.java))
            }
            R.id.app_settings -> {
                val intent = Intent(activity, AppSettings::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                activity.startActivity(intent)
            }
            R.id.create_release_keystore -> {
                val intent = Intent(activity, NewKeyStoreActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                activity.startActivity(intent)
            }
            R.id.daydream_settings -> {
                val intent = Intent(activity, DayDreamUniversalSettingsActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                activity.startActivity(intent)
            }
        }
    }

    private fun openUrl(url: String) {
        val activity = unwrap(context) ?: return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        activity.startActivity(intent)
    }

    private fun unwrap(context: Context): Activity? {
        var currentContext: Context? = context
        while (currentContext !is Activity && currentContext is ContextWrapper) {
            currentContext = currentContext.baseContext
        }
        return currentContext as? Activity
    }

    private fun close() {
        var p: ViewParent? = parent
        while (p != null) {
            if (p is DrawerLayout) {
                p.closeDrawer(this)
                return
            }
            p = p.parent
        }
    }
}
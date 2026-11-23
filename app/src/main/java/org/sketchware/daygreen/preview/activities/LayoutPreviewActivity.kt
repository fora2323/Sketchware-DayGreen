package org.sketchware.daygreen.preview.activities

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.graphics.Canvas as AndroidCanvas
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.besome.sketch.beans.ViewBean
import com.besome.sketch.editor.view.ItemView
import com.besome.sketch.editor.view.ViewPane
import com.besome.sketch.lib.base.BaseAppCompatActivity
import com.google.android.material.R as MaterialR
import a.a.a.jC
import a.a.a.mB
import org.sketchware.daygreen.preview.PreviewLayoutParser
import org.sketchware.daygreen.preview.PreviewResourceResolver
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import pro.sketchware.activities.preview.DrawablePreviewRenderer
import pro.sketchware.utility.SketchwareUtil
import java.io.File
import java.io.StringReader

class LayoutPreviewActivity : BaseAppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdgeNoContrast()
        super.onCreate(savedInstanceState)
        // The Compose TopAppBar replaces the framework action bar.
        supportActionBar?.hide()

        val scId = intent.getStringExtra("sc_id").orEmpty()
        val xml = intent.getStringExtra("xml").orEmpty()
        val filePath = intent.getStringExtra("content")
        val type = intent.getStringExtra("type") ?: "layout"
        val subtitle = intent.getStringExtra("title")
        val initialNight =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES

        if (xml.isEmpty()) {
            SketchwareUtil.toastError("content is null")
        }

        setContent {
            PreviewTheme {
                LayoutPreviewScreen(
                    scId = scId,
                    xml = xml,
                    filePath = filePath,
                    previewType = type,
                    subtitle = subtitle,
                    initialNight = initialNight,
                    onBack = { if (!mB.a()) onBackPressedDispatcher.onBackPressed() }
                )
            }
        }
    }
}

/* ---------------------------------------------------------------- Theme */

/**
 * Uses the app theme (Theme.DayGreen.Settings2027) colors so the preview screen
 * matches the rest of the app. Falls back to Material defaults when an attribute is missing.
 */
@Composable
private fun PreviewTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val base = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    val scheme = base.copy(
        primary = context.themeColor(MaterialR.attr.colorPrimary, base.primary),
        onPrimary = context.themeColor(MaterialR.attr.colorOnPrimary, base.onPrimary),
        surface = context.themeColor(MaterialR.attr.colorSurface, base.surface),
        onSurface = context.themeColor(MaterialR.attr.colorOnSurface, base.onSurface),
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

private fun Context.themeColor(attr: Int, fallback: Color): Color {
    val value = TypedValue()
    return if (theme.resolveAttribute(attr, value, true)) Color(value.data) else fallback
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LayoutPreviewScreen(
    scId: String,
    xml: String,
    filePath: String?,
    previewType: String,
    subtitle: String?,
    initialNight: Boolean,
    onBack: () -> Unit,
) {
    var nightMode by rememberSaveable { mutableStateOf(initialNight) }
    val stageBackground = if (nightMode) Color(0xFF1E1E1E) else Color(0xFFE0E0E0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(titleFor(previewType))
                        if (!subtitle.isNullOrEmpty()) {
                            Text(subtitle, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { nightMode = !nightMode }) {
                        Icon(Icons.Filled.Contrast, contentDescription = "Toggle theme")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(stageBackground),
            contentAlignment = Alignment.Center
        ) {
            when (previewType) {
                "drawable", "svg" -> DrawableStage(scId, filePath, xml, nightMode)
                "menu" -> MenuStage(scId, xml, nightMode)
                else -> LayoutStage(scId, xml, nightMode)
            }
        }
    }
}

private fun titleFor(type: String): String = when (type) {
    "layout" -> "Layout Preview"
    "drawable" -> "Drawable Preview"
    "menu" -> "Menu Preview"
    "svg" -> "SVG Preview"
    else -> "Preview"
}

/* ---------------------------------------------------------------- Layout */

@Composable
private fun LayoutStage(scId: String, xml: String, nightMode: Boolean) {
    // No outer scroll: the phone frame stays fixed on screen.
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        PhoneFrame(nightMode = nightMode) {
            // Recreate the pane when the theme toggle changes, so resources resolve again.
            key(nightMode) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context -> createLayoutPane(context, scId, xml, nightMode) }
                )
            }
        }
    }
}

private fun createLayoutPane(context: Context, scId: String, xml: String, nightMode: Boolean): ViewPane {
    val pane = ViewPane(context, null as AttributeSet?)
    try {
        pane.initialize(scId, true)
        pane.setPadding(0, 0, 0, 0)
        pane.setVerticalScrollBarEnabled(false)
        pane.setResourceManager(jC.d(scId))

        val resolver = PreviewResourceResolver.init(scId, nightMode)
        val views = PreviewLayoutParser(xml, resolver).parse()
        loadViews(pane, views)
    } catch (e: Exception) {
        SketchwareUtil.toastError("Preview Error: ${e.message}")
    }
    return pane
}

private fun loadViews(pane: ViewPane, views: List<ViewBean>) {
    views.forEachIndexed { index, view ->
        if (index == 0) {
            view.parent = "root"
            view.parentType = 1
            view.preParent = null
            view.preParentType = -1
            val item = pane.createItemView(view) as? ItemView
            if (item != null) {
                item.setFixed(true)
                pane.setPreviewRoot(item as View)
            }
        } else {
            val child = pane.createItemView(view) as ItemView
            pane.addViewAndUpdateIndex(child as View)
        }
    }
}

/* ---------------------------------------------------------------- Menu */

private data class MenuItemPreview(
    val title: String,
    val icon: ImageBitmap?,
    val showAlways: Boolean,
)

@Composable
private fun MenuStage(scId: String, xml: String, nightMode: Boolean) {
    val menuItems = remember(scId, xml, nightMode) { parseMenuItems(scId, xml, nightMode) }

    PhoneFrame(nightMode = nightMode) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(28.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Menu Preview",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
                menuItems.filter { it.showAlways }.forEach { item ->
                    item.icon?.let {
                        Image(
                            bitmap = it,
                            contentDescription = item.title,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(start = 8.dp)
                        )
                    }
                }
            }

            if (menuItems.isEmpty()) {
                Text(
                    text = "No menu items",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Overflow-style list of every item, like the menu opened from the toolbar.
            menuItems.forEach { item ->
                Text(
                    text = item.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

private fun parseMenuItems(scId: String, xml: String, nightMode: Boolean): List<MenuItemPreview> {
    val result = mutableListOf<MenuItemPreview>()
    try {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "item") {
                var title = ""
                var icon = ""
                var showAsAction = "never"
                for (i in 0 until parser.attributeCount) {
                    when (parser.getAttributeName(i)) {
                        "title" -> title = parser.getAttributeValue(i)
                        "icon" -> icon = parser.getAttributeValue(i)
                        "showAsAction" -> showAsAction = parser.getAttributeValue(i)
                    }
                }
                val bitmap = if (icon.startsWith("@drawable/")) {
                    DrawablePreviewRenderer
                        .getDrawableFromProject(scId, icon.removePrefix("@drawable/"), 0, nightMode)
                        ?.toImageBitmap()
                } else {
                    null
                }
                result += MenuItemPreview(
                    title = title,
                    icon = bitmap,
                    showAlways = showAsAction.contains("always") || showAsAction.contains("ifRoom")
                )
            }
            event = parser.next()
        }
    } catch (e: Exception) {
        SketchwareUtil.toastError("Menu parse error: ${e.message}")
    }
    return result
}

/* ---------------------------------------------------------------- Drawable / SVG */

@Composable
private fun DrawableStage(scId: String, filePath: String?, xml: String, nightMode: Boolean) {
    val preview = remember(scId, filePath, xml, nightMode) {
        val file = filePath?.let { File(it) }
        file
            ?.let { DrawablePreviewRenderer.renderFromContent(scId, it, xml, nightMode) }
            ?.toImageBitmap()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .checkerboard(),
        contentAlignment = Alignment.Center
    ) {
        if (preview != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    bitmap = preview,
                    contentDescription = null,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "${preview.width} × ${preview.height} px",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                text = "Failed to render drawable/SVG",
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/** Light and dark squares behind transparent drawables, like the Android Studio preview. */
private fun Modifier.checkerboard(): Modifier = this.drawBehind {
    val cell = 16.dp.toPx()
    val light = Color(0xFFFFFFFF)
    val dark = Color(0xFFCCCCCC)
    var y = 0f
    var row = 0
    while (y < size.height) {
        var x = 0f
        var col = 0
        while (x < size.width) {
            drawRect(
                color = if ((row + col) % 2 == 0) light else dark,
                topLeft = Offset(x, y),
                size = Size(cell, cell)
            )
            x += cell
            col++
        }
        y += cell
        row++
    }
}

private fun Drawable.toImageBitmap(): ImageBitmap {
    val w = if (intrinsicWidth > 0) intrinsicWidth else 512
    val h = if (intrinsicHeight > 0) intrinsicHeight else 512
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    setBounds(0, 0, w, h)
    draw(canvas)
    return bitmap.asImageBitmap()
}

/* ---------------------------------------------------------------- Phone frame */

@Composable
private fun PhoneFrame(nightMode: Boolean, content: @Composable BoxScope.() -> Unit) {
    val screenColor = if (nightMode) Color(0xFF121212) else Color.White
    val overlayColor = if (nightMode) Color.White else Color.Black

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .widthIn(max = 360.dp)
            .aspectRatio(360f / 780f),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = screenColor),
        border = BorderStroke(8.dp, Color(0xFF212121)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize(), content = content)
            PhoneStatusBar(textColor = overlayColor)
            PhoneNavBar(color = overlayColor)
        }
    }
}

@Composable
private fun BoxScope.PhoneStatusBar(textColor: Color) {
    Row(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .height(28.dp)
            .background(Color(0x33000000))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "12:00",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = textColor.copy(alpha = 0.85f)
        )
        Text(
            text = "4G",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun BoxScope.PhoneNavBar(color: Color) {
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(48.dp)
            .background(Color(0x33000000)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color.copy(alpha = 0.85f))
        )
    }
}
package com.gabriele.notionlocal.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.ScriptGlyph
import com.gabriele.notionlocal.ui.screen.scriptStyleOf
import com.gabriele.notionlocal.ui.theme.DarkSurfaceVariant
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.ui.theme.NotionWhite
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Pedice e apice disegnati: i due pulsanti della barra Aa accanto a
 * B/I/U/S, e un testo con dentro H₂O e mc². L'immagine va in
 * `app/build/widget-screenshots/scripts.png`, da guardare a occhio.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScriptRenderTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun annotated(spans: List<RichTextSpan>): AnnotatedString {
        val builder = AnnotatedString.Builder()
        for (span in spans) {
            val start = builder.length
            builder.append(span.text)
            scriptStyleOf(span)?.let { builder.addStyle(it, start, builder.length) }
        }
        return builder.toAnnotatedString()
    }

    @Test
    fun barAndTextRender() {
        AppSettings.init(ApplicationProvider.getApplicationContext())
        assertNull(scriptStyleOf(RichTextSpan("a")))
        assertNotNull(scriptStyleOf(RichTextSpan("a", subscript = true)))
        assertNotNull(scriptStyleOf(RichTextSpan("a", superscript = true)))

        val text = annotated(
            listOf(
                RichTextSpan("L'acqua è H"), RichTextSpan("2", subscript = true), RichTextSpan("O, e E = mc"),
                RichTextSpan("2", superscript = true), RichTextSpan(". Il 1"), RichTextSpan("o", superscript = true),
                RichTextSpan(" posto, CO"), RichTextSpan("2", subscript = true), RichTextSpan(".")
            )
        )
        compose.setContent {
            NotionLocalTheme {
                Column(modifier = Modifier.width(400.dp).background(DarkSurfaceVariant)) {
                    Row(modifier = Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        val cell = Modifier.size(44.dp)
                        Box(cell, contentAlignment = Alignment.Center) { Text("B", color = NotionWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                        Box(cell, contentAlignment = Alignment.Center) { Text("U", color = NotionWhite, textDecoration = TextDecoration.Underline, fontSize = 18.sp) }
                        Box(cell, contentAlignment = Alignment.Center) { Text("S", color = NotionWhite, textDecoration = TextDecoration.LineThrough, fontSize = 18.sp) }
                        Box(cell, contentAlignment = Alignment.Center) { ScriptGlyph(superscript = false, description = "Subscript") }
                        Box(cell, contentAlignment = Alignment.Center) { ScriptGlyph(superscript = true, description = "Superscript") }
                    }
                    Text(text, color = NotionWhite, fontSize = 16.sp, lineHeight = 24.sp, modifier = Modifier.padding(16.dp))
                }
            }
        }
        compose.waitForIdle()
        val root = compose.activity.window.decorView.rootView as android.view.ViewGroup
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(android.graphics.Canvas(bitmap))
        val probeX = 20
        val empty = bitmap.getPixel(bitmap.width - 1, bitmap.height - 1)
        var bottom = bitmap.height - 1
        while (bottom > 0 && bitmap.getPixel(probeX, bottom) == empty) bottom--
        val cropped = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, (bottom + 1).coerceAtLeast(1))
        val dir = File("build/widget-screenshots").apply { mkdirs() }
        File(dir, "scripts.png").outputStream().use { cropped.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}

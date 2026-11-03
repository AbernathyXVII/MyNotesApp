package com.gabriele.notionlocal.desktop

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.ui.screen.PatchNoteScreen
import com.gabriele.notionlocal.ui.screen.PatchNotesScreen
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import org.jetbrains.skia.Image

/**
 * **PageFlow sul PC** (dal 04/11/2026). Per ora la finestra mostra le Patch
 * Notes, con lo stesso codice e lo stesso tema del telefono (`shared`): il
 * resto dell'app ci arriva un pezzo per volta.
 */
fun main() {
    val context = DesktopContext()
    AppSettings.init(context)
    val icon = PlatformIcon.load()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "PageFlow",
            icon = icon,
            state = rememberWindowState(width = 520.dp, height = 900.dp)
        ) {
            CompositionLocalProvider(LocalPlatformContext provides context) {
                NotionLocalTheme {
                    var version by remember { mutableStateOf<String?>(null) }
                    val open = version
                    if (open == null) {
                        PatchNotesScreen(onBack = {}, onOpenVersion = { version = it })
                    } else {
                        PatchNoteScreen(version = open, onBack = { version = null })
                    }
                }
            }
        }
    }
}

/** L'icona di PageFlow, la stessa del telefono (cartella `branding`). */
private object PlatformIcon {
    fun load(): BitmapPainter? {
        val bytes = javaClass.getResourceAsStream("/pageflow-icon-512.png")?.use { it.readBytes() } ?: return null
        return BitmapPainter(Image.makeFromEncoded(bytes).toComposeImageBitmap())
    }
}

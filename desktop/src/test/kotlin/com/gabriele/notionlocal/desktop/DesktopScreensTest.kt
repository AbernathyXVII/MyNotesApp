package com.gabriele.notionlocal.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import com.gabriele.notionlocal.data.patchnotes.PatchNotesAsset
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.ui.screen.PatchNoteScreen
import com.gabriele.notionlocal.ui.screen.PatchNotesScreen
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import org.jetbrains.skia.EncodedImageFormat
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **Le schermate del PC** (dal 04/11/2026): disegnate in un'immagine, senza
 * aprire finestre, e salvate in `desktop/build/desktop-screenshots` per
 * guardarle. Le impostazioni vanno in una cartella temporanea: quella vera
 * di PageFlow sul PC non si tocca.
 */
class DesktopScreensTest {
    companion object {
        private val context = DesktopContext(Files.createTempDirectory("pageflow-test").toFile())

        @BeforeClass
        @JvmStatic
        fun setUp() = AppSettings.init(context)
    }

    private fun shoot(name: String, content: @Composable () -> Unit): ByteArray {
        val scene = ImageComposeScene(width = 520, height = 900, density = Density(1f)) {
            CompositionLocalProvider(LocalPlatformContext provides context) {
                NotionLocalTheme { content() }
            }
        }
        // Le Patch Notes si leggono fuori dal filo principale: qualche giro di disegno.
        var time = 0L
        repeat(30) {
            scene.render(time)
            time += 100_000_000L
            Thread.sleep(50)
        }
        val png = scene.render(time).encodeToData(EncodedImageFormat.PNG)!!.bytes
        scene.close()
        File("build/desktop-screenshots", name).apply { parentFile.mkdirs() }.writeBytes(png)
        return png
    }

    @Test
    fun thePatchNotesListShowsTheVersionsFromTheReadme() {
        val notes = PatchNotesAsset.load(context)
        assertTrue("le Patch Notes sul PC sono vuote", notes.versions.size > 100)
        shoot("patch_notes.png") { PatchNotesScreen(onBack = {}, onOpenVersion = {}) }
    }

    @Test
    fun aVersionOpens() {
        val first = PatchNotesAsset.load(context).versions.first()
        shoot("patch_note.png") { PatchNoteScreen(version = first.version, onBack = {}) }
    }
}

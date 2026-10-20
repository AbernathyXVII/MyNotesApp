package com.gabriele.notionlocal

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.webclip.WebClipMode
import com.gabriele.notionlocal.data.webclip.WebClipRequest
import com.gabriele.notionlocal.data.webclip.imageSources
import com.gabriele.notionlocal.data.webclip.toNewBlocks
import com.gabriele.notionlocal.ui.screen.StripImage
import com.gabriele.notionlocal.ui.screen.TallImageViewer
import com.gabriele.notionlocal.ui.screen.WebClipCaptureDialog
import com.gabriele.notionlocal.ui.screen.WebClipRegionDialog
import com.gabriele.notionlocal.ui.screen.WebClipResult
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import java.io.File

/**
 * **Solo nella build di debug**: le parti del Web Clipper che si vedono, da
 * sole, senza aprire le pagine dell'utente. Fa la foto con la finestra vera
 * (`WebClipCaptureDialog`), poi la mostra a strisce come nella pagina, o nel
 * visore a tutto schermo con `--es show viewer`. Il resoconto in
 * `files/webclip-probe/ui.txt`; la foto di prova si cancella con `--es clean 1`.
 *
 * adb shell am start -n com.gabriele.notionlocal/.WebClipUiProbeActivity --es url https://… [--es mode article] [--es show viewer]
 */
class WebClipUiProbeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        AppSettings.init(applicationContext)
        val url = intent.getStringExtra("url") ?: "https://example.com"
        val mode = when (intent.getStringExtra("mode")) {
            "article" -> WebClipMode.ARTICLE
            "region" -> WebClipMode.REGION
            "text" -> WebClipMode.TEXT_AND_IMAGES
            else -> WebClipMode.FULL_PAGE
        }
        val viewer = intent.getStringExtra("show") == "viewer"
        val store = PageImageStore(this)
        val dir = File(filesDir, "webclip-probe").apply { mkdirs() }
        val log = File(dir, "ui.txt")
        val last = File(dir, "last-clip.txt")
        if (intent.getStringExtra("clean") != null) {
            last.takeIf { it.exists() }?.readText()?.trim()?.let { store.fileFor(it).delete() }
            last.delete()
            finish()
            return
        }
        val existing = last.takeIf { it.exists() }?.readText()?.trim()?.takeIf { store.fileFor(it).exists() }
        val started = System.currentTimeMillis()
        setContent {
            NotionLocalTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var shown by remember { mutableStateOf(if (viewer || intent.getStringExtra("reuse") != null) existing else null) }
                    var failed by remember { mutableStateOf(false) }
                    val name = shown
                    val done: (WebClipResult) -> Unit = { result ->
                        log.writeText("ok ${(System.currentTimeMillis() - started) / 1000.0}s ${result.image.width}x${result.image.height} title=${result.pageTitle} size=${result.image.file.length()}\n")
                        last.takeIf { it.exists() }?.readText()?.trim()?.let { store.fileFor(it).delete() }
                        last.writeText(result.fileName)
                        shown = result.fileName
                    }
                    val fail = {
                        log.writeText("FALLITA\n")
                        failed = true
                    }
                    when {
                        name == null && !failed && mode == WebClipMode.REGION -> WebClipRegionDialog(
                            request = WebClipRequest(url, mode, "", ""),
                            store = store,
                            onDone = done,
                            onFailed = fail,
                            onCancel = { finish() }
                        )
                        name == null && !failed -> WebClipCaptureDialog(
                            request = WebClipRequest(url, mode, "", ""),
                            store = store,
                            onDone = done,
                            onFailed = fail,
                            onCancel = { finish() },
                            onContent = { result ->
                                // "Text and Images": il resoconto dei blocchi, poi le immagini scaricate via.
                                val blocks = result.content.toNewBlocks(result.images)
                                val summary = StringBuilder("ok ${(System.currentTimeMillis() - started) / 1000.0}s title=${result.content.title}\n")
                                summary.append("pezzi=${result.content.blocks.size} blocchi=${blocks.size} immagini=${result.images.size}/${result.content.imageSources.size}\n")
                                result.images.values.forEach { file ->
                                    summary.append("img ${store.imageSize(file)} ${store.fileFor(file).length()}\n")
                                }
                                blocks.take(40).forEach { b ->
                                    summary.append("${b.type} ind=${b.indentLevel} w=${b.attrs.width} ${b.spans.joinToString("") { s -> (if (s.bold) "*" else "") + (if (s.link != null) "[" else "") + s.text.take(40) + (if (s.link != null) "]" else "") }.take(110)}\n")
                                }
                                log.writeText(summary.toString())
                                result.images.values.forEach { store.fileFor(it).delete() }
                                failed = true
                            }
                        )
                        name == null -> Text("fallita", modifier = Modifier.padding(24.dp))
                        else -> {
                            val size = store.imageSize(name)
                            if (size == null) {
                                Text("niente immagine", modifier = Modifier.padding(24.dp))
                            } else if (viewer) {
                                TallImageViewer(store.fileFor(name), size.first, size.second, "probe", onDismiss = { finish() })
                            } else {
                                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                                    Text("Strisce: ${size.first} x ${size.second}")
                                    StripImage(store.fileFor(name), size.first, size.second, "probe")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

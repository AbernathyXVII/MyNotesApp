package com.gabriele.notionlocal

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import android.widget.FrameLayout
import com.gabriele.notionlocal.data.webclip.WebClipEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

/**
 * **Solo nella build di debug**: prova il motore del Web Clipper da adb, senza
 * toccare le pagine dell'utente. Le immagini e il resoconto vanno in
 * `files/webclip-probe/`, da leggere con `run-as`.
 *
 * adb shell am start -n com.gabriele.notionlocal/.WebClipProbeActivity --es url https://… [--es step variants|full]
 */
class WebClipProbeActivity : Activity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (intent.getBooleanExtra("whole", false)) runCatching { WebView.enableSlowWholeDocumentDraw() }
        val url = intent.getStringExtra("url") ?: "https://example.com"
        val step = intent.getStringExtra("step") ?: "variants"
        val fit = intent.getBooleanExtra("fit", false)
        val engine = if (fit) {
            // Larga quanto lo schermo: 1440 pixel CSS su 1440 pixel veri.
            WebClipEngine(this, outputScale = resources.displayMetrics.widthPixels / WebClipEngine.DESKTOP_CSS_WIDTH.toFloat())
        } else {
            WebClipEngine(this)
        }
        val root = FrameLayout(this)
        root.addView(engine.webView, FrameLayout.LayoutParams(engine.viewWidthPx, engine.viewHeightPx))
        setContentView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        val dir = File(filesDir, "webclip-probe").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
        val log = StringBuilder()
        fun note(line: String) {
            log.append(line).append('\n')
            File(dir, "log.txt").writeText(log.toString())
        }
        fun save(bitmap: Bitmap, name: String) {
            File(dir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        scope.launch {
            val started = SystemClock.elapsedRealtime()
            fun t() = "${(SystemClock.elapsedRealtime() - started) / 1000.0}s"
            note("url $url view ${engine.viewWidthPx}x${engine.viewHeightPx} hw=${engine.webView.isHardwareAccelerated}")
            val finalUrl = engine.load(url)
            note("caricata ${t()} final=$finalUrl metrics=${engine.metrics()}")
            engine.forceDesktopWidth()
            delay(600)
            note("desktop ${t()} metrics=${engine.metrics()} w=${engine.webView.width} h=${engine.webView.height}")
            note("pulizia ${engine.cleanUp()}")
            if (step == "variants") {
                delay(800)
                val w = engine.webView.width
                val h = engine.webView.height
                // a) draw() così com'è.
                runCatching {
                    val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    b.eraseColor(Color.MAGENTA)
                    engine.webView.draw(Canvas(b))
                    save(b, "a_draw.png")
                }.onFailure { note("a ERRORE $it") }
                note("a ${t()}")
                // b) PixelCopy della finestra, dove sta il WebView.
                runCatching {
                    val b = Bitmap.createBitmap(minOf(w, root.width), minOf(h, root.height), Bitmap.Config.ARGB_8888)
                    val location = IntArray(2).also { engine.webView.getLocationInWindow(it) }
                    val result = suspendCancellableCoroutine { c ->
                        PixelCopy.request(
                            window,
                            android.graphics.Rect(location[0], location[1], location[0] + b.width, location[1] + b.height),
                            b,
                            { r -> c.resume(r) },
                            Handler(Looper.getMainLooper())
                        )
                    }
                    note("b PixelCopy result=$result size=${b.width}x${b.height}")
                    save(b, "b_pixelcopy.png")
                }.onFailure { note("b ERRORE $it") }
                note("b ${t()}")
                // c) capturePicture (vecchio, ma disegna tutto il documento).
                runCatching {
                    @Suppress("DEPRECATION")
                    val picture = engine.webView.capturePicture()
                    note("c picture ${picture.width}x${picture.height}")
                    val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    b.eraseColor(Color.MAGENTA)
                    val canvas = Canvas(b)
                    canvas.translate(0f, -h * 2f)
                    picture.draw(canvas)
                    save(b, "c_picture_window3.png")
                }.onFailure { note("c ERRORE $it") }
                note("c ${t()}")
                // d) software layer, poi draw().
                runCatching {
                    engine.webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                    delay(800)
                    val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    b.eraseColor(Color.MAGENTA)
                    engine.webView.draw(Canvas(b))
                    save(b, "d_software.png")
                    engine.webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                }.onFailure { note("d ERRORE $it") }
                note("d ${t()}")
            } else {
                engine.loadLazyContent()
                note("scorsa ${t()} metrics=${engine.metrics()} pulizia2=${engine.cleanUp()}")
                val image = runCatching { engine.captureFullPage(File(dir, "clip.jpg")) }.onFailure { note("ERRORE $it") }.getOrNull()
                note("foto ${t()} $image size=${File(dir, "clip.jpg").length()}")
            }
            note("FINE")
            root.removeView(engine.webView)
            engine.destroy()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}

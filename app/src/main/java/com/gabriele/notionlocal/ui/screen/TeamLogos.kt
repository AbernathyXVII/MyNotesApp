package com.gabriele.notionlocal.ui.screen

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import com.gabriele.notionlocal.data.widgets.MINUTE_MS
import com.gabriele.notionlocal.data.widgets.WidgetHttp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * **Gli stemmi delle squadre** (dal 17/10/2026, chiesto dall'utente), e nel
 * tennis le bandiere dei giocatori: immagini di ESPN, scaricate piccole
 * (64×64, 4 KB invece dei 65 dell'originale) e tenute nella cache
 * dell'app, così ogni stemma si scarica una volta sola. Degli stemmi si
 * prende la versione per i fondi scuri, se c'è: quello della Juventus è una
 * J nera, che sulla barra scura non si vedeva.
 */
internal object TeamLogos {

    /** Nei test niente rete: gli stemmi restano vuoti. */
    @Volatile
    var enabled = true

    private val memory = LruCache<String, ImageBitmap>(300)
    private val pending = ConcurrentHashMap<String, Deferred<ImageBitmap?>>()
    private val failedAt = ConcurrentHashMap<String, Long>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun cached(url: String): ImageBitmap? = if (url.isBlank()) null else memory.get(url)

    suspend fun load(cacheDir: File, url: String): ImageBitmap? {
        if (url.isBlank()) return null
        memory.get(url)?.let { return it }
        if (!enabled) return null
        // Uno che non è arrivato si riprova dopo dieci minuti.
        failedAt[url]?.let { if (System.currentTimeMillis() - it < 10 * MINUTE_MS) return null }
        val job = pending.computeIfAbsent(url) { scope.async { fetch(cacheDir, url) } }
        val bitmap = job.await()
        pending.remove(url, job)
        if (bitmap != null) memory.put(url, bitmap) else failedAt[url] = System.currentTimeMillis()
        return bitmap
    }

    private fun fetch(cacheDir: File, url: String): ImageBitmap? {
        val file = File(File(cacheDir, "team_logos").apply { mkdirs() }, sha1("dark:$url") + ".png")
        if (file.exists()) {
            decode(file.readBytes())?.let { return it }
            file.delete()
        }
        for (candidate in listOf(darkUrl(url), url).distinct()) {
            val bytes = WidgetHttp.bytes(smallUrl(candidate), maxBytes = 512 * 1024, accept = "image/*") ?: continue
            val bitmap = decode(bytes) ?: continue
            runCatching { file.writeBytes(bytes) }
            return bitmap
        }
        return null
    }

    /** La versione per i fondi scuri ("500-dark"), che ESPN ha per gli stemmi; le bandiere no. */
    fun darkUrl(url: String): String =
        if (url.contains("/i/teamlogos/") && !url.contains("/countries/")) url.replaceFirst("/500/", "/500-dark/") else url

    private fun decode(bytes: ByteArray): ImageBitmap? =
        runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }.getOrNull()

    /** Il servizio di ESPN che rimpicciolisce le immagini: lo stemma a 64×64. */
    fun smallUrl(url: String): String {
        val host = "https://a.espncdn.com/"
        return if (url.startsWith(host) && !url.startsWith(host + "combiner/")) {
            host + "combiner/i?img=/" + url.removePrefix(host) + "&w=64&h=64"
        } else {
            url
        }
    }

    private fun sha1(text: String): String =
        MessageDigest.getInstance("SHA-1").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
}

/** Lo stemma (o la bandiera) in un quadrato di [size]; vuoto finché non arriva, o se non c'è. */
@Composable
internal fun TeamLogo(url: String, size: Dp, modifier: Modifier = Modifier) {
    val cacheDir = LocalContext.current.cacheDir
    val bitmap by produceState(initialValue = TeamLogos.cached(url), url) {
        if (value == null) value = TeamLogos.load(cacheDir, url)
    }
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        bitmap?.let { Image(bitmap = it, contentDescription = null, modifier = Modifier.fillMaxSize()) }
    }
}

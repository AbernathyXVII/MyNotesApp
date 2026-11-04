package com.gabriele.notionlocal.data

import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.PlatformContext
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import org.jetbrains.skia.FilterMipmap
import org.jetbrains.skia.FilterMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.MipmapMode
import org.jetbrains.skia.Rect
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Sul PC: la `Bitmap` di Skia (per mostrarla, `asComposeImageBitmap()`). */
actual typealias PlatformBitmap = Bitmap

actual fun applicationContextOf(context: PlatformContext): PlatformContext = context

/** Sul PC i file stanno nella cartella dei dati di PageFlow: `page-images` è lì dentro. */
actual fun filesDirOf(context: PlatformContext): File = (context as DesktopContext).dataDir

private fun codecOf(file: File): Codec = Codec.makeFromData(Data.makeFromBytes(file.readBytes()))

actual fun imageBoundsOf(file: File): Pair<Int, Int>? = runCatching {
    codecOf(file).use { codec -> (codec.width to codec.height).takeIf { it.first > 0 && it.second > 0 } }
}.getOrNull()

actual fun imageIsTurned(file: File): Boolean = runCatching {
    codecOf(file).use { it.encodedOrigin.swapsWidthHeight() }
}.getOrDefault(false)

/**
 * Sul PC: i pixel come li ha scritti la fotocamera, poi girati e rimpiccioliti
 * insieme, disegnandoli una volta sola nell'immagine da guardare. Come sul
 * telefono, il file non si tocca.
 */
actual fun decodeImage(file: File, targetWidthPx: Int, maxPixels: Long): PlatformBitmap? = codecOf(file).use { codec ->
    val raw = codec.readPixels()
    val origin = codec.encodedOrigin
    val (width, height) = if (origin.swapsWidthHeight()) codec.height to codec.width else codec.width to codec.height
    if (width <= 0 || height <= 0) return@use null
    var scale = minOf(1f, targetWidthPx.toFloat() / width)
    if (width * scale * height * scale > maxPixels) scale = sqrt(maxPixels.toFloat() / (width.toFloat() * height))
    val out = Bitmap().apply { allocN32Pixels(max(1, (width * scale).roundToInt()), max(1, (height * scale).roundToInt())) }
    Canvas(out).use { canvas ->
        canvas.scale(scale, scale)
        // Le misure dell'immagine già girata: con quelle salvate nel file la
        // foto finiva fuori dal quadro (una 40×20 girata va in 20×40).
        canvas.concat(origin.toMatrix(width, height))
        val image = Image.makeFromBitmap(raw)
        val whole = Rect.makeWH(codec.width.toFloat(), codec.height.toFloat())
        canvas.drawImageRect(image, whole, whole, FilterMipmap(FilterMode.LINEAR, MipmapMode.LINEAR), null, true)
        image.close()
    }
    raw.close()
    out
}

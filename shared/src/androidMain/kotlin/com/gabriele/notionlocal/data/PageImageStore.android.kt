package com.gabriele.notionlocal.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.gabriele.notionlocal.platform.PlatformContext
import java.io.File

/** Sul telefono: la `Bitmap` di Android. */
actual typealias PlatformBitmap = Bitmap

actual fun applicationContextOf(context: PlatformContext): PlatformContext = context.applicationContext

actual fun filesDirOf(context: PlatformContext): File = context.filesDir

/** Copia dentro l'app l'immagine scelta dalla galleria. */
suspend fun PageImageStore.saveFromGallery(uri: Uri): Result<String> = saveFromStream(
    open = { appContext.contentResolver.openInputStream(uri) },
    mustBeImage = true,
    cannotOpen = "L'immagine scelta non si riesce ad aprire."
)

/**
 * **Copia dentro l'app un file qualsiasi** scelto dal telefono (dal
 * 06/10/2026): il video, l'audio o il file di un blocco. Tiene la sua
 * estensione, perché l'app che lo aprirà lo riconosca. Restituisce il
 * nome del file nell'app.
 */
suspend fun PageImageStore.saveFile(uri: Uri, extension: String?): Result<String> = saveFromStream(
    open = { appContext.contentResolver.openInputStream(uri) },
    extension = cleanExtension(extension),
    mustBeImage = false,
    cannotOpen = "Il file scelto non si riesce ad aprire."
)

actual fun imageBoundsOf(file: File): Pair<Int, Int>? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    return if (bounds.outWidth > 0 && bounds.outHeight > 0) bounds.outWidth to bounds.outHeight else null
}

actual fun imageIsTurned(file: File): Boolean = orientationOf(file) in setOf(
    ExifInterface.ORIENTATION_ROTATE_90,
    ExifInterface.ORIENTATION_ROTATE_270,
    ExifInterface.ORIENTATION_TRANSPOSE,
    ExifInterface.ORIENTATION_TRANSVERSE
)

private fun orientationOf(file: File): Int = runCatching {
    ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
}.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

actual fun decodeImage(file: File, targetWidthPx: Int, maxPixels: Long): PlatformBitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    if (bounds.outWidth <= 0) return null
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= targetWidthPx) {
        sample *= 2
    }
    // Una copertina ingrandita chiede più pixel di quanti ne
    // stia sullo schermo, ed è giusto: è lì che si vede la
    // differenza fra nitido e sgranato. Ma chiederne quanti ne
    // ha una foto da dodici megapixel vuol dire quarantotto
    // megabyte in memoria per una striscia alta due
    // centimetri, e l'app si chiude. Oltre il tetto si scende
    // di un passo: il file resta quello che è, a essere meno
    // fine è solo la copia che si guarda.
    while ((bounds.outWidth / sample).toLong() * (bounds.outHeight / sample) > maxPixels) {
        sample *= 2
    }
    val decoded = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
    return decoded?.let { uprightOf(it, file) }
}

/**
 * L'immagine girata come va guardata.
 *
 * Le foto scattate col telefono ruotato **non vengono salvate
 * ruotate**: i pixel restano come li ha letti il sensore e dentro
 * al file c'è un'etichetta (EXIF) che dice di quanto girarli. La
 * galleria la legge, `BitmapFactory` no — ed è per questo che una
 * copertina presa dalla galleria si vedeva coricata di novanta
 * gradi. Qui l'etichetta viene letta e applicata una volta sola,
 * quando l'immagine si carica.
 *
 * Il file non viene toccato: gira la copia che si guarda.
 */
private fun uprightOf(bitmap: Bitmap, file: File): Bitmap {
    val matrix = Matrix()
    when (orientationOf(file)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        // Le foto "specchiate" sono rare ma esistono (selfie di
        // certe fotocamere): costano due righe e senza di loro
        // uscirebbero al contrario.
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postRotate(90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postRotate(270f)
            matrix.postScale(-1f, 1f)
        }
        else -> return bitmap
    }

    return runCatching {
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            .also { if (it != bitmap) bitmap.recycle() }
    }.getOrDefault(bitmap)
}

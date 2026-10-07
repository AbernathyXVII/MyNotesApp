package com.gabriele.notionlocal.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Le immagini delle pagine — icone e copertine — dentro la cartella
 * dell'app.
 *
 * Arrivano solo dalla galleria del telefono, e ne viene fatta una copia:
 * l'immagine resta anche se l'originale viene cancellato. (Fino al
 * 25/09/2026 si potevano anche scaricare da un link: tolto su richiesta
 * dell'utente.)
 *
 * Nel database si salva il **nome** del file e non il percorso intero:
 * la cartella dell'app può cambiare posto fra un'installazione e
 * l'altra, o al ripristino di un backup, e un percorso scritto dentro
 * il database diventerebbe un puntatore a niente.
 */
class PageImageStore(context: Context) {

    private val appContext = context.applicationContext
    private val directory: File
        get() = File(appContext.filesDir, DIRECTORY).apply { mkdirs() }

    fun fileFor(name: String): File = File(directory, name)

    /** Copia dentro l'app l'immagine scelta dalla galleria. */
    suspend fun saveFromGallery(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        val name = newName()
        val target = File(directory, name)
        runCatching {
            appContext.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "L'immagine scelta non si riesce ad aprire." }
                target.outputStream().use { output -> input.copyTo(output) }
            }
            require(isImage(target)) {
                "Quel file non sembra un'immagine."
            }
            name
        // Una copia non riuscita (o che non è un'immagine) non deve
        // restare nella cartella. Fino al 25/09/2026 qui si cancellava un
        // file che aveva per nome il messaggio d'errore, cioè nessuno.
        }.onFailure { target.delete() }
    }

    /**
     * **Copia dentro l'app un file qualsiasi** scelto dal telefono (dal
     * 06/10/2026): il video, l'audio o il file di un blocco. Tiene la sua
     * estensione, perché l'app che lo aprirà lo riconosca. Restituisce il
     * nome del file nell'app.
     */
    suspend fun saveFile(uri: Uri, extension: String?): Result<String> = withContext(Dispatchers.IO) {
        val name = newName(extension?.lowercase()?.filter { it.isLetterOrDigit() }?.takeIf { it.isNotEmpty() } ?: "bin")
        val target = File(directory, name)
        runCatching {
            appContext.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Il file scelto non si riesce ad aprire." }
                target.outputStream().use { output -> input.copyTo(output) }
            }
            name
        }.onFailure { target.delete() }
    }

    /**
     * Una copia del file con un nome suo, per una pagina duplicata.
     *
     * Le due pagine **non possono condividere lo stesso file**: cambiando
     * l'icona di una, o cancellandola per sempre, il file se ne va dalla
     * cartella, e l'altra resterebbe con un'immagine che non c'è più.
     * Null se il file non c'è: la copia nasce senza immagine.
     */
    suspend fun copy(name: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val source = File(directory, name)
            if (!source.exists()) return@runCatching null
            // Con la sua estensione: un video copiato resta un ".mp4", e
            // l'app che lo apre lo riconosce (dal 06/10/2026, coi file dei blocchi).
            val copyName = newName(source.extension.ifBlank { IMAGE_EXTENSION })
            source.copyTo(File(directory, copyName))
            copyName
        }.getOrNull()
    }

    /**
     * Toglie un file che non serve più. Un'icona sostituita lascerebbe
     * altrimenti il vecchio file nella cartella per sempre, senza che
     * nessuno possa più arrivarci.
     */
    suspend fun delete(name: String?) = withContext(Dispatchers.IO) {
        if (name.isNullOrBlank()) return@withContext
        runCatching { File(directory, name).delete() }
        Unit
    }

    /**
     * Legge l'immagine già rimpicciolita a quanto serve davvero.
     *
     * Una foto da dodici megapixel in memoria sono quarantotto
     * megabyte: caricarla intera per mostrarla larga un centimetro
     * farebbe chiudere l'app, e non si vedrebbe meglio.
     */
    suspend fun load(name: String, targetWidthPx: Int): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(directory, name)
            if (!file.exists()) return@runCatching null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            if (bounds.outWidth <= 0) return@runCatching null
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= targetWidthPx.coerceAtLeast(1)) {
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
            while (
                (bounds.outWidth / sample).toLong() * (bounds.outHeight / sample) > MAX_PIXELS
            ) {
                sample *= 2
            }
            val decoded = BitmapFactory.decodeFile(
                file.path,
                BitmapFactory.Options().apply { inSampleSize = sample }
            )
            decoded?.let { uprightOf(it, file) }
        }.getOrNull()
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
        val orientation = runCatching {
            ExifInterface(file.path).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val matrix = Matrix()
        when (orientation) {
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

    /** Tutte le immagini delle pagine, via: per "Elimina tutto il contenuto" (vedi `AppCleanup`). */
    fun deleteAll() {
        directory.listFiles()?.forEach { it.delete() }
    }

    /** Decodifica i soli contorni: se non sono quelli di un'immagine, non lo è. */
    private fun isImage(file: File): Boolean {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        return bounds.outWidth > 0 && bounds.outHeight > 0
    }

    private fun newName(extension: String = IMAGE_EXTENSION) = "${UUID.randomUUID()}.$extension"

    private companion object {
        const val DIRECTORY = "page-images"
        const val IMAGE_EXTENSION = "img"

        /** Sei megapixel in memoria sono ventiquattro megabyte: è il tetto. */
        const val MAX_PIXELS = 6_000_000L
    }
}

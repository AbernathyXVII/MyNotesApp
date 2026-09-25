package com.gabriele.notionlocal.data

import android.content.Context
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.notify.NoteAlarms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * **Le due voci in fondo alle impostazioni** (dal 26/09/2026): svuotare la
 * cache e cancellare tutto il contenuto.
 */
object AppCleanup {

    /** Quanto occupa la cache dell'app, in byte. */
    fun cacheBytes(context: Context): Long =
        cacheDirs(context).sumOf { sizeOf(it) }

    /**
     * **Svuota la cache**: i file temporanei che l'app (o Android per lei)
     * può rifare quando servono. Le note non stanno lì: pagine, database e
     * disegni sono nel database, le immagini nella cartella dei file.
     */
    suspend fun clearCache(context: Context) = withContext(Dispatchers.IO) {
        cacheDirs(context).forEach { dir -> dir.listFiles()?.forEach { it.deleteRecursively() } }
    }

    /**
     * **Cancella tutto il contenuto**: ogni pagina, database, riga,
     * disegno, la cronologia delle modifiche e il cestino, e le immagini
     * delle pagine. Restano le impostazioni e i widget della barra, che
     * sono scelte e strumenti, non contenuto.
     *
     * La pagina principale rinasce subito, vuota: tutto il resto dell'app
     * dà per scontato che ci sia (vedi `PageEntity.ROOT_PAGE_ID`). Dopo si
     * rimette la sveglia delle notifiche, che non ha più niente da suonare.
     * Chi chiama deve far ripartire l'app: le schermate aperte hanno
     * ancora in mano quello che c'era.
     */
    suspend fun deleteAllContent(context: Context) = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        db.clearAllTables()
        db.pageDao().insert(PageEntity(id = PageEntity.ROOT_PAGE_ID, title = "My Space", icon = "🏠"))
        PageImageStore(context).deleteAll()
        NoteAlarms.reschedule()
    }

    private fun cacheDirs(context: Context): List<File> =
        listOfNotNull(context.cacheDir, context.externalCacheDir)

    private fun sizeOf(file: File): Long =
        if (file.isDirectory) file.listFiles()?.sumOf { sizeOf(it) } ?: 0L else file.length()
}

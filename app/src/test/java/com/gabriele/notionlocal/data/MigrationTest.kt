package com.gabriele.notionlocal.data

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **La migrazione 28→29 su un database vero**, aperto dall'app com'è.
 *
 * Si costruisce un file alla versione 28 con lo schema che Room generava
 * allora (`schema/v28.sql`, preso dal codice generato prima delle viste
 * collegate), con dentro una pagina; poi lo si apre con
 * `AppDatabase.getInstance`, che è quello che fa il telefono
 * all'aggiornamento. Room esegue la migrazione e **controlla lo schema
 * che ne esce contro quello che si aspetta**: se una colonna mancasse o
 * fosse di un tipo diverso, l'apertura fallirebbe qui invece che sul
 * telefono dell'utente.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class MigrationTest {

    @Test
    fun from28To29TheAppOpensAndThePagesAreStillThere() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = context.getDatabasePath("notion_local.db")
        file.parentFile?.mkdirs()
        file.delete()

        val old = SQLiteDatabase.openOrCreateDatabase(file, null)
        javaClass.classLoader!!.getResourceAsStream("schema/v28.sql")!!.bufferedReader().readLines()
            .filter { it.isNotBlank() && !it.startsWith("--") }
            .forEach { old.execSQL(it) }
        old.version = 28
        old.close()

        val db = AppDatabase.getInstance(context)
        val page = db.pageDao().getById("p1")!!
        assertEquals("Vecchia pagina", page.title)
        assertTrue(page.isDatabase)
        // Le due colonne nuove ci sono, vuote: nessuna pagina di prima era una vista.
        assertNull(page.sourceDatabaseId)
        assertNull(page.viewHiddenColumnIds)
        assertEquals(29, db.openHelper.readableDatabase.version)
        db.close()
    }
}

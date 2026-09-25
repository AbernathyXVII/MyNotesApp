package com.gabriele.notionlocal.data

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.entity.PageFont
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **Le migrazioni su un database vero**, aperto dall'app com'è.
 *
 * Si costruisce un file a una versione vecchia con lo schema che Room
 * generava allora (`schema/vNN.sql`, preso dal codice generato a quel
 * commit), con dentro qualche pagina; poi lo si apre con
 * `AppDatabase.getInstance`, che è quello che fa il telefono
 * all'aggiornamento. Room esegue le migrazioni e **controlla lo schema
 * che ne esce contro quello che si aspetta**: se una colonna mancasse o
 * fosse di un tipo diverso, l'apertura fallirebbe qui invece che sul
 * telefono dell'utente.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class MigrationTest {

    @After
    fun tearDown() {
        AppDatabase.resetForTests()
    }

    /** Un file alla versione `version`, fatto con le istruzioni di `schema/v<version>.sql`. */
    private fun databaseAtVersion(version: Int): Context {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = context.getDatabasePath("notion_local.db")
        file.parentFile?.mkdirs()
        file.delete()

        val old = SQLiteDatabase.openOrCreateDatabase(file, null)
        javaClass.classLoader!!.getResourceAsStream("schema/v$version.sql")!!.bufferedReader().readLines()
            .filter { it.isNotBlank() && !it.startsWith("--") }
            .forEach { old.execSQL(it) }
        old.version = version
        old.close()
        return context
    }

    @Test
    fun from28TheAppOpensAndThePagesAreStillThere() = runBlocking<Unit> {
        val db = AppDatabase.getInstance(databaseAtVersion(28))
        val page = db.pageDao().getById("p1")!!
        assertEquals("Vecchia pagina", page.title)
        assertTrue(page.isDatabase)
        // Le colonne nuove ci sono, vuote: nessuna pagina di prima era una
        // vista, e nessuna scheda aveva un'inquadratura sua.
        assertNull(page.sourceDatabaseId)
        assertNull(page.viewHiddenColumnIds)
        assertNull(page.cardCoverScale)
        assertEquals(30, db.openHelper.readableDatabase.version)
    }

    @Test
    fun from29MsYaHeiGoesBackToTheSystemFontAndTheOtherFontsStay() = runBlocking<Unit> {
        val db = AppDatabase.getInstance(databaseAtVersion(29))
        // Letta senza chiudere l'app: "MS_YAHEI" non esiste più nell'enum.
        val yahei = db.pageDao().getById("p1")!!
        assertEquals("Pagina in MS YaHei", yahei.title)
        assertNull(yahei.pageFont)
        assertEquals(PageFont.SONGTI, db.pageDao().getById("p2")!!.pageFont)
        assertNull(yahei.cardCoverScale)
        assertNull(yahei.cardCoverOffsetX)
        assertNull(yahei.cardCoverOffsetY)
        assertEquals(30, db.openHelper.readableDatabase.version)
    }
}

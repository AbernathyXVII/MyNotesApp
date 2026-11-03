package com.gabriele.notionlocal.data.ocr

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.entity.BlockAttrs
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.attrs
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.ui.screen.elapsedText
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * **Registrare, scansionare, cercare nelle foto** (03/11/2026, chiesto
 * dall'utente: "registrare l'audio dall'app e scansionare documenti con la
 * fotocamera, come Evernote, magari con la ricerca dentro il testo delle
 * foto"): dove vanno la registrazione e le pagine scansionate, e come il
 * testo delle foto si legge una volta e si trova con la ricerca.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CaptureTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: PageRepository
    private lateinit var store: PageImageStore
    private val qui = PageEntity(id = "qui", title = "Qui")
    private val altrove = PageEntity(id = "altrove", title = "Scontrini")

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PageRepository(db)
        repository.createPage(qui)
        repository.createPage(altrove)
        store = PageImageStore(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun text(t: String) = """[{"text":"$t"}]"""

    private fun stored(pageId: String) =
        runBlocking { db.blockDao().getBlocksForPageOnce(pageId).filter { it.parentBlockId == null }.sortedBy { it.orderIndex } }

    /** Una vera immagine nell'app, col nome [name]. */
    private fun photo(name: String) {
        val bitmap = Bitmap.createBitmap(40, 30, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        store.fileFor(name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Un lettore finto: il testo di ogni foto lo decide il test, e conta quante ne legge. */
    private class FakeReader(var answer: String?, var complete: Boolean = true) : TextReader {
        var reads = 0
        override suspend fun read(image: Bitmap): ReadText? {
            reads++
            return answer?.let { ReadText(it, complete) }
        }
    }

    @Test
    fun aFinishedRecordingGoesIntoItsBlock() = runBlocking {
        db.blockDao().insert(BlockEntity(id = "a", pageId = qui.id, type = BlockType.AUDIO, orderIndex = 0))
        val audio = BlockAttrs(file = "r.m4a", name = "Registrazione", mime = "audio/mp4", size = 10)
        assertTrue(repository.putRecording("a", qui.id, audio))
        assertEquals("r.m4a", db.blockDao().getById("a")!!.attrs.file)
        // Il blocco ha già il suo file: la seconda va in un blocco nuovo, subito sotto.
        assertTrue(repository.putRecording("a", qui.id, audio.copy(file = "s.m4a")))
        assertEquals(listOf("r.m4a", "s.m4a"), stored(qui.id).map { it.attrs.file })
        // Il blocco non c'è più: in fondo alla pagina.
        db.blockDao().delete(db.blockDao().getById("a")!!)
        assertTrue(repository.putRecording("a", qui.id, audio.copy(file = "t.m4a")))
        assertEquals(listOf("s.m4a", "t.m4a"), stored(qui.id).map { it.attrs.file })
        assertTrue(stored(qui.id).all { it.type == BlockType.AUDIO })
        // Nemmeno la pagina: niente da fare, il file si butta.
        assertFalse(repository.putRecording("a", "sparita", audio))
    }

    @Test
    fun scannedPagesTakeAnEmptyLineOrGoBelow() = runBlocking {
        db.blockDao().insert(BlockEntity(id = "vuota", pageId = qui.id, textJson = text(""), orderIndex = 0))
        db.blockDao().insert(BlockEntity(id = "dopo", pageId = qui.id, textJson = text("Dopo"), orderIndex = 1))
        val ids = repository.insertScannedPages("vuota", listOf("p1.img", "p2.img"), listOf("Scan (1).jpg", "Scan (2).jpg"))
        assertEquals("vuota", ids.first())
        val page = stored(qui.id)
        assertEquals(listOf(BlockType.IMAGE, BlockType.IMAGE, BlockType.PARAGRAPH), page.map { it.type })
        assertEquals(listOf("p1.img", "p2.img", null), page.map { it.attrs.file })
        assertEquals("Scan (2).jpg", page[1].attrs.name)
        // Una riga col testo resta, e le pagine vanno sotto di lei.
        repository.insertScannedPages("dopo", listOf("p3.img"), listOf(null))
        assertEquals(listOf("p1.img", "p2.img", null, "p3.img"), stored(qui.id).map { it.attrs.file })
        assertEquals(text("Dopo"), stored(qui.id)[2].textJson)
    }

    @Test
    fun eachPhotoIsReadOnceAndFoundBySearch() = runBlocking {
        photo("bar.img")
        photo("vuota.img")
        db.blockDao().insert(BlockEntity(pageId = altrove.id, type = BlockType.IMAGE, attrsJson = BlockAttrs.encode(BlockAttrs(file = "bar.img")), orderIndex = 0))
        db.blockDao().insert(BlockEntity(pageId = qui.id, type = BlockType.IMAGE, attrsJson = BlockAttrs.encode(BlockAttrs(file = "manca.img")), orderIndex = 0))
        val reader = FakeReader("CAFFÈ CENTRALE\nTotale 3,50 EUR")
        assertEquals(1, ImageTexts.readPending(db, store, reader))
        // Già letta: la volta dopo non si rilegge. Il file che non c'è si salta.
        assertEquals(0, ImageTexts.readPending(db, store, reader))
        assertEquals(1, reader.reads)
        assertNull(db.imageTextDao().get("manca.img"))

        val hit = repository.search("totale", inContent = true, inTrash = false).single()
        assertEquals(altrove.id, hit.pageId)
        assertTrue(hit.snippet!!.startsWith("🖼 "))
        assertEquals("totale", hit.snippet!!.substring(hit.matchStart, hit.matchStart + hit.matchLength).lowercase())
        // Il titolo trova ancora la pagina come sempre; una parola che non c'è, niente.
        assertTrue(repository.search("pizzeria", inContent = true, inTrash = false).isEmpty())
    }

    @Test
    fun aPhotoTheReaderCannotReadYetStaysToRead() = runBlocking {
        photo("dopo.img")
        db.blockDao().insert(BlockEntity(pageId = qui.id, type = BlockType.IMAGE, attrsJson = BlockAttrs.encode(BlockAttrs(file = "dopo.img")), orderIndex = 0))
        val reader = FakeReader(null)
        assertEquals(0, ImageTexts.readPending(db, store, reader))
        // Letta a metà (il coreano non c'era ancora): il testo si cerca già, e la volta dopo si rilegge.
        reader.answer = "Menu"
        reader.complete = false
        assertEquals(1, ImageTexts.readPending(db, store, reader))
        assertEquals(1, repository.search("menu", inContent = true, inTrash = false).size)
        reader.answer = "Menu\n메뉴"
        reader.complete = true
        assertEquals(1, ImageTexts.readPending(db, store, reader))
        assertEquals("Menu\n메뉴", db.imageTextDao().get("dopo.img")!!.text)
        assertEquals(0, ImageTexts.readPending(db, store, reader))
    }

    @Test
    fun theTextGoesWithThePageDeletedForever() = runBlocking {
        photo("via.img")
        db.blockDao().insert(BlockEntity(pageId = altrove.id, type = BlockType.IMAGE, attrsJson = BlockAttrs.encode(BlockAttrs(file = "via.img")), orderIndex = 0))
        ImageTexts.readPending(db, store, FakeReader("Ricevuta"))
        assertTrue("via.img" in repository.deletePermanently(altrove.id))
        assertNull(db.imageTextDao().get("via.img"))
    }

    @Test
    fun theRecordingTimeReadsLikeAClock() {
        assertEquals("0:00", elapsedText(0))
        assertEquals("0:42", elapsedText(42_900))
        assertEquals("12:05", elapsedText((12 * 60 + 5) * 1000L))
        assertEquals("1:02:33", elapsedText(((60 + 2) * 60 + 33) * 1000L))
    }
}

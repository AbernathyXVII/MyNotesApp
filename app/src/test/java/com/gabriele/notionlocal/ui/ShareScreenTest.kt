package com.gabriele.notionlocal.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.attrs
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.share.QuickNotes
import com.gabriele.notionlocal.ui.navigation.AppNavHost
import com.gabriele.notionlocal.data.share.Incoming
import com.gabriele.notionlocal.ui.screen.SHARE_SHEET_TAG
import com.gabriele.notionlocal.ui.screen.ShareSheet
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/**
 * **"Condividi con notE"** (02/11/2026): la finestra che sale sopra Chrome o
 * WhatsApp, e quello che salva. La finestra si prova in uno schermo di prova,
 * come le altre: lanciata in `ShareActivity` con `ActivityScenario`, Robolectric
 * la lasciava appesa e i test dopo non trovavano più lo schermo fermo.
 * `ShareActivity` legge l'intent (vedi `IncomingTest`) e mostra questa.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h800dp-mdpi")
class ShareScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db get() = AppDatabase.getInstance(context)

    @Before
    fun setUp() = runBlocking {
        AppSettings.init(context)
        AppSettings.sharePageId = null
        AppDatabase.resetForTests()
        context.getDatabasePath("notion_local.db").delete()
        db.pageDao().insert(PageEntity(id = PageEntity.ROOT_PAGE_ID, title = "My Space"))
        db.pageDao().insert(PageEntity(id = "spesa", title = "Spesa"))
        db.blockDao().insert(BlockEntity(pageId = PageEntity.ROOT_PAGE_ID, type = BlockType.PAGE_LINK, linkedPageId = "spesa", orderIndex = 0))
    }

    @After
    fun tearDown() {
        AppSettings.sharePageId = null
        AppDatabase.resetForTests()
    }

    /** Come finisce: "chiusa" (Save) o la pagina da aprire (Save and open). */
    private var closed = false
    private var opened: String? = null

    private fun share(text: String, subject: String? = null) {
        val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        if (subject != null) intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        val incoming = Incoming.from(intent)!!
        val factory = ViewModelFactory(context)
        compose.setContent {
            NotionLocalTheme {
                ShareSheet(incoming = incoming, factory = factory, onClose = { closed = true }, onOpen = { opened = it })
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(SHARE_SHEET_TAG).fetchSemanticsNodes().isNotEmpty() }
    }

    /** Aspetta, lasciando lavorare il thread dello schermo: è lì che il salvataggio finisce. */
    private fun waitFor(condition: () -> Boolean) {
        compose.waitUntil(10_000) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(20)
            condition()
        }
    }

    private fun linksIn(pageId: String) = runBlocking {
        db.blockDao().getBlocksForPageOnce(pageId).sortedBy { it.orderIndex }.mapNotNull { it.linkedPageId }
    }

    @Test
    fun aLinkFromChromeGoesToTheInboxAsAPageWithItsBookmark() {
        share("https://www.example.com/articolo", "Un articolo").let {
            assertTrue(compose.onAllNodesWithText("Un articolo").fetchSemanticsNodes().isNotEmpty())
            assertTrue(compose.onAllNodesWithText("example.com").fetchSemanticsNodes().isNotEmpty())
            compose.onNodeWithText("Save").performClick()
            waitFor { AppSettings.sharePageId != null && linksIn(AppSettings.sharePageId!!).isNotEmpty() }
            val inbox = AppSettings.sharePageId!!
            runBlocking {
                assertEquals("Inbox", db.pageDao().getById(inbox)!!.title)
                // L'Inbox in fondo al menu principale.
                assertEquals(listOf("spesa", inbox), linksIn(PageEntity.ROOT_PAGE_ID))
                val page = db.pageDao().getById(linksIn(inbox).single())!!
                assertEquals("Un articolo", page.title)
                val bookmark = db.blockDao().getBlocksForPageOnce(page.id).single()
                assertEquals(BlockType.BOOKMARK, bookmark.type)
                assertEquals("https://www.example.com/articolo", bookmark.attrs.url)
            }
            // Salvato, la finestra scende e si torna all'app di prima.
            waitFor { closed }
        }
    }

    @Test
    fun aMessageGoesAtTheEndOfTheChosenPage() {
        AppSettings.sharePageId = "spesa"
        share("Latte\nPane").let {
            assertTrue(compose.onAllNodesWithText("Spesa").fetchSemanticsNodes().isNotEmpty())
            compose.onNodeWithText("At the end of the page").performClick()
            compose.onNodeWithText("Save").performClick()
            waitFor { runBlocking { db.blockDao().getBlocksForPageOnce("spesa").count { it.linkedPageId == null } } == 2 }
            val texts = runBlocking { db.blockDao().getBlocksForPageOnce("spesa") }
                .filter { it.linkedPageId == null }.sortedBy { it.orderIndex }.map { it.textJson }
            assertEquals(listOf("""[{"text":"Latte"}]""", """[{"text":"Pane"}]"""), texts)
        }
    }

    @Test
    fun theTitleCanBeChanged() {
        share("Comprare il latte").let {
            compose.onNodeWithText("Comprare il latte").performTextInput("Spesa: ")
            // "Save and open": la pagina nuova da aprire in notE.
            compose.onNodeWithText("Save and open").performClick()
            waitFor { AppSettings.sharePageId != null && linksIn(AppSettings.sharePageId!!).isNotEmpty() }
            val page = runBlocking { db.pageDao().getById(linksIn(AppSettings.sharePageId!!).single())!! }
            assertTrue(page.title, page.title.contains("Spesa: "))
            waitFor { opened == page.id }
        }
    }
}

/**
 * **La nota al volo** (02/11/2026) dentro l'app: dal tasto rapido, dal widget o
 * dalla scorciatoia si entra in una pagina nuova dell'Inbox; lasciata vuota se
 * ne va.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h800dp-mdpi")
class QuickNoteScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db get() = AppDatabase.getInstance(context)

    @Before
    fun setUp() = runBlocking {
        AppSettings.init(context)
        AppSettings.sharePageId = null
        AppDatabase.resetForTests()
        context.getDatabasePath("notion_local.db").delete()
        db.pageDao().insert(PageEntity(id = PageEntity.ROOT_PAGE_ID, title = "My Space"))
    }

    @After
    fun tearDown() {
        AppSettings.sharePageId = null
        QuickNotes.requested.value = false
        AppDatabase.resetForTests()
    }

    /** Aspetta, lasciando lavorare il thread dello schermo: è lì che nasce la nota. */
    private fun waitFor(condition: () -> Boolean) {
        compose.waitUntil(10_000) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(20)
            condition()
        }
    }

    private fun notesInInbox(): List<String> = runBlocking {
        val inbox = AppSettings.sharePageId ?: return@runBlocking emptyList()
        db.blockDao().getBlocksForPageOnce(inbox).mapNotNull { it.linkedPageId }
    }

    @Test
    fun aQuickNoteOpensAndLeftEmptyGoesAway() {
        QuickNotes.requested.value = true
        val factory = ViewModelFactory(context)
        compose.setContent { NotionLocalTheme { AppNavHost(factory = factory) } }
        // Il primo "indietro" toglie il cursore, e Android non lo ridà (vedi `enterTouchMode`).
        compose.runOnUiThread { compose.activity.enterTouchMode() }
        waitFor { notesInInbox().size == 1 }
        val note = notesInInbox().single()
        waitFor { runBlocking { db.pageDao().getById(note) } != null }
        // Si torna indietro senza aver scritto niente: il primo "indietro" toglie
        // il cursore dalla riga (sul telefono chiude la tastiera), il secondo esce.
        compose.waitForIdle()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        waitFor { runBlocking { db.pageDao().getById(note) } == null }
        assertEquals(emptyList<String>(), notesInInbox())
    }
}

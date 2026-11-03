package com.gabriele.notionlocal.ui

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.TextRange
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.entity.pageLink
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.BACKLINKS_TAG
import com.gabriele.notionlocal.ui.screen.MENTION_MENU_TAG
import com.gabriele.notionlocal.ui.screen.PageEditorScreen
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * **Le menzioni "@" e i backlink nell'editor** (03/11/2026): scritta la
 * chiocciola, il menu propone le pagine; scelta una, nel testo resta il suo
 * titolo col collegamento. In fondo alla pagina citata, chi la cita.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h800dp-mdpi")
class MentionScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db get() = AppDatabase.getInstance(context)

    @Before
    fun setUp() = runBlocking {
        AppSettings.init(context)
        AppDatabase.resetForTests()
        context.getDatabasePath("notion_local.db").delete()
        db.withTransaction {
            db.pageDao().insert(PageEntity(id = "p", title = "Prova"))
            db.pageDao().insert(PageEntity(id = "hotel", title = "Hotel Roma"))
            db.blockDao().insert(BlockEntity(id = "primo", pageId = "p", textJson = """[{"text":"Primo paragrafo"}]""", orderIndex = 0))
            db.blockDao().insert(BlockEntity(id = "ultimo", pageId = "p", textJson = """[{"text":"Ultimo paragrafo"}]""", orderIndex = 1))
        }
    }

    @After
    fun tearDown() {
        compose.waitForIdle()
        Thread.sleep(300)
        compose.waitForIdle()
        AppDatabase.resetForTests()
    }

    private fun settle() {
        repeat(10) {
            compose.mainClock.advanceTimeBy(200)
            Thread.sleep(50)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
            compose.waitForIdle()
        }
    }

    private fun open(pageId: String, waitText: String, onPage: (String) -> Unit = {}) {
        val factory = ViewModelFactory(context)
        compose.setContent {
            NotionLocalTheme {
                PageEditorScreen(pageId = pageId, factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {}, onNavigateToPage = onPage, onNavigateToDatabase = {})
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodes(hasText(waitText, substring = true)).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun spans(blockId: String) = runBlocking {
        Json.decodeFromString<List<RichTextSpan>>(db.blockDao().getById(blockId)!!.textJson)
    }

    @Test
    fun anAtWithAFewLettersMentionsThePage() {
        open("p", "Ultimo paragrafo")
        val field = compose.onNode(hasSetTextAction() and hasText("Ultimo paragrafo", substring = true))
        field.performSemanticsAction(SemanticsActions.RequestFocus)
        compose.waitForIdle()
        // In fondo (il testo del campo comincia con l'a-capo nascosto), una lettera alla volta.
        field.performTextInputSelection(TextRange(1 + "Primo paragrafo\nUltimo paragrafo".length))
        listOf(" ", "@", "H", "o", "t").forEach {
            compose.onNode(hasSetTextAction() and hasText("Ultimo paragrafo", substring = true)).performTextInput(it)
        }
        settle()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag(MENTION_MENU_TAG).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasText("Hotel Roma") and hasAnyAncestor(hasTestTag(MENTION_MENU_TAG))).performClick()
        settle()
        compose.waitUntil(5_000) { spans("ultimo").any { it.link != null } }

        val stored = spans("ultimo")
        assertEquals("Ultimo paragrafo Hotel Roma ", stored.joinToString("") { it.text })
        assertEquals(listOf("Hotel Roma"), stored.filter { it.link == pageLink("hotel") }.map { it.text })
        // Il menu si è chiuso.
        assertEquals(0, compose.onAllNodesWithTag(MENTION_MENU_TAG).fetchSemanticsNodes().size)
    }

    @Test
    fun theMentionedPageShowsWhoMentionsItAndOpensIt() {
        runBlocking {
            db.blockDao().insert(
                BlockEntity(
                    id = "cita", pageId = "p", orderIndex = 2,
                    textJson = Json.encodeToString(listOf(RichTextSpan("Dormire all'"), RichTextSpan("Hotel Roma", link = pageLink("hotel"))))
                )
            )
        }
        var opened: String? = null
        open("hotel", "Hotel Roma") { opened = it }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag(BACKLINKS_TAG).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasText("Dormire all'Hotel Roma") and hasAnyAncestor(hasTestTag(BACKLINKS_TAG))).assertExists()
        compose.onNode(hasText("Prova") and hasAnyAncestor(hasTestTag(BACKLINKS_TAG))).performClick()
        settle()
        assertEquals("p", opened)
    }
}

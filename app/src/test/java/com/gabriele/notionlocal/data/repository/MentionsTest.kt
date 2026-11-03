package com.gabriele.notionlocal.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockAttrs
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.entity.dateLink
import com.gabriele.notionlocal.data.entity.dateLinkTarget
import com.gabriele.notionlocal.data.entity.isInternalLink
import com.gabriele.notionlocal.data.entity.movedDateLink
import com.gabriele.notionlocal.data.entity.pageLink
import com.gabriele.notionlocal.data.entity.pageLinkTarget
import com.gabriele.notionlocal.data.entity.refreshPageMentions
import com.gabriele.notionlocal.data.entity.replaceLinkedText
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.mentionDays
import com.gabriele.notionlocal.ui.screen.mentionJustTyped
import com.gabriele.notionlocal.ui.screen.mentionQueryAt
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * **Menzioni "@" e backlink** (03/11/2026, chiesto dall'utente: "In fondo a
 * ogni pagina vedresti quali altre pagine la richiamano. Insieme, le menzioni
 * @ dentro il testo per citare una pagina o una data al volo"): i
 * collegamenti, il menu, e chi richiama chi nel database.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class MentionsTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: PageRepository

    @Before
    fun setUp() = runBlocking<Unit> {
        AppSettings.init(ApplicationProvider.getApplicationContext())
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = PageRepository(db)
    }

    @After
    fun tearDown() = db.close()

    private fun spansJson(vararg spans: RichTextSpan) = Json.encodeToString(spans.toList())

    @Test
    fun linksSayWhereTheyGo() {
        assertEquals("abc", pageLinkTarget(pageLink("abc")))
        assertNull(pageLinkTarget("https://example.com"))
        val link = dateLink(LocalDate.of(2026, 11, 3), "m1")
        assertEquals("note://date/2026-11-03?m=m1", link)
        assertEquals(LocalDate.of(2026, 11, 3), dateLinkTarget(link))
        assertNull(dateLinkTarget(pageLink("abc")))
        // Spostata a un altro giorno, la menzione tiene il suo segno.
        assertEquals("note://date/2026-12-25?m=m1", movedDateLink(link, LocalDate.of(2026, 12, 25)))
        // Due menzioni dello stesso giorno sono due collegamenti diversi.
        assertNotEquals(dateLink(LocalDate.of(2026, 11, 3)), dateLink(LocalDate.of(2026, 11, 3)))
        assertTrue(isInternalLink(link))
        assertFalse(isInternalLink("https://example.com"))
    }

    @Test
    fun aMentionTakesTheNewTitleKeepingItsLook() {
        val link = pageLink("hotel")
        val spans = listOf(
            RichTextSpan("Dormire al "),
            RichTextSpan("Hotel", link = link, bold = true),
            RichTextSpan(" Roma", link = link),
            RichTextSpan(" e poi "),
            RichTextSpan("Hotel Roma", link = link)
        )
        val fresh = refreshPageMentions(spans, mapOf("hotel" to "Grand Hotel"), "Untitled")!!
        assertEquals(
            listOf(
                RichTextSpan("Dormire al "),
                RichTextSpan("Grand Hotel", link = link, bold = true),
                RichTextSpan(" e poi "),
                RichTextSpan("Grand Hotel", link = link)
            ),
            fresh
        )
        // Già giusti: niente da cambiare. Una pagina sparita resta com'era.
        assertNull(refreshPageMentions(fresh, mapOf("hotel" to "Grand Hotel"), "Untitled"))
        assertNull(refreshPageMentions(fresh, emptyMap(), "Untitled"))
        // Una data cambiata: il testo e il giorno, la formattazione resta.
        val day = dateLink(LocalDate.of(2026, 11, 3), "m1")
        assertEquals(
            listOf(RichTextSpan("Il "), RichTextSpan("25 dicembre 2026", link = movedDateLink(day, LocalDate.of(2026, 12, 25)), italic = true)),
            replaceLinkedText(
                listOf(RichTextSpan("Il "), RichTextSpan("3 novembre 2026", link = day, italic = true)),
                day,
                "25 dicembre 2026",
                movedDateLink(day, LocalDate.of(2026, 12, 25))
            )
        )
    }

    @Test
    fun theAtOpensTheMenuOnlyAtTheStartOfAWord() {
        assertTrue(mentionJustTyped("", "@", 1))
        assertTrue(mentionJustTyped("Vedi ", "Vedi @", 6))
        assertTrue(mentionJustTyped("Riga\n", "Riga\n@", 6))
        assertTrue(mentionJustTyped("(", "(@", 2))
        // Un indirizzo email non è una menzione.
        assertFalse(mentionJustTyped("mario", "mario@", 6))
        // Il nome cercato può avere spazi; uno spazio subito dopo l'"@", un a-capo
        // o l'"@" cancellato chiudono il menu.
        assertEquals("Hotel Ro", mentionQueryAt("Vedi @Hotel Ro", 5, 14))
        assertNull(mentionQueryAt("Vedi @ ", 5, 7))
        assertNull(mentionQueryAt("Vedi @Ho\n", 5, 9))
        assertNull(mentionQueryAt("Vedi Ho", 5, 7))
        assertNull(mentionQueryAt("Vedi @Ho", 5, 5))
        // I giorni: oggi, domani, ieri e "Pick a date…", filtrati per nome.
        val today = LocalDate.of(2026, 11, 3)
        assertEquals(listOf(today, today.plusDays(1), today.minusDays(1), null), mentionDays("", today).map { it.date })
        assertEquals(listOf(today.plusDays(1)), mentionDays("tom", today).map { it.date })
        assertEquals(listOf<LocalDate?>(null), mentionDays("date", today).map { it.date })
        assertTrue(mentionDays("Hotel", today).isEmpty())
    }

    @Test
    fun backlinksAreThePagesThatMentionThisOne() = runBlocking<Unit> {
        repo.createPage(PageEntity(id = "hotel", title = "Hotel Roma", updatedAt = 1))
        repo.createPage(PageEntity(id = "viaggio", title = "Viaggio", updatedAt = 3))
        repo.createPage(PageEntity(id = "diario", title = "Diario", updatedAt = 5))
        repo.createPage(PageEntity(id = "elenco", title = "Elenco", updatedAt = 4))
        repo.createPage(PageEntity(id = "vecchia", title = "Vecchia", trashedAt = 10, updatedAt = 9))
        db.blockDao().insertAll(
            listOf(
                BlockEntity(pageId = "viaggio", orderIndex = 0, textJson = spansJson(RichTextSpan("Dormire all'"), RichTextSpan("Hotel Roma", link = pageLink("hotel")))),
                BlockEntity(pageId = "viaggio", orderIndex = 1, textJson = spansJson(RichTextSpan("Di nuovo: "), RichTextSpan("Hotel Roma", link = pageLink("hotel")))),
                BlockEntity(pageId = "diario", orderIndex = 0, type = BlockType.PAGE_MENTION, attrsJson = BlockAttrs.encode(BlockAttrs(pageId = "hotel"))),
                // La sottopagina nella pagina che la contiene non è un richiamo.
                BlockEntity(pageId = "elenco", orderIndex = 0, type = BlockType.PAGE_LINK, linkedPageId = "hotel"),
                // Dal cestino no; e la pagina che cita se stessa no.
                BlockEntity(pageId = "vecchia", orderIndex = 0, textJson = spansJson(RichTextSpan("Hotel Roma", link = pageLink("hotel")))),
                BlockEntity(pageId = "hotel", orderIndex = 0, textJson = spansJson(RichTextSpan("Questa: "), RichTextSpan("Hotel Roma", link = pageLink("hotel")))),
                // Un'altra pagina col nome simile non conta.
                BlockEntity(pageId = "elenco", orderIndex = 1, textJson = spansJson(RichTextSpan("Hotel", link = pageLink("hotel-2"))))
            )
        )
        val backlinks = repo.backlinksOf("hotel")
        // Le toccate per ultime prima; una volta sola, con la prima riga che cita.
        assertEquals(listOf("diario", "viaggio"), backlinks.map { it.page.id })
        assertNull(backlinks[0].snippet)
        assertEquals("Dormire all'Hotel Roma", backlinks[1].snippet)
    }

    @Test
    fun theMenuFindsPagesByNameAndTheTitlesComeBackInLine() = runBlocking<Unit> {
        db.pageDao().insert(PageEntity(id = "qui", title = "Pagina in cui scrivo", updatedAt = 9))
        db.pageDao().insert(PageEntity(id = "a", title = "Grand Hotel", updatedAt = 1))
        db.pageDao().insert(PageEntity(id = "b", title = "Hotel Roma", updatedAt = 2))
        db.pageDao().insert(PageEntity(id = "c", title = "Notte in hotel", updatedAt = 3))
        db.pageDao().insert(PageEntity(id = "t", title = "Hotel (template)", isTemplate = true))
        db.pageDao().insert(PageEntity(id = "x", title = "Hotel buttato", trashedAt = 1))
        // Prima chi comincia così, poi chi ha una parola così, poi le altre.
        assertEquals(listOf("b", "c", "a"), repo.mentionCandidates("hotel", excludeId = "qui").map { it.id })
        // Senza niente scritto, le toccate per ultime, ma non la pagina in cui si scrive.
        assertEquals(listOf("c", "b", "a"), repo.mentionCandidates("", excludeId = "qui").map { it.id }.filter { it in setOf("a", "b", "c") })
        assertFalse(repo.mentionCandidates("", excludeId = "qui").any { it.id == "qui" })

        db.blockDao().insert(
            BlockEntity(id = "riga", pageId = "qui", orderIndex = 0, textJson = spansJson(RichTextSpan("Vedi "), RichTextSpan("Hotel Roma", link = pageLink("b"))))
        )
        db.pageDao().update(db.pageDao().getById("b")!!.copy(title = "Hotel Roma Centro"))
        assertEquals(1, repo.refreshMentionTitles("qui", "Untitled"))
        assertEquals(spansJson(RichTextSpan("Vedi "), RichTextSpan("Hotel Roma Centro", link = pageLink("b"))), db.blockDao().getById("riga")!!.textJson)
        assertEquals(0, repo.refreshMentionTitles("qui", "Untitled"))
    }
}

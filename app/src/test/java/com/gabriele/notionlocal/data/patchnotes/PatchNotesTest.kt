package com.gabriele.notionlocal.data.patchnotes

import android.app.Application
import android.content.Context
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.ui.screen.inlineMarkdown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * **Le Patch Notes** (30/09/2026): l'app ha la Cronologia del README, la
 * stessa, e ogni versione ha il suo numero, il giorno e l'ora, in ordine.
 *
 * È anche il controllo della Cronologia stessa: una voce nuova scritta
 * senza numero, con un numero sbagliato o con un'ora che torna indietro fa
 * fallire questo test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PatchNotesTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    /** La Cronologia presa dal README, come la prende il build. */
    private fun readmeChronology(): String {
        val text = File("../README.md").readText(Charsets.UTF_8).removePrefix("﻿")
        val start = text.indexOf("## Cronologia degli aggiornamenti")
        val end = text.indexOf("\n## ", start + 1)
        return text.substring(start, end)
    }

    private fun notes() = PatchNotesAsset.load(context)

    @Test
    fun theAppHasTheReadmeChronology() {
        val shipped = context.assets.open(PatchNotesAsset.FILE_NAME).bufferedReader().use { it.readText() }
        assertEquals(readmeChronology(), shipped)
        assertEquals("Cronologia degli aggiornamenti", notes().title)
        assertTrue(notes().intro.isNotEmpty())
    }

    @Test
    fun everyVersionHasItsNumberDayAndTime() {
        val versions = notes().versions
        assertTrue("versioni: ${versions.size}", versions.size >= 150)
        // Ogni titolo "### " della Cronologia è una versione letta.
        assertEquals(readmeChronology().lines().count { it.startsWith("### ") }, versions.size)
        // Dalla prima, v0.1, il giorno in cui nasce il progetto.
        val first = versions.last()
        assertEquals(PatchKind.RELEASE, first.kind)
        assertEquals("0.1", first.version)
        assertEquals("08/09/2026", first.date)
        assertEquals("09:19", first.time)
        versions.forEach { v ->
            assertTrue("${v.version}: ${v.emoji}", v.emoji == if (v.kind == PatchKind.FIX) "🔧" else "🆕")
            assertTrue("${v.version}: vuota", v.blocks.isNotEmpty())
            // L'ora c'è, o si dice che non si sa; con l'ora, la data è un giorno preciso.
            if (v.time == UNKNOWN_TIME) return@forEach
            assertTrue("${v.version}: ${v.time}", Regex("""\d{2}:\d{2}""").matches(v.time))
            assertTrue("${v.version}: ${v.date}", Regex("""\d{2}/\d{2}/\d{4}""").matches(v.date))
        }
        // Le versioni senza ora sono solo le prime, dell'altra chat.
        val unknown = versions.filter { it.time == UNKNOWN_TIME }
        assertTrue(unknown.isNotEmpty() && unknown.all { versions.indexOf(it) > versions.size - 12 })
    }

    @Test
    fun theNumbersFollowEachOther() {
        var minor = 0
        var patch = 0
        notes().versions.asReversed().forEach { v ->
            if (v.kind == PatchKind.FIX) patch++ else {
                minor++
                patch = 0
            }
            val expected = if (v.kind == PatchKind.FIX) "0.$minor.$patch" else "0.$minor"
            assertEquals(v.title, expected, v.version)
        }
    }

    @Test
    fun theNewestIsOnTopAndTimeNeverGoesBack() {
        val format = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        val times = notes().versions.filter { it.time != UNKNOWN_TIME }.map { LocalDateTime.parse("${it.date} ${it.time}", format) }
        times.zipWithNext().forEach { (newer, older) -> assertTrue("$older dopo $newer", !older.isAfter(newer)) }
    }

    @Test
    fun theAppVersionIsTheNewest() {
        val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        assertEquals(notes().versions.first().version, versionName)
    }

    @Test
    fun theMarkdownBecomesBlocks() {
        val notes = parsePatchNotes(
            """
            ## Cronologia degli aggiornamenti

            Una riga
            spezzata a mano.

            ### 🆕 v0.2 — 25/09/2026 · 08:25 — Il titolo
            *(Claude Code sul PC; verificato)*

            L'utente, parola per parola:
            > «Prima riga
            > seconda riga»

            - **[Nuova funzionalità]** Una voce
              che continua sotto
              - Una voce rientrata
            1. Numerata
              | a | b |
              |---|---|
              | 1 | 2 |

            ### 🔧 v0.1.1 — 24/09/2026 · 23:17 — Una correzione
            - **[Bug fix]** Corretto
            """.trimIndent()
        )
        assertEquals(listOf(NoteBlock.Paragraph("Una riga spezzata a mano.")), notes.intro)
        val (release, fix) = notes.versions
        assertEquals(PatchKind.RELEASE, release.kind)
        assertEquals("0.2", release.version)
        assertEquals("25/09/2026", release.date)
        assertEquals("08:25", release.time)
        assertEquals("Il titolo", release.title)
        assertEquals(
            listOf(
                NoteBlock.Paragraph("*(Claude Code sul PC; verificato)*"),
                NoteBlock.Paragraph("L'utente, parola per parola:"),
                NoteBlock.Quote("«Prima riga seconda riga»"),
                NoteBlock.Bullet(0, "**[Nuova funzionalità]** Una voce che continua sotto"),
                NoteBlock.Bullet(1, "Una voce rientrata"),
                NoteBlock.Bullet(0, "Numerata", marker = "1."),
                NoteBlock.Table(1, listOf(listOf("a", "b"), listOf("1", "2")))
            ),
            release.blocks
        )
        assertEquals(PatchKind.FIX, fix.kind)
        assertEquals(listOf(NoteBlock.Bullet(0, "**[Bug fix]** Corretto")), fix.blocks)
    }

    @Test
    fun boldItalicStrikeAndCodeInsideALine() {
        val text = inlineMarkdown("a **grassetto** *corsivo* ~~via~~ `codice` 2 * 3")
        assertEquals("a grassetto corsivo via codice 2 * 3", text.text)
        fun styleOf(word: String) = text.spanStyles.filter { text.text.indexOf(word) in it.start until it.end }.map { it.item }
        assertTrue(styleOf("grassetto").any { it.fontWeight == FontWeight.Bold })
        assertTrue(styleOf("corsivo").any { it.fontStyle == FontStyle.Italic })
        assertTrue(styleOf("via").any { it.textDecoration == TextDecoration.LineThrough })
        assertTrue(styleOf("codice").any { it.fontFamily != null })
        // L'asterisco fra due spazi resta un asterisco e non accende il corsivo.
        assertTrue(styleOf("3").none { it.fontStyle == FontStyle.Italic })
        // Il grassetto dentro il corsivo, come nelle righe "*(… **31** …)*".
        val nested = inlineMarkdown("*(schema alla **31**)*")
        assertEquals("(schema alla 31)", nested.text)
        assertTrue(nested.spanStyles.any { it.item.fontWeight == FontWeight.Bold && it.item.fontStyle == FontStyle.Italic })
    }
}

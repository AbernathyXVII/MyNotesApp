package com.gabriele.notionlocal.data

import com.gabriele.notionlocal.data.settings.withRecentEmoji
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * **Le emoji del selettore delle icone** (07/10/2026): le categorie di
 * Notion, la ricerca per nome nella lingua dell'app e in inglese, i colori
 * della pelle, le emoji recenti. Si leggono i file veri degli asset.
 */
class EmojiCatalogTest {

    private val assets = File("src/main/assets/emoji")

    /** Il catalogo come lo carica l'app: i nomi delle otto lingue insieme. */
    private fun catalog(supported: (String) -> Boolean = { true }): EmojiCatalog {
        val names = listOf("en", "it", "de", "fr", "es", "zh", "ko", "ja").flatMap { File(assets, "names_$it.txt").readLines() }
        return EmojiCatalog.parse(File(assets, "emoji.txt").readText(), names, supported)
    }

    @Test
    fun theCategoriesAreNotionsInNotionsOrder() {
        val catalog = catalog()
        assertEquals(EmojiCategory.entries.toList(), catalog.sections.map { it.first })
        assertEquals("😀", catalog.sections.first().second.first().emoji)
        val flags = catalog.sections.last().second.map { it.emoji }
        assertTrue("🇮🇹" in flags)
        // "#️⃣" comincia come una categoria, ma è un'emoji: c'è.
        assertTrue(catalog.all.any { it.emoji == "#️⃣" })
        assertTrue(catalog.all.size > 1800)
        // Niente varianti dei colori della pelle nella griglia: le fa withSkinTone.
        assertFalse(catalog.all.any { it.emoji.contains("🏽") })
    }

    @Test
    fun theSearchFindsTheNamesInEveryLanguage_withoutCaringForAccents() {
        val catalog = catalog()
        // Tutti i gatti, prima le faccine come su Notion, poi il muso e il gatto.
        val gatti = catalog.search("gatto").map { it.emoji }
        assertEquals("😺", gatti.first())
        assertTrue("🐱" in gatti && "🐈" in gatti)
        assertEquals("🐱", catalog.search("Cat FACE").first().emoji)
        // Ogni parola scritta è l'inizio di una parola del nome: "pizz" trova la pizza…
        assertTrue(catalog.search("pizz").any { it.emoji == "🍕" })
        // …e "cat" mette i gatti prima della "catena".
        val cat = catalog.search("cat").map { it.emoji }
        assertTrue(cat.indexOf("🐱") < cat.indexOf("⛓️"))
        assertTrue(catalog.search("caffe").any { it.emoji == "☕" })
        assertTrue(catalog.search("caffè").any { it.emoji == "☕" })
        assertEquals("🇮🇹", catalog.search("bandiera italia").first().emoji)
        // Le altre lingue dell'app ci sono tutte, anche quelle senza spazi fra le parole.
        assertTrue(catalog.search("Katze").any { it.emoji == "🐱" })
        assertTrue(catalog.search("猫").any { it.emoji == "🐱" })
        assertTrue(catalog.search("zzzqqq").isEmpty())
        assertTrue(catalog.search("   ").isEmpty())
    }

    @Test
    fun whatThePhoneCannotDrawIsLeftOut() {
        val catalog = catalog { it != "🐱" }
        assertFalse(catalog.all.any { it.emoji == "🐱" })
        assertTrue(catalog.search("cat").none { it.emoji == "🐱" })
    }

    @Test
    fun theSkinToneGoesAfterTheFirstCharacter() {
        assertEquals("👋🏽", withSkinTone("👋", 3))
        assertEquals("👋", withSkinTone("👋", 0))
        // Il selettore di presentazione se ne va: "🕵️" diventa "🕵🏿".
        assertEquals("🕵🏿", withSkinTone("🕵️", 5))
        // Nelle sequenze con lo ZWJ il colore va sulla prima persona.
        assertEquals("🧑🏻‍💻", withSkinTone("🧑‍💻", 1))
        val catalog = catalog()
        assertTrue(catalog.all.first { it.emoji == "👋" }.acceptsTone)
        assertFalse(catalog.all.first { it.emoji == "😀" }.acceptsTone)
    }

    @Test
    fun theRecentEmojisKeepTheLastOnesWithoutDuplicates() {
        var list = emptyList<String>()
        listOf("😀", "🐱", "🍕", "🐱").forEach { list = list.withRecentEmoji(it, max = 3) }
        assertEquals(listOf("🐱", "🍕", "😀"), list)
        list = list.withRecentEmoji("🚀", max = 3)
        assertEquals(listOf("🚀", "🐱", "🍕"), list)
        assertEquals(list, list.withRecentEmoji("  "))
    }

    @Test
    fun thePageIconFieldsFollowTheChoice() {
        assertEquals("🐱", IconChoice.Emoji("🐱").iconField)
        assertEquals(DEFAULT_PAGE_ICON, IconChoice.Image("a.png").iconField)
        assertEquals("a.png", IconChoice.Image("a.png").imageField)
        assertEquals(DEFAULT_PAGE_ICON, IconChoice.None.iconField)
        // Il 📄 di riserva non è un'emoji scelta.
        assertEquals(null, chosenEmoji(DEFAULT_PAGE_ICON))
        assertEquals("🐱", chosenEmoji("🐱"))
        // L'immagine di prima se ne va, a meno che non sia proprio quella scelta.
        assertEquals("a.png", replacedImage("a.png", IconChoice.Emoji("🐱")))
        assertEquals(null, replacedImage("a.png", IconChoice.Image("a.png")))
        assertEquals(null, replacedImage(null, IconChoice.None))
    }
}

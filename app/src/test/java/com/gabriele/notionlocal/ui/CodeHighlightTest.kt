package com.gabriele.notionlocal.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import com.gabriele.notionlocal.ui.screen.CODE_LANGUAGES
import com.gabriele.notionlocal.ui.screen.DEFAULT_CODE_LANGUAGE
import com.gabriele.notionlocal.ui.screen.highlightCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **I colori del blocco "Code"** (06/10/2026): parole chiave, stringhe e
 * commenti colorati secondo il linguaggio; il testo resta quello.
 */
class CodeHighlightTest {

    /** Il colore del primo carattere di `piece` dentro il testo colorato; null se non ne ha. */
    private fun AnnotatedString.colorOf(piece: String, from: Int = 0): Color? {
        val at = text.indexOf(piece, from)
        require(at >= 0) { "\"$piece\" non c'è" }
        return spanStyles.lastOrNull { at >= it.start && at < it.end }?.item?.color
    }

    @Test
    fun kotlinKeywordsStringsAndCommentsHaveTheirColors() {
        val code = "fun main() {\n    val nome = \"notE\" // il nome\n    println(nome)\n}"
        val colored = highlightCode(code, "Kotlin")
        assertEquals(code, colored.text)
        val keyword = colored.colorOf("fun")
        assertEquals(keyword, colored.colorOf("val"))
        val string = colored.colorOf("\"notE\"")
        val comment = colored.colorOf("// il nome")
        val function = colored.colorOf("println")
        listOf(keyword, string, comment, function).forEach { assertTrue(it != null) }
        assertEquals(4, setOf(keyword, string, comment, function).size)
        // Il nome di una variabile resta del colore del testo.
        assertEquals(null, colored.colorOf("nome ="))
    }

    @Test
    fun pythonUsesTheHashForComments() {
        val colored = highlightCode("def f():\n    return 1  # uno", "Python")
        assertNotEquals(null, colored.colorOf("# uno"))
        assertEquals(colored.colorOf("def"), colored.colorOf("return"))
        assertNotEquals(null, colored.colorOf("1"))
    }

    @Test
    fun sqlKeywordsAreFoundInAnyCase() {
        val colored = highlightCode("SELECT nome FROM pagine where id = 1", "SQL")
        assertEquals(colored.colorOf("SELECT"), colored.colorOf("where"))
        assertNotEquals(null, colored.colorOf("SELECT"))
    }

    @Test
    fun plainTextStaysPlain() {
        val colored = highlightCode("fun \"x\" // y", "Plain Text")
        assertTrue(colored.spanStyles.isEmpty())
    }

    @Test
    fun theDefaultLanguageIsInTheList() {
        assertTrue(DEFAULT_CODE_LANGUAGE in CODE_LANGUAGES)
        assertEquals(CODE_LANGUAGES.size, CODE_LANGUAGES.toSet().size)
    }
}

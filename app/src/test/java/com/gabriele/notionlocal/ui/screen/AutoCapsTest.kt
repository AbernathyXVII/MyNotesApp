package com.gabriele.notionlocal.ui.screen

import android.app.Application
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextInputService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * **La maiuscola automatica** (29/09/2026): a campo vuoto la tastiera parte
 * in maiuscolo; se c'è già qualcosa, anche solo uno spazio, no.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AutoCapsTest {

    private fun at(text: String, cursor: Int = text.length) = TextFieldValue(text, TextRange(cursor))

    @Test
    fun anEmptyFieldStartsWithACapital() {
        assertEquals(KeyboardCapitalization.Sentences, autoCapsFor(at("")))
    }

    @Test
    fun anythingAlreadyWrittenMeansNoCapital() {
        // Uno spazio scritto a mano, un punto seguito da spazio, l'inizio di un testo che c'è già.
        assertEquals(KeyboardCapitalization.None, autoCapsFor(at(" ")))
        assertEquals(KeyboardCapitalization.None, autoCapsFor(at("Ciao. ")))
        assertEquals(KeyboardCapitalization.None, autoCapsFor(at("ciao", 0)))
    }

    @Test
    fun betweenLettersTheKeyboardIsLeftAlone() {
        // Lì la tastiera non mette maiuscole da sola, e riavviarla spezzerebbe la parola.
        assertNull(autoCapsFor(at("C")))
        assertNull(autoCapsFor(at("Ciao mo")))
        assertNull(autoCapsFor(at("Ciao ")))
    }

    @Test
    fun inTheTextOfAPageEveryEmptyLineCounts() {
        assertEquals(KeyboardCapitalization.Sentences, autoCapsFor(at("Prima riga\n")))
        assertEquals(KeyboardCapitalization.Sentences, autoCapsFor(at("Prima\n\nTerza", 6)))
        assertEquals(KeyboardCapitalization.None, autoCapsFor(at("Prima\nseconda", 6)))
    }

    @Test
    fun onlyPlainTextFieldsFollowTheRule() {
        assertTrue(autoCapsApplies(ImeOptions()))
        assertTrue(autoCapsApplies(ImeOptions(capitalization = KeyboardCapitalization.Sentences)))
        listOf(KeyboardType.Number, KeyboardType.Password, KeyboardType.Email, KeyboardType.Uri, KeyboardType.Ascii)
            .forEach { assertTrue(!autoCapsApplies(ImeOptions(keyboardType = it))) }
        assertTrue(!autoCapsApplies(ImeOptions(capitalization = KeyboardCapitalization.Characters)))
    }

    /** Il servizio della tastiera della finestra, che si segna cosa gli si chiede. */
    private class Keyboard : PlatformTextInputService {
        val starts = mutableListOf<ImeOptions>()
        override fun startInput(
            value: TextFieldValue,
            imeOptions: ImeOptions,
            onEditCommand: (List<androidx.compose.ui.text.input.EditCommand>) -> Unit,
            onImeActionPerformed: (androidx.compose.ui.text.input.ImeAction) -> Unit
        ) {
            starts += imeOptions
        }
        override fun stopInput() {}
        override fun showSoftwareKeyboard() {}
        override fun hideSoftwareKeyboard() {}
        override fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue) {}
    }

    @Test
    fun theKeyboardIsRestartedOnlyWhereItWouldPutACapital() {
        val keyboard = Keyboard()
        val window = TextInputService(keyboard)
        val service = AutoCapsTextInputService(window)
        val session = service.startInput(at(""), ImeOptions(), {}, {})
        assertEquals(listOf(KeyboardCapitalization.Sentences), keyboard.starts.map { it.capitalization })
        // Si scrive una parola: nessun riavvio.
        listOf("C", "Ci", "Cia", "Ciao", "Ciao.").forEach { session.updateState(null, at(it)) }
        assertEquals(1, keyboard.starts.size)
        // Dopo ". " la tastiera metterebbe la maiuscola: il campo non è vuoto, quindi no.
        session.updateState(null, at("Ciao. "))
        assertEquals(KeyboardCapitalization.None, keyboard.starts.last().capitalization)
        session.updateState(null, at("Ciao. c"))
        session.updateState(null, at("Ciao. co"))
        assertEquals(2, keyboard.starts.size)
        // Cancellato tutto, di nuovo la maiuscola.
        session.updateState(null, at(""))
        assertEquals(KeyboardCapitalization.Sentences, keyboard.starts.last().capitalization)
        assertEquals(3, keyboard.starts.size)
    }

    @Test
    fun aFieldThatAlreadyHasTextOpensWithoutCapital() {
        val keyboard = Keyboard()
        val service = AutoCapsTextInputService(TextInputService(keyboard))
        service.startInput(at(" "), ImeOptions(capitalization = KeyboardCapitalization.Sentences), {}, {})
        assertEquals(KeyboardCapitalization.None, keyboard.starts.single().capitalization)
    }

    @Test
    fun aFieldThatLostTheKeyboardDoesNotTakeItBack() {
        val keyboard = Keyboard()
        val window = TextInputService(keyboard)
        val first = AutoCapsTextInputService(window).startInput(at("Ciao"), ImeOptions(), {}, {})
        // Un altro campo prende la tastiera.
        AutoCapsTextInputService(window).startInput(at("Altro"), ImeOptions(), {}, {})
        val before = keyboard.starts.size
        first.updateState(null, at(""))
        assertEquals(before, keyboard.starts.size)
    }

    @Test
    fun numbersAndPasswordsAreNeverTouched() {
        val keyboard = Keyboard()
        val service = AutoCapsTextInputService(TextInputService(keyboard))
        val session = service.startInput(at(""), ImeOptions(keyboardType = KeyboardType.Password), {}, {})
        session.updateState(null, at("a. "))
        session.updateState(null, at(""))
        assertEquals(listOf(KeyboardCapitalization.None), keyboard.starts.map { it.capitalization })
    }

    /**
     * **Ovunque si scriva**: ogni campo di testo dell'app sta dentro un
     * `AutoCaps { }`. Un campo nuovo lasciato fuori farebbe fallire questo test.
     */
    @Test
    fun everyTextFieldInTheAppIsWrapped() {
        val sources = File("src/main/java").walkTopDown().filter { it.extension == "kt" && it.name != "AutoCaps.kt" }.toList()
        assertTrue("sorgenti non trovati", sources.isNotEmpty())
        val call = Regex("""(?<![A-Za-z0-9_.])(BasicTextField|OutlinedTextField|TextField)\(""")
        val missing = mutableListOf<String>()
        var found = 0
        sources.forEach { file ->
            val lines = file.readLines()
            lines.forEachIndexed { index, line ->
                val match = call.find(line) ?: return@forEachIndexed
                if (line.substring(0, match.range.first).isNotBlank()) return@forEachIndexed
                found++
                val previous = lines.subList(0, index).lastOrNull { it.isNotBlank() }?.trim()
                if (previous != "AutoCaps {") missing += "${file.name}:${index + 1}"
            }
        }
        assertTrue("nessun campo trovato", found > 30)
        assertEquals(emptyList<String>(), missing)
    }
}

package com.gabriele.notionlocal.ui

import android.app.Application
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.CopiedLine
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.ui.screen.LinkClipboard
import com.gabriele.notionlocal.ui.screen.WholePageTextToolbar
import com.gabriele.notionlocal.ui.screen.copyPageToClipboard
import com.gabriele.notionlocal.ui.screen.pastedFormat
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel.PageCopyLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **Tutta la pagina selezionata** (20/10/2026): "Select all" del menu del
 * testo seleziona la pagina, e la pagina copiata torna uguale in notE.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PageSelectionTest {

    private val context get() = ApplicationProvider.getApplicationContext<Application>()

    /** Un menu del testo finto, che si ricorda cosa gli è arrivato. */
    private class FakeToolbar : TextToolbar {
        var selectAll: (() -> Unit)? = null
        var copy: (() -> Unit)? = null
        override val status = TextToolbarStatus.Hidden
        override fun hide() = Unit
        override fun showMenu(
            rect: Rect,
            onCopyRequested: (() -> Unit)?,
            onPasteRequested: (() -> Unit)?,
            onCutRequested: (() -> Unit)?,
            onSelectAllRequested: (() -> Unit)?
        ) {
            selectAll = onSelectAllRequested
            copy = onCopyRequested
        }
    }

    @Test
    fun selectAllInTheTextMenuSelectsThePage() {
        val base = FakeToolbar()
        var pageSelected = false
        var fieldSelected = false
        var copied = false
        WholePageTextToolbar(base) { pageSelected = true }
            .showMenu(Rect.Zero, { copied = true }, null, null, { fieldSelected = true })
        base.selectAll!!()
        assertTrue(pageSelected)
        assertFalse(fieldSelected)
        // Le altre voci restano quelle del campo.
        base.copy!!()
        assertTrue(copied)

        // Senza "Select all" nel menu (tutto già selezionato), non lo aggiunge.
        WholePageTextToolbar(base) { pageSelected = true }.showMenu(Rect.Zero, null, null, null, null)
        assertNull(base.selectAll)
    }

    @Test
    fun theCopiedPageComesBackTheSameInNote() {
        copyPageToClipboard(
            context,
            listOf(
                PageCopyLine(listOf(RichTextSpan(text = "Capitolo", bold = true)), CopiedLine(BlockType.PARAGRAPH)),
                PageCopyLine(listOf(RichTextSpan(text = "punto")), CopiedLine(BlockType.BULLET_LIST_ITEM, indent = 1)),
                PageCopyLine(listOf(RichTextSpan(text = "fatto")), CopiedLine(BlockType.CHECKBOX, checked = true)),
                PageCopyLine(listOf(RichTextSpan(text = "A\tB")), null)
            )
        )
        val base = object : androidx.compose.ui.platform.ClipboardManager {
            override fun getText(): AnnotatedString? = null
            override fun setText(annotatedString: AnnotatedString) = Unit
        }
        val pasted = LinkClipboard(context, base).getText()!!
        assertEquals("Capitolo\npunto\nfatto\nA\tB", pasted.text)
        val lines = TextFieldValue(pasted).pastedFormat(0).lines
        assertEquals(
            listOf(BlockType.PARAGRAPH to 0, BlockType.BULLET_LIST_ITEM to 1, BlockType.CHECKBOX to 0),
            lines.map { it.type to it.indent }
        )
        assertTrue(lines[2].checked)
    }
}

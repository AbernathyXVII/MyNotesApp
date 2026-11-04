package com.gabriele.notionlocal.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.NativeClipboard
import androidx.compose.ui.text.input.TextFieldValue
import com.gabriele.notionlocal.data.entity.CopiedLine
import com.gabriele.notionlocal.data.entity.RichTextSpan

/** Sul PC, per ora: gli appunti di sempre, e l'incolla porta il testo e basta. */
actual class LinkClipboard(
    private val base: ClipboardManager,
    private val clipboard: Clipboard
) : ClipboardManager by base, Clipboard by clipboard {
    actual var nextPasteMode: PasteMode = PasteMode.LINKS_ONLY

    override val nativeClipboard: NativeClipboard get() = clipboard.nativeClipboard

    actual fun formatPaste(before: TextFieldValue, after: TextFieldValue): TextFieldValue = after
}

@Composable
actual fun rememberLinkClipboard(
    richSelection: () -> List<RichTextSpan>?,
    selectedLines: () -> List<CopiedLine?>?
): LinkClipboard {
    val base = LocalClipboardManager.current
    val clipboard = LocalClipboard.current
    return remember(base, clipboard) { if (base is LinkClipboard) base else LinkClipboard(base, clipboard) }
}

@Composable
actual fun rememberEditShortcuts(): (EditShortcut) -> Unit = remember { { } }

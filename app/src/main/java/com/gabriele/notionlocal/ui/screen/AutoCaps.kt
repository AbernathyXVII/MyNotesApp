package com.gabriele.notionlocal.ui.screen

import android.text.TextUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.text.input.TextInputSession

/**
 * **La maiuscola automatica** (dal 29/09/2026), ovunque si scriva: a campo
 * vuoto la tastiera parte in maiuscolo; se nel campo c'è già qualcosa —
 * anche solo uno spazio — no. Nei campi di più righe (il testo di una
 * pagina) conta la riga: su una riga vuota si parte in maiuscolo.
 *
 * Da sola la tastiera non sa farlo: con "Sentences" mette la maiuscola
 * anche dopo uno spazio iniziale o dopo ". ", senza, mai. Qui si cambia
 * la richiesta mentre si scrive, e si riavvia la tastiera (è l'unico
 * momento in cui la rilegge) **solo nei punti in cui lei metterebbe la
 * maiuscola**: a riga vuota, dopo uno spazio o un a capo. Lì non c'è una
 * parola a metà da perdere; fra una lettera e l'altra non la si tocca mai.
 *
 * Vale per i campi di testo normale (`KeyboardType.Text`) che non hanno
 * già una regola loro (`Characters`, per esempio "DELETE" da scrivere
 * tutto maiuscolo): numeri, password, email, indirizzi e codici colore
 * restano come sono.
 *
 * Ogni campo di testo dell'app sta dentro un `AutoCaps { }` (lo controlla
 * `AutoCapsTest`): è un involucro e non un'impostazione della schermata
 * perché finestre, fogli e menu hanno ognuno la sua tastiera.
 */
@Composable
fun AutoCaps(content: @Composable () -> Unit) {
    val current = LocalTextInputService.current
    if (current == null || current is AutoCapsTextInputService) {
        content()
        return
    }
    val wrapped = remember(current) { AutoCapsTextInputService(current) }
    CompositionLocalProvider(LocalTextInputService provides wrapped, content = content)
}

/** Se la regola vale per un campo con queste impostazioni. */
internal fun autoCapsApplies(options: ImeOptions): Boolean =
    options.keyboardType == KeyboardType.Text &&
        (options.capitalization == KeyboardCapitalization.None ||
            options.capitalization == KeyboardCapitalization.Sentences)

/**
 * Cosa chiedere alla tastiera per [value]: `Sentences` a riga vuota,
 * `None` dove lei metterebbe la maiuscola ma la riga ha già qualcosa, null
 * (lasciare com'è) altrove — fra una lettera e l'altra la tastiera non
 * mette maiuscole comunque, e riavviarla lì spezzerebbe la parola.
 */
internal fun autoCapsFor(value: TextFieldValue): KeyboardCapitalization? {
    val text = value.text
    val cursor = value.selection.min.coerceIn(0, text.length)
    val lineStart = if (cursor == 0) 0 else text.lastIndexOf('\n', cursor - 1) + 1
    val lineEnd = text.indexOf('\n', cursor).let { if (it < 0) text.length else it }
    if (lineStart == lineEnd) return KeyboardCapitalization.Sentences
    val keyboardWouldCapitalize = TextUtils.getCapsMode(text, cursor, TextUtils.CAP_MODE_SENTENCES) != 0
    return if (keyboardWouldCapitalize) KeyboardCapitalization.None else null
}

/** Vedi `AutoCaps`. Si mette fra i campi e il servizio della tastiera della finestra. */
internal class AutoCapsTextInputService private constructor(
    platform: AutoCapsPlatform
) : TextInputService(platform) {
    constructor(inner: TextInputService) : this(AutoCapsPlatform(inner))
}

private class AutoCapsPlatform(private val inner: TextInputService) : PlatformTextInputService {
    private var session: TextInputSession? = null
    private var base = ImeOptions.Default
    private var caps = KeyboardCapitalization.None
    private var onEditCommand: (List<EditCommand>) -> Unit = {}
    private var onImeAction: (ImeAction) -> Unit = {}

    private fun options() = if (autoCapsApplies(base)) base.copy(capitalization = caps) else base

    override fun startInput(
        value: TextFieldValue,
        imeOptions: ImeOptions,
        onEditCommand: (List<EditCommand>) -> Unit,
        onImeActionPerformed: (ImeAction) -> Unit
    ) {
        base = imeOptions
        this.onEditCommand = onEditCommand
        onImeAction = onImeActionPerformed
        // Aprendo un campo che ha già qualcosa, niente maiuscola.
        caps = autoCapsFor(value) ?: KeyboardCapitalization.None
        session = inner.startInput(value, options(), onEditCommand, onImeActionPerformed)
    }

    override fun stopInput() {
        session?.let { inner.stopInput(it) }
        session = null
    }

    override fun showSoftwareKeyboard() {
        session?.showSoftwareKeyboard()
    }

    override fun hideSoftwareKeyboard() {
        session?.hideSoftwareKeyboard()
    }

    override fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue) {
        val current = session ?: return
        current.updateState(oldValue, newValue)
        // Solo se la tastiera è ancora di questo campo: riavviarla da qui
        // la porterebbe via a quello che la sta usando.
        if (!current.isOpen || !autoCapsApplies(base)) return
        val wanted = autoCapsFor(newValue) ?: return
        if (wanted == caps) return
        caps = wanted
        session = inner.startInput(newValue, options(), onEditCommand, onImeAction)
    }

    override fun notifyFocusedRect(rect: Rect) {
        session?.notifyFocusedRect(rect)
    }

    override fun updateTextLayoutResult(
        textFieldValue: TextFieldValue,
        offsetMapping: OffsetMapping,
        textLayoutResult: TextLayoutResult,
        textFieldToRootTransform: (Matrix) -> Unit,
        innerTextFieldBounds: Rect,
        decorationBoxBounds: Rect
    ) {
        session?.updateTextLayoutResult(
            textFieldValue,
            offsetMapping,
            textLayoutResult,
            textFieldToRootTransform,
            innerTextFieldBounds,
            decorationBoxBounds
        )
    }
}

package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.foundation.relocation.bringIntoViewResponder
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Rect

/**
 * **La pagina segue il cursore, non il pezzo di testo** (dal 19/10/2026,
 * chiesto dall'utente: la pagina "si sposta da sola senza senso").
 *
 * Di suo Compose, quando un campo di testo prende il fuoco o la tastiera
 * accorcia lo schermo, porta in vista **tutto il campo**. Finché il testo di
 * una pagina era un campo solo, alto quanto la pagina, non succedeva niente;
 * coi pezzi di 50 righe (v0.113.1) "tutto il campo" vuol dire mettere l'inizio
 * del pezzo in cima allo schermo (toccando a metà pagina, la pagina saltava
 * su) o la sua fine appena sopra la tastiera (unendo col backspace una riga a
 * quella del pezzo sopra, la pagina saltava giù).
 *
 * Qui, per un pezzo di testo, a chi fa scorrere la pagina si dice dove sta il
 * **cursore** ([caretRect]): sia quando qualcuno chiede di portare in vista
 * qualcosa del campo (il fuoco, la selezione, il cursore che si muove), sia
 * quando la tastiera sale e la pagina tiene in vista "quello che ha il fuoco".
 * Così la pagina si muove solo se il cursore finirebbe sotto la tastiera o
 * sotto i pulsanti in alto, e solo di quanto basta.
 */
class CaretInView {
    /**
     * Il cursore, coi margini sopra e sotto, nelle coordinate del campo (il
     * riquadro che porta [followCaretOnly], dopo i suoi margini); null se non
     * si sa ancora (il campo non è stato misurato).
     */
    var caretRect: () -> Rect? = { null }
}

/**
 * Sul riquadro che contiene il campo, **dopo** i suoi margini: le richieste dei
 * figli diventano il cursore, e quello che la pagina tiene in vista del campo
 * col fuoco è il cursore.
 *
 * Il secondo pezzo, fino a Compose 1.6, passava da un aggancio interno di
 * Compose (`ModifierLocalFocusedBoundsObserver`), che dalla 1.7 non c'è più: la
 * pagina chiede al fuoco "dov'è il campo che ha il fuoco". Dal 03/11/2026
 * (Compose 1.11) glielo dice `focusRect`, la parte del campo che conta per il
 * fuoco: il cursore. Vale per il campo qui dentro, che è il primo a poter
 * prendere il fuoco sotto questo riquadro.
 */
fun Modifier.followCaretOnly(caret: CaretInView): Modifier =
    this
        .bringIntoViewResponder(object : BringIntoViewResponder {
            // Chiunque chieda (il fuoco chiede tutto il campo), si porta in vista il cursore.
            override fun calculateRectForParent(localRect: Rect): Rect = caret.caretRect() ?: localRect

            // Il campo non scorre per conto suo: c'è solo da passare la richiesta in su.
            override suspend fun bringChildIntoView(localRect: () -> Rect?) = Unit
        })
        .focusProperties {
            caret.caretRect()?.let { focusRect = it }
        }

package com.gabriele.notionlocal.ui.theme

import androidx.compose.ui.text.font.FontFamily
import com.gabriele.notionlocal.data.entity.PageFont

/** Il nome che l'utente ha chiesto, quello che si vede nella barra Aa. */
val PageFont.label: String
    get() = when (this) {
        PageFont.HELVETICA -> "Helvetica"
        PageFont.GARAMOND -> "Garamond"
        PageFont.ARIAL -> "Arial"
        PageFont.VERDANA -> "Verdana"
        PageFont.GEORGIA -> "Georgia"
        PageFont.CALIBRI -> "Calibri"
        PageFont.FUTURA -> "Futura"
        PageFont.TIMES_NEW_ROMAN -> "Times New Roman"
        PageFont.CAMBRIA -> "Cambria"
        PageFont.CONSOLAS -> "Consolas"
        PageFont.SONGTI -> "Songti"
        PageFont.KAITI -> "Kaiti"
        PageFont.SIMSUN -> "SimSun"
        PageFont.MINCHO -> "Mincho"
        PageFont.GOTHIC -> "Gothic"
        PageFont.KAISEI -> "Kaisei"
    }

/**
 * Il font libero che disegna davvero il testo: nel menu di scelta sta
 * fra parentesi dopo il nome. Per i font scaricati è anche il nome con
 * cui lo si chiede a Google, quindi va scritto esattamente come nel
 * catalogo di Google Fonts — salvo SimSun, che dice anche il peso (vedi
 * `fontFamily`).
 */
val PageFont.lookalike: String
    get() = when (this) {
        // Arial nasce come copia di Helvetica, e il suo sosia (Arimo)
        // sarebbe stato identico a quello di Arial: Inter è della stessa
        // famiglia di caratteri senza grazie "svizzeri", e almeno si
        // distingue.
        PageFont.HELVETICA -> "Inter"
        PageFont.GARAMOND -> "EB Garamond"
        PageFont.ARIAL -> "Arimo"
        // Di Verdana non esiste un sosia libero fedele: Noto Sans è il
        // più vicino per ampiezza e leggibilità.
        PageFont.VERDANA -> "Noto Sans"
        PageFont.GEORGIA -> "Gelasio"
        PageFont.CALIBRI -> "Carlito"
        PageFont.FUTURA -> "Jost"
        PageFont.TIMES_NEW_ROMAN -> "Tinos"
        PageFont.CAMBRIA -> "Caladea"
        PageFont.CONSOLAS -> "Inconsolata"
        PageFont.SONGTI -> "Noto Serif SC"
        // Un kaishu a pennello: il più vicino al Kaiti fra quelli liberi
        // che coprono il cinese semplificato.
        PageFont.KAITI -> "Ma Shan Zheng"
        // SimSun è un Song dai tratti sottili: lo stesso Noto Serif SC del
        // Songti, ma nel peso Light, che gli somiglia e dal Songti si
        // distingue. (Al suo posto c'era MS YaHei → Noto Sans SC, identico
        // al font cinese di sistema: tolto il 25/09/2026.)
        PageFont.SIMSUN -> "Noto Serif SC Light"
        PageFont.MINCHO -> "Noto Serif JP"
        PageFont.GOTHIC -> "Noto Sans JP"
        PageFont.KAISEI -> "Kaisei Opti"
    }

/** I tre gruppi del menu, separati da un divisore, nell'ordine chiesto dall'utente. */
enum class PageFontGroup { LATIN, CHINESE, JAPANESE }

val PageFont.group: PageFontGroup
    get() = when (this) {
        PageFont.SONGTI, PageFont.KAITI, PageFont.SIMSUN -> PageFontGroup.CHINESE
        PageFont.MINCHO, PageFont.GOTHIC, PageFont.KAISEI -> PageFontGroup.JAPANESE
        else -> PageFontGroup.LATIN
    }

/**
 * **La famiglia da dare al testo** di una pagina con questo font: sul telefono
 * i font inclusi nell'app e quelli scaricati da Google (vedi
 * `PageFonts.android.kt`); sul PC, per ora, il carattere di sistema più
 * simile (con grazie, senza, a spaziatura fissa).
 */
expect fun PageFont.fontFamily(): FontFamily

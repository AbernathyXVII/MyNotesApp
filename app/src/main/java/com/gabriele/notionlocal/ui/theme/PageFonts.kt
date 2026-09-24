package com.gabriele.notionlocal.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import com.gabriele.notionlocal.R
import com.gabriele.notionlocal.data.entity.PageFont
import androidx.compose.ui.text.font.Font as ResourceFont
import androidx.compose.ui.text.googlefonts.Font as DownloadableFont

/**
 * I font delle pagine: il nome che vede l'utente, il sosia libero che lo
 * disegna davvero, e da dove arriva.
 *
 * **Nessuno dei font che l'utente ha chiesto è davvero dentro l'app.**
 * Helvetica, Arial, Calibri e gli altri sono commerciali: il telefono
 * non li ha e un'app non li può distribuire. Al loro posto ci sono dei
 * sosia con licenza libera (SIL Open Font License), alcuni disegnati
 * apposta con le stesse identiche misure dell'originale (Carlito per
 * Calibri, Arimo per Arial, Tinos per Times New Roman, Caladea per
 * Cambria, Gelasio per Georgia). Scelta dell'utente: **nella barra Aa si
 * legge solo il nome originale**, nel menu di scelta il nome originale
 * col sosia fra parentesi.
 *
 * **Da dove arrivano**, scelta dell'utente anche questa:
 *  - gli occidentali stanno **dentro l'app** (`res/font`, circa 12 MB):
 *    funzionano sempre, anche senza rete. Le licenze sono in
 *    `assets/font_licenses/OFL.txt`;
 *  - i cinesi e i giapponesi pesano 10-20 MB l'uno, e **si scaricano da
 *    Google Play Services** la prima volta che servono; poi restano sul
 *    telefono. Finché non sono arrivati — o se il telefono è offline la
 *    prima volta — il testo si vede col font di sistema, e cambia da solo
 *    appena il font c'è.
 *
 * Il Lishu (隶书) l'utente l'aveva chiesto, ma un Lishu libero non
 * esiste: su sua scelta è stato tolto.
 */

/** Il corpo del testo quando la pagina non ne ha scelto uno: quello che c'era prima dei font. */
const val DEFAULT_PAGE_FONT_SIZE = 16
const val MIN_PAGE_FONT_SIZE = 5
const val MAX_PAGE_FONT_SIZE = 72

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
        PageFont.MS_YAHEI -> "MS YaHei"
        PageFont.MINCHO -> "Mincho"
        PageFont.GOTHIC -> "Gothic"
        PageFont.KAISEI -> "Kaisei"
    }

/**
 * Il font libero che disegna davvero il testo: nel menu di scelta sta
 * fra parentesi dopo il nome. Per i font scaricati è anche il nome con
 * cui lo si chiede a Google, quindi va scritto esattamente come nel
 * catalogo di Google Fonts.
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
        PageFont.MS_YAHEI -> "Noto Sans SC"
        PageFont.MINCHO -> "Noto Serif JP"
        PageFont.GOTHIC -> "Noto Sans JP"
        PageFont.KAISEI -> "Kaisei Opti"
    }

/** I tre gruppi del menu, separati da un divisore, nell'ordine chiesto dall'utente. */
enum class PageFontGroup { LATIN, CHINESE, JAPANESE }

val PageFont.group: PageFontGroup
    get() = when (this) {
        PageFont.SONGTI, PageFont.KAITI, PageFont.MS_YAHEI -> PageFontGroup.CHINESE
        PageFont.MINCHO, PageFont.GOTHIC, PageFont.KAISEI -> PageFontGroup.JAPANESE
        else -> PageFontGroup.LATIN
    }

/** Il fornitore dei font scaricabili: Google Play Services, coi certificati di `res/values/font_certs.xml`. */
private val googleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

/** Un font dentro l'app: normale e grassetto, e il corsivo se il font ce l'ha. */
private fun bundled(regular: Int, bold: Int, italic: Int? = null, boldItalic: Int? = null) = FontFamily(
    listOfNotNull(
        ResourceFont(regular, FontWeight.Normal),
        ResourceFont(bold, FontWeight.Bold),
        italic?.let { ResourceFont(it, FontWeight.Normal, FontStyle.Italic) },
        boldItalic?.let { ResourceFont(it, FontWeight.Bold, FontStyle.Italic) }
    )
)

/**
 * Un font scaricato da Google. Il grassetto si chiede solo se il font ce
 * l'ha: Ma Shan Zheng ne ha uno solo, e lì il grassetto lo simula il
 * telefono.
 */
private fun downloadable(name: String, hasBold: Boolean = true): FontFamily {
    val font = GoogleFont(name)
    return FontFamily(
        listOfNotNull(
            DownloadableFont(googleFont = font, fontProvider = googleFontsProvider, weight = FontWeight.Normal),
            if (hasBold) {
                DownloadableFont(googleFont = font, fontProvider = googleFontsProvider, weight = FontWeight.Bold)
            } else {
                null
            }
        )
    )
}

/**
 * La famiglia da dare al testo. Costruita una volta sola per font e poi
 * riusata: ricrearla a ogni disegno farebbe ricominciare il caricamento.
 */
private val families = mutableMapOf<PageFont, FontFamily>()

fun PageFont.fontFamily(): FontFamily = families.getOrPut(this) {
    when (this) {
        PageFont.HELVETICA -> bundled(R.font.inter_regular, R.font.inter_bold, R.font.inter_italic, R.font.inter_bold_italic)
        PageFont.GARAMOND -> bundled(R.font.eb_garamond_regular, R.font.eb_garamond_bold, R.font.eb_garamond_italic, R.font.eb_garamond_bold_italic)
        PageFont.ARIAL -> bundled(R.font.arimo_regular, R.font.arimo_bold, R.font.arimo_italic, R.font.arimo_bold_italic)
        PageFont.VERDANA -> bundled(R.font.noto_sans_regular, R.font.noto_sans_bold, R.font.noto_sans_italic, R.font.noto_sans_bold_italic)
        PageFont.GEORGIA -> bundled(R.font.gelasio_regular, R.font.gelasio_bold, R.font.gelasio_italic, R.font.gelasio_bold_italic)
        PageFont.CALIBRI -> bundled(R.font.carlito_regular, R.font.carlito_bold, R.font.carlito_italic, R.font.carlito_bold_italic)
        PageFont.FUTURA -> bundled(R.font.jost_regular, R.font.jost_bold, R.font.jost_italic, R.font.jost_bold_italic)
        PageFont.TIMES_NEW_ROMAN -> bundled(R.font.tinos_regular, R.font.tinos_bold, R.font.tinos_italic, R.font.tinos_bold_italic)
        PageFont.CAMBRIA -> bundled(R.font.caladea_regular, R.font.caladea_bold, R.font.caladea_italic, R.font.caladea_bold_italic)
        // Inconsolata non ha il corsivo: lo simula il telefono.
        PageFont.CONSOLAS -> bundled(R.font.inconsolata_regular, R.font.inconsolata_bold)
        PageFont.SONGTI -> downloadable(lookalike)
        PageFont.KAITI -> downloadable(lookalike, hasBold = false)
        PageFont.MS_YAHEI -> downloadable(lookalike)
        PageFont.MINCHO -> downloadable(lookalike)
        PageFont.GOTHIC -> downloadable(lookalike)
        PageFont.KAISEI -> downloadable(lookalike)
    }
}

/**
 * Il carattere del testo di una pagina: il font scelto (null = quello di
 * sistema, come prima) e il corpo. Passa da un `CompositionLocal` perché
 * il testo di una pagina si disegna in una dozzina di posti diversi —
 * il campo unito, le isole, le celle delle tabelle, i toggle — in fondo
 * a catene di composable che non hanno altro motivo per saperlo.
 *
 * **Il corpo scala tutto in proporzione**, non solo il testo normale: i
 * titoli H1-H3, l'altezza delle righe, i segni degli elenchi. A 16, il
 * corpo di sempre, ogni misura resta esattamente quella di prima; a 32
 * tutto è grande il doppio, e un titolo resta un titolo.
 */
@Immutable
data class PageTypography(
    val fontFamily: FontFamily? = null,
    val size: Int = DEFAULT_PAGE_FONT_SIZE
) {
    val scale: Float get() = size / DEFAULT_PAGE_FONT_SIZE.toFloat()

    /** Una misura di testo portata al corpo scelto. */
    fun scaled(unit: TextUnit): TextUnit = if (unit.isSpecified) unit * scale else unit

    /** Uno stile di testo della pagina col font e il corpo scelti. */
    fun apply(style: TextStyle): TextStyle = style.copy(
        fontFamily = fontFamily ?: style.fontFamily,
        fontSize = scaled(style.fontSize),
        lineHeight = scaled(style.lineHeight)
    )

    /**
     * Solo il font, non il corpo: per il titolo della pagina, che ha già
     * una misura sua e a 72 diventerebbe più largo dello schermo.
     */
    fun fontOnly(style: TextStyle): TextStyle = style.copy(fontFamily = fontFamily ?: style.fontFamily)
}

val LocalPageTypography = staticCompositionLocalOf { PageTypography() }

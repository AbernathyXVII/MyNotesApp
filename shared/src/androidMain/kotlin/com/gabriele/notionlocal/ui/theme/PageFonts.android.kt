package com.gabriele.notionlocal.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.gabriele.notionlocal.shared.R
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

// Il corpo del testo (predefinito, minimo, massimo): `PageFontSizes.kt`, nel codice comune.

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
 * telefono. `regular` è il peso da usare per il testo normale: di solito
 * Normal, Light per SimSun (Compose, cercando il normale, prende il più
 * vicino che la famiglia ha).
 */
private fun downloadable(name: String, hasBold: Boolean = true, regular: FontWeight = FontWeight.Normal): FontFamily {
    val font = GoogleFont(name)
    return FontFamily(
        listOfNotNull(
            DownloadableFont(googleFont = font, fontProvider = googleFontsProvider, weight = regular),
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

actual fun PageFont.fontFamily(): FontFamily = families.getOrPut(this) {
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
        PageFont.SIMSUN -> downloadable("Noto Serif SC", regular = FontWeight.Light)
        PageFont.MINCHO -> downloadable(lookalike)
        PageFont.GOTHIC -> downloadable(lookalike)
        PageFont.KAISEI -> downloadable(lookalike)
    }
}

package com.gabriele.notionlocal.data

import com.gabriele.notionlocal.data.entity.PageEntity

/**
 * **L'emoji di riserva delle pagine**, il foglietto che si vede quando
 * nessuno ha scelto un'icona. Nel campo `icon` vuol dire "niente scelto":
 * una pagina lo mostra, un database mostra il segno della tabella, la
 * pagina di una riga niente.
 */
const val DEFAULT_PAGE_ICON = "📄"

/** L'emoji scelta davvero, o null se nel campo c'è solo quella di riserva. */
fun chosenEmoji(icon: String?): String? = icon?.takeIf { it.isNotBlank() && it != DEFAULT_PAGE_ICON }

val PageEntity.chosenEmoji: String? get() = chosenEmoji(icon)

/** Se la pagina ha un'icona scelta: un'immagine, un'icona di Material o un'emoji. */
val PageEntity.hasChosenIcon: Boolean get() = iconImage != null || iconSymbol != null || chosenEmoji != null

/**
 * **Un'icona di Material** con il suo colore (dal 08/10/2026, la scheda
 * "Icons"): `name` è quello della libreria ("Home"), `color` "#RRGGBB" o
 * null per il colore del testo (bianco sul tema scuro). Nel database sta in
 * una stringa sola: "Home" o "Home|#E03E3E".
 */
data class IconSymbol(val name: String, val color: String? = null) {
    val encoded: String get() = if (color == null) name else "$name|$color"

    companion object {
        fun parse(value: String?): IconSymbol? {
            if (value.isNullOrBlank()) return null
            val name = value.substringBefore('|').trim()
            if (name.isEmpty()) return null
            val color = value.substringAfter('|', "").trim().takeIf { it.isNotEmpty() }
            return IconSymbol(name, color)
        }
    }
}

/**
 * **Cosa si è scelto nel selettore delle icone** (dal 07/10/2026, come su
 * Notion): un'emoji, un'immagine caricata, o niente ("Remove").
 */
sealed interface IconChoice {
    data class Emoji(val emoji: String) : IconChoice
    data class Image(val fileName: String) : IconChoice
    data class Symbol(val symbol: IconSymbol) : IconChoice
    data object None : IconChoice
}

/** I tre campi della pagina per una scelta: l'emoji (o quella di riserva), l'immagine e l'icona di Material. */
val IconChoice.iconField: String get() = (this as? IconChoice.Emoji)?.emoji ?: DEFAULT_PAGE_ICON
val IconChoice.imageField: String? get() = (this as? IconChoice.Image)?.fileName
val IconChoice.symbolField: String? get() = (this as? IconChoice.Symbol)?.symbol?.encoded

/**
 * L'immagine che una scelta manda via, da cancellare dalla cartella: quella
 * di prima, se la nuova non è proprio lei.
 */
fun replacedImage(previous: String?, choice: IconChoice): String? =
    previous?.takeIf { it != choice.imageField }

/** L'icona della pagina di una riga, per i database: l'immagine, l'icona di Material o l'emoji scelta. */
data class RowIconRef(val emoji: String?, val image: String?, val symbol: String? = null)

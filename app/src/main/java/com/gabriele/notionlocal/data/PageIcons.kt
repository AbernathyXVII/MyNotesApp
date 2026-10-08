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

/** Se la pagina ha un'icona scelta: un'immagine o un'emoji. */
val PageEntity.hasChosenIcon: Boolean get() = iconImage != null || chosenEmoji != null

/**
 * **Cosa si è scelto nel selettore delle icone** (dal 07/10/2026, come su
 * Notion): un'emoji, un'immagine caricata, o niente ("Remove").
 */
sealed interface IconChoice {
    data class Emoji(val emoji: String) : IconChoice
    data class Image(val fileName: String) : IconChoice
    data object None : IconChoice
}

/** I due campi della pagina per una scelta: l'emoji (o quella di riserva) e l'immagine. */
val IconChoice.iconField: String get() = (this as? IconChoice.Emoji)?.emoji ?: DEFAULT_PAGE_ICON
val IconChoice.imageField: String? get() = (this as? IconChoice.Image)?.fileName

/**
 * L'immagine che una scelta manda via, da cancellare dalla cartella: quella
 * di prima, se la nuova non è proprio lei.
 */
fun replacedImage(previous: String?, choice: IconChoice): String? =
    previous?.takeIf { it != choice.imageField }

/** L'icona della pagina di una riga, per i database: l'immagine o l'emoji scelta. */
data class RowIconRef(val emoji: String?, val image: String?)

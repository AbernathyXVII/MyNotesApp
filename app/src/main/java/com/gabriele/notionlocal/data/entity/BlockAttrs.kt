package com.gabriele.notionlocal.data.entity

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * **Quello che un blocco ha in più del testo** (dal 06/10/2026), salvato in
 * `BlockEntity.attrsJson`.
 *
 * Serve ai blocchi chiesti dall'utente quel giorno — riquadro, codice,
 * immagine, video, audio, file, segnalibro, link a una pagina — e ognuno
 * usa solo le voci che lo riguardano. Una colonna sola in JSON invece di
 * dieci colonne quasi sempre vuote: ogni voce ha un valore predefinito,
 * quindi aggiungerne una domani non chiede una migrazione, come per
 * `RichTextSpan`.
 *
 * I file stanno nella cartella delle immagini dell'app (`PageImageStore`),
 * per **nome** e non per percorso: lo stesso motivo per cui ci stanno le
 * icone e le copertine.
 */
@Serializable
data class BlockAttrs(
    /** Il file copiato dentro l'app: l'immagine, il video, l'audio o il file. */
    val file: String? = null,
    /** Un indirizzo: il video o l'audio presi da un link, il sito del segnalibro, il file da un link. */
    val url: String? = null,
    /** Il nome del file com'era sul telefono; per il segnalibro, il titolo del sito. */
    val name: String? = null,
    /** Il tipo del file ("video/mp4", "application/pdf"…): serve per aprirlo con l'app giusta. */
    val mime: String? = null,
    /** Quanto pesa il file, in byte. */
    val size: Long? = null,
    /** L'immagine: quanto è larga, in frazione della larghezza della pagina (da 0,2 a 1). */
    val width: Float? = null,
    /** Il codice: il linguaggio scelto, col nome com'è nell'elenco (`CODE_LANGUAGES`). */
    val language: String? = null,
    /** Il riquadro: l'emoji dell'icona; null = 💡, "" = nessuna icona. */
    val icon: String? = null,
    /** Il riquadro: l'immagine messa al posto dell'emoji. */
    val iconImage: String? = null,
    /** Il riquadro: un'icona di Material, "Home" o "Home|#E03E3E" (vedi `IconSymbol`). */
    val symbol: String? = null,
    /** Il riquadro: il colore dello sfondo, "#RRGGBB"; null = il grigio di sempre. */
    val color: String? = null,
    /** "Link to page": la pagina richiamata. */
    val pageId: String? = null,
    /** Il segnalibro: la descrizione del sito. */
    val description: String? = null,
    /** Il segnalibro: l'immagine di anteprima del sito, scaricata nell'app. */
    val image: String? = null
) {
    /** I file dell'app che questo blocco usa: vanno copiati con lui e cancellati con lui. */
    val files: List<String> get() = listOfNotNull(file, iconImage, image)

    companion object {
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

        /** Le voci di un blocco; un JSON rovinato vale come vuoto, invece di far chiudere l'app. */
        fun parse(attrsJson: String?): BlockAttrs =
            attrsJson?.takeIf { it.isNotBlank() }
                ?.let { runCatching { json.decodeFromString<BlockAttrs>(it) }.getOrNull() }
                ?: BlockAttrs()

        fun encode(attrs: BlockAttrs): String = json.encodeToString(attrs)
    }
}

/** Le voci in più di un blocco: vedi `BlockAttrs`. */
val BlockEntity.attrs: BlockAttrs get() = BlockAttrs.parse(attrsJson)

/** L'emoji di riserva di un riquadro, quella con cui nasce: la lampadina, come su Notion. */
const val DEFAULT_CALLOUT_ICON = "💡"

/**
 * I tipi che si scrivono **su più righe dentro un blocco solo**: l'Invio
 * va a capo dentro il riquadro, la citazione o il codice invece di fare
 * una riga nuova sotto. Diventando un tipo che sta su una riga, il loro
 * testo si divide in tante righe quante sono le sue (vedi
 * `PageRepository.convertMultiline`).
 */
val MULTILINE_TYPES = setOf(BlockType.CALLOUT, BlockType.QUOTE, BlockType.CODE)

/** I blocchi che mostrano un file o un indirizzo e non hanno testo da scrivere. */
val MEDIA_TYPES = setOf(BlockType.IMAGE, BlockType.VIDEO, BlockType.AUDIO, BlockType.FILE, BlockType.BOOKMARK)

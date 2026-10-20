package com.gabriele.notionlocal.data.entity

/**
 * **Un blocco da creare** (dal 20/10/2026, per il Web Clipper): il tipo, il
 * testo coi suoi stili, le voci in più (il file di un'immagine), il rientro
 * degli elenchi e, per una tabella, le celle riga per riga. Vedi
 * `PageRepository.insertAtFirstEmptyLine`.
 */
data class NewBlock(
    val type: BlockType = BlockType.PARAGRAPH,
    val spans: List<RichTextSpan> = emptyList(),
    val attrs: BlockAttrs = BlockAttrs(),
    val indentLevel: Int = 0,
    val cells: List<List<String>>? = null
)

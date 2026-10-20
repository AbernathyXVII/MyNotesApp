package com.gabriele.notionlocal.data.entity

/**
 * **Le righe di una pagina nell'ordine in cui si leggono** (dal 20/10/2026):
 * ogni blocco seguito da quelli che ha dentro (un toggle, le sue righe), con
 * quanto è annidato. Servono al Web Clipper, per trovare la prima riga vuota,
 * e a "Move to line…", per scegliere sotto quale riga mettere un blocco.
 */
data class PageLine(val block: BlockEntity, val depth: Int)

fun List<BlockEntity>.inReadingOrder(): List<PageLine> {
    val children = groupBy { it.parentBlockId }.mapValues { (_, list) -> list.sortedBy { it.orderIndex } }
    val out = ArrayList<PageLine>(size)
    fun visit(parentId: String?, depth: Int) {
        for (block in children[parentId].orEmpty()) {
            out += PageLine(block, depth)
            visit(block.id, depth + 1)
        }
    }
    visit(null, 0)
    return out
}

/**
 * **Il primo spazio libero dall'alto** (chiesto dall'utente per il Web
 * Clipper: "di default nel primo blocco vuoto"): la prima riga di testo senza
 * niente scritto (o solo spazi) e senza righe dentro. Null se non ce n'è.
 */
fun List<BlockEntity>.firstEmptyLine(textOf: (BlockEntity) -> String): BlockEntity? {
    val parents = mapNotNullTo(HashSet()) { it.parentBlockId }
    return inReadingOrder().firstOrNull { (block, _) ->
        block.type == BlockType.PARAGRAPH && block.id !in parents && textOf(block).isBlank()
    }?.block
}

/**
 * I posti dove "Move to line…" può mettere [moving]: tutte le righe tranne
 * lui e quelle che ha dentro (un blocco non va dentro se stesso).
 */
fun List<BlockEntity>.moveTargetsFor(moving: String): List<PageLine> {
    val lines = inReadingOrder()
    val start = lines.indexOfFirst { it.block.id == moving }
    if (start < 0) return lines
    val depth = lines[start].depth
    var end = start + 1
    while (end < lines.size && lines[end].depth > depth) end++
    return lines.subList(0, start) + lines.subList(end, lines.size)
}

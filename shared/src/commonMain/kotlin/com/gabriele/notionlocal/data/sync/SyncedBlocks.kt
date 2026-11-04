package com.gabriele.notionlocal.data.sync

import com.gabriele.notionlocal.data.entity.BlockAttrs
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.TableCellEntity
import com.gabriele.notionlocal.data.entity.attrs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * **Una riga del contenuto di un blocco sincronizzato** (dal 03/11/2026):
 * quello che deve essere uguale in tutte le copie, senza id né pagina.
 * [depth] è 0 per i figli diretti del blocco.
 *
 * Una sottopagina o un database dentro il blocco ha la sua casa in una
 * pagina sola: nelle altre copie è un "Link to page" a lei, e per il
 * confronto lo è anche nella sua (vedi [SyncedBlocks.lineOf]).
 */
data class SyncedLine(
    val depth: Int,
    val type: BlockType,
    val textJson: String,
    val isChecked: Boolean,
    val isExpanded: Boolean,
    val tableRows: Int,
    val tableCols: Int,
    val numberStartsAt: Int?,
    val indentLevel: Int,
    val notifyAt: Long?,
    val attrsJson: String,
    /** Le celle di una tabella, (riga, colonna, testo) in ordine: le legge solo il repository. */
    val cells: List<Triple<Int, Int, String>> = emptyList()
)

/**
 * **I blocchi sincronizzati** (dal 03/11/2026, chiesti dall'utente: "lo
 * stesso pezzo di testo in più pagine, che cambia ovunque insieme").
 *
 * Un blocco `SYNCED` è un contenitore, come un toggle sempre aperto; le sue
 * copie, in qualunque pagina, hanno lo stesso `BlockAttrs.syncId`. Nessuna è
 * l'originale: ognuna ha le sue righe vere, nella sua pagina, e scrivendo
 * in una le altre si riscrivono uguali (`PageRepository.pushSynced`).
 *
 * Chi si accorge del cambiamento è la pagina aperta (`SyncedWatcher`), che
 * vede i suoi blocchi a ogni modifica: una copia diversa da com'era, e non
 * per una riscrittura arrivata da un'altra copia ([expect]), si manda alle
 * altre dopo una pausa ([schedule]), come un salvataggio. La spinta gira
 * fuori dalla pagina, così arriva anche uscendo subito.
 */
object SyncedBlocks {
    /** La pausa dopo l'ultima modifica: le lettere di fila fanno una riscrittura sola. */
    @Volatile
    var debounceMs = 600L

    /** I tipi che la prima riga tiene facendo "Turn into" (`PageRepository.makeSynced`): gli altri diventano testo. */
    val FIRST_LINE_TYPES = setOf(
        BlockType.PARAGRAPH,
        BlockType.BULLET_LIST_ITEM,
        BlockType.NUMBERED_LIST_ITEM,
        BlockType.CHECKBOX,
        BlockType.CALLOUT,
        BlockType.QUOTE
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pending = mutableMapOf<String, Job>()
    private val expected = mutableMapOf<String, String>()

    /** Le righe sotto [rootId] in ordine, con la profondità, fra [blocks] (anche tutta la pagina). */
    fun subtree(rootId: String, blocks: List<BlockEntity>): List<Pair<Int, BlockEntity>> {
        val byParent = blocks.groupBy { it.parentBlockId }
        val out = mutableListOf<Pair<Int, BlockEntity>>()
        val visited = mutableSetOf(rootId)
        fun walk(parentId: String, depth: Int) {
            byParent[parentId].orEmpty().sortedBy { it.orderIndex }.forEach { child ->
                if (!visited.add(child.id)) return@forEach
                out += depth to child
                walk(child.id, depth + 1)
            }
        }
        walk(rootId, 0)
        return out
    }

    /**
     * La riga [block] com'è per il confronto e per le copie. Una sottopagina
     * o un database diventa un "Link to page" (la pagina vive in una casa
     * sola), un blocco sincronizzato dentro un altro un toggle: due gruppi
     * uno dentro l'altro si riscriverebbero a vicenda.
     */
    fun lineOf(depth: Int, block: BlockEntity, cells: List<TableCellEntity> = emptyList()): SyncedLine =
        when (block.type) {
            BlockType.PAGE_LINK, BlockType.DATABASE_LINK -> SyncedLine(
                depth = depth,
                type = BlockType.PAGE_MENTION,
                textJson = "[]",
                isChecked = false,
                isExpanded = true,
                tableRows = 2,
                tableCols = 2,
                numberStartsAt = null,
                indentLevel = block.indentLevel,
                notifyAt = null,
                attrsJson = BlockAttrs.encode(BlockAttrs(pageId = block.linkedPageId))
            )
            else -> SyncedLine(
                depth = depth,
                type = if (block.type == BlockType.SYNCED) BlockType.TOGGLE else block.type,
                textJson = block.textJson,
                isChecked = block.isChecked,
                isExpanded = block.isExpanded,
                tableRows = block.tableRows,
                tableCols = block.tableCols,
                numberStartsAt = block.numberStartsAt,
                indentLevel = block.indentLevel,
                notifyAt = block.notifyAt,
                attrsJson = if (block.type == BlockType.SYNCED) {
                    BlockAttrs.encode(block.attrs.copy(syncId = null))
                } else {
                    block.attrsJson
                },
                cells = cells.sortedWith(compareBy({ it.rowIndex }, { it.colIndex }))
                    .map { Triple(it.rowIndex, it.colIndex, it.text) }
            )
        }

    /** Il contenuto del blocco [rootId] fra [blocks], senza le celle delle tabelle. */
    fun linesOf(rootId: String, blocks: List<BlockEntity>): List<SyncedLine> =
        subtree(rootId, blocks).map { (depth, block) -> lineOf(depth, block) }

    /** Una firma del contenuto per vedere se è cambiato: senza le celle, che la pagina non vede. */
    fun keyOf(lines: List<SyncedLine>): String = lines.map { it.copy(cells = emptyList()) }.toString()

    /** Il pezzo di [BlockAttrs] JSON che trova le copie del gruppo [syncId] (`BlockDao.getSyncedBlocks`). */
    fun patternOf(syncId: String): String = "%\"$syncId\"%"

    /** La copia [blockId] è appena stata riscritta col contenuto [key]: la pagina che la vede non deve rimandarla. */
    fun expect(blockId: String, key: String) {
        synchronized(expected) { expected[blockId] = key }
    }

    /** Vero, una volta, se [key] è la riscrittura attesa di [blockId] (vedi [expect]). */
    fun isEcho(blockId: String, key: String): Boolean = synchronized(expected) {
        if (expected[blockId] == key) {
            expected.remove(blockId)
            true
        } else {
            false
        }
    }

    /** Una pagina che apre la copia [blockId] la prende com'è: le attese vecchie non valgono più. */
    fun forget(blockId: String) {
        synchronized(expected) { expected.remove(blockId) }
    }

    /**
     * Manda il contenuto del gruppo [syncId] alle copie, [debounceMs] dopo
     * l'ultima chiamata. Partita, la spinta finisce anche se ne arriva
     * un'altra: quella dopo riparte da capo, coi dati di allora.
     */
    fun schedule(syncId: String, push: suspend () -> Unit) {
        synchronized(pending) {
            pending[syncId]?.cancel()
            pending[syncId] = scope.launch {
                delay(debounceMs)
                withContext(NonCancellable) { runCatching { push() } }
            }
        }
    }

    /** Aspetta le spinte in viaggio (per i test). */
    suspend fun flush() {
        synchronized(pending) { pending.values.toList() }.joinAll()
    }
}

/**
 * **L'occhio di una pagina aperta sui suoi blocchi sincronizzati** (vedi
 * [SyncedBlocks]): a ogni lettura dal database confronta ogni copia con
 * com'era, e quelle cambiate qui le manda alle altre con [push].
 */
class SyncedWatcher(private val push: suspend (String) -> Unit) {
    private val seen = mutableMapOf<String, String>()

    fun onLoaded(blocks: List<BlockEntity>) {
        val synced = blocks.filter { it.type == BlockType.SYNCED }
        if (synced.isEmpty() && seen.isEmpty()) return
        for (block in synced) {
            val syncId = block.attrs.syncId ?: continue
            val key = SyncedBlocks.keyOf(SyncedBlocks.linesOf(block.id, blocks))
            val before = seen.put(block.id, key)
            when {
                before == null -> SyncedBlocks.forget(block.id)
                before == key -> Unit
                SyncedBlocks.isEcho(block.id, key) -> Unit
                else -> SyncedBlocks.schedule(syncId) { push(block.id) }
            }
        }
        seen.keys.retainAll(synced.map { it.id }.toSet())
    }

    /** Una cella di una tabella scritta: le celle non passano dai blocchi, la spinta si chiede qui. */
    fun onCellChanged(tableBlockId: String, blocks: List<BlockEntity>) {
        val byId = blocks.associateBy { it.id }
        var current = byId[tableBlockId]
        val visited = mutableSetOf<String>()
        while (current != null && visited.add(current.id)) {
            if (current.type == BlockType.SYNCED) {
                val syncId = current.attrs.syncId ?: return
                val id = current.id
                SyncedBlocks.schedule(syncId) { push(id) }
                return
            }
            current = current.parentBlockId?.let { byId[it] }
        }
    }

    /** Un'altra pagina: si riparte da capo. */
    fun reset() = seen.clear()
}

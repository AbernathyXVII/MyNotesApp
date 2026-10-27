package com.gabriele.notionlocal.data.buttons

import androidx.room.withTransaction
import com.gabriele.notionlocal.data.entity.BlockAttrs
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.NewBlock
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.repository.DatabaseRepository
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.security.AppLock
import java.time.ZoneId

/**
 * Dove sta il bottone toccato: le azioni che dicono "qui" partono da lì.
 * [pageId] è la pagina in cui vanno i blocchi di "Insert blocks" (per il
 * bottone di una riga, null: la pagina della riga, fatta nascere al bisogno);
 * [blockId] il blocco del bottone, per "sopra" e "sotto"; [rowId] la riga di
 * "This page".
 */
data class ButtonPlace(
    val pageId: String?,
    val blockId: String? = null,
    val rowId: String? = null
)

/**
 * Quello che il motore dei bottoni chiede alla schermata: la domanda di
 * "Show confirmation", le pagine e i link da aprire, la notifica.
 */
interface ButtonHost {
    /** "Show confirmation": true per andare avanti, false per fermarsi. */
    suspend fun confirm(message: String): Boolean

    /**
     * Mette i blocchi di "Insert blocks". L'editor della pagina aperta lo fa
     * lui, per poterlo annullare; null vuol dire "fallo tu" e il motore li
     * scrive nel database.
     */
    suspend fun insertBlocks(pageId: String, anchorBlockId: String?, place: InsertPlace, blocks: List<NewBlock>): Boolean? = null

    fun openPage(pageId: String, isDatabase: Boolean)
    fun openUrl(url: String)

    /** False se il telefono non la lascia mostrare. */
    fun notify(title: String, text: String): Boolean
}

/** Perché un'azione non si è potuta fare: la schermata lo dice con due parole. */
enum class ButtonProblem {
    /** Il database scelto non c'è più (o è nel cestino). */
    NO_DATABASE,
    /** Il database è protetto da password e chiuso. */
    LOCKED_DATABASE,
    /** "This page" ma il bottone non sta in una riga né nella pagina di una riga. */
    NO_THIS_PAGE,
    /** La pagina da aprire, o in cui mettere i blocchi, non c'è più. */
    NO_PAGE,
    /** La pagina è bloccata ("Lock page"): non ci si scrive. */
    LOCKED_PAGE,
    /** Una proprietà scelta non c'è più, o non è più di un tipo che si può cambiare. */
    NO_PROPERTY,
    /** Un valore che non va bene per la proprietà (un numero che non è un numero, un tag cancellato). */
    BAD_VALUE,
    /** Il filtro di "Edit pages in" guarda una proprietà che non c'è più: per sicurezza non si cambia niente. */
    NO_FILTER,
    /** Le notifiche dell'app sono spente nel telefono. */
    NOTIFICATIONS_OFF
}

/**
 * Com'è andato un tocco: quante azioni sono state fatte, se una conferma l'ha
 * fermato, i problemi, quante righe sono nate, quante sono cambiate nei
 * database scelti ([changedRows]) e se è cambiata "This page", e quanti
 * blocchi sono stati messi.
 */
data class ButtonOutcome(
    val done: Int,
    val cancelled: Boolean,
    val problems: List<ButtonProblem>,
    val createdRows: Int,
    val changedRows: Int,
    val insertedBlocks: Int,
    val thisPageChanged: Boolean = false
)

/**
 * **Il motore dei bottoni** (dal 27/10/2026): esegue le azioni di un
 * `ButtonConfig` una dopo l'altra. Un'azione che non si può fare (il
 * database cancellato, la proprietà sparita) si salta e finisce fra i
 * problemi, e le altre vanno avanti, come su Notion; solo "Cancel" in una
 * conferma ferma quelle dopo.
 *
 * Le pagine e i link da aprire si aprono **alla fine**: uscendo dalla pagina
 * a metà, le azioni rimaste si perderebbero con lei. Si apre l'ultima pagina
 * chiesta, dopo i link.
 */
class ButtonRunner(private val pages: PageRepository) {

    private val db = pages.database
    private val rows = DatabaseRepository(db)
    private val dao = db.databaseDao()

    suspend fun run(
        config: ButtonConfig,
        place: ButtonPlace,
        host: ButtonHost,
        now: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): ButtonOutcome {
        val problems = mutableListOf<ButtonProblem>()
        var done = 0
        var created = 0
        var changed = 0
        var inserted = 0
        var thisPage = false
        var pageToOpen: Pair<String, Boolean>? = null
        val urls = mutableListOf<String>()

        for (step in config.steps) {
            when (step) {
                is ButtonStep.Confirm -> if (!host.confirm(step.message)) {
                    return ButtonOutcome(done, true, problems, created, changed, inserted, thisPage)
                }

                is ButtonStep.InsertBlocks -> inserted += insertBlocks(step, place, host, problems)

                is ButtonStep.AddPage -> {
                    val target = database(step.databaseId, problems) ?: continue
                    val row = addRow(target, step, now, zone, problems)
                    created++
                    if (step.openAfter) {
                        val rowPage = pages.ensureRowPage(row.id)
                        if (rowPage != null) pageToOpen = rowPage to false else problems += ButtonProblem.NO_PAGE
                    }
                }

                is ButtonStep.EditPages -> {
                    val count = editPages(step, place, now, zone, problems)
                    if (step.databaseId == null) thisPage = thisPage || count > 0 else changed += count
                }

                is ButtonStep.Open -> {
                    val pageId = step.pageId
                    if (pageId != null) {
                        val page = db.pageDao().getById(pageId)?.takeIf { it.trashedAt == null }
                        if (page != null) pageToOpen = page.id to page.isDatabase else problems += ButtonProblem.NO_PAGE
                    } else if (step.url.isNotBlank()) {
                        urls += withScheme(step.url.trim())
                    }
                }

                is ButtonStep.Notify -> {
                    val title = step.title.ifBlank { config.name }
                    if (!host.notify(title, step.text)) problems += ButtonProblem.NOTIFICATIONS_OFF
                }
            }
            done++
        }
        urls.forEach { host.openUrl(it) }
        pageToOpen?.let { (id, isDatabase) -> host.openPage(id, isDatabase) }
        return ButtonOutcome(done, false, problems, created, changed, inserted, thisPage)
    }

    /** Il database vero di [id]: quello di cui è una vista, se è una vista collegata. */
    private suspend fun database(id: String?, problems: MutableList<ButtonProblem>): PageEntity? {
        val page = id?.let { db.pageDao().getById(it) }
        val real = page?.sourceDatabaseId?.let { db.pageDao().getById(it) } ?: page
        if (real == null || real.trashedAt != null || !real.isDatabase) {
            problems += ButtonProblem.NO_DATABASE
            return null
        }
        if (real.isProtected && !AppLock.isUnlocked(real.id)) {
            problems += ButtonProblem.LOCKED_DATABASE
            return null
        }
        return real
    }

    private suspend fun addRow(
        target: PageEntity,
        step: ButtonStep.AddPage,
        now: Long,
        zone: ZoneId,
        problems: MutableList<ButtonProblem>
    ): DatabaseRowEntity = db.withTransaction {
        val last = dao.getRowsForPageOnce(target.id).maxOfOrNull { it.orderIndex } ?: -1
        val row = DatabaseRowEntity(pageId = target.id, title = step.title.trim(), orderIndex = last + 1, createdAt = now)
        rows.addRow(row)
        val columns = dao.getColumnsForPageOnce(target.id)
        val values = newValues(row, emptyMap(), columns, step.edits, now, zone, problems)
        writeValues(row.id, columns, values, single = false)
        row
    }

    /** Quante righe sono cambiate. */
    private suspend fun editPages(
        step: ButtonStep.EditPages,
        place: ButtonPlace,
        now: Long,
        zone: ZoneId,
        problems: MutableList<ButtonProblem>
    ): Int {
        if (step.databaseId == null) {
            val row = place.rowId?.let { dao.getRowById(it) }
            if (row == null) {
                problems += ButtonProblem.NO_THIS_PAGE
                return 0
            }
            // Anche la riga del bottone sta in un database che può essere chiuso.
            database(row.pageId, problems) ?: return 0
            val columns = dao.getColumnsForPageOnce(row.pageId)
            val current = dao.getCellsForRowOnce(row.id).associate { it.columnId to it.value }
            val values = newValues(row, current, columns, step.edits, now, zone, problems)
            if (values.isEmpty()) return 0
            // Una riga sola: la casella si porta dietro le sotto-righe, come toccandola.
            db.withTransaction { writeValues(row.id, columns, values, single = true) }
            return 1
        }

        val target = database(step.databaseId, problems) ?: return 0
        val columns = dao.getColumnsForPageOnce(target.id)
        val allRows = dao.getRowsForPageOnce(target.id)
        val cells = dao.getAllCellsForPageOnce(target.id).groupBy({ it.rowId }, { it.columnId to it.value })
            .mapValues { (_, pairs) -> pairs.toMap() }
        val filter = step.filter
        val filterColumn = filter?.columnId?.takeIf { it != NAME_PROPERTY_ID }?.let { id -> columns.find { it.id == id } }
        if (filter != null && filter.columnId != NAME_PROPERTY_ID && filterColumn == null) {
            problems += ButtonProblem.NO_FILTER
            return 0
        }
        val chosen = allRows.filter { row ->
            filter == null || filter.matches(
                filterColumn,
                if (filter.columnId == NAME_PROPERTY_ID) row.title else cells[row.id]?.get(filter.columnId).orEmpty()
            )
        }
        // **Prima si leggono tutti i valori, poi si scrivono**: invertendo
        // le caselle di righe che hanno sotto-righe, quelle scritte prima
        // cambierebbero le altre prima del loro turno. Così ogni riga ha
        // esattamente quello che chiede il bottone.
        val planned = chosen.map { row ->
            row to newValues(row, cells[row.id].orEmpty(), columns, step.edits, now, zone, problems)
        }.filter { it.second.isNotEmpty() }
        if (planned.isEmpty()) return 0
        db.withTransaction {
            planned.forEach { (row, values) -> writeValues(row.id, columns, values, single = false) }
        }
        return planned.size
    }

    /**
     * I valori nuovi di una riga, solo quelli che cambiano: proprietà → valore
     * ([NAME_PROPERTY_ID] per il nome). Una proprietà cambiata due volte nello
     * stesso passo parte dal valore già cambiato.
     */
    private fun newValues(
        row: DatabaseRowEntity,
        current: Map<String, String>,
        columns: List<DatabaseColumnEntity>,
        edits: List<PropertyEdit>,
        now: Long,
        zone: ZoneId,
        problems: MutableList<ButtonProblem>
    ): Map<String, String> {
        val values = LinkedHashMap<String, String>()
        for (edit in edits) {
            if (edit.columnId == NAME_PROPERTY_ID) {
                val before = values[NAME_PROPERTY_ID] ?: row.title
                val after = when (edit.op) {
                    EditOp.SET -> edit.value.trim()
                    EditOp.CLEAR -> ""
                    else -> null
                }
                if (after == null) problems += ButtonProblem.BAD_VALUE else if (after != before) values[NAME_PROPERTY_ID] = after
                continue
            }
            val column = columns.find { it.id == edit.columnId }?.takeIf { it.type.buttonEditable }
            if (column == null) {
                problems += ButtonProblem.NO_PROPERTY
                continue
            }
            val before = values[column.id] ?: current[column.id].orEmpty()
            val after = edit.applyTo(column, before, now, zone)
            when {
                after == null -> problems += ButtonProblem.BAD_VALUE
                after != before -> values[column.id] = after
                // Tornato com'era (aggiunto e poi tolto): non cambia niente.
                else -> values.remove(column.id)
            }
        }
        // Una casella mai toccata non ha la cella: "" e "false" sono la stessa cosa.
        return values.filter { (id, value) ->
            val column = columns.find { it.id == id }
            !(column?.type == ColumnType.CHECKBOX && value != "true" && current[id].orEmpty() != "true")
        }
    }

    private suspend fun writeValues(rowId: String, columns: List<DatabaseColumnEntity>, values: Map<String, String>, single: Boolean) {
        values.forEach { (id, value) ->
            if (id == NAME_PROPERTY_ID) {
                rows.setRowTitle(rowId, value)
            } else if (single && columns.find { it.id == id }?.type == ColumnType.CHECKBOX) {
                rows.setCheckbox(rowId, id, value)
            } else {
                rows.setCellValue(rowId, id, value)
            }
        }
    }

    /** Quanti blocchi sono stati messi. */
    private suspend fun insertBlocks(
        step: ButtonStep.InsertBlocks,
        place: ButtonPlace,
        host: ButtonHost,
        problems: MutableList<ButtonProblem>
    ): Int {
        val blocks = step.blocks.map { it.toNewBlock() }
        if (blocks.isEmpty()) return 0
        val pageId = place.pageId ?: place.rowId?.let { pages.ensureRowPage(it) }
        val page = pageId?.let { db.pageDao().getById(it) }?.takeIf { it.trashedAt == null }
        if (page == null) {
            problems += ButtonProblem.NO_PAGE
            return 0
        }
        if (page.isLocked) {
            problems += ButtonProblem.LOCKED_PAGE
            return 0
        }
        // Sopra o sotto un bottone che qui non c'è (quello di una riga): in fondo alla pagina.
        val nextToButton = step.place == InsertPlace.ABOVE_BUTTON || step.place == InsertPlace.BELOW_BUTTON
        val anchor = place.blockId?.takeIf { nextToButton && page.id == place.pageId }
        val where = if (nextToButton && anchor == null) InsertPlace.BOTTOM_OF_PAGE else step.place
        val handled = host.insertBlocks(page.id, anchor, where, blocks)
        if (handled == null) pages.insertBlocksAt(page.id, anchor, where, blocks)
        return if (handled == false) 0 else blocks.size
    }

    private fun withScheme(url: String): String =
        if (Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:").containsMatchIn(url)) url else "https://$url"
}

/** Il blocco da creare per una riga di "Insert blocks". */
fun ButtonBlock.toNewBlock(): NewBlock {
    val blockType = type.takeIf { it in BUTTON_BLOCK_TYPES }?.let { BlockType.valueOf(it) } ?: BlockType.PARAGRAPH
    val line = text.replace('\n', ' ')
    return NewBlock(
        type = blockType,
        spans = if (blockType == BlockType.DIVIDER || line.isEmpty()) emptyList() else listOf(RichTextSpan(line)),
        attrs = BlockAttrs()
    )
}

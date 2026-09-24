package com.gabriele.notionlocal.data.repository

import androidx.room.withTransaction
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.TextStats
import com.gabriele.notionlocal.data.textStatsOf
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEditEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.entity.TableCellEntity
import com.gabriele.notionlocal.data.entity.plainText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Punto unico di accesso ai dati di pagine, blocchi e celle delle
 * tabelle semplici. I ViewModel parlano con il Repository, mai
 * direttamente con i DAO — così se un domani cambiamo come sono
 * salvati i dati, la UI non deve saperlo.
 */
class PageRepository(private val db: AppDatabase) {

    private val pageDao = db.pageDao()
    private val blockDao = db.blockDao()
    private val tableCellDao = db.tableCellDao()
    private val pageEditDao = db.pageEditDao()

    /**
     * Per le scritture che devono sopravvivere alla schermata che le ha
     * chieste — oggi solo la cronologia delle modifiche. Il repository
     * vive quanto l'app, quindi questo scope non è "per sempre": è
     * lungo quanto il processo, e non c'è niente da annullare.
     */
    private val historyScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val json = Json { ignoreUnknownKeys = true }

    private companion object {
        const val DAY_MILLIS = 24L * 60 * 60 * 1000
        const val DEFAULT_PAGE_EMOJI = "📄"
        // Quanto testo mostrare intorno alla parola trovata: meno prima,
        // di più dopo, perché si legge da sinistra a destra.
        const val SNIPPET_BEFORE = 30
        const val SNIPPET_AFTER = 70
    }

    fun getRootPages(): Flow<List<PageEntity>> = pageDao.getRootPages()

    fun getChildPages(parentId: String): Flow<List<PageEntity>> =
        pageDao.getChildPages(parentId)

    suspend fun getPage(pageId: String): PageEntity? = pageDao.getById(pageId)

    /**
     * Quanto testo ha scritto l'utente in una pagina: la voce "X words"
     * del menu dei tre puntini. Le regole del conteggio stanno in
     * `TextStats`; qui si decide **che cosa** si conta.
     *
     * Si conta il testo dei blocchi — paragrafi, titoli, elenchi, caselle,
     * toggle **e quello che c'è dentro ai toggle**, anche chiusi — e
     * quello delle celle delle tabelle semplici. Resta fuori tutto quello
     * che l'utente non ha scritto in questa pagina, come ha chiesto:
     *  - **i database**, righe e celle comprese: sono un'altra cosa, anche
     *    quando stanno dentro la pagina;
     *  - i blocchi che rimandano a un'altra pagina o a un database: il
     *    nome che mostrano è quello dell'altra pagina;
     *  - **il titolo della pagina**: non è nel corpo del testo, e spesso
     *    non l'ha scritto nessuno — le pagine nuove nascono con un nome
     *    messo dall'app ("Senza titolo", "Untitled").
     *
     * Una "riga" è un blocco con del testo, come ha chiesto l'utente
     * ("in realtà quanti blocchi"): i blocchi vuoti non contano, e una
     * tabella conta una volta sola, qualunque sia il numero di celle.
     * Delle tabelle si guardano solo le celle che si vedono: rimpicciolita
     * una tabella, le celle rimaste fuori restano salvate ma non si
     * leggono più.
     */
    suspend fun textStats(pageId: String): TextStats {
        var total = TextStats()
        for (block in blockDao.getBlocksForPageOnce(pageId)) {
            when (block.type) {
                BlockType.TABLE -> {
                    val cells = tableCellDao.getCellsForBlockOnce(block.id).filter {
                        it.rowIndex < block.tableRows &&
                            it.colIndex < block.tableCols &&
                            it.text.isNotBlank()
                    }
                    if (cells.isEmpty()) continue
                    total += cells.fold(TextStats()) { sum, cell -> sum + textStatsOf(cell.text) }
                        .copy(lines = 1)
                }
                BlockType.PAGE_LINK, BlockType.DATABASE_LINK, BlockType.DIVIDER -> Unit
                else -> {
                    val text = runCatching {
                        json.decodeFromString<List<RichTextSpan>>(block.textJson)
                    }.getOrNull()?.plainText().orEmpty()
                    if (text.isBlank()) continue
                    total += textStatsOf(text).copy(lines = 1)
                }
            }
        }
        return total
    }

    /** La pagina seguita nel tempo: vedi `PageDao.observeById`. */
    fun observePage(pageId: String): Flow<PageEntity?> = pageDao.observeById(pageId)

    /** Le pagine e i database messi fra i preferiti, tranne quelli nel cestino. */
    fun observeFavorites(): Flow<List<PageEntity>> = pageDao.observeFavorites()

    /**
     * Mette o toglie una pagina dai preferiti, segnando quando.
     *
     * La pagina radice non si può mettere: è quella da cui si parte
     * sempre, e un collegamento a lei non porterebbe da nessuna parte.
     * La UI non offre nemmeno il pulsante, ma il divieto sta anche qui:
     * una regola che vive in un posto solo prima o poi viene aggirata
     * da una strada nuova.
     */
    suspend fun setFavorite(pageId: String, favorite: Boolean) {
        if (pageId == PageEntity.ROOT_PAGE_ID) return
        pageDao.setFavorite(pageId, favorite, if (favorite) System.currentTimeMillis() else null)
    }

    /** L'immagine dell'icona di una pagina, o null per toglierla: una colonna sola. */
    suspend fun setIconImage(pageId: String, fileName: String?) =
        pageDao.setIconImage(pageId, fileName)

    /** Blocca o sblocca il **contenuto** di una pagina. */
    suspend fun setLocked(pageId: String, locked: Boolean) =
        pageDao.setLocked(pageId, locked)

    /** Blocca o sblocca **l'impaginazione** di un database (vista, ordine, filtro, colonne). */
    suspend fun setViewLocked(pageId: String, locked: Boolean) =
        pageDao.setViewLocked(pageId, locked)

    /**
     * Butta una pagina nel cestino.
     *
     * La pagina **non viene cancellata**: resta nel database con tutto
     * il suo contenuto e si segna solo quando è stata buttata. Quello
     * che sparisce davvero sono i blocchi che la richiamavano nelle
     * altre pagine — è lì che vive l'albero di navigazione, quindi
     * togliere quelli è ciò che la fa sparire da dov'era.
     *
     * Da lì la si ritrova nel Cestino della barra laterale, che la
     * ripristina o la cancella per davvero.
     */
    suspend fun moveToTrash(pageId: String) {
        if (pageId == PageEntity.ROOT_PAGE_ID) return
        db.withTransaction {
            pageDao.setTrashedAt(pageId, System.currentTimeMillis())
            blockDao.getBlocksLinkingTo(pageId).forEach { blockDao.delete(it) }
        }
    }

    // --- Cestino ---

    /** Le pagine nel cestino, dall'ultima buttata. */
    fun observeTrash(): Flow<List<PageEntity>> = pageDao.observeTrash()

    /**
     * Tira fuori una pagina dal cestino e la rimette **in fondo al menu
     * principale**: il posto da cui era stata tolta non c'è più (il suo
     * collegamento è stato cancellato buttandola), e in fondo alla pagina
     * da cui si parte sempre la si ritrova di sicuro.
     *
     * Le pagine di una riga di database fanno eccezione: il loro posto è
     * la riga, che le ha sempre tenute, e un collegamento in più nel menu
     * sarebbe un doppione.
     */
    suspend fun restoreFromTrash(pageId: String) {
        val page = pageDao.getById(pageId) ?: return
        db.withTransaction {
            pageDao.setTrashedAt(pageId, null)
            if (!page.isRowPage && blockDao.getBlocksLinkingTo(pageId).isEmpty()) {
                appendLink(PageEntity.ROOT_PAGE_ID, pageId, page.isDatabase)
            }
        }
    }

    /**
     * Cancella una pagina **per davvero**, con le pagine che contiene.
     *
     * "Contiene" vuol dire le pagine richiamate dai suoi collegamenti e, se
     * è un database, quelle aperte dalle sue righe — e poi quelle dentro
     * di loro, giù fino in fondo. Ma **solo se non sono richiamate anche
     * da fuori**: una sottopagina collegata pure da un'altra parte non è
     * sua soltanto, e cancellandola sparirebbe anche dall'altra.
     *
     * Restituisce i nomi dei file immagine (icone e copertine) delle
     * pagine cancellate, che chi chiama toglie dalla cartella: il
     * repository non sa dov'è.
     */
    suspend fun deletePermanently(pageId: String): List<String> {
        if (pageId == PageEntity.ROOT_PAGE_ID) return emptyList()
        val databaseDao = db.databaseDao()
        return db.withTransaction {
            val doomed = linkedSetOf(pageId)
            val queue = ArrayDeque(listOf(pageId))
            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                val page = pageDao.getById(current) ?: continue
                val children = buildList {
                    blockDao.getBlocksForPageOnce(current).mapNotNullTo(this) { it.linkedPageId }
                    if (page.isDatabase) {
                        databaseDao.getRowsForPageOnce(current).mapNotNullTo(this) { it.linkedPageId }
                    }
                }.distinct()
                for (child in children) {
                    if (child in doomed || child == PageEntity.ROOT_PAGE_ID) continue
                    val linkedFromOutside = blockDao.getBlocksLinkingTo(child).any { it.pageId !in doomed }
                    val openedFromOutside = databaseDao.getRowsLinkingTo(child).any { it.pageId !in doomed }
                    if (!linkedFromOutside && !openedFromOutside) {
                        doomed += child
                        queue += child
                    }
                }
            }
            val pages = pageDao.getByIds(doomed.toList())
            // Se la pagina non era nel cestino, qualcuno la richiama
            // ancora: quei collegamenti porterebbero a una pagina che non
            // c'è più.
            doomed.forEach { id -> blockDao.getBlocksLinkingTo(id).forEach { blockDao.delete(it) } }
            pages.forEach { pageDao.delete(it) }
            pages.flatMap { listOfNotNull(it.iconImage, it.coverImage) }
        }
    }

    /**
     * Toglie per sempre dal cestino quello che la regola scelta dice di
     * togliere: le pagine buttate da più di trenta giorni, o tutte.
     * Si chiama all'avvio dell'app. Restituisce i file immagine da
     * cancellare, come `deletePermanently`.
     */
    suspend fun purgeTrash(emptyAll: Boolean, retentionDays: Int): List<String> {
        val ids = if (emptyAll) {
            pageDao.getAllTrashedIds()
        } else {
            pageDao.getTrashedBefore(System.currentTimeMillis() - retentionDays * DAY_MILLIS)
        }
        return ids.flatMap { deletePermanently(it) }
    }

    /** Svuota il cestino adesso. */
    suspend fun emptyTrash(): List<String> = pageDao.getAllTrashedIds().flatMap { deletePermanently(it) }

    // --- L'albero delle pagine (barra laterale) ---

    /**
     * Un nodo dell'albero: una pagina, un database, o la riga di un
     * database che non è ancora mai stata aperta come pagina (e quindi
     * una pagina sua non ce l'ha: nasce la prima volta che la si tocca).
     */
    data class PageTreeNode(
        val pageId: String?,
        val rowId: String?,
        val title: String,
        val emoji: String,
        val iconImage: String?,
        val isDatabase: Boolean,
        val hasChildren: Boolean
    ) {
        /** Chi è, per ricordarsi se è aperto: la pagina, o la riga se la pagina non c'è. */
        val key: String get() = pageId ?: "row:$rowId"
    }

    suspend fun treeRoot(): PageTreeNode? =
        pageDao.getById(PageEntity.ROOT_PAGE_ID)?.let { nodeFor(it) }

    /**
     * I figli di una pagina nell'albero, nell'ordine in cui compaiono.
     *
     * L'albero non sta in una colonna "genitore": vive nei collegamenti
     * dentro il contenuto delle pagine, e per i database nelle loro righe.
     * Quindi i figli di una pagina sono i suoi collegamenti, **nell'ordine
     * del testo** — anche quelli dentro un toggle, al punto in cui stanno —
     * e i figli di un database sono le sue righe.
     */
    suspend fun treeChildren(pageId: String): List<PageTreeNode> {
        val page = pageDao.getById(pageId) ?: return emptyList()
        if (page.isDatabase) {
            return db.databaseDao().getRowsForPageOnce(pageId).mapNotNull { row ->
                val linked = row.linkedPageId?.let { pageDao.getById(it) }
                if (linked?.trashedAt != null) return@mapNotNull null
                if (linked != null) {
                    nodeFor(linked).copy(title = row.title.ifBlank { linked.title })
                } else {
                    PageTreeNode(
                        pageId = null,
                        rowId = row.id,
                        title = row.title,
                        emoji = DEFAULT_PAGE_EMOJI,
                        iconImage = null,
                        isDatabase = false,
                        hasChildren = false
                    )
                }
            }
        }
        val blocks = blockDao.getBlocksForPageOnce(pageId)
        return inDocumentOrder(blocks)
            .filter { it.type == BlockType.PAGE_LINK || it.type == BlockType.DATABASE_LINK }
            .mapNotNull { it.linkedPageId }
            .distinct()
            .mapNotNull { id -> pageDao.getById(id)?.takeIf { it.trashedAt == null } }
            .map { nodeFor(it) }
    }

    /** Le pagine fra cui scegliere quella da aprire all'avvio: tutte, tranne il cestino e la principale. */
    suspend fun startupCandidates(): List<PageEntity> = pageDao.getAllLivePages()

    /** Crea la pagina di una riga che non l'ha ancora, come fa il pulsante OPEN. */
    suspend fun ensureRowPage(rowId: String): String? {
        val databaseDao = db.databaseDao()
        val row = databaseDao.getRowById(rowId) ?: return null
        row.linkedPageId?.let { return it }
        val page = PageEntity(title = row.title.ifBlank { "Untitled" }, isRowPage = true)
        pageDao.insert(page)
        databaseDao.updateRow(row.copy(linkedPageId = page.id))
        return page.id
    }

    private suspend fun nodeFor(page: PageEntity): PageTreeNode = PageTreeNode(
        pageId = page.id,
        rowId = null,
        title = page.title,
        emoji = page.icon,
        iconImage = page.iconImage,
        isDatabase = page.isDatabase,
        hasChildren = if (page.isDatabase) {
            db.databaseDao().countRows(page.id) > 0
        } else {
            blockDao.countLinksInPage(page.id) > 0
        }
    )

    /** I blocchi come si leggono: in fila, e i figli di un toggle subito dopo di lui. */
    private fun inDocumentOrder(blocks: List<BlockEntity>): List<BlockEntity> {
        val byParent = blocks.groupBy { it.parentBlockId }
        val result = mutableListOf<BlockEntity>()
        fun visit(parentId: String?) {
            byParent[parentId].orEmpty().sortedBy { it.orderIndex }.forEach { block ->
                result += block
                visit(block.id)
            }
        }
        visit(null)
        return result
    }

    // --- Ricerca ---

    /**
     * Un risultato della ricerca. `snippet` è il pezzo di testo in cui
     * compare la parola, se la si è trovata nel contenuto; `matchStart` e
     * `matchLength` dicono dove sta dentro quel pezzo, per evidenziarla.
     */
    data class SearchHit(
        val pageId: String?,
        val rowId: String?,
        val isDatabase: Boolean,
        val title: String,
        val emoji: String,
        val iconImage: String?,
        val snippet: String?,
        val matchStart: Int,
        val matchLength: Int,
        val inTrash: Boolean
    )

    /**
     * Cerca nei titoli e, se richiesto, nel contenuto: il testo dei
     * blocchi, le celle delle tabelle semplici e i nomi delle righe dei
     * database. Una pagina compare **una volta sola**, col primo pezzo di
     * testo in cui la parola si trova.
     *
     * Le pagine nel cestino restano fuori, a meno che non si sia chiesto
     * di cercare anche lì.
     */
    suspend fun search(query: String, inContent: Boolean, inTrash: Boolean): List<SearchHit> {
        val needle = query.trim()
        if (needle.isEmpty()) return emptyList()
        val hits = LinkedHashMap<String, SearchHit>()

        fun allowed(page: PageEntity) = page.trashedAt == null || inTrash

        fun hitFor(page: PageEntity, snippet: Snippet?) = SearchHit(
            pageId = page.id,
            rowId = null,
            isDatabase = page.isDatabase,
            title = page.title,
            emoji = page.icon,
            iconImage = page.iconImage,
            snippet = snippet?.text,
            matchStart = snippet?.start ?: 0,
            matchLength = snippet?.length ?: 0,
            inTrash = page.trashedAt != null
        )

        pageDao.searchByTitle(needle).filter { allowed(it) }.forEach { page ->
            hits[page.id] = hitFor(page, null)
        }

        if (inContent) {
            for (block in blockDao.searchBlocks(needle)) {
                val text = runCatching { json.decodeFromString<List<RichTextSpan>>(block.textJson) }
                    .getOrNull()?.plainText() ?: continue
                val snippet = snippetAround(text, needle) ?: continue
                val existing = hits[block.pageId]
                if (existing?.snippet != null) continue
                val page = pageDao.getById(block.pageId)?.takeIf { allowed(it) } ?: continue
                hits[page.id] = hitFor(page, snippet)
            }
            for (cell in tableCellDao.searchCells(needle)) {
                val snippet = snippetAround(cell.text, needle) ?: continue
                val block = blockDao.getById(cell.blockId) ?: continue
                if (hits[block.pageId]?.snippet != null) continue
                val page = pageDao.getById(block.pageId)?.takeIf { allowed(it) } ?: continue
                hits[page.id] = hitFor(page, snippet)
            }
            for (row in db.databaseDao().searchRows(needle)) {
                val database = pageDao.getById(row.pageId)?.takeIf { allowed(it) } ?: continue
                val linked = row.linkedPageId?.let { pageDao.getById(it) }
                if (linked != null) {
                    // Il nome della riga è il titolo della sua pagina: se
                    // c'è, l'ha già trovata la ricerca nei titoli.
                    if (allowed(linked) && hits[linked.id] == null) hits[linked.id] = hitFor(linked, null)
                    continue
                }
                hits["row:${row.id}"] = SearchHit(
                    pageId = null,
                    rowId = row.id,
                    isDatabase = false,
                    title = row.title,
                    emoji = DEFAULT_PAGE_EMOJI,
                    iconImage = null,
                    snippet = database.title,
                    matchStart = 0,
                    matchLength = 0,
                    inTrash = database.trashedAt != null
                )
            }
        }
        return hits.values.toList()
    }

    private data class Snippet(val text: String, val start: Int, val length: Int)

    /**
     * Il pezzo di testo intorno alla parola trovata: un po' prima, di più
     * dopo, coi puntini dove si è tagliato. Null se nel testo vero la
     * parola non c'è (la query guarda il JSON, che ha anche i nomi dei
     * campi).
     */
    private fun snippetAround(text: String, needle: String): Snippet? {
        val index = text.indexOf(needle, ignoreCase = true)
        if (index < 0) return null
        val from = (index - SNIPPET_BEFORE).coerceAtLeast(0)
        val to = (index + needle.length + SNIPPET_AFTER).coerceAtMost(text.length)
        val prefix = if (from > 0) "…" else ""
        val suffix = if (to < text.length) "…" else ""
        val body = text.substring(from, to).replace('\n', ' ')
        return Snippet(prefix + body + suffix, prefix.length + (index - from), needle.length)
    }

    /**
     * Sposta una pagina dentro un'altra: sparisce il collegamento che
     * la mostrava dov'era e ne nasce uno in fondo alla pagina scelta.
     *
     * Il tipo del collegamento nuovo lo decide la pagina spostata: un
     * database si mostra sempre come database anche in casa d'altri.
     */
    suspend fun movePageTo(pageId: String, destinationPageId: String) {
        if (pageId == PageEntity.ROOT_PAGE_ID || pageId == destinationPageId) return
        val page = pageDao.getById(pageId) ?: return
        db.withTransaction {
            val existing = blockDao.getBlocksLinkingTo(pageId)
            // Se un collegamento sta già nella pagina di destinazione
            // non se ne aggiunge un altro: la pagina è già lì, e due
            // collegamenti uguali sarebbero solo un doppione da
            // cancellare a mano.
            val alreadyThere = existing.any { it.pageId == destinationPageId }
            existing.filter { it.pageId != destinationPageId }.forEach { blockDao.delete(it) }
            if (alreadyThere) return@withTransaction
            appendLink(destinationPageId, pageId, page.isDatabase)
        }
    }

    /** Dove mettere la copia fatta da "Duplicate". */
    sealed interface DuplicateTarget {
        /**
         * Accanto all'originale: il collegamento subito sotto il suo, o,
         * per la pagina di una riga, una riga nuova subito sotto la sua.
         */
        data object NextToOriginal : DuplicateTarget

        /** In fondo a una pagina. Il menu principale è la pagina radice. */
        data class IntoPage(val pageId: String) : DuplicateTarget

        /** In fondo alla pagina di una riga di database, che se non è mai stata aperta nasce adesso. */
        data class IntoRow(val rowId: String) : DuplicateTarget
    }

    /**
     * Fa una copia di una pagina, con tutto quello che contiene, e la mette
     * dove si è scelto. Restituisce l'id della copia, o null se la pagina, o
     * quella in cui metterla, non c'è più.
     *
     * **Si copia tutto, sottopagine comprese**, come su Notion: le pagine
     * richiamate dai collegamenti, quelle delle righe se è un database, e
     * giù fino in fondo. Prima la copia puntava alle stesse sottopagine
     * dell'originale, e allora non era una copia: scrivendo in una
     * sottopagina della copia cambiava anche l'originale, rinominare la
     * pagina di una riga rinominava la riga in tutti e due i database, e
     * buttarne una nel cestino la toglieva da entrambi.
     *
     * Non si copiano i preferiti e il cestino, che sono scelte fatte su
     * quella pagina lì e non su cosa contiene. Le immagini sì, ognuna col
     * suo file (`copyImage`, che sa dov'è la cartella): due pagine con lo
     * stesso file perderebbero l'immagine insieme appena una la cambia.
     *
     * Solo la copia di partenza prende `titleSuffix` nel titolo; le pagine
     * dentro di lei tengono il loro nome.
     */
    suspend fun duplicatePage(
        pageId: String,
        target: DuplicateTarget,
        titleSuffix: String,
        copyImage: suspend (String) -> String?
    ): String? {
        if (pageId == PageEntity.ROOT_PAGE_ID) return null
        val original = pageDao.getById(pageId) ?: return null
        val databaseDao = db.databaseDao()
        return db.withTransaction {
            val destinationPageId = when (target) {
                DuplicateTarget.NextToOriginal -> null
                is DuplicateTarget.IntoPage -> target.pageId
                is DuplicateTarget.IntoRow -> ensureRowPage(target.rowId) ?: return@withTransaction null
            }
            // Dentro un database non si mette un collegamento: il suo
            // contenuto sono righe, non blocchi.
            if (destinationPageId != null && pageDao.getById(destinationPageId)?.isDatabase != false) {
                return@withTransaction null
            }

            // Dove sta l'originale, letto prima di copiare. La pagina di
            // una riga copiata accanto a sé resta una riga dello stesso
            // database; messa altrove diventa una pagina come le altre.
            val sourceRow = if (destinationPageId == null && original.isRowPage) {
                databaseDao.getRowsLinkingTo(pageId).firstOrNull()
            } else {
                null
            }
            val sourceLink = if (destinationPageId == null && sourceRow == null) {
                blockDao.getBlocksLinkingTo(pageId).firstOrNull()
            } else {
                null
            }

            val copyTitle = "${original.title} ($titleSuffix)".trimStart()
            val copyId = copyPageDeep(pageId, mutableMapOf(), copyImage) { page ->
                page.copy(title = copyTitle, isRowPage = sourceRow != null)
            } ?: return@withTransaction null

            val now = System.currentTimeMillis()
            when {
                sourceRow != null -> {
                    databaseDao.shiftRowOrder(sourceRow.pageId, sourceRow.orderIndex + 1, 1)
                    val rowId = UUID.randomUUID().toString()
                    databaseDao.insertRow(
                        sourceRow.copy(
                            id = rowId,
                            title = copyTitle,
                            linkedPageId = copyId,
                            orderIndex = sourceRow.orderIndex + 1,
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                    // Le proprietà vengono con lei: una riga duplicata
                    // senza i suoi valori sarebbe solo una riga nuova.
                    databaseDao.getCellsForRowOnce(sourceRow.id).forEach { cell ->
                        databaseDao.insertCell(cell.copy(id = UUID.randomUUID().toString(), rowId = rowId))
                    }
                }
                sourceLink != null -> {
                    blockDao.shiftOrderIndexes(
                        sourceLink.pageId,
                        sourceLink.parentBlockId,
                        sourceLink.orderIndex + 1,
                        1
                    )
                    blockDao.insert(
                        sourceLink.copy(
                            id = UUID.randomUUID().toString(),
                            linkedPageId = copyId,
                            orderIndex = sourceLink.orderIndex + 1
                        )
                    )
                }
                // Scelta una pagina, o un originale che non sta da nessuna
                // parte: in fondo, dove la si ritrova di sicuro.
                else -> appendLink(destinationPageId ?: PageEntity.ROOT_PAGE_ID, copyId, original.isDatabase)
            }
            copyId
        }
    }

    /**
     * Copia una pagina e quello che contiene, giù fino in fondo. Va
     * chiamata dentro una transazione.
     *
     * `copies` tiene originale → copia. Una pagina richiamata due volte si
     * copia una volta sola, e una che richiama una pagina sopra di lei
     * (basta un collegamento all'indietro) punta alla copia di quella
     * invece di ricominciare all'infinito. `adjust` ritocca la copia di
     * partenza prima di salvarla.
     */
    private suspend fun copyPageDeep(
        pageId: String,
        copies: MutableMap<String, String>,
        copyImage: suspend (String) -> String?,
        adjust: (PageEntity) -> PageEntity = { it }
    ): String? {
        copies[pageId]?.let { return it }
        val original = pageDao.getById(pageId) ?: return null
        val databaseDao = db.databaseDao()
        val copyId = UUID.randomUUID().toString()
        copies[pageId] = copyId
        val now = System.currentTimeMillis()

        // Le colonne cambiano id, e sono richiamate dalle celle e dalla
        // pagina stessa: raggruppamento, calendario, ordine e filtro. Senza
        // tradurre anche quelli la copia si apriva col raggruppamento o il
        // filtro spariti, perché puntavano alle colonne dell'originale.
        val columns = if (original.isDatabase) databaseDao.getColumnsForPageOnce(pageId) else emptyList()
        val newColumnIds = columns.associate { it.id to UUID.randomUUID().toString() }
        fun column(id: String?) = id?.let { newColumnIds[it] ?: it }

        pageDao.insert(
            adjust(
                original.copy(
                    id = copyId,
                    iconImage = original.iconImage?.let { copyImage(it) },
                    coverImage = original.coverImage?.let { copyImage(it) },
                    boardGroupColumnId = column(original.boardGroupColumnId),
                    tableGroupColumnId = column(original.tableGroupColumnId),
                    calendarDateColumnId = column(original.calendarDateColumnId),
                    sortColumnId = column(original.sortColumnId),
                    filterColumnId = column(original.filterColumnId),
                    isFavorite = false,
                    favoritedAt = null,
                    trashedAt = null,
                    createdAt = now,
                    updatedAt = now
                )
            )
        )

        // I blocchi, genitori prima dei figli: `parentBlockId` deve
        // trovare il suo blocco già salvato, e gli id vecchi vanno
        // tradotti nei nuovi, altrimenti i figli di un toggle della copia
        // resterebbero appesi a quello dell'originale. Le sottopagine si
        // copiano prima di salvare il collegamento che le richiama.
        val blocks = inDocumentOrder(blockDao.getBlocksForPageOnce(pageId))
        val newBlockIds = blocks.associate { it.id to UUID.randomUUID().toString() }
        val copiedBlocks = blocks.map { block ->
            block.copy(
                id = newBlockIds.getValue(block.id),
                pageId = copyId,
                parentBlockId = block.parentBlockId?.let { newBlockIds[it] },
                linkedPageId = block.linkedPageId?.let { copyLinkedPage(it, copies, copyImage) }
            )
        }
        blockDao.insertAll(copiedBlocks)

        // Le tabelle semplici tengono il testo in una tabella a parte, per
        // blocco: prima la copia le aveva vuote.
        blocks.filter { it.type == BlockType.TABLE }.forEach { table ->
            tableCellDao.getCellsForBlockOnce(table.id).forEach { cell ->
                tableCellDao.insertCell(
                    cell.copy(id = UUID.randomUUID().toString(), blockId = newBlockIds.getValue(table.id))
                )
            }
        }

        if (original.isDatabase) {
            columns.forEach { column ->
                databaseDao.insertColumn(column.copy(id = newColumnIds.getValue(column.id), pageId = copyId))
            }
            val rows = databaseDao.getRowsForPageOnce(pageId)
            val newRowIds = rows.associate { it.id to UUID.randomUUID().toString() }
            rows.forEach { row ->
                databaseDao.insertRow(
                    row.copy(
                        id = newRowIds.getValue(row.id),
                        pageId = copyId,
                        // Una pagina di riga buttata nel cestino resta
                        // dov'è: la riga copiata ne avrà una nuova la
                        // prima volta che la si apre.
                        linkedPageId = row.linkedPageId
                            ?.takeIf { pageDao.getById(it)?.trashedAt == null }
                            ?.let { copyPageDeep(it, copies, copyImage) },
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
            databaseDao.getAllCellsForPageOnce(pageId).forEach { cell ->
                val rowId = newRowIds[cell.rowId] ?: return@forEach
                val columnId = newColumnIds[cell.columnId] ?: return@forEach
                databaseDao.insertCell(cell.copy(id = UUID.randomUUID().toString(), rowId = rowId, columnId = columnId))
            }
        }
        return copyId
    }

    /**
     * La pagina a cui punterà un collegamento della copia: la copia della
     * sottopagina. La pagina principale e quelle nel cestino non si
     * copiano — la prima c'è una volta sola, le altre sono state buttate —
     * e il collegamento resta a loro, com'era nell'originale.
     */
    private suspend fun copyLinkedPage(
        pageId: String,
        copies: MutableMap<String, String>,
        copyImage: suspend (String) -> String?
    ): String? {
        if (pageId == PageEntity.ROOT_PAGE_ID) return pageId
        val page = pageDao.getById(pageId) ?: return null
        if (page.trashedAt != null) return pageId
        return copyPageDeep(pageId, copies, copyImage)
    }

    /** Un collegamento a una pagina in fondo a un'altra, fuori da ogni toggle. */
    private suspend fun appendLink(pageId: String, linkedPageId: String, isDatabase: Boolean) {
        blockDao.insert(
            BlockEntity(
                pageId = pageId,
                type = if (isDatabase) BlockType.DATABASE_LINK else BlockType.PAGE_LINK,
                linkedPageId = linkedPageId,
                orderIndex = (blockDao.lastOrderIndex(pageId) ?: -1) + 1
            )
        )
    }

    /**
     * Una pagina fra cui scegliere dove mettere la copia, e dove sta: il
     * database per la pagina di una riga, la pagina che la contiene per le
     * altre. Serve a distinguere due pagine con lo stesso nome — e dopo un
     * "Duplicate" ce ne sono sempre due.
     */
    data class DestinationHit(val node: PageTreeNode, val place: String?)

    /**
     * Le pagine il cui titolo contiene la parola cercata, fra cui scegliere
     * dove mettere una copia: quelle di testo e quelle delle righe di tutti
     * i database, anche le righe mai aperte (la loro pagina nascerà se la
     * si sceglie). I database no, e niente dal cestino.
     *
     * Nemmeno le pagine che non stanno da nessuna parte, senza un
     * collegamento né una riga che le apra: si trovano solo cercandole, e
     * una copia messa lì dentro sparirebbe con loro.
     */
    suspend fun searchDestinations(query: String): List<DestinationHit> {
        val needle = query.trim()
        if (needle.isEmpty()) return emptyList()
        val databaseDao = db.databaseDao()
        val hits = mutableListOf<DestinationHit>()
        for (page in pageDao.searchByTitle(needle)) {
            if (page.isDatabase || page.trashedAt != null) continue
            val container = if (page.isRowPage) {
                databaseDao.getRowsLinkingTo(page.id).firstOrNull()?.let { pageDao.getById(it.pageId) }
            } else {
                blockDao.getBlocksLinkingTo(page.id).firstOrNull()?.let { pageDao.getById(it.pageId) }
            }
            val isRoot = page.id == PageEntity.ROOT_PAGE_ID
            if (!isRoot && (container == null || container.trashedAt != null)) continue
            hits += DestinationHit(nodeFor(page), container?.title)
        }
        // Il nome di una riga già aperta è il titolo della sua pagina,
        // che la ricerca qui sopra ha già trovato.
        for (row in databaseDao.searchRows(needle)) {
            if (row.linkedPageId != null) continue
            val database = pageDao.getById(row.pageId)?.takeIf { it.trashedAt == null } ?: continue
            hits += DestinationHit(
                PageTreeNode(
                    pageId = null,
                    rowId = row.id,
                    title = row.title,
                    emoji = DEFAULT_PAGE_EMOJI,
                    iconImage = null,
                    isDatabase = false,
                    hasChildren = false
                ),
                database.title
            )
        }
        return hits
    }

    /** Le pagine dentro cui se ne può spostare un'altra: vedi `PageDao.getMoveDestinations`. */
    suspend fun moveDestinations(exceptPageId: String): List<PageEntity> =
        pageDao.getMoveDestinations().filterNot { it.id == exceptPageId }

    // --- Cronologia delle modifiche (la voce "Updates") ---

    fun observeEdits(pageId: String): Flow<List<PageEditEntity>> =
        pageEditDao.observeForPage(pageId)

    suspend fun recordEdit(edit: PageEditEntity) = pageEditDao.insert(edit)

    /**
     * Scrive una voce di cronologia **dopo una pausa**, e su uno scope
     * che non muore con chi l'ha chiesta.
     *
     * Le due cose servono insieme. La pausa: quello che si è appena
     * scritto viaggia verso il database e torna indietro, e leggendo
     * subito si prenderebbe il testo di un attimo prima — la cronologia
     * racconterebbe una correzione troncata all'ultima lettera. Lo
     * scope a sé: l'ultima sessione di scrittura si chiude proprio
     * mentre si esce dalla pagina, e lo scope del ViewModel a quel
     * punto è già stato annullato, quindi quella riga non verrebbe
     * scritta mai — che è esattamente la modifica che si vuole
     * ritrovare.
     *
     * `build` restituisce null quando non c'è niente da registrare.
     */
    fun recordEditAfterSettling(delayMillis: Long, build: suspend () -> PageEditEntity?) {
        historyScope.launch {
            delay(delayMillis)
            build()?.let { pageEditDao.insert(it) }
        }
    }

    suspend fun clearEdits(pageId: String) = pageEditDao.clearForPage(pageId)

    suspend fun createPage(page: PageEntity) = pageDao.insert(page)

    suspend fun updatePage(page: PageEntity) {
        val now = System.currentTimeMillis()
        pageDao.update(page.copy(updatedAt = now))
        // Se questa pagina è la pagina di una riga di database, il suo
        // titolo È il nome della riga: rinominandola qui dentro va
        // aggiornata anche la tabella, altrimenti tornando indietro si
        // ritrova il nome vecchio. Sulle pagine normali non corrisponde
        // nessuna riga e la scrittura non tocca niente.
        db.databaseDao().setRowTitleByLinkedPage(page.id, page.title, now)
    }

    suspend fun deletePage(page: PageEntity) = pageDao.delete(page)

    fun getBlocks(pageId: String): Flow<List<BlockEntity>> =
        blockDao.getBlocksForPage(pageId)

    suspend fun saveBlock(block: BlockEntity) = blockDao.insert(block)

    /**
     * "Fai posto da qui in poi": sposta avanti di `by` la numerazione
     * dei blocchi fratelli da `fromIndex` compreso. Vedi
     * `BlockDao.shiftOrderIndexes`.
     */
    data class BlockShift(
        val pageId: String,
        val parentBlockId: String?,
        val fromIndex: Int,
        val by: Int
    )

    /**
     * Applica **in un colpo solo** un cambiamento che tocca più
     * blocchi: fare posto, salvarne alcuni, cancellarne altri.
     *
     * Tutto dentro una transazione, e non è un dettaglio di
     * prestazioni: Room avvisa chi osserva la tabella **ad ogni
     * scrittura**, quindi salvare venti blocchi uno per uno faceva
     * ridisegnare la pagina venti volte di fila. In mezzo a quella
     * tempesta il campo di testo su cui si sta scrivendo viene
     * ricomposto di continuo, il fuoco non riesce a passare al blocco
     * nuovo e i caratteri successivi finiscono in quello vecchio — è
     * il bug per cui premendo Invio e scrivendo in fretta si otteneva
     * tutto attaccato in una riga sola. Con la transazione l'avviso è
     * uno, e la pagina si ridisegna una volta.
     */
    suspend fun applyBlockChanges(
        shift: BlockShift? = null,
        saved: List<BlockEntity> = emptyList(),
        deleted: List<BlockEntity> = emptyList()
    ) = db.withTransaction {
        if (shift != null && shift.by != 0) {
            blockDao.shiftOrderIndexes(
                shift.pageId,
                shift.parentBlockId,
                shift.fromIndex,
                shift.by
            )
        }
        if (saved.isNotEmpty()) blockDao.insertAll(saved)
        deleted.forEach { blockDao.delete(it) }
    }

    /**
     * Divide un blocco mettendo la parte prima del cursore in un blocco
     * **nuovo sopra**, e lasciando a quello di partenza la parte dopo.
     *
     * Il punto di tutto è che il blocco di partenza resta lo stesso, con
     * il suo campo di testo e la tastiera attaccata (vedi il commento in
     * `splitBlockAt`). Sta qui e non nel ViewModel perché l'indice va
     * **riletto dal database dentro la transazione**: andando a capo in
     * fretta la copia che ha in mano la schermata è di un a-capo fa, e
     * usare quell'indice creava due righe con lo stesso posto.
     *
     * Restituisce false se il blocco non c'era più e non ha diviso niente:
     * il ViewModel la riga nuova l'ha già mostrata, e deve saperlo per
     * toglierla.
     */
    suspend fun splitBlockAbove(
        blockId: String,
        newBlockId: String,
        newBlockType: BlockType,
        textBeforeJson: String,
        textAfterJson: String
    ): Boolean = db.withTransaction {
        val current = blockDao.getById(blockId) ?: return@withTransaction false
        blockDao.shiftOrderIndexes(
            current.pageId,
            current.parentBlockId,
            current.orderIndex,
            1
        )
        blockDao.insertAll(
            listOf(
                BlockEntity(
                    id = newBlockId,
                    pageId = current.pageId,
                    parentBlockId = current.parentBlockId,
                    orderIndex = current.orderIndex,
                    type = newBlockType,
                    textJson = textBeforeJson,
                    // La spunta e il "riparti da N" appartengono alla
                    // riga di sopra, che continua quella che c'era.
                    isChecked = current.isChecked,
                    numberStartsAt = current.numberStartsAt,
                    indentLevel = current.indentLevel
                ),
                current.copy(
                    orderIndex = current.orderIndex + 1,
                    textJson = textAfterJson,
                    // Una riga appena nata non è ancora fatta.
                    isChecked = false,
                    numberStartsAt = null
                )
            )
        )
        true
    }

    /**
     * Un blocco diventa divisore e sotto nasce una riga vuota, dove si
     * continua a scrivere. Del blocco si cambia **solo il tipo**: prima
     * veniva risalvato per intero dalla copia della schermata, che dal
     * menu "/" aveva ancora dentro il comando appena tolto — il divisore
     * si teneva addosso il suo "/d", invisibile ma nel database.
     */
    suspend fun turnIntoDividerWithLineBelow(blockId: String, newBlockId: String) =
        db.withTransaction {
            val current = blockDao.getById(blockId) ?: return@withTransaction
            blockDao.setType(current.id, BlockType.DIVIDER)
            blockDao.shiftOrderIndexes(current.pageId, current.parentBlockId, current.orderIndex + 1, 1)
            blockDao.insert(
                BlockEntity(
                    id = newBlockId,
                    pageId = current.pageId,
                    parentBlockId = current.parentBlockId,
                    orderIndex = current.orderIndex + 1
                )
            )
        }

    /**
     * Una riga nuova come **primo figlio** di un blocco (il contenuto di
     * un toggle): i figli che c'erano scalano di un posto.
     */
    suspend fun insertFirstChild(parent: BlockEntity, newBlockId: String, textJson: String) =
        db.withTransaction {
            blockDao.shiftOrderIndexes(parent.pageId, parent.id, 0, 1)
            blockDao.insert(
                BlockEntity(
                    id = newBlockId,
                    pageId = parent.pageId,
                    parentBlockId = parent.id,
                    orderIndex = 0,
                    textJson = textJson
                )
            )
        }

    /**
     * Una riga nuova **subito sotto** un blocco, allo stesso livello: i
     * fratelli che seguono scalano di un posto. Rilegge il blocco dal
     * database invece di fidarsi della copia in mano a chi chiama, per
     * la stessa ragione di `splitBlockAbove`: con due Invio ravvicinati
     * quella copia ha già la posizione vecchia.
     *
     * Restituisce false se il blocco non c'era più, come `splitBlockAbove`.
     */
    suspend fun insertSiblingBelow(
        block: BlockEntity,
        newBlockId: String,
        type: BlockType,
        textJson: String
    ): Boolean = db.withTransaction {
        val current = blockDao.getById(block.id) ?: return@withTransaction false
        blockDao.shiftOrderIndexes(current.pageId, current.parentBlockId, current.orderIndex + 1, 1)
        blockDao.insert(
            BlockEntity(
                id = newBlockId,
                pageId = current.pageId,
                parentBlockId = current.parentBlockId,
                orderIndex = current.orderIndex + 1,
                type = type,
                textJson = textJson,
                // Un toggle nuovo nasce chiuso come quello da cui viene:
                // si stava lavorando a toggle chiusi, non c'è motivo di
                // aprirne uno di colpo.
                isExpanded = current.isExpanded
            )
        )
        true
    }

    /**
     * Cambia tipo a un blocco che ha dei figli — un toggle che torna
     * testo — portando i figli **fuori**, subito sotto di lui e allo
     * stesso livello, nell'ordine in cui erano.
     *
     * I fratelli che seguivano il blocco scalano di tanti posti quanti
     * sono i figli, così nessuno si ritrova con lo stesso numero d'ordine
     * di un altro. Tutto in una transazione: a metà strada la pagina
     * avrebbe righe col genitore sbagliato.
     */
    suspend fun unnestAndConvert(blockId: String, newType: BlockType) = db.withTransaction {
        val block = blockDao.getById(blockId) ?: return@withTransaction
        val children = blockDao.getChildren(blockId)
        if (children.isNotEmpty()) {
            blockDao.shiftOrderIndexes(
                block.pageId,
                block.parentBlockId,
                block.orderIndex + 1,
                children.size
            )
            children.forEachIndexed { i, child ->
                blockDao.update(
                    child.copy(
                        parentBlockId = block.parentBlockId,
                        orderIndex = block.orderIndex + 1 + i
                    )
                )
            }
        }
        blockDao.setType(blockId, newType)
    }

    suspend fun deleteBlock(block: BlockEntity) = blockDao.delete(block)

    /** Cambia solo da che numero riparte un elemento di elenco, senza riscrivere il testo. */
    suspend fun setNumberStart(blockId: String, startsAt: Int?) =
        blockDao.setNumberStart(blockId, startsAt)

    /** Cambia solo il testo di un blocco, senza toccarne posto e stato. */
    suspend fun setBlockText(blockId: String, textJson: String) =
        blockDao.setText(blockId, textJson)

    /**
     * "Turn into page": il database dentro una pagina smette di essere
     * mostrato lì e diventa un collegamento, che si apre a schermo
     * intero. Il database non viene toccato — righe, colonne, viste,
     * filtri restano tutti — cambia solo **il blocco che lo mostra**.
     */
    suspend fun turnDatabaseBlockIntoPage(blockId: String) =
        blockDao.setType(blockId, BlockType.PAGE_LINK)

    /**
     * "Turn into database": il contrario. Ogni collegamento a pagina che
     * richiama questo database torna a mostrarlo dentro la pagina.
     */
    suspend fun turnPageLinksIntoDatabase(databasePageId: String) =
        blockDao.retypeLinks(databasePageId, BlockType.PAGE_LINK, BlockType.DATABASE_LINK)

    /** Se da qualche parte questo database è richiamato come pagina, e quindi si può rimettere dentro. */
    suspend fun isShownAsPage(databasePageId: String): Boolean =
        blockDao.countLinks(databasePageId, BlockType.PAGE_LINK) > 0

    /** Apre o chiude un toggle, senza riscriverne il testo. */
    suspend fun setBlockExpanded(blockId: String, expanded: Boolean) =
        blockDao.setExpanded(blockId, expanded)

    /** Cambia solo il tipo di un blocco, senza riscriverne il testo. */
    suspend fun setBlockType(blockId: String, type: BlockType) =
        blockDao.setType(blockId, type)

    /** Cambia solo il rientro di un blocco, senza riscriverne il testo. */
    suspend fun setIndentLevel(blockId: String, level: Int) =
        blockDao.setIndentLevel(blockId, level)

    suspend fun replaceAllBlocks(pageId: String, blocks: List<BlockEntity>) {
        blockDao.deleteAllForPage(pageId)
        blockDao.insertAll(blocks)
    }

    // --- Celle delle tabelle semplici (blocchi TABLE) ---

    fun getTableCells(blockId: String): Flow<List<TableCellEntity>> =
        tableCellDao.getCellsForBlock(blockId)

    suspend fun setTableCellText(blockId: String, row: Int, col: Int, text: String) {
        val existing = tableCellDao.getCell(blockId, row, col)
        if (existing != null) {
            tableCellDao.insertCell(existing.copy(text = text))
        } else {
            tableCellDao.insertCell(
                TableCellEntity(blockId = blockId, rowIndex = row, colIndex = col, text = text)
            )
        }
    }
}

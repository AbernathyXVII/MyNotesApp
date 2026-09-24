package com.gabriele.notionlocal.data.repository

import androidx.room.withTransaction
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.TextStats
import com.gabriele.notionlocal.data.textStatsOf
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.DatabaseCellEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.PageEditEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.PageFont
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
import kotlinx.serialization.encodeToString
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

    /** Il font del testo di una pagina (null = quello di sistema). */
    suspend fun setPageFont(pageId: String, font: PageFont?) =
        pageDao.setPageFont(pageId, font)

    /** Il corpo del testo di una pagina (null = quello di partenza). */
    suspend fun setPageFontSize(pageId: String, size: Int?) =
        pageDao.setPageFontSize(pageId, size)

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
    suspend fun moveToTrash(pageId: String): PageChange.Trashed? {
        if (pageId == PageEntity.ROOT_PAGE_ID) return null
        return db.withTransaction {
            val links = blockDao.getBlocksLinkingTo(pageId)
            pageDao.setTrashedAt(pageId, System.currentTimeMillis())
            links.forEach { blockDao.delete(it) }
            // Restituito per Annulla: dove stavano i collegamenti tolti.
            PageChange.Trashed(pageId, links)
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
        // Le righe di un database semplice non sono pagine: nell'albero
        // il database è una foglia, e le righe si guardano aprendolo.
        if (page.isSimpleDatabase) return emptyList()
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
        // **Le righe di un database semplice non diventano mai pagine**
        // (`PageEntity.isSimpleDatabase`). Le schermate non lo chiedono
        // già da sé, ma questo è l'unico punto da cui una pagina di riga
        // nasce: il divieto sta qui, così nessuna strada nuova — un
        // menu, una ricerca, un "Duplicate" — può aggirarlo per sbaglio.
        if (pageDao.getById(row.pageId)?.isSimpleDatabase == true) return null
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
        hasChildren = if (page.isSimpleDatabase) {
            false
        } else if (page.isDatabase) {
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
                // Una riga di un database semplice non ha una pagina da
                // aprire: il risultato porta **al database**, e sotto il
                // nome della riga dice in quale. `rowId` resta, perché
                // due righe dello stesso database sono due risultati.
                if (database.isSimpleDatabase) {
                    hits["row:${row.id}"] = SearchHit(
                        pageId = database.id,
                        rowId = row.id,
                        isDatabase = true,
                        title = row.title,
                        emoji = database.icon,
                        iconImage = database.iconImage,
                        snippet = database.title,
                        matchStart = 0,
                        matchLength = 0,
                        inTrash = database.trashedAt != null
                    )
                    continue
                }
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
     * Quello che "Move to" non può offrire come destinazione per una
     * pagina: **lei stessa e tutto quello che contiene**, giù fino in
     * fondo — le pagine richiamate dai suoi collegamenti, le pagine delle
     * righe se è un database, e così via — più le righe mai aperte dei
     * database là dentro, che si scelgono per id di riga e non di pagina.
     *
     * Spostare una pagina dentro una sua sottopagina farebbe un giro
     * chiuso: la sottopagina sta dentro la pagina, la pagina dentro la
     * sottopagina, e nessuna delle due si raggiunge più dal menu
     * principale. Prima l'elenco di "Move to" non lo impediva.
     */
    data class MoveExclusions(val pageIds: Set<String>, val rowIds: Set<String>)

    suspend fun moveExclusions(pageId: String): MoveExclusions {
        val pages = pagesInside(pageId)
        val databaseDao = db.databaseDao()
        val rows = pages
            .mapNotNull { pageDao.getById(it)?.takeIf { page -> page.isDatabase } }
            .flatMap { database -> databaseDao.getRowsForPageOnce(database.id).map { it.id } }
            .toSet()
        return MoveExclusions(pages, rows)
    }

    /** Una pagina e tutte quelle dentro di lei (vedi `moveExclusions`). La principale mai: non sta dentro niente. */
    private suspend fun pagesInside(pageId: String): Set<String> {
        val databaseDao = db.databaseDao()
        val found = linkedSetOf(pageId)
        val queue = ArrayDeque(listOf(pageId))
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val page = pageDao.getById(current) ?: continue
            val children = buildList {
                blockDao.getBlocksForPageOnce(current).mapNotNullTo(this) { it.linkedPageId }
                if (page.isDatabase) {
                    databaseDao.getRowsForPageOnce(current).mapNotNullTo(this) { it.linkedPageId }
                }
            }
            for (child in children) {
                if (child != PageEntity.ROOT_PAGE_ID && found.add(child)) queue += child
            }
        }
        return found
    }

    /**
     * "Move to": la pagina lascia il posto in cui stava e finisce **in
     * fondo** alla pagina scelta — anche se la si sceglie uguale a quella
     * in cui sta già: "in fondo" è la regola, sempre. Restituisce false se
     * lo spostamento non si può fare (vedi sotto) e niente è cambiato.
     *
     * - La destinazione è una pagina, o una **riga** di database: in quel
     *   caso si finisce nella pagina della riga, che se non è mai stata
     *   aperta nasce adesso. Dentro un database no: il suo contenuto sono
     *   righe, non blocchi. Né dentro la pagina stessa o una sua
     *   sottopagina (`moveExclusions`).
     * - **Un database spostato arriva sempre come collegamento a
     *   pagina**, anche se prima era mostrato dentro la pagina: chiesto
     *   dall'utente il 24/09/2026 — se lo si vuole di nuovo aperto e
     *   visibile lì, c'è "Turn into database" (tenendo premuto il
     *   collegamento, o dai tre puntini del database). Prima si
     *   ricreava sempre come database aperto.
     * - **La pagina di una riga esce dal suo database**, come su Notion:
     *   la riga si toglie, con i valori delle sue proprietà, e la pagina
     *   diventa una pagina come le altre, con nome e contenuto. Prima
     *   restava anche nel database, cioè in due posti insieme.
     */
    suspend fun movePageTo(pageId: String, destination: PageTreeNode): PageChange.Moved? {
        if (pageId == PageEntity.ROOT_PAGE_ID) return null
        val inside = pagesInside(pageId)
        val destinationPageId = when {
            destination.pageId != null -> destination.pageId
            destination.rowId != null -> {
                val row = db.databaseDao().getRowById(destination.rowId) ?: return null
                if (row.pageId in inside) return null
                ensureRowPage(destination.rowId) ?: return null
            }
            else -> return null
        }
        return moveInto(pageId, destinationPageId, inside)
    }

    /**
     * Lo spostamento vero e proprio, con la destinazione già decisa.
     * Restituisce, per Annulla, tutto quello che serve a rimettere la
     * pagina dov'era: i collegamenti tolti, e la riga con i valori delle
     * sue proprietà se era la pagina di una riga.
     */
    private suspend fun moveInto(pageId: String, destinationPageId: String, inside: Set<String>): PageChange.Moved? {
        if (destinationPageId in inside) return null
        val page = pageDao.getById(pageId) ?: return null
        val target = pageDao.getById(destinationPageId) ?: return null
        if (target.isDatabase || target.trashedAt != null) return null
        val databaseDao = db.databaseDao()
        return db.withTransaction {
            val links = blockDao.getBlocksLinkingTo(pageId)
            links.forEach { blockDao.delete(it) }
            val rows = if (page.isRowPage) databaseDao.getRowsLinkingTo(pageId) else emptyList()
            val cells = rows.flatMap { databaseDao.getCellsForRowOnce(it.id) }
            if (page.isRowPage) {
                rows.forEach { databaseDao.deleteRow(it) }
                pageDao.setRowPage(pageId, false)
            }
            val newLinkId = appendLink(destinationPageId, pageId, isDatabase = false)
            PageChange.Moved(pageId, destinationPageId, links, rows, cells, newLinkId)
        }
    }

    // --- Annulla e Ripristina per quello che si fa alle pagine ---

    /**
     * Un cambiamento fatto **alle pagine** — non ai blocchi di quella
     * aperta — che Annulla sa disfare e Ripristina rifare: "Move to",
     * "Duplicate" e "Move to trash", dal menu dei tre puntini o dal menu
     * del blocco.
     *
     * Le foto di Annulla sono i blocchi di una pagina sola, e questi
     * cambiamenti toccano altre pagine (quella che si sposta, quella in
     * cui arriva, la copia); per questo hanno un passo a parte, che si
     * porta dietro quello che serve a tornare indietro. Chiesto
     * dall'utente il 24/09/2026.
     */
    sealed interface PageChange {
        /** Spostata: i collegamenti e le righe che aveva prima, e il collegamento nuovo. */
        data class Moved(
            val pageId: String,
            val destinationPageId: String,
            val oldLinks: List<BlockEntity>,
            val oldRows: List<DatabaseRowEntity>,
            val oldCells: List<DatabaseCellEntity>,
            val newLinkId: String
        ) : PageChange

        /** Duplicata: la copia, e il posto in cui sta — un collegamento, o una riga nuova del database. */
        data class Duplicated(
            val copyId: String,
            val links: List<BlockEntity>,
            val rows: List<DatabaseRowEntity>,
            val cells: List<DatabaseCellEntity>
        ) : PageChange

        /** Buttata nel cestino: i collegamenti che la mostravano. */
        data class Trashed(val pageId: String, val oldLinks: List<BlockEntity>) : PageChange
    }

    /** Dove sta la copia appena fatta da "Duplicate", per poterla disfare. */
    suspend fun describeCopy(copyId: String): PageChange.Duplicated {
        val databaseDao = db.databaseDao()
        val rows = databaseDao.getRowsLinkingTo(copyId)
        return PageChange.Duplicated(
            copyId = copyId,
            links = blockDao.getBlocksLinkingTo(copyId),
            rows = rows,
            cells = rows.flatMap { databaseDao.getCellsForRowOnce(it.id) }
        )
    }

    /**
     * Annulla un cambiamento. Restituisce quello che serve a Ripristina
     * (lo stesso cambiamento, aggiornato a com'è adesso), o null se non
     * c'è più niente da annullare — la pagina nel frattempo è stata
     * cancellata per sempre.
     *
     * - **Spostata** → torna dov'era: al posto del suo collegamento di
     *   prima (dentro il toggle, se era lì), o nella sua riga, con le
     *   proprietà che aveva.
     * - **Duplicata** → la copia va **nel cestino**, come su Notion quando
     *   si toglie una pagina: se nel frattempo ci si è scritto dentro,
     *   niente è perso. Se era una riga nuova del database, la riga si
     *   toglie (e la copia nel cestino diventa una pagina normale, che
     *   ripristinata dal cestino va in fondo al menu principale).
     * - **Buttata** → esce dal cestino e torna al suo posto.
     */
    suspend fun undoPageChange(change: PageChange): PageChange? = db.withTransaction {
        val databaseDao = db.databaseDao()
        when (change) {
            is PageChange.Moved -> {
                pageDao.getById(change.pageId) ?: return@withTransaction null
                blockDao.getBlocksLinkingTo(change.pageId).forEach { blockDao.delete(it) }
                change.oldRows.forEach { row ->
                    if (pageDao.getById(row.pageId) != null) databaseDao.insertRow(row)
                }
                change.oldCells.forEach { cell ->
                    if (databaseDao.getRowById(cell.rowId) != null) databaseDao.insertCell(cell)
                }
                val backInRow = databaseDao.getRowsLinkingTo(change.pageId).isNotEmpty()
                if (backInRow) pageDao.setRowPage(change.pageId, true)
                putLinksBack(change.pageId, change.oldLinks, fallbackToMainMenu = !backInRow)
                change
            }
            is PageChange.Duplicated -> {
                pageDao.getById(change.copyId) ?: return@withTransaction null
                val now = describeCopy(change.copyId)
                now.rows.forEach { databaseDao.deleteRow(it) }
                if (now.rows.isNotEmpty()) pageDao.setRowPage(change.copyId, false)
                now.links.forEach { blockDao.delete(it) }
                pageDao.setTrashedAt(change.copyId, System.currentTimeMillis())
                now
            }
            is PageChange.Trashed -> {
                val page = pageDao.getById(change.pageId) ?: return@withTransaction null
                pageDao.setTrashedAt(change.pageId, null)
                val inRow = page.isRowPage && databaseDao.getRowsLinkingTo(change.pageId).isNotEmpty()
                putLinksBack(change.pageId, change.oldLinks, fallbackToMainMenu = !inRow)
                change
            }
        }
    }

    /** Ripristina un cambiamento annullato. Come `undoPageChange`: null se non si può più. */
    suspend fun redoPageChange(change: PageChange): PageChange? = when (change) {
        is PageChange.Moved -> {
            pageDao.getById(change.pageId)?.let {
                moveInto(change.pageId, change.destinationPageId, pagesInside(change.pageId))
            }
        }
        is PageChange.Duplicated -> db.withTransaction {
            pageDao.getById(change.copyId) ?: return@withTransaction null
            val databaseDao = db.databaseDao()
            pageDao.setTrashedAt(change.copyId, null)
            change.rows.forEach { row ->
                if (pageDao.getById(row.pageId) != null) databaseDao.insertRow(row)
            }
            change.cells.forEach { cell ->
                if (databaseDao.getRowById(cell.rowId) != null) databaseDao.insertCell(cell)
            }
            val inRow = databaseDao.getRowsLinkingTo(change.copyId).isNotEmpty()
            if (inRow) pageDao.setRowPage(change.copyId, true)
            putLinksBack(change.copyId, change.links, fallbackToMainMenu = !inRow)
            change
        }
        is PageChange.Trashed -> moveToTrash(change.pageId)
    }

    /**
     * Rimette i collegamenti a una pagina dov'erano: stessa pagina, stesso
     * toggle, stesso posto fra i fratelli (quelli da lì in giù scalano di
     * uno, così nessuno si ritrova col suo stesso numero d'ordine). Se il
     * toggle non c'è più si mette in fondo alla pagina; se non c'è più
     * nemmeno la pagina si salta. Se alla fine la pagina non è richiamata
     * da nessuna parte, e `fallbackToMainMenu`, va in fondo al menu
     * principale — come quando la si ripristina dal cestino — invece di
     * restare irraggiungibile.
     */
    private suspend fun putLinksBack(pageId: String, links: List<BlockEntity>, fallbackToMainMenu: Boolean) {
        var placed = false
        for (link in links) {
            if (pageDao.getById(link.pageId) == null) continue
            if (blockDao.getById(link.id) != null) {
                placed = true
                continue
            }
            val parentExists = link.parentBlockId?.let { blockDao.getById(it) != null } ?: true
            val block = if (parentExists) {
                link
            } else {
                link.copy(parentBlockId = null, orderIndex = (blockDao.lastOrderIndex(link.pageId) ?: -1) + 1)
            }
            blockDao.shiftOrderIndexes(block.pageId, block.parentBlockId, block.orderIndex, 1)
            blockDao.insert(block)
            placed = true
        }
        if (!placed && fallbackToMainMenu && blockDao.getBlocksLinkingTo(pageId).isEmpty()) {
            val page = pageDao.getById(pageId) ?: return
            appendLink(PageEntity.ROOT_PAGE_ID, pageId, page.isDatabase)
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
        /**
         * Il collegamento accanto a cui mettere la copia, per
         * `NextToOriginal`: quello su cui si è aperto il menu del blocco.
         * Null dal menu dei tre puntini, dove si usa il primo che si trova
         * — una pagina di solito ne ha uno solo, ma se ne ha due la copia
         * deve nascere sotto quello toccato, non sotto l'altro.
         */
        besideLinkBlockId: String? = null,
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
                besideLinkBlockId
                    ?.let { blockDao.getById(it) }
                    ?.takeIf { it.linkedPageId == pageId }
                    ?: blockDao.getBlocksLinkingTo(pageId).firstOrNull()
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
     * "Duplicate" dal menu di un blocco: una copia della riga subito sotto
     * di lei, con lo stesso tipo, testo, colori, rientro e spunta.
     *
     * Un toggle si copia **coi suoi figli**, e una tabella con le sue
     * celle, che stanno in una tabella a parte. Se dentro un toggle c'è il
     * collegamento a una sottopagina o un database, anche quelli si
     * copiano, come fa "Duplicate" di una pagina: due collegamenti alla
     * stessa sottopagina non sarebbero una copia, scrivere in una
     * cambierebbe anche l'altra.
     *
     * Restituisce l'id della copia, o null se il blocco non c'è più.
     */
    suspend fun duplicateBlock(blockId: String, copyImage: suspend (String) -> String?): String? {
        val original = blockDao.getById(blockId) ?: return null
        return db.withTransaction {
            blockDao.shiftOrderIndexes(
                original.pageId,
                original.parentBlockId,
                original.orderIndex + 1,
                1
            )
            copyBlockTree(
                block = original,
                parentBlockId = original.parentBlockId,
                orderIndex = original.orderIndex + 1,
                copies = mutableMapOf(),
                copyImage = copyImage
            )
        }
    }

    /** Un blocco e, sotto, i suoi figli: vedi `duplicateBlock`. Dentro una transazione. */
    private suspend fun copyBlockTree(
        block: BlockEntity,
        parentBlockId: String?,
        orderIndex: Int,
        copies: MutableMap<String, String>,
        copyImage: suspend (String) -> String?
    ): String {
        val copyId = UUID.randomUUID().toString()
        blockDao.insert(
            block.copy(
                id = copyId,
                parentBlockId = parentBlockId,
                orderIndex = orderIndex,
                linkedPageId = block.linkedPageId?.let { copyLinkedPage(it, copies, copyImage) }
            )
        )
        if (block.type == BlockType.TABLE) {
            tableCellDao.getCellsForBlockOnce(block.id).forEach { cell ->
                tableCellDao.insertCell(cell.copy(id = UUID.randomUUID().toString(), blockId = copyId))
            }
        }
        blockDao.getChildren(block.id).sortedBy { it.orderIndex }.forEach { child ->
            copyBlockTree(child, copyId, child.orderIndex, copies, copyImage)
        }
        return copyId
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
    private suspend fun appendLink(pageId: String, linkedPageId: String, isDatabase: Boolean): String {
        val link = BlockEntity(
            pageId = pageId,
            type = if (isDatabase) BlockType.DATABASE_LINK else BlockType.PAGE_LINK,
            linkedPageId = linkedPageId,
            orderIndex = (blockDao.lastOrderIndex(pageId) ?: -1) + 1
        )
        blockDao.insert(link)
        return link.id
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
            // Copiare "dentro" una riga vuol dire dentro la sua pagina, e
            // una riga di un database semplice una pagina non ce l'ha.
            if (database.isSimpleDatabase) continue
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
    suspend fun turnIntoDividerWithLineBelow(
        blockId: String,
        newBlockId: String,
        /**
         * Il testo che scende nella riga nuova, da "Turn into" nel menu del
         * blocco: il divisore non lo mostra, e lì sarebbe sparito alla
         * vista. Null dal menu "/", dove la riga è vuota.
         */
        carriedTextJson: String? = null
    ) =
        db.withTransaction {
            val current = blockDao.getById(blockId) ?: return@withTransaction
            blockDao.setType(current.id, BlockType.DIVIDER)
            blockDao.shiftOrderIndexes(current.pageId, current.parentBlockId, current.orderIndex + 1, 1)
            blockDao.insert(
                BlockEntity(
                    id = newBlockId,
                    pageId = current.pageId,
                    parentBlockId = current.parentBlockId,
                    orderIndex = current.orderIndex + 1,
                    textJson = carriedTextJson ?: "[]"
                )
            )
            if (carriedTextJson != null) {
                blockDao.setText(current.id, json.encodeToString(listOf(RichTextSpan(text = ""))))
            }
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
        unnestChildren(blockId)
        blockDao.setType(blockId, newType)
    }

    /**
     * Solo la prima metà di `unnestAndConvert`: i figli escono, il tipo
     * resta. Serve prima delle trasformazioni che non sono un semplice
     * cambio di tipo — in pagina, in database, in tabella, in divisore —
     * dove i figli di un toggle sarebbero rimasti appesi a un blocco che
     * non li mostra, spariti alla vista.
     */
    suspend fun unnestChildren(blockId: String) = db.withTransaction {
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

    /**
     * "Turn into database" dal menu di un blocco: **quel** collegamento
     * torna a mostrare il database dentro la pagina. A differenza della
     * voce dei tre puntini (`turnPageLinksIntoDatabase`) gli altri
     * collegamenti allo stesso database, se ce ne sono, restano come sono:
     * si è scelto un blocco, non il database.
     */
    suspend fun turnPageLinkIntoDatabase(blockId: String) =
        blockDao.setType(blockId, BlockType.DATABASE_LINK)

    /**
     * "Rename" dal menu di un blocco: cambia solo il titolo. Se la pagina
     * è quella di una riga, cambia anche il nome della riga, come quando
     * la si rinomina da dentro (vedi `updatePage`).
     */
    suspend fun renamePage(pageId: String, title: String) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            pageDao.setTitle(pageId, title, now)
            db.databaseDao().setRowTitleByLinkedPage(pageId, title, now)
        }
    }

    /**
     * Quante pagine delle righe di un database hanno qualcosa dentro —
     * del testo, un blocco che non sia una riga vuota, un'icona, una
     * copertina. È quello che "Turn into simple database" butterebbe via
     * per sempre: una pagina di riga mai scritta non conta, perde solo il
     * nome, che resta alla riga.
     */
    suspend fun countRowPagesWithContent(databasePageId: String): Int =
        db.databaseDao().getRowsForPageOnce(databasePageId)
            .mapNotNull { it.linkedPageId }
            .distinct()
            .count { rowPageId ->
                val page = pageDao.getById(rowPageId) ?: return@count false
                page.iconImage != null || page.coverImage != null ||
                    blockDao.getBlocksForPageOnce(rowPageId).any { block ->
                        block.type != BlockType.PARAGRAPH || runCatching {
                            json.decodeFromString<List<RichTextSpan>>(block.textJson)
                        }.getOrNull()?.plainText().orEmpty().isNotBlank()
                    }
            }

    /**
     * "Turn into simple database": le righe tornano solo testo, e **le
     * loro pagine si cancellano per sempre**, con quello che c'è dentro —
     * non nel cestino: una pagina di riga nel cestino non avrebbe più una
     * riga a cui tornare, perché le righe di un database semplice non ne
     * hanno. Nome e proprietà delle righe restano.
     *
     * Restituisce i file immagine da togliere dalla cartella, come
     * `deletePermanently`.
     */
    suspend fun turnIntoSimpleDatabase(databasePageId: String): List<String> {
        val databaseDao = db.databaseDao()
        return db.withTransaction {
            val rowPages = databaseDao.getRowsForPageOnce(databasePageId)
                .mapNotNull { it.linkedPageId }
                .distinct()
            val files = rowPages.flatMap { deletePermanently(it) }
            databaseDao.unlinkRowPages(databasePageId)
            pageDao.setSimpleDatabase(databasePageId, true)
            files
        }
    }

    /**
     * "Turn into complex database": le righe tornano a essere pagine.
     * Ognuna nasce la prima volta che la si apre, come in ogni altro
     * database (`ensureRowPage`), e da subito compare nella barra laterale
     * sotto il database.
     */
    suspend fun turnIntoComplexDatabase(databasePageId: String) =
        pageDao.setSimpleDatabase(databasePageId, false)

    /** Apre o chiude un toggle, senza riscriverne il testo. */
    suspend fun setBlockExpanded(blockId: String, expanded: Boolean) =
        blockDao.setExpanded(blockId, expanded)

    /** Cambia solo il tipo di un blocco, senza riscriverne il testo. */
    suspend fun setBlockType(blockId: String, type: BlockType) =
        blockDao.setType(blockId, type)

    /** Cambia solo il rientro di un blocco, senza riscriverne il testo. */
    suspend fun setIndentLevel(blockId: String, level: Int) =
        blockDao.setIndentLevel(blockId, level)

    /**
     * I collegamenti di una foto di Annulla che **non devono tornare**:
     * quelli a una pagina che non c'è più, che è nel cestino, o che nel
     * frattempo è stata spostata in un'altra pagina ("Move to").
     *
     * Le foto sono i blocchi di tutta la pagina, scattate prima; quello
     * che è successo dopo alle pagine collegate non lo sanno. Rimettere il
     * collegamento a una pagina spostata la faceva stare in due posti;
     * rimetterlo a una pagina **cancellata** — un database eliminato dalle
     * sue impostazioni, poi Annulla — era peggio: il blocco puntava a una
     * pagina inesistente, il database lo rifiutava (chiave esterna) e
     * l'app si chiudeva. Trovato il 24/09/2026 leggendo il codice.
     */
    private suspend fun withoutStaleLinks(pageId: String, blocks: List<BlockEntity>): List<BlockEntity> {
        val stale = mutableSetOf<String>()
        val checked = blocks.map { block ->
            val linkedId = block.linkedPageId ?: return@map block
            val linked = pageDao.getById(linkedId)
            val isLink = block.type == BlockType.PAGE_LINK || block.type == BlockType.DATABASE_LINK
            if (!isLink) {
                // Un blocco che è stato un collegamento e poi è tornato
                // testo può tenersi l'id della pagina: se quella non c'è
                // più, l'id va tolto per la stessa chiave esterna.
                return@map if (linked == null) block.copy(linkedPageId = null) else block
            }
            val movedAway = blockDao.getBlocksLinkingTo(linkedId).any { it.pageId != pageId }
            if (linked == null || linked.trashedAt != null || movedAway) stale += block.id
            block
        }
        if (stale.isEmpty()) return checked
        // Un collegamento non ha figli, ma per sicurezza non si lascia
        // nessun blocco appeso a uno tolto: la chiave esterna del
        // genitore lo rifiuterebbe allo stesso modo.
        var result = checked.filterNot { it.id in stale }
        while (true) {
            val ids = result.map { it.id }.toSet()
            val orphaned = result.filter { it.parentBlockId != null && it.parentBlockId !in ids }
            if (orphaned.isEmpty()) return result
            result = result - orphaned.toSet()
        }
    }

    /**
     * Rimette la pagina com'era in una foto dei suoi blocchi: è quello che
     * fanno Annulla e Ripristina.
     *
     * **Le celle delle tabelle si salvano da parte e si rimettono.** I
     * blocchi si cancellano tutti e si riscrivono, e le celle sono legate
     * al loro blocco con `CASCADE`: cancellando la tabella il database
     * portava via anche il suo testo. Un Annulla qualsiasi — anche di una
     * lettera scritta tre righe più su — svuotava ogni tabella della
     * pagina. Trovato il 24/09/2026 leggendo il codice. Le celle tornano
     * solo per le tabelle che ci sono ancora nella foto; il loro testo
     * resta quello di adesso, perché Annulla non lo segue (vedi
     * `PageEditorViewModel`, "Annulla / Ripristina").
     */
    suspend fun replaceAllBlocks(pageId: String, snapshot: List<BlockEntity>) {
        db.withTransaction {
            val blocks = withoutStaleLinks(pageId, snapshot)
            val keptIds = blocks.map { it.id }.toSet()
            val cells = tableCellDao.getCellsForPageOnce(pageId).filter { it.blockId in keptIds }
            blockDao.deleteAllForPage(pageId)
            blockDao.insertAll(blocks)
            cells.forEach { tableCellDao.insertCell(it) }
        }
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

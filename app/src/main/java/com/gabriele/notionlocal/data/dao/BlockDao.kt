package com.gabriele.notionlocal.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockDao {

    /**
     * Salva un blocco: lo crea se non c'è, lo **aggiorna al suo posto**
     * se c'è già.
     *
     * **Non `REPLACE`.** Era `@Insert(onConflict = REPLACE)`, e in SQLite
     * `REPLACE` non aggiorna: **cancella la riga e ne inserisce una
     * nuova**. La cancellazione fa scattare il vincolo verso i figli
     * (`parentBlockId`, a cascata), quindi salvare un blocco che ne ha
     * voleva dire **cancellare tutti i suoi figli**. Aprire o chiudere un
     * toggle lo salva: ogni volta il contenuto spariva. Visto nel
     * database il 22/09/2026 — cinque righe dentro un toggle, un tocco
     * sulla freccia, zero righe. Lo stesso valeva per spostarlo su o giù
     * e per cambiargli tipo.
     *
     * `@Upsert` prova a inserire e, se l'id esiste già, aggiorna: la riga
     * non viene mai cancellata, i figli restano dove sono.
     */
    @Upsert
    suspend fun insert(block: BlockEntity)

    /** Come `insert`, per più blocchi insieme. */
    @Upsert
    suspend fun insertAll(blocks: List<BlockEntity>)

    @Update
    suspend fun update(block: BlockEntity)

    @Delete
    suspend fun delete(block: BlockEntity)

    @Query("SELECT * FROM blocks WHERE pageId = :pageId ORDER BY orderIndex ASC")
    fun getBlocksForPage(pageId: String): Flow<List<BlockEntity>>

    /**
     * Cambia SOLO la numerazione, senza toccare il resto della riga.
     * Salvare l'intera entità riscriverebbe anche `textJson` con la copia
     * che il chiamante ha in mano, che può essere più vecchia di quello
     * che l'utente ha appena scritto — ed è così che un tocco sul numero
     * cancellava il testo della riga.
     */
    @Query("UPDATE blocks SET numberStartsAt = :startsAt, numberResetHere = 0 WHERE id = :blockId")
    suspend fun setNumberStart(blockId: String, startsAt: Int?)

    /**
     * Tipo e pagina richiamata insieme, senza riscrivere il testo: serve a
     * far diventare una riga il collegamento a una vista collegata. Salvare
     * il blocco intero dalla copia della schermata riscriverebbe anche il
     * comando "/..." appena tolto (vedi `turnIntoDividerWithLineBelow`).
     */
    @Query("UPDATE blocks SET type = :type, linkedPageId = :linkedPageId WHERE id = :blockId")
    suspend fun setTypeAndLink(blockId: String, type: BlockType, linkedPageId: String)

    /** Come setNumberStart: cambia il tipo senza riscrivere il testo della riga. */
    @Query("UPDATE blocks SET type = :type WHERE id = :blockId")
    suspend fun setType(blockId: String, type: BlockType)

    /**
     * Fa posto: sposta avanti di `by` la numerazione di tutti i blocchi
     * fratelli da `fromIndex` in poi.
     *
     * **Una query sola, non una riga alla volta.** Salvare i blocchi uno
     * per uno voleva dire una scrittura per ciascuno, e Room avvisa chi
     * osserva la tabella dopo ognuna: su una pagina con trenta blocchi,
     * un solo Invio scatenava trenta ridisegni completi della pagina di
     * fila, il campo di testo veniva ricomposto in mezzo alla
     * digitazione e il fuoco non riusciva più a spostarsi sul blocco
     * nuovo. In più la numerazione veniva calcolata sulla lista che
     * aveva in mano la UI, che nel frattempo poteva essere già vecchia:
     * due Invii ravvicinati lasciavano due blocchi con lo stesso
     * numero. Qui invece conta il database, non la copia in memoria.
     *
     * `IS` e non `=` sul genitore: i blocchi di primo livello ce
     * l'hanno nullo, e in SQL `NULL = NULL` non è vero.
     */
    @Query(
        "UPDATE blocks SET orderIndex = orderIndex + :by " +
            "WHERE pageId = :pageId AND parentBlockId IS :parentBlockId AND orderIndex >= :fromIndex"
    )
    suspend fun shiftOrderIndexes(pageId: String, parentBlockId: String?, fromIndex: Int, by: Int)

    /** Come setNumberStart: cambia il rientro senza riscrivere il testo della riga. */
    @Query("UPDATE blocks SET indentLevel = :level WHERE id = :blockId")
    suspend fun setIndentLevel(blockId: String, level: Int)

    /**
     * Cambia **solo** il testo di un blocco.
     *
     * Scrivere una lettera non deve poter spostare il blocco né
     * cambiargli la spunta: salvando l'entità intera si rispediva anche
     * il `orderIndex` che la schermata aveva in mano, che dopo un a-capo
     * è quello di prima — e la riga appena creata se la ritrovava
     * addosso.
     */
    @Query("UPDATE blocks SET textJson = :textJson WHERE id = :blockId")
    suspend fun setText(blockId: String, textJson: String)

    /**
     * Un blocco letto adesso dal database.
     *
     * Serve dentro le transazioni che spostano le righe: la copia che
     * ha in mano la schermata può essere di un attimo fa, e a chi scrive
     * in fretta "un attimo fa" vuol dire con l'indice sbagliato.
     */
    @Query("SELECT * FROM blocks WHERE id = :blockId")
    suspend fun getById(blockId: String): BlockEntity?

    /**
     * Ricerca full-text nel contenuto testuale dei blocchi. textJson
     * contiene il testo serializzato, quindi il LIKE funziona comunque
     * (cerca la sottostringa anche dentro il JSON) — semplice e
     * sufficientemente efficace per un uso locale/personale.
     */
    @Query("SELECT DISTINCT pageId FROM blocks WHERE textJson LIKE '%' || :query || '%'")
    suspend fun searchPageIdsInBlocks(query: String): List<String>

    /**
     * I blocchi il cui testo contiene la parola cercata, per mostrarne un
     * pezzo nei risultati. Il confronto è sul JSON, quindi può trovare
     * anche i nomi dei campi ("bold", "text"): chi chiama ricontrolla sul
     * testo vero.
     */
    @Query("SELECT * FROM blocks WHERE textJson LIKE '%' || :query || '%'")
    suspend fun searchBlocks(query: String): List<BlockEntity>

    /**
     * Se una pagina contiene almeno un collegamento a un'altra: decide se
     * nell'albero ha il triangolino. Le viste collegate non contano: non
     * stanno nell'albero (vedi `PageRepository.treeChildren`), e una pagina
     * che ha solo quelle avrebbe avuto un triangolino che apre il vuoto.
     */
    @Query(
        "SELECT COUNT(*) FROM blocks WHERE pageId = :pageId AND linkedPageId IS NOT NULL " +
            "AND linkedPageId NOT IN (SELECT id FROM pages WHERE sourceDatabaseId IS NOT NULL)"
    )
    suspend fun countLinksInPage(pageId: String): Int

    @Query("DELETE FROM blocks WHERE pageId = :pageId")
    suspend fun deleteAllForPage(pageId: String)

    /** Quanti blocchi di un certo tipo rimandano a una pagina. */
    @Query("SELECT COUNT(*) FROM blocks WHERE linkedPageId = :pageId AND type = :type")
    suspend fun countLinks(pageId: String, type: BlockType): Int

    /**
     * Cambia il modo in cui una pagina viene mostrata dove è richiamata:
     * tutti i blocchi di tipo `from` che la richiamano diventano `to`.
     * È "Turn into page" / "Turn into database": il database resta
     * esattamente lo stesso, cambia solo il blocco che lo mostra.
     */
    @Query("UPDATE blocks SET type = :to WHERE linkedPageId = :pageId AND type = :from")
    suspend fun retypeLinks(pageId: String, from: BlockType, to: BlockType)

    /** Apre o chiude un toggle senza riscrivere il resto della riga. */
    @Query("UPDATE blocks SET isExpanded = :expanded WHERE id = :blockId")
    suspend fun setExpanded(blockId: String, expanded: Boolean)

    /** I figli diretti di un blocco (il contenuto di un toggle), in ordine. */
    @Query("SELECT * FROM blocks WHERE parentBlockId = :blockId ORDER BY orderIndex ASC")
    suspend fun getChildren(blockId: String): List<BlockEntity>

    /** I blocchi di una pagina, una volta sola: serve per duplicarla. */
    @Query("SELECT * FROM blocks WHERE pageId = :pageId ORDER BY orderIndex ASC")
    suspend fun getBlocksForPageOnce(pageId: String): List<BlockEntity>

    /**
     * I blocchi che **rimandano** a una pagina, ovunque stiano.
     *
     * Qui l'albero di navigazione non è in un campo della pagina: una
     * pagina sta "dentro" un'altra perché in quell'altra c'è un blocco
     * che la richiama. Per spostarla o per toglierla di mezzo bisogna
     * quindi partire da questi blocchi, non dalla pagina.
     */
    @Query("SELECT * FROM blocks WHERE linkedPageId = :pageId")
    suspend fun getBlocksLinkingTo(pageId: String): List<BlockEntity>

    /** Il posto più in fondo fra i blocchi di primo livello di una pagina. */
    @Query(
        "SELECT MAX(orderIndex) FROM blocks WHERE pageId = :pageId AND parentBlockId IS NULL"
    )
    suspend fun lastOrderIndex(pageId: String): Int?
}

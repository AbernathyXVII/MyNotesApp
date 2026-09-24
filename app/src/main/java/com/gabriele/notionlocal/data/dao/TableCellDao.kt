package com.gabriele.notionlocal.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gabriele.notionlocal.data.entity.TableCellEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TableCellDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCell(cell: TableCellEntity)

    @Query("SELECT * FROM table_cells WHERE blockId = :blockId")
    fun getCellsForBlock(blockId: String): Flow<List<TableCellEntity>>

    /** Le celle di una tabella, una volta sola: serve per duplicare la pagina che la contiene. */
    @Query("SELECT * FROM table_cells WHERE blockId = :blockId")
    suspend fun getCellsForBlockOnce(blockId: String): List<TableCellEntity>

    /** Le celle delle tabelle semplici che contengono la parola cercata. */
    @Query("SELECT * FROM table_cells WHERE text LIKE '%' || :query || '%'")
    suspend fun searchCells(query: String): List<TableCellEntity>

    @Query("SELECT * FROM table_cells WHERE blockId = :blockId AND rowIndex = :row AND colIndex = :col LIMIT 1")
    suspend fun getCell(blockId: String, row: Int, col: Int): TableCellEntity?
}

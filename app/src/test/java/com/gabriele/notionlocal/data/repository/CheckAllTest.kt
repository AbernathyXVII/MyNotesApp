package com.gabriele.notionlocal.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseCellEntity
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** "Activate all" e "Deactivate all" di una colonna Checkbox (29/09/2026). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CheckAllTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: DatabaseRepository
    private val database = PageEntity(title = "Spesa", isDatabase = true)
    private val done = DatabaseColumnEntity(pageId = database.id, name = "Fatto", type = ColumnType.CHECKBOX)
    private val spuntata = DatabaseRowEntity(pageId = database.id, title = "Pane", orderIndex = 0, updatedAt = 1L)
    private val nonSpuntata = DatabaseRowEntity(pageId = database.id, title = "Latte", orderIndex = 1, updatedAt = 1L)
    private val maiToccata = DatabaseRowEntity(pageId = database.id, title = "Uova", orderIndex = 2, updatedAt = 1L)

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        PageRepository(db).createPage(database)
        repo = DatabaseRepository(db)
        val dao = db.databaseDao()
        dao.insertColumn(done)
        listOf(spuntata, nonSpuntata, maiToccata).forEach { dao.insertRow(it) }
        dao.insertCell(DatabaseCellEntity(rowId = spuntata.id, columnId = done.id, value = "true"))
        dao.insertCell(DatabaseCellEntity(rowId = nonSpuntata.id, columnId = done.id, value = "false"))
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun values() = runBlocking {
        listOf(spuntata, nonSpuntata, maiToccata).map { db.databaseDao().getCell(it.id, done.id)?.value }
    }

    private fun edited(row: DatabaseRowEntity) = runBlocking { db.databaseDao().getRowById(row.id)!!.updatedAt }

    @Test
    fun activateAllChecksEveryRowAndLeavesTheCheckedOneAlone() = runBlocking<Unit> {
        repo.setAllChecked(listOf(spuntata.id, nonSpuntata.id, maiToccata.id), done.id, checked = true)
        assertEquals(listOf("true", "true", "true"), values())
        // Era già spuntata: non risulta modificata adesso.
        assertEquals(1L, edited(spuntata))
        assertEquals(true, edited(nonSpuntata)!! > 1L)
    }

    @Test
    fun deactivateAllUnchecksAndAnUntouchedCellStaysUntouched() = runBlocking<Unit> {
        repo.setAllChecked(listOf(spuntata.id, nonSpuntata.id, maiToccata.id), done.id, checked = false)
        assertEquals(listOf("false", "false", null), values())
        assertEquals(1L, edited(nonSpuntata))
        assertEquals(1L, edited(maiToccata))
    }

    @Test
    fun onlyTheGivenRowsChange() = runBlocking<Unit> {
        repo.setAllChecked(listOf(nonSpuntata.id), done.id, checked = true)
        assertEquals(listOf("true", "true", null), values())
    }
}

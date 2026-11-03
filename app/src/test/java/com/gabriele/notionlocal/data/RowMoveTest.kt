package com.gabriele.notionlocal.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.DatabaseRepository
import com.gabriele.notionlocal.data.repository.PageRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **"Move up" / "Move down"** tenendo premuto il nome di una riga
 * (29/09/2026): la riga va subito prima o subito dopo quella che le sta
 * accanto nella tabella come la si vede.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class RowMoveTest {

    private lateinit var db: AppDatabase
    private lateinit var rows: DatabaseRepository
    private val games = PageEntity(title = "Giochi", isDatabase = true)

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        PageRepository(db).createPage(games)
        rows = DatabaseRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun add(vararg titles: String, orders: List<Int> = titles.indices.toList()) = runBlocking {
        titles.mapIndexed { i, title ->
            DatabaseRowEntity(pageId = games.id, title = title, orderIndex = orders[i]).also { db.databaseDao().insertRow(it) }
        }
    }

    private fun order() = runBlocking { db.databaseDao().getRowsForPageOnce(games.id) }.map { it.title }

    @Test
    fun upAndDownSwapWithTheNeighbour() = runBlocking<Unit> {
        val (a, b, c) = add("A", "B", "C")
        rows.moveRowNextTo(games.id, c.id, b.id, before = true)
        assertEquals(listOf("A", "C", "B"), order())
        rows.moveRowNextTo(games.id, a.id, c.id, before = false)
        assertEquals(listOf("C", "A", "B"), order())
    }

    @Test
    fun withAFilterItJumpsOverTheHiddenRows() = runBlocking<Unit> {
        // Si vedono A, C ed E (B e D sono filtrate): E sale sopra C.
        val (_, _, c, _, e) = add("A", "B", "C", "D", "E")
        rows.moveRowNextTo(games.id, e.id, c.id, before = true)
        assertEquals(listOf("A", "B", "E", "C", "D"), order())
    }

    @Test
    fun rowsTiedOnTheSameIndexGetTheirOwnPlace() = runBlocking<Unit> {
        val made = add("A", "B", "C", orders = listOf(0, 5, 5))
        rows.moveRowNextTo(games.id, made[0].id, made.last().id, before = false)
        val saved = db.databaseDao().getRowsForPageOnce(games.id)
        assertEquals(listOf(0, 1, 2), saved.map { it.orderIndex })
        assertEquals("A", saved.last().title)
    }

    @Test
    fun movingDoesNotTouchLastEdited() = runBlocking<Unit> {
        val (a, b) = add("A", "B")
        val before = db.databaseDao().getRowsForPageOnce(games.id).associate { it.id to it.updatedAt }
        rows.moveRowNextTo(games.id, b.id, a.id, before = true)
        assertEquals(listOf("B", "A"), order())
        assertEquals(before, db.databaseDao().getRowsForPageOnce(games.id).associate { it.id to it.updatedAt })
    }
}

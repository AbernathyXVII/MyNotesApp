package com.gabriele.notionlocal.ui

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseLayout
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.GroupLevel
import com.gabriele.notionlocal.data.entity.GroupOrder
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.ViewArrangement
import com.gabriele.notionlocal.ui.screen.buildLevelGroups
import com.gabriele.notionlocal.ui.screen.buildRowGroups
import com.gabriele.notionlocal.ui.screen.displayedRowIds
import com.gabriele.notionlocal.viewmodel.DatabaseTableState
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **"Sort groups"** (28/10/2026, chiesto dall'utente: "come posso spostare il
 * gruppo Checked sotto Unchecked?", per "Done" nel suo Main calendar): i
 * gruppi a mano, in ordine crescente o decrescente; il gruppo senza valore
 * resta in fondo, e uno nuovo va dopo quelli messi a mano.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class GroupOrderTest {

    private val done = DatabaseColumnEntity(id = "done", pageId = "cal", name = "Done", type = ColumnType.CHECKBOX)
    private val kind = DatabaseColumnEntity(
        id = "kind", pageId = "cal", name = "Tipo", type = ColumnType.SELECT,
        optionsJson = """[{"label":"Lavoro"},{"label":"Casa"},{"label":"Sport"}]"""
    )
    private val hours = DatabaseColumnEntity(id = "hours", pageId = "cal", name = "Ore", type = ColumnType.NUMBER)

    private fun row(id: String, order: Int) = DatabaseRowEntity(id = id, pageId = "cal", title = id, orderIndex = order)

    private val state = DatabaseTableState(
        columns = listOf(done, kind, hours),
        rows = listOf(row("riunione", 0), row("spesa", 1), row("corsa", 2), row("senza", 3)),
        cellValues = mapOf(
            ("riunione" to "done") to "true", ("riunione" to "kind") to "Lavoro", ("riunione" to "hours") to "10",
            ("spesa" to "kind") to "Casa", ("spesa" to "hours") to "2",
            ("corsa" to "done") to "true", ("corsa" to "kind") to "Sport", ("corsa" to "hours") to "1.5"
        )
    )

    @Before
    fun setUp() {
        AppSettings.init(ApplicationProvider.getApplicationContext())
    }

    private fun keys(level: GroupLevel) = buildLevelGroups(state, level).map { it.key }

    @Test
    fun ofItsOwnCheckedComesFirst() {
        assertEquals(listOf("true", "false"), keys(GroupLevel(done)))
    }

    @Test
    fun uncheckedCanGoFirstByHandOrDescending() {
        assertEquals(listOf("false", "true"), keys(GroupLevel(done, manual = listOf("false", "true"))))
        // Per nome, al contrario: Unchecked prima di Checked.
        assertEquals(listOf("false", "true"), keys(GroupLevel(done, order = GroupOrder.DESCENDING)))
        assertEquals(listOf("true", "false"), keys(GroupLevel(done, order = GroupOrder.ASCENDING)))
    }

    @Test
    fun tagsByHandByNameAndTheEmptyGroupLast() {
        // Di suo l'ordine dei tag; "senza" non ha un tipo e sta in fondo.
        assertEquals(listOf("Lavoro", "Casa", "Sport", ""), keys(GroupLevel(kind)))
        assertEquals(listOf("Casa", "Lavoro", "Sport", ""), keys(GroupLevel(kind, order = GroupOrder.ASCENDING)))
        assertEquals(listOf("Sport", "Lavoro", "Casa", ""), keys(GroupLevel(kind, order = GroupOrder.DESCENDING)))
        // A mano: quelli messi in ordine, poi quelli nuovi (qui "Casa") nell'ordine di sempre.
        assertEquals(listOf("Sport", "Lavoro", "Casa", ""), keys(GroupLevel(kind, manual = listOf("Sport", "Lavoro"))))
        // Una chiave che non c'è più (un tag cancellato) non fa niente.
        assertEquals(listOf("Sport", "Lavoro", "Casa", ""), keys(GroupLevel(kind, manual = listOf("Vecchio", "Sport", "Lavoro"))))
    }

    @Test
    fun numbersAsNumbers() {
        assertEquals(listOf("10", "2", "1.5", ""), keys(GroupLevel(hours, order = GroupOrder.DESCENDING)))
        assertEquals(listOf("1.5", "2", "10", ""), keys(GroupLevel(hours, order = GroupOrder.ASCENDING)))
    }

    @Test
    fun hideEmptyComesAfterTheOrder() {
        // "Sport" ha una riga, gli altri anche: con un tag vuoto in più, nascosto, l'ordine resta.
        val withEmpty = DatabaseColumnEntity(
            id = "kind", pageId = "cal", name = "Tipo", type = ColumnType.SELECT,
            optionsJson = """[{"label":"Lavoro"},{"label":"Vuoto"},{"label":"Casa"},{"label":"Sport"}]"""
        )
        val groups = buildRowGroups(state, withEmpty, hideEmpty = true, order = GroupOrder.MANUAL, manual = listOf("Vuoto", "Casa"))
        assertEquals(listOf("Casa", "Lavoro", "Sport", ""), groups.map { it.key })
        val shown = buildRowGroups(state, withEmpty, hideEmpty = false, order = GroupOrder.MANUAL, manual = listOf("Vuoto", "Casa"))
        assertEquals(listOf("Vuoto", "Casa", "Lavoro", "Sport", ""), shown.map { it.key })
    }

    @Test
    fun thePageArrowsFollowTheOrder() {
        // Le frecce "pagina prima/dopo" leggono le righe come si vedono: prima le Unchecked.
        assertEquals(
            listOf("spesa", "senza", "riunione", "corsa"),
            displayedRowIds(state, ViewArrangement(DatabaseLayout.TABLE, tableLevels = listOf(GroupLevel(done, manual = listOf("false", "true")))))
        )
    }
}

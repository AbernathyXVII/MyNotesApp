package com.gabriele.notionlocal.data

import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.SORT_BY_NAME
import com.gabriele.notionlocal.data.entity.SortRuleSpec
import com.gabriele.notionlocal.data.entity.existingIn
import com.gabriele.notionlocal.data.entity.sortRules
import com.gabriele.notionlocal.data.entity.withSortRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * **Le regole di ordinamento in un elenco solo** (17/10/2026): tutte, dalla
 * prima all'ultima, nell'ordine in cui contano.
 */
class SortRulesTest {

    private val page = PageEntity(title = "Videogiochi", isDatabase = true)
    private val five = listOf(
        SortRuleSpec("a"),
        SortRuleSpec("b", descending = true),
        SortRuleSpec(SORT_BY_NAME),
        SortRuleSpec("c", descending = true),
        SortRuleSpec("d")
    )

    @Test
    fun allTheRulesInOneList() {
        val sorted = page.withSortRules(five)
        assertEquals(five, sorted.sortRules())
        // I campi di prima restano vuoti: non li legge più nessuno.
        assertNull(sorted.sortColumnId)
        assertNull(sorted.thenSortColumnId)
    }

    @Test
    fun noRulesLeaveNothingBehind() {
        val cleared = page.withSortRules(five).withSortRules(emptyList())
        assertNull(cleared.sortRulesJson)
        assertEquals(emptyList<SortRuleSpec>(), cleared.sortRules())
    }

    @Test
    fun aPropertyCountsOnceAndBrokenJsonIsNothing() {
        assertEquals(
            listOf(SortRuleSpec("a"), SortRuleSpec("b")),
            page.withSortRules(listOf(SortRuleSpec("a"), SortRuleSpec("b"), SortRuleSpec("a", true))).sortRules()
        )
        assertEquals(emptyList<SortRuleSpec>(), page.copy(sortRulesJson = "{non è json").sortRules())
    }

    @Test
    fun onlyRulesOnPropertiesThatStillExist() {
        val columns = listOf(DatabaseColumnEntity(id = "a", pageId = page.id, name = "A", type = ColumnType.TEXT, orderIndex = 0))
        assertEquals(listOf(SortRuleSpec("a"), SortRuleSpec(SORT_BY_NAME)), five.existingIn(columns))
    }
}

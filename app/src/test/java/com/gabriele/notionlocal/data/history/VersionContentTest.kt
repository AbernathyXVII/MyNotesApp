package com.gabriele.notionlocal.data.history

import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.InkStrokeEntity
import com.gabriele.notionlocal.data.entity.InkTool
import com.gabriele.notionlocal.data.entity.TableCellEntity
import com.gabriele.notionlocal.data.entity.VersionStamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.lang.reflect.Modifier
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * **"Page history"** (02/11/2026): quello che si salva di una pagina, e
 * quante versioni si tengono.
 */
class VersionContentTest {

    private fun fieldsOf(type: Class<*>) =
        type.declaredFields.filterNot { Modifier.isStatic(it.modifiers) || it.isSynthetic }.map { it.name }.toSet()

    @Test
    fun noFieldOfTheEntitiesIsLeftOut() {
        // Un campo nuovo di un blocco, di una cella o di un tratto va anche nella
        // versione: altrimenti ripristinando tornerebbe al suo valore di sempre.
        assertEquals(fieldsOf(BlockEntity::class.java) - "pageId", fieldsOf(VersionBlock::class.java))
        assertEquals(fieldsOf(TableCellEntity::class.java), fieldsOf(VersionCell::class.java))
        assertEquals(fieldsOf(InkStrokeEntity::class.java) - "pageId", fieldsOf(VersionStroke::class.java))
    }

    private val toggle = BlockEntity(
        id = "t", pageId = "p", type = BlockType.TOGGLE, textJson = """[{"text":"Un toggle"}]""", orderIndex = 1,
        isExpanded = false, attrsJson = """{"color":"red"}""", notifyAt = 123L
    )
    private val inside = BlockEntity(id = "c", pageId = "p", parentBlockId = "t", textJson = """[{"text":"Dentro due parole"}]""", orderIndex = 0)
    private val first = BlockEntity(id = "a", pageId = "p", type = BlockType.CHECKBOX, isChecked = true, textJson = """[{"text":"Prima"}]""", orderIndex = 0)
    private val cell = TableCellEntity(id = "x", blockId = "a", rowIndex = 1, colIndex = 0, text = "cella scritta")
    private val stroke = InkStrokeEntity(id = "s", pageId = "p", anchor = "a", tool = InkTool.MARKER, color = 5L, width = 2.5f, points = "1,2;3,4", createdAt = 9L)

    @Test
    fun aVersionComesBackAsItWas() {
        val content = VersionContent.of("Titolo", listOf(toggle, inside, first), listOf(cell), listOf(stroke))
        val back = VersionCodec.decode(VersionCodec.encode(content))
        assertEquals(content, back)
        assertEquals(listOf(first, toggle, inside).toSet(), back.blocks.map { it.toEntity("p") }.toSet())
        assertEquals(cell, back.cells.single().toEntity())
        assertEquals(stroke, back.ink.single().toEntity("p"))
        assertEquals(3, back.lines)
        // "Un toggle", "Dentro due parole", "Prima", "cella scritta".
        assertEquals(2 + 3 + 1 + 2, back.words)
    }

    @Test
    fun theFingerprintDoesNotDependOnOrderNorOnAnOpenToggle() {
        val one = VersionContent.of("T", listOf(toggle, inside, first), listOf(cell), listOf(stroke))
        val other = VersionContent.of("T", listOf(first, inside, toggle.copy(isExpanded = true)), listOf(cell), listOf(stroke))
        assertEquals(VersionCodec.hash(one), VersionCodec.hash(other))
        assertNotEquals(VersionCodec.hash(one), VersionCodec.hash(VersionContent.of("T", listOf(toggle, inside, first.copy(isChecked = false)), listOf(cell), listOf(stroke))))
        assertNotEquals(VersionCodec.hash(one), VersionCodec.hash(VersionContent.of("Altro", listOf(toggle, inside, first), listOf(cell), listOf(stroke))))
    }

    private val zone = ZoneId.of("Europe/Rome")
    private val now = at(2026, 11, 2, 16, 0)

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0) =
        LocalDateTime.of(year, month, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun allOfTheLastDayThenOnePerDayForThirtyDays() {
        val versions = listOf(
            VersionStamp("adesso", at(2026, 11, 2, 15, 50)),
            VersionStamp("stamattina", at(2026, 11, 2, 9)),
            VersionStamp("ieri sera", at(2026, 11, 1, 21)),
            // Più di 24 ore fa: di ogni giorno resta l'ultima.
            VersionStamp("ieri mattina", at(2026, 11, 1, 10)),
            VersionStamp("ieri prima", at(2026, 11, 1, 8)),
            VersionStamp("31 sera", at(2026, 10, 31, 22)),
            VersionStamp("31 mattina", at(2026, 10, 31, 7)),
            VersionStamp("un mese fa", at(2026, 10, 4, 12)),
            VersionStamp("troppo vecchia", at(2026, 9, 30, 12))
        )
        assertEquals(
            setOf("ieri prima", "31 mattina", "troppo vecchia"),
            VersionRetention.toDrop(versions.shuffled(), now, zone).toSet()
        )
    }

    @Test
    fun theNewestStaysEvenIfItIsOld() {
        val versions = listOf(VersionStamp("unica", at(2026, 6, 1, 12)), VersionStamp("prima", at(2026, 5, 1, 12)))
        assertEquals(listOf("prima"), VersionRetention.toDrop(versions, now, zone))
    }
}

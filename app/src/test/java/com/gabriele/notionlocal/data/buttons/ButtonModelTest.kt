package com.gabriele.notionlocal.data.buttons

import com.gabriele.notionlocal.data.IconChoice
import com.gabriele.notionlocal.data.entity.BlockAttrs
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.encodeDateRange
import com.gabriele.notionlocal.data.entity.parseDateRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * **I bottoni, i conti** (27/10/2026, chiesti dall'utente: "una
 * proprietà/funzione che mi consente di triggerare una qualsiasi azione"):
 * cosa fa ogni cambio a ogni tipo di proprietà, quali righe passano il filtro
 * di "Edit pages in", e come si salvano le azioni.
 */
class ButtonModelTest {

    private val zone = ZoneId.of("Europe/Rome")
    private val now = LocalDateTime.of(2026, 10, 28, 15, 42).atZone(zone).toInstant().toEpochMilli()

    private fun column(type: ColumnType, options: String = "[]") =
        DatabaseColumnEntity(id = "c", pageId = "db", name = "P", type = type, optionsJson = options)

    private val tags = column(
        ColumnType.MULTI_SELECT,
        """[{"label":"Casa"},{"label":"Lavoro"},{"label":"Urgente"}]"""
    )
    private val status = column(ColumnType.SELECT, """[{"label":"Da fare"},{"label":"Fatto"}]""")

    private fun edit(op: EditOp, value: String = "") = PropertyEdit(columnId = "c", op = op, value = value)

    @Test
    fun aCheckboxIsCheckedUncheckedAndToggled() {
        val box = column(ColumnType.CHECKBOX)
        assertEquals("true", edit(EditOp.CHECK).applyTo(box, "", now, zone))
        assertEquals("false", edit(EditOp.UNCHECK).applyTo(box, "true", now, zone))
        assertEquals("true", edit(EditOp.TOGGLE).applyTo(box, "", now, zone))
        assertEquals("false", edit(EditOp.TOGGLE).applyTo(box, "true", now, zone))
    }

    @Test
    fun aNumberGrowsAndShrinksAndAnEmptyCellStartsFromZero() {
        val number = column(ColumnType.NUMBER)
        assertEquals("1", edit(EditOp.ADD, "1").applyTo(number, "", now, zone))
        assertEquals("6", edit(EditOp.ADD, "1").applyTo(number, "5", now, zone))
        assertEquals("4", edit(EditOp.REMOVE, "1").applyTo(number, "5", now, zone))
        assertEquals("-1", edit(EditOp.REMOVE, "1").applyTo(number, "0", now, zone))
        // Coi decimali senza gli errori dei double: 0,1 + 0,2 fa 0,3.
        assertEquals("0.3", edit(EditOp.ADD, "0,2").applyTo(number, "0.1", now, zone))
        assertEquals("2.5", edit(EditOp.SET, "2.50").applyTo(number, "9", now, zone))
        assertEquals("0", edit(EditOp.REMOVE, "1.5").applyTo(number, "1.5", now, zone))
        assertEquals("", edit(EditOp.CLEAR).applyTo(number, "9", now, zone))
        // Un numero che non è un numero: la cella resta com'è.
        assertNull(edit(EditOp.ADD, "uno").applyTo(number, "5", now, zone))
        assertNull(edit(EditOp.ADD, "1").applyTo(number, "cinque", now, zone))
    }

    @Test
    fun aSelectTakesOnlyATagItHas() {
        assertEquals("Fatto", edit(EditOp.SET, "Fatto").applyTo(status, "Da fare", now, zone))
        assertNull(edit(EditOp.SET, "Cancellato").applyTo(status, "Da fare", now, zone))
        assertEquals("", edit(EditOp.CLEAR).applyTo(status, "Fatto", now, zone))
    }

    @Test
    fun multiSelectAddsRemovesAndReplacesTags() {
        assertEquals("Casa|Urgente", edit(EditOp.ADD, "Urgente").applyTo(tags, "Casa", now, zone))
        // Già c'è: non si raddoppia.
        assertEquals("Casa", edit(EditOp.ADD, "Casa").applyTo(tags, "Casa", now, zone))
        assertEquals("Lavoro", edit(EditOp.REMOVE, "Casa").applyTo(tags, "Casa|Lavoro", now, zone))
        assertEquals("Lavoro|Urgente", edit(EditOp.SET, "Lavoro|Urgente").applyTo(tags, "Casa", now, zone))
        assertNull(edit(EditOp.ADD, "Sconosciuto").applyTo(tags, "Casa", now, zone))
    }

    @Test
    fun aDateBecomesNowOrTodayAndKeepsItsNotification() {
        val date = column(ColumnType.DATE)
        val nowValue = parseDateRange(edit(EditOp.NOW).applyTo(date, "", now, zone)!!)!!
        assertEquals(now, nowValue.start)
        assertTrue(nowValue.hasTime)
        val today = parseDateRange(edit(EditOp.TODAY).applyTo(date, "", now, zone)!!)!!
        assertEquals(LocalDateTime.of(2026, 10, 28, 0, 0).atZone(zone).toInstant().toEpochMilli(), today.start)
        assertFalse(today.hasTime)
        // Una data con la notifica la tiene, con le sue notifiche in anticipo.
        val reminded = encodeDateRange(1_000L, notify = true, reminders = listOf(0, 60))
        val moved = parseDateRange(edit(EditOp.NOW).applyTo(date, reminded, now, zone)!!)!!
        assertTrue(moved.notify)
        assertEquals(listOf(0, 60), moved.reminders)
    }

    @Test
    fun textIsWrittenOrCleared() {
        val text = column(ColumnType.TEXT)
        assertEquals("Ciao", edit(EditOp.SET, "Ciao").applyTo(text, "", now, zone))
        assertEquals("", edit(EditOp.CLEAR).applyTo(text, "Ciao", now, zone))
    }

    @Test
    fun theFilterOfEditPages() {
        val box = column(ColumnType.CHECKBOX)
        // Una casella mai toccata non ha la cella: è "non spuntata".
        assertTrue(ButtonFilter("c", listOf("false")).matches(box, ""))
        assertFalse(ButtonFilter("c", listOf("true")).matches(box, ""))
        assertTrue(ButtonFilter("c", listOf("true")).matches(box, "true"))
        // Fra i tag basta averne uno.
        assertTrue(ButtonFilter("c", listOf("Urgente", "Lavoro")).matches(tags, "Casa|Lavoro"))
        assertFalse(ButtonFilter("c", listOf("Urgente")).matches(tags, "Casa|Lavoro"))
        // I numeri come numeri, il testo senza badare alle maiuscole.
        assertTrue(ButtonFilter("c", listOf("2")).matches(column(ColumnType.NUMBER), "2.0"))
        assertTrue(ButtonFilter("c", listOf("pasta")).matches(column(ColumnType.TEXT), " Pasta "))
        assertTrue(ButtonFilter(NAME_PROPERTY_ID, listOf("pasta")).matches(null, "Pasta"))
        // Un filtro senza valori non passa niente: non diventa "tutte le righe".
        assertFalse(ButtonFilter("c", emptyList()).matches(box, ""))
        assertFalse(ButtonFilter("c", listOf(" ")).matches(column(ColumnType.TEXT), ""))
    }

    @Test
    fun theOpsOfEachTypeStartWithTheUsualOne() {
        assertEquals(EditOp.CHECK, ColumnType.CHECKBOX.editOps().first())
        assertEquals(EditOp.ADD, ColumnType.NUMBER.editOps().first())
        assertEquals(EditOp.NOW, ColumnType.DATE.editOps().first())
        assertTrue(ColumnType.TEXT.buttonEditable)
        assertFalse(ColumnType.BUTTON.buttonEditable)
        assertFalse(ColumnType.RELATION.buttonEditable)
        assertFalse(ColumnType.CREATED_TIME.buttonEditable)
    }

    @Test
    fun aButtonIsSavedWithAllItsActions() {
        val config = ButtonConfig(
            name = "Fatto oggi",
            icon = "✅",
            steps = listOf(
                ButtonStep.Confirm(message = "Sicuro?"),
                ButtonStep.InsertBlocks(blocks = listOf(ButtonBlock(type = "CHECKBOX", text = "Bere")), place = InsertPlace.TOP_OF_PAGE),
                ButtonStep.AddPage(databaseId = "db", title = "Spesa", edits = listOf(PropertyEdit(columnId = "c", op = EditOp.CHECK)), openAfter = true),
                ButtonStep.EditPages(databaseId = "db", filter = ButtonFilter("c", listOf("true")), edits = listOf(PropertyEdit(columnId = "c", op = EditOp.UNCHECK))),
                ButtonStep.Open(url = "notion.so"),
                ButtonStep.Notify(title = "Ciao", text = "Fatto")
            )
        )
        assertEquals(config, ButtonConfig.parse(ButtonConfig.encode(config)))
        // Dentro un blocco, con le altre voci.
        val attrs = BlockAttrs(button = config, color = "#E03E3E")
        assertEquals(attrs, BlockAttrs.parse(BlockAttrs.encode(attrs)))
        // La copia collegata ha solo il bottone vero.
        assertEquals("abc", BlockAttrs.parse(BlockAttrs.encode(BlockAttrs(buttonLink = "abc"))).buttonLink)
    }

    @Test
    fun aColumnThatWasSomethingElseIsAnEmptyButton() {
        assertEquals(ButtonConfig(), ButtonConfig.parse("[]"))
        assertEquals(ButtonConfig(), ButtonConfig.parse("""[{"label":"Fatto"}]"""))
        assertEquals(ButtonConfig(), ButtonConfig.parse("{rotto"))
        assertEquals(ButtonConfig(), ButtonConfig.parse(null))
        assertFalse(ButtonConfig().isSet)
    }

    @Test
    fun theIconIsAnEmojiAnImageOrASymbol() {
        val emoji = ButtonConfig().withIcon(IconChoice.Emoji("🚀"))
        assertEquals("🚀", emoji.icon)
        assertTrue(emoji.hasIcon)
        val image = emoji.withIcon(IconChoice.Image("foto.png"))
        assertNull(image.icon)
        assertEquals("foto.png", image.iconImage)
        // L'immagine del bottone si copia e si cancella col blocco.
        assertTrue("foto.png" in BlockAttrs(button = image).files)
        assertFalse(image.withIcon(IconChoice.None).hasIcon)
    }

    @Test
    fun insertBlocksMakesTheBlocksWritten() {
        val todo = ButtonBlock(type = "CHECKBOX", text = "Bere\nacqua").toNewBlock()
        assertEquals(BlockType.CHECKBOX, todo.type)
        assertEquals("Bere acqua", todo.spans.single().text)
        assertTrue(ButtonBlock(type = "DIVIDER", text = "x").toNewBlock().spans.isEmpty())
        // Un tipo che "Insert blocks" non conosce diventa testo.
        assertEquals(BlockType.PARAGRAPH, ButtonBlock(type = "TABLE", text = "a").toNewBlock().type)
        assertTrue(ButtonBlock(text = "").toNewBlock().spans.isEmpty())
    }
}

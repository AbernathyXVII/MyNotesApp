package com.gabriele.notionlocal.data.formula

import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.DatabaseSnapshot
import com.gabriele.notionlocal.data.entity.PlainRollupFormatter
import com.gabriele.notionlocal.data.entity.RelationConfig
import com.gabriele.notionlocal.data.entity.RollupValue
import com.gabriele.notionlocal.data.entity.encodeRelationConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

/**
 * **Le formule** (28/10/2026): come si leggono, come si calcolano e che cosa
 * succede quando sono scritte male. Gli esempi sono quelli dell'utente:
 * "giorni che mancano", "prezzo × quantità", "se è spuntato allora…".
 * Adesso è mercoledì 28/10/2026, 15:30, a Berlino.
 */
class FormulaEngineTest {

    private val zone = ZoneId.of("Europe/Berlin")
    private val now = ZonedDateTime.of(2026, 10, 28, 15, 30, 0, 0, zone).toInstant().toEpochMilli()
    private fun day(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).atStartOfDay(zone).toInstant().toEpochMilli()

    private val db = "spesa"
    private fun column(name: String, type: ColumnType, options: String = "[]") =
        DatabaseColumnEntity(id = name.lowercase(), pageId = db, name = name, type = type, optionsJson = options)

    private val price = column("Prezzo", ColumnType.NUMBER)
    private val quantity = column("Quantità", ColumnType.NUMBER)
    private val done = column("Fatto", ColumnType.CHECKBOX)
    private val due = column("Scadenza", ColumnType.DATE)
    private val tags = column("Tag", ColumnType.MULTI_SELECT)
    private val status = column("Stato", ColumnType.SELECT)
    private val note = column("Nota", ColumnType.TEXT)
    private val created = column("Creata", ColumnType.CREATED_TIME)
    private val duration = column("Durata", ColumnType.DURATION)
    private val columns = listOf(price, quantity, done, due, tags, status, note, created, duration)

    private val row = DatabaseRowEntity(id = "latte", pageId = db, title = "Latte", createdAt = day(2026, 10, 1) + 9 * 3_600_000L)

    private val values = mapOf(
        price to "2.5",
        quantity to "4",
        done to "true",
        due to day(2026, 11, 2).toString(),
        tags to "bio|offerta",
        status to "In corso",
        // Dalle 10:00 alle 11:30: 90 minuti.
        duration to "${day(2026, 10, 28) + 10 * 3_600_000L}|${day(2026, 10, 28) + 11 * 3_600_000L + 30 * 60_000L}|t"
    )

    private fun cells(overrides: Map<DatabaseColumnEntity, String> = emptyMap()) =
        (values + overrides).entries.associate { (row.id to it.key.id) to it.value }

    private fun formula(name: String, expression: String) =
        column(name, ColumnType.FORMULA, FormulaConfig.encode(FormulaConfig(expression)))

    /** Il risultato di [expression] sulla riga di prova. */
    private fun eval(
        expression: String,
        overrides: Map<DatabaseColumnEntity, String> = emptyMap(),
        extra: List<DatabaseColumnEntity> = emptyList(),
        rollups: Map<Pair<String, String>, RollupValue> = emptyMap(),
        related: Map<String, DatabaseSnapshot> = emptyMap(),
        nameTitles: Set<String> = setOf(FormulaEngine.NAME)
    ): FormulaValue {
        val f = formula("F", expression)
        return FormulaEngine.computeAll(
            columns + extra + f, listOf(row), cells(overrides), rollups, related, nameTitles,
            now, zone, PlainRollupFormatter, Locale.ITALIAN
        )[row.id to f.id]!!
    }

    private fun num(expression: String, overrides: Map<DatabaseColumnEntity, String> = emptyMap()) =
        (eval(expression, overrides) as FormulaValue.Num).value

    private fun text(expression: String) = (eval(expression) as FormulaValue.Text).value
    private fun bool(expression: String, overrides: Map<DatabaseColumnEntity, String> = emptyMap()) =
        (eval(expression, overrides) as FormulaValue.Bool).value

    private fun error(expression: String) = (eval(expression) as FormulaValue.Error).error

    // --- Gli esempi dell'utente ---

    @Test
    fun priceTimesQuantity() {
        assertEquals(10.0, num("prop(\"Prezzo\") * prop(\"Quantità\")"), 0.0)
        // Col segno della tastiera, e con le virgolette curve che mette il telefono.
        assertEquals(10.0, num("prop(“Prezzo”) × prop(“Quantità”)"), 0.0)
        // Il nome con le minuscole lo trova lo stesso.
        assertEquals(10.0, num("prop(\"prezzo\") * prop(\"quantità\")"), 0.0)
    }

    @Test
    fun daysLeft() {
        assertEquals(5.0, num("dateBetween(prop(\"Scadenza\"), today(), \"days\")"), 0.0)
        assertEquals(5.0, num("dateBetween(prop(\"Scadenza\"), today(), \"giorni\")"), 0.0)
        // Da adesso (le 15:30) mancano 4 giorni interi e qualche ora.
        assertEquals(4.0, num("dateBetween(prop(\"Scadenza\"), now(), \"days\")"), 0.0)
        assertEquals(-5.0, num("dateBetween(today(), prop(\"Scadenza\"), \"days\")"), 0.0)
        assertEquals(0.0, num("dateBetween(prop(\"Scadenza\"), today(), \"weeks\")"), 0.0)
        assertEquals(120.0, num("dateBetween(prop(\"Scadenza\"), today(), \"hours\")"), 0.0)
    }

    @Test
    fun ifChecked() {
        assertEquals("✓ Finito", text("if(prop(\"Fatto\"), \"✓ Finito\", \"Da fare\")"))
        assertEquals(
            FormulaValue.Text("Da fare"),
            eval("if(prop(\"Fatto\"), \"✓ Finito\", \"Da fare\")", mapOf(done to ""))
        )
        assertEquals(FormulaValue.Num(1.0), eval("prop(\"Fatto\") ? 1 : 0"))
        assertEquals(false, bool("not prop(\"Fatto\")"))
        assertEquals(false, bool("!prop(\"Fatto\")"))
        assertEquals(false, bool("not(prop(\"Fatto\"))"))
    }

    // --- Operatori ---

    @Test
    fun arithmeticFollowsTheUsualOrder() {
        assertEquals(7.0, num("1 + 2 * 3"), 0.0)
        assertEquals(9.0, num("(1 + 2) * 3"), 0.0)
        assertEquals(512.0, num("2 ^ 3 ^ 2"), 0.0)
        assertEquals(-4.0, num("-2 ^ 2"), 0.0)
        assertEquals(2.5, num("10 / 4"), 0.0)
        assertEquals(2.0, num("10 % 4"), 0.0)
        assertEquals(1500.0, num("1.5e3"), 0.0)
        assertEquals(2.0, num("10 ÷ 5"), 0.0)
        assertEquals(FormulaError.Kind.DIVISION_BY_ZERO, error("1 / 0").kind)
        assertEquals(FormulaError.Kind.DIVISION_BY_ZERO, error("mod(1, 0)").kind)
    }

    @Test
    fun comparisonsAndLogic() {
        assertTrue(bool("prop(\"Prezzo\") > 2 and prop(\"Quantità\") <= 4"))
        assertTrue(bool("prop(\"Prezzo\") ≥ 2.5 && prop(\"Quantità\") ≠ 3"))
        assertTrue(bool("prop(\"Stato\") == \"In corso\""))
        // Un "=" solo vale "==", come si scrive a mano.
        assertTrue(bool("prop(\"Stato\") = \"In corso\""))
        assertTrue(bool("\"b\" > \"A\""))
        assertTrue(bool("prop(\"Scadenza\") > today()"))
        assertEquals(false, bool("prop(\"Prezzo\") > 3 or prop(\"Fatto\") == false"))
    }

    @Test
    fun onlyTheBranchThatCountsIsCalculated() {
        // La divisione per zero sta nel ramo che non serve: niente errore.
        assertEquals(1.0, num("if(true, 1, 1 / 0)"), 0.0)
        assertEquals(false, bool("false and 1 / 0 > 0"))
        assertEquals(true, bool("true or 1 / 0 > 0"))
        assertEquals(FormulaError.Kind.DIVISION_BY_ZERO, error("if(false, 1, 1 / 0)").kind)
    }

    @Test
    fun ifsTakesTheFirstTrueCondition() {
        val f = "ifs(prop(\"Prezzo\") > 10, \"caro\", prop(\"Prezzo\") > 2, \"medio\", \"economico\")"
        assertEquals("medio", text(f))
        assertEquals(FormulaValue.Text("economico"), eval(f, mapOf(price to "1")))
        assertEquals(FormulaError.Kind.WRONG_ARGUMENTS, error("ifs(true, 1)").kind)
    }

    @Test
    fun anEmptyPropertyCountsAsZeroOrAsNothing() {
        assertEquals(1.0, num("prop(\"Quantità\") + 1", mapOf(quantity to "")), 0.0)
        assertEquals(false, bool("prop(\"Quantità\") > 0", mapOf(quantity to "")))
        assertTrue(bool("empty(prop(\"Nota\"))"))
        assertEquals(FormulaValue.Empty, eval("prop(\"Nota\")"))
        assertEquals("x", text("prop(\"Nota\") + \"x\""))
        // Una formula vuota non è un errore: non vale niente.
        assertEquals(FormulaValue.Empty, eval(""))
        assertEquals(FormulaValue.Empty, eval("   "))
    }

    // --- Testo ---

    @Test
    fun textFunctions() {
        assertEquals("Totale: 2.5", text("\"Totale: \" + prop(\"Prezzo\")"))
        assertEquals("Latte - In corso", text("concat(prop(\"Name\"), \" - \", prop(\"Stato\"))"))
        assertEquals("bio, offerta", text("prop(\"Tag\")"))
        assertTrue(bool("contains(prop(\"Tag\"), \"OFFERTA\")"))
        assertEquals(5.0, num("length(prop(\"Name\"))"), 0.0)
        assertEquals("LATTE", text("upper(prop(\"Name\"))"))
        assertEquals("notE", text("slice(\"notE app\", 0, 4)"))
        assertEquals("2026/10/28", text("replaceAll(\"2026-10-28\", \"-\", \"/\")"))
        assertEquals("2026/10-28", text("replace(\"2026-10-28\", \"-\", \"/\")"))
        assertEquals("In corso, x", text("join(\", \", prop(\"Stato\"), prop(\"Nota\"), \"x\")"))
        assertEquals(12.5, num("toNumber(\"12,5\")"), 0.0)
        assertEquals("abab", text("repeat(\"ab\", 2)"))
        assertEquals("true", text("format(prop(\"Fatto\"))"))
        // Una stringa con le virgolette dentro.
        assertEquals("disse \"ciao\"", text("\"disse \\\"ciao\\\"\""))
        assertEquals("l'uno", text("'l\\'uno'"))
    }

    // --- Numeri ---

    @Test
    fun numberFunctions() {
        assertEquals(2.35, num("round(2.345, 2)"), 0.0)
        assertEquals(3.0, num("round(2.5)"), 0.0)
        assertEquals(2.0, num("floor(2.9)"), 0.0)
        assertEquals(3.0, num("ceil(2.1)"), 0.0)
        assertEquals(3.0, num("abs(-3)"), 0.0)
        assertEquals(1.0, num("min(4, 1, 3)"), 0.0)
        assertEquals(4.0, num("max(4, 1, 3)"), 0.0)
        assertEquals(8.0, num("sum(4, 1, 3)"), 0.0)
        assertEquals(6.5, num("sum(prop(\"Prezzo\"), prop(\"Quantità\"))"), 0.0)
        assertEquals(2.0, num("mean(1, 3)"), 0.0)
        assertEquals(3.0, num("sqrt(9)"), 0.0)
        assertEquals(8.0, num("pow(2, 3)"), 0.0)
        assertEquals(1.0, num("toNumber(prop(\"Fatto\"))"), 0.0)
        // I decimali sbagliati dei calcoli non si vedono.
        assertEquals("0.3", FormulaEngine.display(eval("0.1 + 0.2"), PlainRollupFormatter))
        assertEquals("0", FormulaEngine.display(eval("-0 * 1"), PlainRollupFormatter))
    }

    // --- Date ---

    @Test
    fun dateFunctions() {
        val moved = eval("dateAdd(prop(\"Scadenza\"), 1, \"months\")") as FormulaValue.Date
        assertEquals(day(2026, 12, 2), moved.millis)
        assertEquals(false, moved.hasTime)
        assertEquals(day(2026, 10, 26), (eval("dateSubtract(prop(\"Scadenza\"), 1, \"weeks\")") as FormulaValue.Date).millis)
        assertEquals(day(2027, 2, 2), (eval("dateAdd(prop(\"Scadenza\"), 1, \"quarters\")") as FormulaValue.Date).millis)
        assertEquals("02/12/2026", text("formatDate(dateAdd(prop(\"Scadenza\"), 1, \"months\"), \"DD/MM/YYYY\")"))
        assertEquals("mercoledì 28 ottobre 2026, 15:30", text("formatDate(now(), \"dddd D MMMM YYYY, HH:mm\")"))
        // Il testo fra parentesi quadre resta com'è, come in Notion: senza, la "a" sarebbe AM/PM.
        assertEquals("Scade il 2", text("formatDate(prop(\"Scadenza\"), \"[Scade il] D\")"))
        assertEquals("ore 15:30 PM", text("formatDate(now(), \"[ore] HH:mm A\")"))
        // Il 2 novembre 2026 è un lunedì.
        assertEquals(1.0, num("day(prop(\"Scadenza\"))"), 0.0)
        assertEquals(2.0, num("date(prop(\"Scadenza\"))"), 0.0)
        assertEquals(11.0, num("month(prop(\"Scadenza\"))"), 0.0)
        assertEquals(2026.0, num("year(now())"), 0.0)
        assertEquals(15.0, num("hour(now())"), 0.0)
        assertEquals(30.0, num("minute(now())"), 0.0)
        assertEquals(45.0, num("week(prop(\"Scadenza\"))"), 0.0)
        assertEquals(day(2026, 10, 28), (eval("today()") as FormulaValue.Date).millis)
        assertEquals(now, (eval("now()") as FormulaValue.Date).millis)
        assertEquals(day(2026, 3, 1), (eval("parseDate(\"2026-03-01\")") as FormulaValue.Date).millis)
        assertEquals(FormulaError.Kind.TYPE, error("dateAdd(prop(\"Scadenza\"), 1, \"secoli\")").kind)
        // Una data vuota non si sposta.
        assertEquals(
            FormulaError.Kind.TYPE,
            (eval("dateAdd(prop(\"Scadenza\"), 1, \"days\")", mapOf(due to "")) as FormulaValue.Error).error.kind
        )
    }

    @Test
    fun aDateRangeHasAStartAndAnEnd() {
        val range = "${day(2026, 11, 2)}|${day(2026, 11, 6)}"
        val value = eval("prop(\"Scadenza\")", mapOf(due to range)) as FormulaValue.Date
        assertEquals(day(2026, 11, 6), value.end)
        assertEquals(4.0, num("dateBetween(dateEnd(prop(\"Scadenza\")), dateStart(prop(\"Scadenza\")), \"days\")", mapOf(due to range)), 0.0)
    }

    // --- Le proprietà ---

    @Test
    fun everyKindOfPropertyIsRead() {
        assertEquals(FormulaValue.Date(row.createdAt, hasTime = true), eval("prop(\"Creata\")"))
        assertEquals(90.0, num("prop(\"Durata\")"), 0.0)
        // Il nome della riga, anche col nome dato alla sua colonna.
        assertEquals(FormulaValue.Text("Latte"), eval("prop(\"Name\")"))
        assertEquals(FormulaValue.Text("Latte"), eval("prop(\"Prodotto\")", nameTitles = setOf("Name", "Prodotto")))

        // Relation: i nomi delle righe collegate. Rollup: il numero che dà.
        val shops = DatabaseSnapshot(
            columns = emptyList(),
            rows = listOf(
                DatabaseRowEntity(id = "coop", pageId = "negozi", title = "Coop"),
                DatabaseRowEntity(id = "lidl", pageId = "negozi", title = "Lidl")
            ),
            cells = emptyMap()
        )
        val shop = column("Negozio", ColumnType.RELATION, encodeRelationConfig(RelationConfig(database = "negozi")))
        val count = column("Quanti negozi", ColumnType.ROLLUP)
        val f = formula("F", "prop(\"Negozio\") + \" (\" + prop(\"Quanti negozi\") + \")\"")
        val result = FormulaEngine.computeAll(
            columns + shop + count + f, listOf(row),
            cells(mapOf(shop to "coop|lidl")),
            mapOf((row.id to count.id) to RollupValue("2", number = 2.0)),
            mapOf("negozi" to shops), setOf("Name"), now, zone, PlainRollupFormatter, Locale.ITALIAN
        )
        assertEquals(FormulaValue.Text("Coop, Lidl (2)"), result[row.id to f.id])
    }

    @Test
    fun aFormulaCanReadAnotherFormula() {
        val total = formula("Totale", "prop(\"Prezzo\") * prop(\"Quantità\")")
        val withVat = formula("Con IVA", "round(prop(\"Totale\") * 1.22, 2)")
        val result = FormulaEngine.computeAll(
            columns + withVat + total, listOf(row), cells(), emptyMap(), emptyMap(), setOf("Name"),
            now, zone, PlainRollupFormatter, Locale.ITALIAN
        )
        assertEquals(FormulaValue.Num(10.0), result[row.id to total.id])
        assertEquals(FormulaValue.Num(12.2), result[row.id to withVat.id])
    }

    @Test
    fun twoFormulasThatReadEachOtherSayCircular() {
        val a = formula("A", "prop(\"B\") + 1")
        val b = formula("B", "prop(\"A\") + 1")
        val ok = formula("C", "prop(\"Prezzo\")")
        val result = FormulaEngine.computeAll(
            listOf(price, a, b, ok), listOf(row), cells(), emptyMap(), emptyMap(), setOf("Name"),
            now, zone, PlainRollupFormatter, Locale.ITALIAN
        )
        assertEquals(FormulaError.Kind.CIRCULAR, (result[row.id to a.id] as FormulaValue.Error).error.kind)
        assertEquals(FormulaError.Kind.CIRCULAR, (result[row.id to b.id] as FormulaValue.Error).error.kind)
        // Le altre si calcolano lo stesso.
        assertEquals(FormulaValue.Num(2.5), result[row.id to ok.id])
        // Anche una che legge se stessa.
        assertEquals(FormulaError.Kind.CIRCULAR, error("prop(\"F\") * 2").kind)
    }

    // --- Gli errori ---

    @Test
    fun mistakesBecomeErrorsThatSayWhat() {
        assertEquals(FormulaError(FormulaError.Kind.UNKNOWN_PROPERTY, "Boh"), error("prop(\"Boh\") + 1"))
        assertEquals(FormulaError(FormulaError.Kind.UNKNOWN_FUNCTION, "foo"), error("foo(1)"))
        assertEquals(FormulaError(FormulaError.Kind.WRONG_ARGUMENTS, "if"), error("if(true, 1)"))
        assertEquals(FormulaError.Kind.TYPE, error("\"a\" * 2").kind)
        assertEquals(FormulaError.Kind.INCOMPLETE, error("1 +").kind)
        assertEquals(FormulaError(FormulaError.Kind.MISSING, ")"), error("(1 + 2"))
        assertEquals(FormulaError(FormulaError.Kind.MISSING, ":"), error("true ? 1"))
        assertEquals(FormulaError(FormulaError.Kind.MISSING, "\""), error("\"abc"))
        assertEquals(FormulaError(FormulaError.Kind.UNKNOWN_NAME, "Prezzo"), error("Prezzo * 2"))
        assertEquals(FormulaError.Kind.PROP_NEEDS_NAME, error("prop(1)").kind)
        assertEquals(FormulaError(FormulaError.Kind.UNEXPECTED, "#"), error("1 # 2"))
        assertEquals(FormulaError(FormulaError.Kind.UNEXPECTED, "2"), error("1 2"))
        // Un errore dentro un'operazione passa fuori.
        assertEquals(FormulaError.Kind.UNKNOWN_PROPERTY, error("if(prop(\"Boh\"), 1, 2)").kind)
    }

    @Test
    fun aVeryDeepFormulaIsAnErrorNotACrash() {
        assertEquals(FormulaValue.Num(1.0), eval("(".repeat(50) + "1" + ")".repeat(50)))
        assertTrue(eval("(".repeat(20_000) + "1" + ")".repeat(20_000)) is FormulaValue.Error)
    }

    // --- Leggere, ordinare, rinominare ---

    @Test
    fun displayAndSortNumbers() {
        val f = PlainRollupFormatter
        assertEquals("✓", FormulaEngine.display(FormulaValue.Bool(true), f))
        assertEquals("", FormulaEngine.display(FormulaValue.Bool(false), f))
        assertEquals("", FormulaEngine.display(FormulaValue.Error(FormulaError(FormulaError.Kind.SYNTAX)), f))
        assertEquals("10", FormulaEngine.display(FormulaValue.Num(10.0), f))
        assertEquals(f.date(day(2026, 11, 2)), FormulaEngine.display(FormulaValue.Date(day(2026, 11, 2)), f))
        assertEquals(1.0, FormulaEngine.sortNumber(FormulaValue.Bool(true)))
        assertEquals(day(2026, 11, 2).toDouble(), FormulaEngine.sortNumber(FormulaValue.Date(day(2026, 11, 2))))
        assertNull(FormulaEngine.sortNumber(FormulaValue.Text("x")))
    }

    @Test
    fun renamingAPropertyKeepsTheFormulasThatReadIt() {
        val before = "prop(\"Prezzo\") * prop( 'prezzo' ) + prop(“Prezzo”) + prop(\"Prezzo totale\")"
        assertEquals(
            "prop(\"Costo\") * prop(\"Costo\") + prop(\"Costo\") + prop(\"Prezzo totale\")",
            FormulaEngine.renameProperty(before, "Prezzo", "Costo")
        )
        // Un nome con le virgolette, o con il dollaro, si scrive protetto.
        assertEquals("prop(\"Il \\\"costo\\\" $\")", FormulaEngine.renameProperty("prop(\"Prezzo\")", "Prezzo", "Il \"costo\" $"))
        assertEquals("1 + 1", FormulaEngine.renameProperty("1 + 1", "Prezzo", "Costo"))
    }

    @Test
    fun theConfigSurvivesOtherOptions() {
        assertEquals("", FormulaConfig.parse("[]").expression)
        assertEquals("", FormulaConfig.parse(null).expression)
        assertEquals("", FormulaConfig.parse("{rotto").expression)
        val config = FormulaConfig("prop(\"A\") + 1")
        assertEquals(config, FormulaConfig.parse(FormulaConfig.encode(config)))
    }
}

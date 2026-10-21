package com.gabriele.notionlocal.data.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * **"Relation" e "Rollup"** (21/10/2026): i conti, senza database. L'esempio
 * è quello dell'utente: i gruppi K-pop, le loro canzoni, le vittorie ai
 * music show.
 */
class RelationsTest {

    private val zone = ZoneId.of("Europe/Rome")
    private fun day(text: String) = LocalDate.parse(text).atStartOfDay(zone).toInstant().toEpochMilli()

    // I gruppi.
    private val ive = DatabaseRowEntity(id = "ive", pageId = "groups", title = "IVE")
    private val aespa = DatabaseRowEntity(id = "aespa", pageId = "groups", title = "aespa")
    private val groups = DatabaseSnapshot(columns = emptyList(), rows = listOf(ive, aespa), cells = emptyMap())

    // Le canzoni: il gruppo (una Relation), l'uscita, le settimane in classifica, se è la title track.
    private val songGroup = DatabaseColumnEntity(
        id = "song-group", pageId = "songs", name = "Group", type = ColumnType.RELATION,
        optionsJson = encodeRelationConfig(RelationConfig(database = "groups"))
    )
    private val release = DatabaseColumnEntity(id = "release", pageId = "songs", name = "Release", type = ColumnType.DATE)
    private val weeks = DatabaseColumnEntity(id = "weeks", pageId = "songs", name = "Weeks", type = ColumnType.NUMBER)
    private val titleTrack = DatabaseColumnEntity(id = "title", pageId = "songs", name = "Title track", type = ColumnType.CHECKBOX)
    private val iAm = DatabaseRowEntity(id = "i-am", pageId = "songs", title = "I AM")
    private val supernova = DatabaseRowEntity(id = "supernova", pageId = "songs", title = "Supernova")
    private val armageddon = DatabaseRowEntity(id = "armageddon", pageId = "songs", title = "Armageddon")
    private val songs = DatabaseSnapshot(
        columns = listOf(songGroup, release, weeks, titleTrack),
        rows = listOf(iAm, supernova, armageddon),
        cells = mapOf(
            ("i-am" to "song-group") to "ive",
            ("supernova" to "song-group") to "aespa",
            ("armageddon" to "song-group") to "aespa",
            ("i-am" to "release") to day("2023-04-10").toString(),
            ("supernova" to "release") to day("2024-05-13").toString(),
            ("armageddon" to "release") to day("2024-05-27").toString(),
            ("i-am" to "weeks") to "12",
            ("supernova" to "weeks") to "20",
            ("armageddon" to "weeks") to "7",
            ("supernova" to "title") to "true",
            ("armageddon" to "title") to "true"
        )
    )
    private val databases = mapOf("groups" to groups, "songs" to songs)

    private fun rollup(property: String, calc: RollupCalc, linked: String) =
        computeRollup(RollupConfig("win-song", property, calc), linked, songs, { databases[it] }, PlainRollupFormatter, zone)

    @Test
    fun showOriginalReadsTheNamesAndTheRelationOfTheLinkedRows() {
        assertEquals("I AM, Supernova", rollup(SORT_BY_NAME, RollupCalc.SHOW_ORIGINAL, "i-am|supernova").text)
        // Il gruppo di una canzone è anche lui una Relation: si leggono i nomi a due passi.
        assertEquals("IVE, aespa, aespa", rollup("song-group", RollupCalc.SHOW_ORIGINAL, "i-am|supernova|armageddon").text)
        assertEquals("IVE, aespa", rollup("song-group", RollupCalc.SHOW_UNIQUE, "i-am|supernova|armageddon").text)
    }

    @Test
    fun countsAndPercentages() {
        val all = "i-am|supernova|armageddon"
        assertEquals(RollupValue("3", 3.0), rollup(SORT_BY_NAME, RollupCalc.COUNT_ALL, all))
        assertEquals("2", rollup("song-group", RollupCalc.COUNT_UNIQUE, all).text)
        // I AM non è una title track: una casella vuota.
        assertEquals("1", rollup("title", RollupCalc.UNCHECKED, all).text)
        assertEquals("2", rollup("title", RollupCalc.CHECKED, all).text)
        assertEquals("66.7%", rollup("title", RollupCalc.PERCENT_CHECKED, all).text)
        assertEquals("0", rollup("weeks", RollupCalc.COUNT_EMPTY, all).text)
        assertEquals("100%", rollup("weeks", RollupCalc.PERCENT_NOT_EMPTY, all).text)
        // Nessuna riga collegata: si conta zero, le percentuali non hanno niente da dire.
        assertEquals("0", rollup(SORT_BY_NAME, RollupCalc.COUNT_ALL, "").text)
        assertTrue(rollup("title", RollupCalc.PERCENT_CHECKED, "").isEmpty)
    }

    @Test
    fun numbers() {
        val all = "i-am|supernova|armageddon"
        assertEquals(RollupValue("39", 39.0), rollup("weeks", RollupCalc.SUM, all))
        assertEquals("13", rollup("weeks", RollupCalc.AVERAGE, all).text)
        assertEquals("12", rollup("weeks", RollupCalc.MEDIAN, all).text)
        assertEquals("7", rollup("weeks", RollupCalc.MIN, all).text)
        assertEquals("20", rollup("weeks", RollupCalc.MAX, all).text)
        assertEquals("13", rollup("weeks", RollupCalc.RANGE, all).text)
        assertEquals("16", rollup("weeks", RollupCalc.MEDIAN, "i-am|supernova").text)
    }

    @Test
    fun dates() {
        val all = "i-am|supernova|armageddon"
        assertEquals("2023-04-10", rollup("release", RollupCalc.EARLIEST_DATE, all).text)
        assertEquals(day("2024-05-27"), rollup("release", RollupCalc.LATEST_DATE, all).date)
        assertEquals("14", rollup("release", RollupCalc.DATE_RANGE, "supernova|armageddon").text)
    }

    @Test
    fun rowsThatAreGoneAreSkipped() {
        assertEquals("I AM", rollup(SORT_BY_NAME, RollupCalc.SHOW_ORIGINAL, "i-am|deleted").text)
        assertEquals(listOf("Supernova"), relationTitles("deleted|supernova", songs))
    }

    @Test
    fun theRollupsOfAWholeDatabaseAndTheDatabasesTwoStepsAway() {
        val winSong = DatabaseColumnEntity(
            id = "win-song", pageId = "wins", name = "Song", type = ColumnType.RELATION,
            optionsJson = encodeRelationConfig(RelationConfig(database = "songs"))
        )
        val artist = DatabaseColumnEntity(
            id = "artist", pageId = "wins", name = "Artist", type = ColumnType.ROLLUP,
            optionsJson = encodeRollupConfig(RollupConfig(relation = "win-song", property = "song-group"))
        )
        val win1 = DatabaseRowEntity(id = "w1", pageId = "wins", title = "Music Bank")
        val win2 = DatabaseRowEntity(id = "w2", pageId = "wins", title = "Inkigayo")
        val cells = mapOf(("w1" to "win-song") to "supernova")
        // Per leggere il gruppo serve anche il database dei gruppi.
        assertEquals(setOf("groups"), secondHopDatabases(listOf(winSong, artist), mapOf("songs" to songs)))
        assertEquals(emptySet<String>(), secondHopDatabases(listOf(winSong, artist), databases))
        val rollups = computeRollups(listOf(winSong, artist), listOf(win1, win2), cells, databases, PlainRollupFormatter)
        // Solo le celle con qualcosa da mostrare.
        assertEquals(mapOf(("w1" to "artist") to RollupValue("aespa")), rollups)
    }

    @Test
    fun theTwinCellsFollowTheChanges() {
        val twin = mapOf("ive" to "old-song", "aespa" to "supernova|i-am|armageddon")
        // I AM passa da aespa a IVE: IVE la guadagna, aespa la perde.
        val changes = reverseChanges("i-am", before = listOf("aespa"), after = listOf("ive")) { twin[it] }
        assertEquals(mapOf("ive" to "old-song|i-am", "aespa" to "supernova|armageddon"), changes)
        // Una riga di là che non l'aveva già: niente da togliere.
        assertEquals(emptyMap<String, String>(), reverseChanges("old-song", listOf("aespa"), emptyList()) { twin[it] })
        // Supernova resta di aespa: niente da scrivere.
        assertEquals(emptyMap<String, String>(), reverseChanges("supernova", listOf("aespa"), listOf("aespa")) { twin[it] })
        val removed = reverseChanges("supernova", listOf("aespa"), emptyList()) { twin[it] }
        assertEquals(mapOf("aespa" to "i-am|armageddon"), removed)
    }

    @Test
    fun theConfigurationSurvivesAndBelongsToItsType() {
        val config = RelationConfig(database = "groups", reverse = "twin")
        val column = songGroup.copy(optionsJson = encodeRelationConfig(config))
        assertEquals(config, column.relationConfig())
        assertTrue(column.relationConfig()!!.twoWay)
        assertNull(column.copy(type = ColumnType.TEXT).relationConfig())
        assertNull(column.rollupConfig())
        // Un vecchio elenco di tag non è una configurazione.
        assertNull(songGroup.copy(optionsJson = "[]").relationConfig())
        assertEquals(listOf("a", "b"), relationIds("a|b|a||"))
        assertEquals("a|b", encodeRelationIds(listOf("a", "b", "a")))
    }

    @Test
    fun theCalculationsFollowThePropertyType() {
        assertTrue(RollupCalc.SUM in rollupCalcsFor(ColumnType.NUMBER))
        assertTrue(RollupCalc.SUM !in rollupCalcsFor(null))
        assertTrue(RollupCalc.LATEST_DATE in rollupCalcsFor(ColumnType.CREATED_TIME))
        assertTrue(RollupCalc.SHOW_UNIQUE !in rollupCalcsFor(ColumnType.CHECKBOX))
        assertEquals(RollupCalc.SHOW_ORIGINAL, RollupCalc.of("NOPE"))
    }

    @Test
    fun aCopiedDatabaseKeepsItsLinksInside() {
        val self = DatabaseColumnEntity(
            id = "next", pageId = "songs", name = "Next single", type = ColumnType.RELATION,
            optionsJson = encodeRelationConfig(RelationConfig(database = "songs", reverse = "prev"))
        )
        val other = songGroup.copy(optionsJson = encodeRelationConfig(RelationConfig(database = "groups", reverse = "group-songs")))
        val count = DatabaseColumnEntity(
            id = "count", pageId = "songs", name = "Count", type = ColumnType.ROLLUP,
            optionsJson = encodeRollupConfig(RollupConfig(relation = "next", property = "weeks", calc = RollupCalc.SUM))
        )
        val ids = mapOf("next" to "next2", "prev" to "prev2", "song-group" to "group2", "weeks" to "weeks2", "count" to "count2")
        // Dentro la copia: anche la gemella è della copia.
        assertEquals(RelationConfig("copy", "prev2"), self.copy(optionsJson = copiedLinkOptions(self, "songs", "copy", ids)).relationConfig())
        // Verso un altro database: in un senso solo, la gemella di là è dell'originale.
        assertEquals(RelationConfig("groups"), other.copy(optionsJson = copiedLinkOptions(other, "songs", "copy", ids)).relationConfig())
        assertEquals(
            RollupConfig("next2", "weeks2", RollupCalc.SUM),
            count.copy(optionsJson = copiedLinkOptions(count, "songs", "copy", ids)).rollupConfig()
        )
        assertEquals("[]", copiedLinkOptions(weeks.copy(optionsJson = "[]"), "songs", "copy", ids))
    }

    @Test
    fun aChartCanHaveABarForEachLinkedRow() {
        val winSong = DatabaseColumnEntity(
            id = "win-song", pageId = "wins", name = "Song", type = ColumnType.RELATION,
            optionsJson = encodeRelationConfig(RelationConfig(database = "songs"))
        )
        val wins = (1..4).map { DatabaseRowEntity(id = "w$it", pageId = "wins", title = "Win $it") }
        val cells = mapOf(
            ("w1" to "win-song") to "supernova",
            ("w2" to "win-song") to "supernova",
            ("w3" to "win-song") to "i-am"
        )
        val labels = ChartLabels(untitled = "Untitled", empty = "Empty", checked = "Checked", unchecked = "Unchecked", day = { it.toString() })
        val points = chartPoints(wins, listOf(winSong), cells, ChartSettings(groupColumnId = "win-song"), labels, zone, databases)
        assertEquals(setOf("Supernova" to 2.0, "I AM" to 1.0, "Empty" to 1.0), points.map { it.label to it.value }.toSet())
    }

    @Test
    fun theColumnCalculationCountsEachLinkedRow() {
        val values = listOf("a|b", "c", "")
        assertEquals(CalculationResult.Count(3), calculate(Calculation.COUNT_VALUES, ColumnType.RELATION, values))
        assertEquals(CalculationResult.Count(3), calculate(Calculation.COUNT_UNIQUE, ColumnType.RELATION, values + "a"))
        assertEquals(CalculationResult.Count(1), calculate(Calculation.COUNT_EMPTY, ColumnType.RELATION, values))
    }
}

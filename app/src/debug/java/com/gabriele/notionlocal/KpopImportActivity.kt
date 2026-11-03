package com.gabriele.notionlocal

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.withTransaction
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseCellEntity
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.DateGroupBy
import com.gabriele.notionlocal.data.entity.GroupRuleSpec
import com.gabriele.notionlocal.data.entity.BOARD_COLUMNS_HIDE_EMPTY
import com.gabriele.notionlocal.data.entity.boardGroupRules
import com.gabriele.notionlocal.data.entity.encodeGroupRules
import com.gabriele.notionlocal.data.entity.DateUnit
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.RelationConfig
import com.gabriele.notionlocal.data.entity.RollupCalc
import com.gabriele.notionlocal.data.entity.RollupConfig
import com.gabriele.notionlocal.data.entity.SORT_BY_NAME
import com.gabriele.notionlocal.data.entity.SelectOption
import com.gabriele.notionlocal.data.entity.SortRuleSpec
import com.gabriele.notionlocal.data.entity.TAG_COLORS
import com.gabriele.notionlocal.data.entity.encodeRelationConfig
import com.gabriele.notionlocal.data.entity.encodeRelationIds
import com.gabriele.notionlocal.data.entity.encodeRollupConfig
import com.gabriele.notionlocal.data.entity.encodeSortRules
import com.gabriele.notionlocal.data.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate

/**
 * **Solo nella build di debug**: le vittorie ai music show K-pop di
 * soridata.com (dal 2006) nel database "KPOP Music Shows" dell'utente
 * (21/10/2026), con due database nuovi collegati dalle Relation: "KPOP
 * Songs" e "KPOP Groups", in fondo alla pagina "KPOP".
 *
 * Il file si prepara sul PC (`soridata/wins.json`) e si mette in
 * `files/kpop_wins.json` dell'app (adb push e run-as cp); il risultato va in
 * `files/kpop-import-log.txt`. Una volta sola: se "KPOP Music Shows" ha già
 * una Relation non tocca niente.
 *
 * adb shell am start -n com.gabriele.notionlocal/.KpopImportActivity
 */
class KpopImportActivity : ComponentActivity() {

    @Serializable
    private data class Win(
        val date: String,
        val show: String,
        val artist: String,
        val artistSlug: String,
        val song: String,
        val songId: Long
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.getInstance(applicationContext)
        lifecycleScope.launch {
            val report = runCatching { withContext(Dispatchers.IO) { import(db) } }
                .getOrElse { "ERRORE\n" + it.stackTraceToString() }
            File(filesDir, "kpop-import-log.txt").writeText(report)
            Log.i("KpopImport", report)
            finish()
        }
    }

    private suspend fun import(db: AppDatabase): String {
        val wins = Json { ignoreUnknownKeys = true }
            .decodeFromString<List<Win>>(File(filesDir, "kpop_wins.json").readText())
            .sortedWith(compareBy({ it.date }, { SHOWS.indexOf(it.show) }))
        val dao = db.databaseDao()
        val pages = db.pageDao()
        val blocks = db.blockDao()
        return db.withTransaction {
            pages.getById(WINS_DB) ?: error("manca KPOP Music Shows")
            val columns = dao.getColumnsForPageOnce(WINS_DB)
            if (columns.any { it.type == ColumnType.RELATION }) error("KPOP Music Shows ha già una Relation: già importato?")
            val groupColumn = columns.single { it.name == "Group" && it.type == ColumnType.MULTI_SELECT }
            val showColumn = columns.single { it.name == "Music show" && it.type == ColumnType.SELECT }
            val dateColumn = columns.single { it.name == "Win date" && it.type == ColumnType.DATE }
            val container = blocks.getBlocksLinkingTo(WINS_DB).first().pageId
            val now = System.currentTimeMillis()

            // I due database nuovi, semplici come quello delle vittorie, in fondo a "KPOP".
            val songsDb = PageEntity(title = "KPOP Songs", isDatabase = true, isSimpleDatabase = true, nameColumnTitle = "Song")
            val groupsDb = PageEntity(title = "KPOP Groups", isDatabase = true, isSimpleDatabase = true, nameColumnTitle = "Group")
            pages.insert(songsDb)
            pages.insert(groupsDb)
            var blockOrder = (blocks.getBlocksForPageOnce(container).filter { it.parentBlockId == null }.maxOfOrNull { it.orderIndex } ?: -1) + 1
            listOf(songsDb, groupsDb).forEach { page ->
                blocks.insert(BlockEntity(pageId = container, type = BlockType.PAGE_LINK, linkedPageId = page.id, orderIndex = blockOrder++))
            }

            // Le Relation, tutte nei due sensi, e i Rollup.
            fun relation(id: String, page: String, name: String, target: String, twin: String, order: Int) = DatabaseColumnEntity(
                id = id, pageId = page, name = name, type = ColumnType.RELATION, orderIndex = order,
                optionsJson = encodeRelationConfig(RelationConfig(database = target, reverse = twin))
            )
            fun rollup(page: String, name: String, config: RollupConfig, order: Int) = DatabaseColumnEntity(
                pageId = page, name = name, type = ColumnType.ROLLUP, orderIndex = order, optionsJson = encodeRollupConfig(config)
            )
            val winSongId = newId()
            val songWinsId = newId()
            val songGroupId = newId()
            val groupSongsId = newId()
            val winSong = relation(winSongId, WINS_DB, "Song", songsDb.id, songWinsId, (columns.maxOfOrNull { it.orderIndex } ?: -1) + 1)
            val songGroup = relation(songGroupId, songsDb.id, "Group", groupsDb.id, groupSongsId, 0)
            val songWins = relation(songWinsId, songsDb.id, "Wins", WINS_DB, winSongId, 1)
            val groupSongs = relation(groupSongsId, groupsDb.id, "Songs", songsDb.id, songGroupId, 0)
            val songWinCount = rollup(songsDb.id, "Number of wins", RollupConfig(songWinsId, SORT_BY_NAME, RollupCalc.COUNT_ALL), 2)
            val groupWinCount = rollup(groupsDb.id, "Number of wins", RollupConfig(groupSongsId, songWinsId, RollupCalc.COUNT_VALUES), 2)
            listOf(
                winSong, songGroup, songWins, groupSongs,
                songWinCount,
                rollup(songsDb.id, "First win", RollupConfig(songWinsId, dateColumn.id, RollupCalc.EARLIEST_DATE), 3),
                rollup(songsDb.id, "Last win", RollupConfig(songWinsId, dateColumn.id, RollupCalc.LATEST_DATE), 4),
                rollup(songsDb.id, "Music shows", RollupConfig(songWinsId, showColumn.id, RollupCalc.SHOW_UNIQUE), 5),
                rollup(groupsDb.id, "Winning songs", RollupConfig(groupSongsId, SORT_BY_NAME, RollupCalc.COUNT_ALL), 1),
                groupWinCount
            ).forEach { dao.insertColumn(it) }

            // I gruppi: col nome dei tag che l'utente ha già ("aespa", non "Aespa").
            val options = Json { ignoreUnknownKeys = true }.decodeFromString<List<SelectOption>>(groupColumn.optionsJson).toMutableList()
            fun label(artist: String): String {
                options.find { it.label.equals(artist, ignoreCase = true) }?.let { return it.label }
                val color = TAG_COLORS[1 + options.size % (TAG_COLORS.size - 1)].hex
                options += SelectOption(label = artist, color = color)
                return artist
            }
            val groupRows = LinkedHashMap<String, DatabaseRowEntity>()
            val groupLabels = HashMap<String, String>()
            wins.forEach { win ->
                groupRows.getOrPut(win.artistSlug) {
                    val name = label(win.artist)
                    groupLabels[win.artistSlug] = name
                    DatabaseRowEntity(pageId = groupsDb.id, title = name, orderIndex = groupRows.size, createdAt = now, updatedAt = now)
                }
            }
            val songRows = LinkedHashMap<Long, DatabaseRowEntity>()
            val songGroupOf = HashMap<Long, String>()
            wins.forEach { win ->
                songRows.getOrPut(win.songId) {
                    songGroupOf[win.songId] = win.artistSlug
                    DatabaseRowEntity(pageId = songsDb.id, title = win.song, orderIndex = songRows.size, createdAt = now, updatedAt = now)
                }
            }
            groupRows.values.forEach { dao.insertRow(it) }
            songRows.values.forEach { dao.insertRow(it) }

            // Le vittorie, dalla più vecchia, dopo le righe che c'erano già.
            var rowOrder = (dao.getRowsForPageOnce(WINS_DB).maxOfOrNull { it.orderIndex } ?: -1) + 1
            val winsOfSong = HashMap<Long, MutableList<String>>()
            wins.forEach { win ->
                val row = DatabaseRowEntity(pageId = WINS_DB, title = win.song, orderIndex = rowOrder++, createdAt = now, updatedAt = now)
                dao.insertRow(row)
                val day = LocalDate.parse(win.date).atStartOfDay(AppSettings.zoneId).toInstant().toEpochMilli()
                val song = songRows.getValue(win.songId)
                dao.insertCell(DatabaseCellEntity(rowId = row.id, columnId = groupColumn.id, value = groupLabels.getValue(win.artistSlug)))
                dao.insertCell(DatabaseCellEntity(rowId = row.id, columnId = showColumn.id, value = SHOW_LABELS.getValue(win.show)))
                dao.insertCell(DatabaseCellEntity(rowId = row.id, columnId = dateColumn.id, value = day.toString()))
                dao.insertCell(DatabaseCellEntity(rowId = row.id, columnId = winSongId, value = song.id))
                winsOfSong.getOrPut(win.songId) { mutableListOf() } += row.id
            }
            // Le altre metà delle Relation.
            val songsOfGroup = HashMap<String, MutableList<String>>()
            songRows.forEach { (songId, row) ->
                val group = groupRows.getValue(songGroupOf.getValue(songId))
                dao.insertCell(DatabaseCellEntity(rowId = row.id, columnId = songGroupId, value = group.id))
                dao.insertCell(DatabaseCellEntity(rowId = row.id, columnId = songWinsId, value = encodeRelationIds(winsOfSong.getValue(songId))))
                songsOfGroup.getOrPut(group.id) { mutableListOf() } += row.id
            }
            songsOfGroup.forEach { (groupId, songs) ->
                dao.insertCell(DatabaseCellEntity(rowId = groupId, columnId = groupSongsId, value = encodeRelationIds(songs)))
            }
            dao.updateColumn(groupColumn.copy(optionsJson = Json.encodeToString(options)))

            // Canzoni e gruppi dal più premiato.
            pages.update(pages.getById(songsDb.id)!!.copy(sortRulesJson = encodeSortRules(listOf(SortRuleSpec(songWinCount.id, descending = true)))))
            pages.update(pages.getById(groupsDb.id)!!.copy(sortRulesJson = encodeSortRules(listOf(SortRuleSpec(groupWinCount.id, descending = true)))))

            // La bacheca: colonne per programma, fasce per settimana dalla più recente, come Soridata.
            val board = pages.getById(BOARD_VIEW)
            if (board != null) {
                val hidden = (board.viewHiddenColumnIds.orEmpty().split(",").filter { it.isNotBlank() } +
                    listOf(winSongId) + columns.filter { it.name == "Album" || it.name == "Release date" }.map { it.id }).distinct()
                // Dal 28/10/2026 le fasce sono la seconda regola dei raggruppamenti della bacheca.
                val columnsRule = board.boardGroupRules().firstOrNull()
                    ?: columns.sortedBy { it.orderIndex }.firstOrNull { it.type == ColumnType.SELECT }
                        ?.let { GroupRuleSpec(it.id, hideEmpty = BOARD_COLUMNS_HIDE_EMPTY) }
                val weeks = GroupRuleSpec(dateColumn.id, dateBy = DateGroupBy(DateUnit.WEEK, newestFirst = true))
                pages.update(
                    board.copy(
                        boardGroupRulesJson = encodeGroupRules(listOfNotNull(columnsRule) + weeks),
                        viewHiddenColumnIds = hidden.joinToString(",")
                    )
                )
            }

            "OK\nvittorie: ${wins.size}\ncanzoni: ${songRows.size}\ngruppi: ${groupRows.size}\n" +
                "tag nuovi: ${options.size}\nKPOP Songs: ${songsDb.id}\nKPOP Groups: ${groupsDb.id}\n" +
                "dal ${wins.first().date} al ${wins.last().date}"
        }
    }

    private fun newId() = java.util.UUID.randomUUID().toString()

    companion object {
        private const val WINS_DB = "7b7d8ff1-a788-4779-8792-62ed053117b1"
        private const val BOARD_VIEW = "c510310e-0169-4a4f-ad03-cb04bd1fc8b1"
        private val SHOWS = listOf("The Show", "Show Champion", "M Countdown", "Music Bank", "Show! Music Core", "Inkigayo")

        /** I programmi di Soridata → le opzioni che l'utente ha già in "Music show". */
        private val SHOW_LABELS = mapOf(
            "The Show" to "The Show (Tuesday)",
            "Show Champion" to "Show Champion (Wednesday)",
            "M Countdown" to "M Countdown (Thursday)",
            "Music Bank" to "Music Bank (Friday)",
            "Show! Music Core" to "Show! Music Core (Saturday)",
            "Inkigayo" to "Inkigayo (Sunday)"
        )
    }
}

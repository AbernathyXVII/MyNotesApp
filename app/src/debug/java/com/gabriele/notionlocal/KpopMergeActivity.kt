package com.gabriele.notionlocal

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.DatabaseLayout
import com.gabriele.notionlocal.data.repository.PageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * **Solo nella build di debug**: i tre database K-pop dell'utente in uno
 * solo con tre viste (21/10/2026, chiesto dall'utente): "KPOP Music Shows"
 * resta il database, con la bacheca delle vittorie come vista di partenza, e
 * prende due viste Table con le righe di "KPOP Songs" e "KPOP Groups" ("Data
 * source"). I due collegamenti in fondo alla pagina "KPOP" se ne vanno; i
 * database restano, sono le sorgenti delle viste. Una volta sola.
 *
 * adb shell am start -n com.gabriele.notionlocal/.KpopMergeActivity
 */
class KpopMergeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.getInstance(applicationContext)
        lifecycleScope.launch {
            val report = runCatching { withContext(Dispatchers.IO) { merge(db) } }
                .getOrElse { "ERRORE\n" + it.stackTraceToString() }
            File(filesDir, "kpop-merge-log.txt").writeText(report)
            Log.i("KpopMerge", report)
            finish()
        }
    }

    private suspend fun merge(db: AppDatabase): String {
        val pages = PageRepository(db)
        val pageDao = db.pageDao()
        val host = pageDao.getById(WINS_DB) ?: error("manca KPOP Music Shows")
        val databases = pageDao.getLinkableDatabases()
        val songs = databases.single { it.title == "KPOP Songs" }
        val groups = databases.single { it.title == "KPOP Groups" }
        val views = pageDao.getViewsOnce(WINS_DB)
        if (views.any { it.sourceDatabaseId != WINS_DB }) error("ha già viste di altri database: già fatto?")

        // La bacheca diventa la vista di partenza: la tabella delle vittorie se ne va.
        if (views.any { it.databaseLayout == DatabaseLayout.BOARD } && host.databaseLayout != DatabaseLayout.BOARD) {
            check(pages.deleteStartingView(WINS_DB)) { "la vista di partenza non si toglie" }
        }
        // Le due Table, ordinate dal più premiato come i loro database.
        val songsView = pages.createView(WINS_DB, "KPOP Songs", DatabaseLayout.TABLE, songs.id) ?: error("vista KPOP Songs")
        val groupsView = pages.createView(WINS_DB, "KPOP Groups", DatabaseLayout.TABLE, groups.id) ?: error("vista KPOP Groups")
        pageDao.update(pageDao.getById(songsView)!!.copy(sortRulesJson = songs.sortRulesJson))
        pageDao.update(pageDao.getById(groupsView)!!.copy(sortRulesJson = groups.sortRulesJson))
        pages.setActiveView(WINS_DB, null)

        // I collegamenti ai due database in fondo alla pagina: ora sono viste.
        val removed = listOf(songs.id, groups.id).flatMap { id ->
            db.blockDao().getBlocksLinkingTo(id).filter { it.type == BlockType.PAGE_LINK && it.pageId == KPOP_PAGE }
        }
        removed.forEach { db.blockDao().delete(it) }

        val after = pageDao.getById(WINS_DB)!!
        return "OK\nvista di partenza: ${after.databaseLayout} ${after.viewName}\n" +
            "viste: " + pageDao.getViewsOnce(WINS_DB).joinToString { "${it.viewName} (${it.databaseLayout})" } +
            "\ncollegamenti tolti: ${removed.size}"
    }

    companion object {
        private const val WINS_DB = "7b7d8ff1-a788-4779-8792-62ed053117b1"
        private const val KPOP_PAGE = "7c1561e2-ee8e-41b4-b983-110bc8f31e69"
    }
}

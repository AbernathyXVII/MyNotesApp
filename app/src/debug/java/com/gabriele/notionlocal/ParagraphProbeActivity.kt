package com.gabriele.notionlocal

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import kotlinx.coroutines.launch
import java.io.File

/**
 * **Solo nella build di debug**: la pagina di prova del "Paragraph"
 * (21/10/2026), con paragrafi, elenchi numerati e puntati, caselle e un
 * toggle con dentro un elenco e una casella. Fatta una volta, collegata in
 * fondo a "My Space" come una pagina normale (così l'utente la trova e la
 * può buttare), poi si riapre quella.
 *
 * adb shell am start -n com.gabriele.notionlocal/.ParagraphProbeActivity
 */
class ParagraphProbeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.getInstance(applicationContext)
        val remembered = File(filesDir, "paragraph-probe.txt")
        lifecycleScope.launch {
            val existing = remembered.takeIf { it.exists() }?.readText()?.trim()
                ?.takeIf { db.pageDao().getById(it)?.trashedAt == null && db.pageDao().getById(it) != null }
            val pageId = existing ?: create(db).also { remembered.writeText(it) }
            startActivity(
                Intent(this@ParagraphProbeActivity, MainActivity::class.java)
                    .putExtra("notify.pageId", pageId)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
            finish()
        }
    }

    private suspend fun create(db: AppDatabase): String {
        val page = PageEntity(title = "Paragraph – prova")
        db.pageDao().insert(page)
        fun line(text: String, at: Int, type: BlockType = BlockType.PARAGRAPH, parent: String? = null, checked: Boolean = false) =
            BlockEntity(
                pageId = page.id,
                parentBlockId = parent,
                type = type,
                textJson = """[{"text":"$text"}]""",
                orderIndex = at,
                isChecked = checked
            )
        val toggle = line("Un toggle", 9, BlockType.TOGGLE).copy(isExpanded = true)
        db.blockDao().insertAll(
            listOf(
                line("Primo paragrafo", 0),
                line("Secondo paragrafo", 1),
                line("Primo numero", 2, BlockType.NUMBERED_LIST_ITEM),
                line("Secondo numero", 3, BlockType.NUMBERED_LIST_ITEM),
                line("Un pallino", 4, BlockType.BULLET_LIST_ITEM),
                line("Un altro pallino", 5, BlockType.BULLET_LIST_ITEM),
                line("Una cosa da fare", 6, BlockType.CHECKBOX),
                line("Una cosa fatta", 7, BlockType.CHECKBOX, checked = true),
                line("Un paragrafo prima del toggle", 8),
                toggle,
                line("Pallino nel toggle", 0, BlockType.BULLET_LIST_ITEM, parent = toggle.id),
                line("Casella nel toggle", 1, BlockType.CHECKBOX, parent = toggle.id),
                line("Ultimo paragrafo", 10)
            )
        )
        // Il collegamento in fondo a "My Space", come una sottopagina normale.
        val root = db.blockDao().getBlocksForPageOnce(PageEntity.ROOT_PAGE_ID).filter { it.parentBlockId == null }
        db.blockDao().insert(
            BlockEntity(
                pageId = PageEntity.ROOT_PAGE_ID,
                type = BlockType.PAGE_LINK,
                linkedPageId = page.id,
                orderIndex = (root.maxOfOrNull { it.orderIndex } ?: -1) + 1
            )
        )
        return page.id
    }
}

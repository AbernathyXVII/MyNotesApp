package com.gabriele.notionlocal.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.buttons.ButtonBlock
import com.gabriele.notionlocal.data.buttons.ButtonConfig
import com.gabriele.notionlocal.data.buttons.ButtonFilter
import com.gabriele.notionlocal.data.buttons.ButtonStep
import com.gabriele.notionlocal.data.buttons.EditOp
import com.gabriele.notionlocal.data.buttons.InsertPlace
import com.gabriele.notionlocal.data.buttons.PropertyEdit
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.ButtonEditorContent
import com.gabriele.notionlocal.ui.screen.ButtonEditorEnv
import com.gabriele.notionlocal.ui.screen.ButtonEditorState
import com.gabriele.notionlocal.ui.screen.ButtonPill
import com.gabriele.notionlocal.ui.screen.LocalButtonEditorEnv
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * **I bottoni disegnati davvero** (27/10/2026), su un'immagine da guardare a
 * occhio in `app/build/button-screenshots/`: le pastiglie (vuota, con
 * l'emoji, la copia collegata, quella del bottone cancellato, quella di una
 * cella) e la finestra delle azioni con un'azione per tipo.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h2600dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ButtonsRenderTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()

    @After
    fun tearDown() {
        db.close()
    }

    /** La finestra disegnata su un'immagine, a mano, tagliata dove finisce il disegno (come `WidgetsRenderTest`). */
    private fun snapshot(name: String) {
        compose.waitForIdle()
        val content = compose.activity.window.decorView.rootView as android.view.ViewGroup
        val bitmap = Bitmap.createBitmap(content.width, content.height, Bitmap.Config.ARGB_8888)
        content.draw(android.graphics.Canvas(bitmap))
        val probeX = (bitmap.width * 0.3).toInt()
        val empty = bitmap.getPixel(bitmap.width - 1, bitmap.height - 1)
        var bottom = bitmap.height - 1
        while (bottom > 0 && bitmap.getPixel(probeX, bottom) == empty) bottom--
        val cropped = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, (bottom + 1).coerceAtLeast(1))
        val dir = File("build/button-screenshots").apply { mkdirs() }
        File(dir, name).outputStream().use { cropped.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun theButtonsAndTheirWindowRender() = runBlocking<Unit> {
        AppSettings.init(context)
        val pages = PageRepository(db)
        pages.createPage(PageEntity(id = "tasks", title = "Tasks", icon = "✅", isDatabase = true))
        db.databaseDao().insertColumn(DatabaseColumnEntity(id = "done", pageId = "tasks", name = "Done", type = ColumnType.CHECKBOX, orderIndex = 0))
        db.databaseDao().insertColumn(DatabaseColumnEntity(id = "count", pageId = "tasks", name = "Count", type = ColumnType.NUMBER, orderIndex = 1))
        db.databaseDao().insertColumn(
            DatabaseColumnEntity(
                id = "status", pageId = "tasks", name = "Status", type = ColumnType.MULTI_SELECT, orderIndex = 2,
                optionsJson = """[{"label":"Casa","color":"#3A6590"},{"label":"Lavoro","color":"#984F49"},{"label":"Urgente","color":"#89692C"}]"""
            )
        )
        val config = ButtonConfig(
            name = "Fine giornata",
            icon = "🌙",
            steps = listOf(
                ButtonStep.Confirm(message = "Chiudo la giornata?"),
                ButtonStep.InsertBlocks(
                    blocks = listOf(ButtonBlock(type = "CHECKBOX", text = "Bere acqua"), ButtonBlock(type = "DIVIDER")),
                    place = InsertPlace.BELOW_BUTTON
                ),
                ButtonStep.AddPage(
                    databaseId = "tasks", title = "Diario",
                    edits = listOf(PropertyEdit(columnId = "status", op = EditOp.ADD, value = "Casa|Urgente")),
                    openAfter = true
                ),
                ButtonStep.EditPages(
                    databaseId = "tasks",
                    filter = ButtonFilter("done", listOf("false")),
                    edits = listOf(PropertyEdit(columnId = "count", op = EditOp.ADD, value = "1"))
                ),
                ButtonStep.Open(url = "https://notion.so"),
                ButtonStep.Notify(title = "Fatto", text = "Buonanotte")
            )
        )
        val env = ButtonEditorEnv(pages, null, PageImageStore(context))
        compose.setContent {
            NotionLocalTheme {
                // Come nella finestra dal basso: lo sfondo dà al testo il suo colore chiaro.
                CompositionLocalProvider(LocalButtonEditorEnv provides env) {
                    Surface(color = DarkSheet, modifier = Modifier.width(400.dp)) {
                    Column {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ButtonPill(config = ButtonConfig(), linked = false, running = false, onClick = {}, onLongClick = null)
                            ButtonPill(config = config, linked = false, running = false, onClick = {}, onLongClick = null)
                            ButtonPill(config = config, linked = true, running = false, onClick = {}, onLongClick = null)
                            ButtonPill(config = null, linked = true, running = false, onClick = {}, onLongClick = null)
                            ButtonPill(config = config.copy(icon = null, name = "Preso"), linked = false, running = false, onClick = {}, onLongClick = null, compact = true)
                        }
                        ButtonEditorContent(
                            state = ButtonEditorState(config),
                            inDatabase = false,
                            thisPageDatabaseId = null,
                            linked = false,
                            onCancel = {},
                            onDone = {},
                            modifier = Modifier.height(1500.dp)
                        )
                    }
                    }
                }
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Tasks").fetchSemanticsNodes().size >= 2 }
        snapshot("buttons.png")
    }
}

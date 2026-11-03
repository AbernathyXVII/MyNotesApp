package com.gabriele.notionlocal.ui

import android.app.Application
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockAttrs
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.attrs
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.ImageBlockContent
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.MediaRequestKind
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * **Ridimensionare un'immagine** (20/10/2026): le maniglie solo dopo
 * "Resize" o "Full resize" dal menu; "Resize" tiene la forma, "Full resize"
 * muove solo il bordo afferrato, come una finestra.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h2000dp-xhdpi")
class ImageResizeTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var db: AppDatabase
    private lateinit var viewModel: PageEditorViewModel
    private val page = PageEntity(title = "Foto")
    private val image = BlockEntity(
        pageId = page.id,
        type = BlockType.IMAGE,
        attrsJson = BlockAttrs.encode(BlockAttrs(file = "missing.jpg", width = 0.5f, aspect = 0.5f)),
        orderIndex = 0
    )

    @Before
    fun setUp() = runBlocking<Unit> {
        AppSettings.init(ApplicationProvider.getApplicationContext())
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val pages = PageRepository(db)
        pages.createPage(page)
        db.blockDao().insert(image)
        viewModel = PageEditorViewModel(pages)
        viewModel.load(page.id)
        waitFor("l'immagine") { viewModel.blocks.value.size == 1 }
    }

    @After
    fun tearDown() {
        // Prima il ViewModel, poi il database: un suo lavoro in sospeso sul database chiuso
        // finirebbe come errore nel test dopo.
        viewModel.viewModelScope.cancel()
        shadowOf(Looper.getMainLooper()).idle()
        db.close()
    }

    private fun waitFor(what: String, condition: () -> Boolean) {
        val end = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < end) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(10)
        }
        fail("Non è successo: $what")
    }

    private fun saved() = runBlocking { db.blockDao().getById(image.id)!! }

    @Test
    fun theHandlesShowOnlyWhenAskedFromTheMenu() {
        compose.setContent {
            NotionLocalTheme {
                val block = viewModel.blocks.value.single()
                ImageBlockContent(block = block, viewModel = viewModel, locked = false, onLongPress = {})
            }
        }
        // All'inizio niente maniglie.
        compose.onAllNodesWithContentDescription("Resize").assertCountEquals(0)

        // "Resize": le due ai lati, e i pulsanti per finire.
        viewModel.requestMedia(image.id, MediaRequestKind.RESIZE)
        compose.waitForIdle()
        compose.onAllNodesWithContentDescription("Resize").assertCountEquals(2)
        compose.onNodeWithText("Done").performClick()
        compose.waitForIdle()
        compose.onAllNodesWithContentDescription("Resize").assertCountEquals(0)

        // "Full resize": i quattro lati e i quattro angoli.
        viewModel.requestMedia(image.id, MediaRequestKind.FULL_RESIZE)
        compose.waitForIdle()
        compose.onAllNodesWithContentDescription("Resize").assertCountEquals(8)
        // "Original size": la misura e la forma tornano quelle dell'immagine.
        compose.onNodeWithText("Original size").performClick()
        waitFor("la misura originale") { saved().attrs.width == null }
        assertNull(saved().attrs.aspect)
        assertNull(saved().attrs.left)
    }
}

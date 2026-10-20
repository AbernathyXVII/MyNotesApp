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
import com.gabriele.notionlocal.ui.screen.ImageFrame
import com.gabriele.notionlocal.ui.screen.dragged
import com.gabriele.notionlocal.ui.screen.frameOf
import com.gabriele.notionlocal.ui.screen.widened
import com.gabriele.notionlocal.ui.screen.withFrame
import com.gabriele.notionlocal.ui.screen.withOriginalSize
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.MediaRequestKind
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
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
    fun resizeKeepsTheCenterAndTheShape() {
        val frame = ImageFrame(left = 250f, width = 500f, height = 250f)
        val wider = frame.widened(side = 1, dx = 100f, pageWidth = 1000f, minWidth = 250f)
        assertEquals(ImageFrame(150f, 700f, 350f), wider)
        // Mai oltre la pagina, mai sotto il minimo.
        assertEquals(1000f, frame.widened(1, 900f, 1000f, 250f).width)
        assertEquals(250f, frame.widened(-1, 900f, 1000f, 250f).width)
    }

    @Test
    fun fullResizeMovesOnlyTheEdgeYouHold() {
        val frame = ImageFrame(left = 100f, width = 400f, height = 300f)
        // Il bordo destro: solo la larghezza.
        assertEquals(ImageFrame(100f, 450f, 300f), frame.dragged(1, 0, 50f, 0f, 1000f, 48f))
        // Il bordo sinistro: comincia più a destra, il destro resta dov'era.
        assertEquals(ImageFrame(150f, 350f, 300f), frame.dragged(-1, 0, 50f, 0f, 1000f, 48f))
        // Sotto e sopra: solo l'altezza.
        assertEquals(ImageFrame(100f, 400f, 360f), frame.dragged(0, 1, 0f, 60f, 1000f, 48f))
        assertEquals(ImageFrame(100f, 400f, 240f), frame.dragged(0, -1, 0f, 60f, 1000f, 48f))
        // Un angolo muove due bordi.
        assertEquals(ImageFrame(100f, 420f, 330f), frame.dragged(1, 1, 20f, 30f, 1000f, 48f))
        // Non esce dalla pagina e non sparisce.
        assertEquals(0f, frame.dragged(-1, 0, -500f, 0f, 1000f, 48f).left)
        assertEquals(900f, frame.dragged(1, 0, 900f, 0f, 1000f, 48f).width)
        assertEquals(48f, frame.dragged(0, 1, 0f, -900f, 1000f, 48f).height)
    }

    @Test
    fun theFrameIsSavedOnlyWithWhatChanged() {
        val attrs = BlockAttrs(file = "a.jpg")
        // A tutta pagina e al centro: niente da scrivere.
        assertEquals(attrs, attrs.withFrame(ImageFrame(0f, 1000f, 500f), 1000f, full = false))
        // "Resize": larghezza, al centro, la forma non si tocca.
        assertEquals(attrs.copy(width = 0.5f), attrs.withFrame(ImageFrame(250f, 500f, 250f), 1000f, full = false))
        // "Full resize": anche dove comincia e la forma.
        assertEquals(attrs.copy(width = 0.4f, left = 0.1f, aspect = 0.5f), attrs.withFrame(ImageFrame(100f, 400f, 200f), 1000f, full = true))
        // "Original size" toglie tutto.
        assertEquals(attrs, attrs.copy(width = 0.4f, left = 0.1f, aspect = 0.5f).withOriginalSize())
        // Riletto dal blocco, il riquadro è lo stesso.
        assertEquals(ImageFrame(100f, 400f, 200f), frameOf(attrs.copy(width = 0.4f, left = 0.1f, aspect = 0.5f), 1000f, natural = 0.75f))
        assertEquals(ImageFrame(250f, 500f, 375f), frameOf(attrs.copy(width = 0.5f), 1000f, natural = 0.75f))
    }

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

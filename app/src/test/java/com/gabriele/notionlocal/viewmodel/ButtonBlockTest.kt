package com.gabriele.notionlocal.viewmodel

import android.app.Application
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.buttons.ButtonBlock
import com.gabriele.notionlocal.data.buttons.ButtonConfig
import com.gabriele.notionlocal.data.buttons.ButtonStep
import com.gabriele.notionlocal.data.buttons.InsertPlace
import com.gabriele.notionlocal.data.buttons.toNewBlock
import com.gabriele.notionlocal.data.entity.BlockAttrs
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.attrs
import com.gabriele.notionlocal.data.repository.PageRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * **Il blocco "Button" nell'editor** (27/10/2026): nasce dal menu "/" e apre
 * subito la finestra delle azioni, "Turn into" ne fa il nome col testo, le
 * azioni si salvano e si annullano, i blocchi di "Insert blocks" si annullano
 * in un passo, e le copie collegate rimandano al bottone vero.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ButtonBlockTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: PageRepository
    private lateinit var viewModel: PageEditorViewModel
    private val page = PageEntity(id = "qui", title = "Qui")
    private val altrove = PageEntity(id = "altrove", title = "Altrove")

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PageRepository(db)
        repository.createPage(page)
        repository.createPage(altrove)
        viewModel = PageEditorViewModel(repository)
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

    private fun stored(pageId: String = page.id) =
        runBlocking { db.blockDao().getBlocksForPageOnce(pageId).sortedBy { it.orderIndex } }

    private fun openWith(vararg texts: String): List<BlockEntity> {
        runBlocking {
            texts.forEachIndexed { i, t -> db.blockDao().insert(BlockEntity(pageId = page.id, textJson = """[{"text":"$t"}]""", orderIndex = i)) }
        }
        viewModel.load(page.id)
        waitFor("le righe") { viewModel.blocks.value.size == texts.size }
        return viewModel.blocks.value
    }

    @Test
    fun theSlashMenuMakesAnEmptyButtonAndAsksWhatToDo() {
        val riga = openWith("/button").single()
        viewModel.convertToButton(riga)
        waitFor("il bottone") { stored().single().type == BlockType.BUTTON }
        val block = stored().single()
        assertEquals(ButtonConfig(), block.attrs.button)
        assertEquals("[]", block.textJson)
        // La finestra delle azioni la apre il blocco, appena c'è.
        waitFor("la richiesta") { viewModel.buttonEditRequest.value == block.id }
        viewModel.consumeButtonEditRequest()
        assertNull(viewModel.buttonEditRequest.value)
    }

    @Test
    fun turnIntoButtonMakesTheTextItsName() {
        val riga = openWith("Bevi acqua").single()
        viewModel.convertToButton(riga, name = "Bevi acqua")
        waitFor("il bottone") { stored().single().type == BlockType.BUTTON }
        assertEquals("Bevi acqua", stored().single().attrs.button?.name)
        // Si annulla: torna la riga di testo.
        waitFor("il bottone mostrato") { viewModel.blocks.value.single().type == BlockType.BUTTON }
        viewModel.undo()
        waitFor("la riga di prima") { stored().single().type == BlockType.PARAGRAPH }
    }

    @Test
    fun theActionsAreSavedAndCanBeUndone() {
        val riga = openWith("/button").single()
        viewModel.convertToButton(riga)
        waitFor("il bottone mostrato") { viewModel.blocks.value.single().type == BlockType.BUTTON }
        val config = ButtonConfig(name = "Nota", steps = listOf(ButtonStep.InsertBlocks(blocks = listOf(ButtonBlock(text = "Ciao")))))
        viewModel.saveButton(riga.id, config)
        waitFor("le azioni") { stored().single().attrs.button == config }
        waitFor("le azioni mostrate") { viewModel.blocks.value.single().attrs.button == config }
        viewModel.undo()
        waitFor("il bottone vuoto") { stored().single().attrs.button == ButtonConfig() }
    }

    @Test
    fun insertBlocksInTheOpenPageIsOneUndoStep() {
        val righe = openWith("Prima", "/button")
        val button = righe[1]
        viewModel.convertToButton(button)
        waitFor("il bottone mostrato") { viewModel.blocks.value.any { it.type == BlockType.BUTTON } }
        val inserted = runBlocking {
            viewModel.insertButtonBlocks(
                page.id, button.id, InsertPlace.BELOW_BUTTON,
                listOf(ButtonBlock(text = "Uno").toNewBlock(), ButtonBlock(type = "CHECKBOX", text = "Due").toNewBlock())
            )
        }
        assertEquals(true, inserted)
        waitFor("i blocchi nuovi") { stored().size == 4 }
        assertEquals(listOf(BlockType.PARAGRAPH, BlockType.BUTTON, BlockType.PARAGRAPH, BlockType.CHECKBOX), stored().map { it.type })
        waitFor("i blocchi mostrati") { viewModel.blocks.value.size == 4 }
        viewModel.undo()
        waitFor("tolti in un passo") { stored().size == 2 }
        assertEquals(listOf(BlockType.PARAGRAPH, BlockType.BUTTON), stored().map { it.type })
    }

    @Test
    fun anotherPageIsLeftToTheEngine() {
        openWith("Qui")
        val answer = runBlocking { viewModel.insertButtonBlocks(altrove.id, null, InsertPlace.BOTTOM_OF_PAGE, listOf(ButtonBlock(text = "X").toNewBlock())) }
        assertNull(answer)
        assertTrue(stored(altrove.id).isEmpty())
    }

    @Test
    fun aLinkedCopyGoesAtTheBottomOfTheOtherPageAndPointsToTheRealButton() = runBlocking<Unit> {
        db.blockDao().insert(BlockEntity(pageId = altrove.id, textJson = """[{"text":"Già qui"}]""", orderIndex = 0))
        val riga = openWith("/button").single()
        viewModel.convertToButton(riga, name = "Vero")
        waitFor("il bottone mostrato") { viewModel.blocks.value.single().type == BlockType.BUTTON }
        val original = viewModel.blocks.value.single()
        var done = false
        viewModel.linkButtonIn(original, altrove.id) { done = true }
        waitFor("la copia") { done }
        val copy = stored(altrove.id).last()
        assertEquals(BlockType.BUTTON, copy.type)
        assertEquals(BlockAttrs(buttonLink = original.id), copy.attrs)
        // Una copia della copia rimanda comunque al bottone vero.
        val second = repository.addLinkedButton(copy.id, page.id)!!
        assertEquals(original.id, db.blockDao().getById(second)!!.attrs.buttonLink)
        // Cambiando le azioni da una copia (il bottone vero sta altrove) cambiano le sue.
        val config = ButtonConfig(name = "Cambiato", steps = listOf(ButtonStep.Confirm()))
        viewModel.saveButton(original.id, config)
        waitFor("le azioni") { runBlocking { db.blockDao().getById(original.id)!!.attrs.button } == config }
        assertEquals(config, viewModel.observeBlock(original.id).first()!!.attrs.button)
        // Tolto il bottone vero, la copia non trova più niente.
        db.blockDao().delete(db.blockDao().getById(original.id)!!)
        assertNull(viewModel.observeBlock(original.id).first())
    }

    @Test
    fun linkToButtonListsTheRealButtonsOfThePagesNotInTheTrash() = runBlocking<Unit> {
        val cestino = PageEntity(id = "cestino", title = "Buttata", trashedAt = 1L)
        repository.createPage(cestino)
        db.blockDao().insert(BlockEntity(id = "b1", pageId = page.id, type = BlockType.BUTTON, orderIndex = 0, attrsJson = BlockAttrs.encode(BlockAttrs(button = ButtonConfig(name = "Uno")))))
        db.blockDao().insert(BlockEntity(id = "b2", pageId = altrove.id, type = BlockType.BUTTON, orderIndex = 0, attrsJson = BlockAttrs.encode(BlockAttrs(button = ButtonConfig(name = "Due")))))
        db.blockDao().insert(BlockEntity(id = "copia", pageId = altrove.id, type = BlockType.BUTTON, orderIndex = 1, attrsJson = BlockAttrs.encode(BlockAttrs(buttonLink = "b1"))))
        db.blockDao().insert(BlockEntity(id = "b3", pageId = cestino.id, type = BlockType.BUTTON, orderIndex = 0, attrsJson = BlockAttrs.encode(BlockAttrs(button = ButtonConfig(name = "Tre")))))
        val choices = viewModel.buttonChoices()
        // Per nome della pagina: "Altrove" prima di "Qui"; né la copia né quello nel cestino.
        assertEquals(listOf("b2", "b1"), choices.map { it.blockId })
        assertEquals(listOf("Due", "Uno"), choices.map { it.config.name })
        assertEquals("Altrove", choices.first().page.title)
    }

    @Test
    fun linkToButtonTurnsTheLineIntoALinkedCopy() {
        runBlocking {
            db.blockDao().insert(BlockEntity(id = "vero", pageId = altrove.id, type = BlockType.BUTTON, orderIndex = 0, attrsJson = BlockAttrs.encode(BlockAttrs(button = ButtonConfig(name = "Vero")))))
        }
        val riga = openWith("/link to button").single()
        viewModel.convertToLinkedButton(riga, "vero")
        waitFor("la copia") { stored().single().type == BlockType.BUTTON }
        assertEquals(BlockAttrs(buttonLink = "vero"), stored().single().attrs)
        assertEquals("[]", stored().single().textJson)
    }

    @Test
    fun aDuplicatedButtonHasItsOwnActions() {
        val riga = openWith("/button").single()
        val config = ButtonConfig(name = "Uno", steps = listOf(ButtonStep.Confirm(message = "Sicuro?")))
        viewModel.convertToButton(riga, name = "Uno")
        waitFor("il bottone mostrato") { viewModel.blocks.value.single().type == BlockType.BUTTON }
        viewModel.saveButton(riga.id, config)
        waitFor("le azioni") { stored().single().attrs.button == config }
        viewModel.duplicateBlock(riga.id, com.gabriele.notionlocal.data.PageImageStore(ApplicationProvider.getApplicationContext()))
        waitFor("la copia") { stored().size == 2 }
        val (first, second) = stored()
        assertEquals(config, second.attrs.button)
        assertNull(second.attrs.buttonLink)
        assertTrue(first.id != second.id)
    }
}

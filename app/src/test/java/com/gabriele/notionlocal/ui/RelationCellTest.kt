package com.gabriele.notionlocal.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.DatabaseSnapshot
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.RelationConfig
import com.gabriele.notionlocal.data.entity.encodeRelationConfig
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.LocalRelated
import com.gabriele.notionlocal.ui.screen.RelationCell
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **La cella di una Relation su una riga sola** (21/10/2026): le canzoni che
 * ci stanno intere, poi "+N". Sul telefono l'ultima restava tagliata a metà,
 * una freccia e due puntini.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class RelationCellTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val songs = (1..6).map { DatabaseRowEntity(id = "s$it", pageId = "songs", title = "Canzone $it") }
    private val snapshot = DatabaseSnapshot(columns = emptyList(), rows = songs, cells = emptyMap(), page = PageEntity(id = "songs", title = "KPOP Songs", isDatabase = true))
    private val column = DatabaseColumnEntity(
        pageId = "groups", name = "Songs", type = ColumnType.RELATION,
        optionsJson = encodeRelationConfig(RelationConfig(database = "songs"))
    )

    @Before
    fun setUp() {
        AppSettings.init(ApplicationProvider.getApplicationContext())
    }

    private fun show(width: Int, value: String) {
        compose.setContent {
            NotionLocalTheme {
                CompositionLocalProvider(LocalRelated provides mapOf("songs" to snapshot)) {
                    Box(modifier = Modifier.width(width.dp)) {
                        RelationCell(column = column, value = value, onValueChange = {}, contentPadding = 0.dp)
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun whatDoesNotFitBecomesPlusN() {
        // Nei test i caratteri sono finti: una canzone, freccia compresa, misura 27 punti.
        show(100, songs.joinToString("|") { it.id })
        val shown = songs.count { compose.onAllNodesWithText(it.title).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(true, shown in 1..5)
        compose.onNodeWithText("+${songs.size - shown}").assertExists()
    }

    @Test
    fun whenEverythingFitsThereIsNoPlus() {
        show(1200, "s1|s2")
        compose.onNodeWithText("Canzone 1").assertExists()
        compose.onNodeWithText("Canzone 2").assertExists()
        assertEquals(0, compose.onAllNodesWithText("+", substring = true).fetchSemanticsNodes().size)
    }

    @Test
    fun aVeryLongFirstNameIsShortenedAndTheOthersCounted() {
        show(20, "s1|s2|s3")
        compose.onNodeWithText("Canzone 1").assertExists()
        compose.onNodeWithText("+2").assertExists()
    }
}

package com.gabriele.notionlocal.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.geometry.Offset
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.screen.SamsungTimeDialog
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import java.time.LocalTime
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **"Done" prende il numero che si vede in mezzo** (16/10/2026): "scelgo
 * l'ora precisa ma rimane sempre alle 9:00", "non mi fa mettere le 15:01".
 * Prima valeva il numero di quando la rotella si era fermata del tutto, e
 * "Done" premuto mentre si assestava teneva quello di prima.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h800dp-xhdpi")
class TimeDialogTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var picked: LocalTime? = null

    @Before
    fun setUp() {
        AppSettings.init(ApplicationProvider.getApplicationContext())
        compose.setContent {
            NotionLocalTheme {
                SamsungTimeDialog(initial = LocalTime.of(9, 0), onPick = { picked = it }, onDismiss = {})
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun doneWhileTheMinutesAreStillMovingKeepsWhatIsShown() {
        // Il tempo lo si manda avanti a mano: la rotella resta a metà corsa.
        compose.mainClock.autoAdvance = false
        try {
            compose.onNodeWithText("00").performTouchInput {
                swipe(start = Offset(centerX, bottom), end = Offset(centerX, top - 3 * height), durationMillis = 120)
            }
            compose.mainClock.advanceTimeBy(48)
            compose.onNodeWithText("Done").performClick()
            compose.mainClock.advanceTimeBy(16)
        } finally {
            // La rotella deve finire di girare qui: lasciata a metà, i test
            // dopo trovavano la finestra ancora in movimento e non partivano.
            compose.mainClock.autoAdvance = true
            compose.waitForIdle()
        }
        val time = picked
        assertNotNull("Done non ha scelto niente", time)
        assertNotEquals("I minuti sono rimasti quelli di partenza", 0, time!!.minute)
    }
}

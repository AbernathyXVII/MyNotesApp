package com.gabriele.notionlocal.data.notify

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.widgets.CountdownTimer
import com.gabriele.notionlocal.data.widgets.MINUTE_MS
import com.gabriele.notionlocal.data.widgets.TimerWidget
import com.gabriele.notionlocal.data.widgets.WidgetKind
import com.gabriele.notionlocal.data.widgets.WidgetStore
import com.gabriele.notionlocal.data.widgets.WidgetsState
import com.gabriele.notionlocal.data.widgets.backToSetup
import com.gabriele.notionlocal.data.widgets.finishedBy
import com.gabriele.notionlocal.data.widgets.notifiedTimers
import com.gabriele.notionlocal.data.widgets.pausedAt
import com.gabriele.notionlocal.data.widgets.resumedAt
import com.gabriele.notionlocal.data.widgets.startedAt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * **I timer che corrono, nella tendina** (30/09/2026): una notifica fissa
 * per timer partito, col conto alla rovescia di Android mentre corre, il
 * tempo fermo in pausa, i pulsanti, e via quando finisce o la si toglie.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class TimerNotificationsTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val manager get() = context.getSystemService(NotificationManager::class.java)

    private val now = 1_800_000_000_000L
    private val setup = CountdownTimer(id = "t1", durationMs = 10 * MINUTE_MS)
    private val running = setup.startedAt(now)

    private fun stateWith(vararg timers: CountdownTimer) = WidgetsState(widgets = listOf(TimerWidget(id = "w1", timers = timers.toList())))

    private fun shown(timerId: String = "t1"): Notification? = shadowOf(manager).getNotification(TimerNotifications.idOf(timerId))

    private fun actionTitles(n: Notification) = n.actions.orEmpty().map { it.title.toString() }

    @Before
    fun setUp() {
        AppSettings.init(context)
        AppSettings.notificationsMuted = false
        AppSettings.mutedUntil = null
        manager.cancelAll()
    }

    @Test
    fun onlyStartedTimersNotFinishedNorDismissedHaveOne() {
        val paused = running.pausedAt(now + MINUTE_MS).copy(id = "t2")
        val finished = running.finishedBy(now + 10 * MINUTE_MS).first.copy(id = "t3")
        val dismissed = running.copy(id = "t4", notificationDismissed = true)
        val state = stateWith(setup.copy(id = "t0"), running, paused, finished, dismissed)
        assertEquals(setOf("t1", "t2"), state.notifiedTimers().keys)
        assertEquals("w1", state.notifiedTimers().getValue("t1").first)
    }

    @Test
    fun startingPausingOrResumingBringsItBack() {
        val dismissed = running.copy(notificationDismissed = true)
        assertFalse(dismissed.pausedAt(now + 1_000).notificationDismissed)
        val pausedDismissed = dismissed.pausedAt(now + 1_000).copy(notificationDismissed = true)
        assertFalse(pausedDismissed.resumedAt(now + 2_000).notificationDismissed)
        assertFalse(dismissed.backToSetup().startedAt(now).notificationDismissed)
    }

    @Test
    fun aRunningTimerCountsDownInTheShade() {
        TimerNotifications.sync(context, stateWith(setup), stateWith(running))
        val n = checkNotNull(shown())
        assertEquals(TimerNotifications.CHANNEL, n.channelId)
        assertTrue(n.flags and Notification.FLAG_ONGOING_EVENT != 0)
        // Il conto alla rovescia lo fa Android, fino alla fine.
        assertTrue(n.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertTrue(n.extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN))
        assertEquals(running.endsAt, n.`when`)
        assertEquals("Timer · 10 m", n.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
        assertEquals(listOf("Pause", "Cancel"), actionTitles(n))
        // La richiesta di "live update" di Android 16.
        assertTrue(n.extras.getBoolean("android.requestPromotedOngoing"))
        assertNotNull(n.deleteIntent)
    }

    @Test
    fun aPausedTimerShowsTheTimeLeftStill() {
        val paused = running.pausedAt(now + 5 * MINUTE_MS + 28_000)
        TimerNotifications.sync(context, stateWith(running), stateWith(paused))
        val n = checkNotNull(shown())
        assertFalse(n.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertEquals("Paused · 4:32", n.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertEquals(listOf("Resume", "Cancel"), actionTitles(n))
    }

    @Test
    fun itGoesWhenTheTimerEndsOrIsCancelled() {
        TimerNotifications.sync(context, stateWith(setup), stateWith(running))
        assertNotNull(shown())
        val finished = running.finishedBy(running.endsAt!!).first
        TimerNotifications.sync(context, stateWith(running), stateWith(finished))
        assertNull(shown())

        TimerNotifications.sync(context, stateWith(setup), stateWith(running))
        TimerNotifications.sync(context, stateWith(running), stateWith(running.backToSetup()))
        assertNull(shown())
    }

    @Test
    fun unchangedTimersAreNotPostedAgain() {
        TimerNotifications.sync(context, stateWith(setup), stateWith(running))
        manager.cancelAll()
        // Cambia un altro widget, non il timer: la notifica tolta resta tolta.
        TimerNotifications.sync(context, stateWith(running), stateWith(running).copy(hidden = true))
        assertNull(shown())
    }

    @Test
    fun restorePutsBackOnlyTheMissingOnes() {
        TimerNotifications.restore(context, stateWith(running, setup.copy(id = "t2")))
        assertNotNull(shown())
        assertNull(shown("t2"))
        assertEquals(1, shadowOf(manager).allNotifications.size)
    }

    @Test
    fun turnedOffNotificationsShowNothing() {
        AppSettings.notificationsMuted = true
        TimerNotifications.sync(context, stateWith(setup), stateWith(running))
        assertNull(shown())
    }

    @Test
    fun theButtonsPauseResumeAndCancelTheTimer() {
        WidgetStore.init(context)
        WidgetStore.add(WidgetKind.TIMER)
        val widget = WidgetStore.state.widgets.filterIsInstance<TimerWidget>().last()
        val timerId = widget.timers.single().id
        WidgetStore.timerStart(widget.id, timerId)
        assertEquals(listOf("Pause", "Cancel"), actionTitles(checkNotNull(shown(timerId))))

        // Quello che fa `TimerActionReceiver` (senza la sveglia di Android,
        // che nei test aprirebbe il database in sottofondo).
        fun press(action: String) = assertTrue(TimerNotifications.handle(action, widget.id, timerId))
        fun timer() = WidgetStore.state.widgets.filterIsInstance<TimerWidget>().single { it.id == widget.id }.timers.single()

        press(TimerNotifications.ACTION_PAUSE)
        assertFalse(timer().running)
        assertEquals(listOf("Resume", "Cancel"), actionTitles(checkNotNull(shown(timerId))))

        press(TimerNotifications.ACTION_RESUME)
        assertTrue(timer().running)
        assertEquals(listOf("Pause", "Cancel"), actionTitles(checkNotNull(shown(timerId))))

        // Tolta con uno scorrimento: il timer corre ancora, la notifica non torna.
        press(TimerNotifications.ACTION_DISMISSED)
        assertTrue(timer().running)
        assertTrue(timer().notificationDismissed)
        WidgetStore.restoreTimerNotifications()
        assertNull(shown(timerId))

        // In pausa dal widget: torna.
        WidgetStore.timerPause(widget.id, timerId)
        assertNotNull(shown(timerId))

        press(TimerNotifications.ACTION_CANCEL)
        assertFalse(timer().started)
        assertNull(shown(timerId))
        // Un'azione che non è delle nostre non fa niente.
        assertFalse(TimerNotifications.handle("something.else", widget.id, timerId))
        WidgetStore.remove(widget.id)
    }
}

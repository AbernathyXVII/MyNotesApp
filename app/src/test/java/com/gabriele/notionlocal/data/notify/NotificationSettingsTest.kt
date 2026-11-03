package com.gabriele.notionlocal.data.notify

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.settings.NotificationPriority
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * **La pagina Notifications** (30/09/2026): vibrazione, priorità e pallino
 * sull'icona si salvano, e ogni combinazione ha il suo canale — Android non
 * lascia cambiarle a un canale che c'è già —, con quelli vecchi tolti.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class NotificationSettingsTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val manager get() = context.getSystemService(NotificationManager::class.java)

    @Before
    fun setUp() {
        AppSettings.init(context)
    }

    @After
    fun tearDown() {
        AppSettings.notificationVibrate = true
        AppSettings.notificationPriority = NotificationPriority.HIGH
        AppSettings.notificationBadge = true
    }

    private fun reminderChannels(): List<NotificationChannel> =
        manager.notificationChannels.filter { it.id.startsWith("reminders") }

    @Test
    fun theDefaultsAreHowItWasBefore() {
        assertTrue(AppSettings.notificationVibrate)
        assertEquals(NotificationPriority.HIGH, AppSettings.notificationPriority)
        assertTrue(AppSettings.notificationBadge)
    }

    @Test
    fun theChoicesAreSaved() {
        AppSettings.notificationVibrate = false
        AppSettings.notificationPriority = NotificationPriority.LOW
        AppSettings.notificationBadge = false
        // Riletti come all'avvio dell'app.
        AppSettings.init(context)
        assertFalse(AppSettings.notificationVibrate)
        assertEquals(NotificationPriority.LOW, AppSettings.notificationPriority)
        assertFalse(AppSettings.notificationBadge)
    }

    @Test
    fun everyCombinationHasItsChannel() {
        val ids = NotificationPriority.entries.flatMap { priority ->
            listOf(true, false).flatMap { vibrate ->
                listOf(true, false).map { badge -> Notifier.remindersChannel(priority, vibrate, badge) }
            }
        }
        assertEquals(12, ids.toSet().size)
        assertEquals("reminders.high.vibrate.badge", Notifier.remindersChannel(NotificationPriority.HIGH, true, true))
    }

    @Test
    fun theChannelFollowsTheChoicesAndTheOldOnesGo() {
        // Il canale di prima delle scelte.
        manager.createNotificationChannel(NotificationChannel("reminders", "Reminders", NotificationManager.IMPORTANCE_HIGH))

        Notifier.ensureChannels(context)
        val high = reminderChannels().single()
        assertEquals("reminders.high.vibrate.badge", high.id)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, high.importance)
        assertTrue(high.shouldVibrate())
        assertTrue(high.canShowBadge())
        assertEquals(null, high.sound)

        AppSettings.notificationPriority = NotificationPriority.NORMAL
        AppSettings.notificationVibrate = false
        AppSettings.notificationBadge = false
        Notifier.ensureChannels(context)
        val normal = reminderChannels().single()
        assertEquals("reminders.normal.still.nobadge", normal.id)
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, normal.importance)
        assertFalse(normal.shouldVibrate())
        assertFalse(normal.canShowBadge())

        AppSettings.notificationPriority = NotificationPriority.LOW
        Notifier.ensureChannels(context)
        assertEquals(NotificationManager.IMPORTANCE_LOW, reminderChannels().single().importance)
        // Timer e pomodoro restano com'erano.
        assertEquals(NotificationManager.IMPORTANCE_HIGH, manager.getNotificationChannel(Notifier.CHANNEL_TIMERS).importance)
    }

    @Test
    fun theTestNotificationUsesTheChosenChannel() {
        // Bassa: niente suono da far partire nel test.
        AppSettings.notificationPriority = NotificationPriority.LOW
        assertTrue(Notifier.postTest(context))
        val posted = shadowOf(manager).allNotifications.single()
        assertEquals(Notifier.currentRemindersChannel, posted.channelId)
    }
}

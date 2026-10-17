package com.gabriele.notionlocal.data

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * **Gli indirizzi si aprono in una Custom Tab, nella pila di notE**
 * (17/10/2026): "indietro" dal browser torna all'app, non a quella usata
 * prima. Vale per link sul testo, segnalibri, video e celle URL.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class OpenUrlTest {

    private val session = "android.support.customtabs.extra.SESSION"

    @Test
    fun aWebPageFromTheAppIsACustomTabInItsOwnStack() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        assertTrue(BlockFiles.openUrl(activity, "https://example.com"))
        val started = shadowOf(activity).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, started.action)
        assertEquals("https://example.com", started.dataString)
        assertTrue(started.extras!!.containsKey(session))
        assertEquals(0, started.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    @Test
    fun anEmailIsNotACustomTab() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        assertTrue(BlockFiles.openUrl(activity, "mailto:mario@example.com"))
        val started = shadowOf(activity).nextStartedActivity
        assertEquals("mailto:mario@example.com", started.dataString)
        assertFalse(started.extras?.containsKey(session) == true)
    }

    @Test
    fun withoutAnActivityItGetsItsOwnTask() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        assertTrue(BlockFiles.openUrl(app, "https://example.com"))
        val started = shadowOf(app).nextStartedActivity
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, started.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

package com.gabriele.notionlocal.ui

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.entity.TimeFormatKind
import com.gabriele.notionlocal.data.settings.AppLanguage
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.settings.DateStyle
import com.gabriele.notionlocal.ui.format.Formats
import androidx.compose.runtime.snapshots.Snapshot
import java.time.LocalDateTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **"Date format" e "Time format" sono in Settings, uguali per tutta l'app**
 * (16/10/2026): prima si sceglievano per ogni colonna Date. "Hidden" toglie
 * l'ora solo dalle date dei database.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class GlobalTimeFormatTest {

    private val at = LocalDateTime.of(2026, 10, 20, 15, 7)
    private lateinit var before: Triple<DateStyle, TimeFormatKind, AppLanguage>

    /**
     * Le impostazioni sono stato di Compose: cambiate fuori da un'istantanea
     * restavano "in sospeso", e i test dell'interfaccia lanciati dopo
     * aspettavano per sempre che l'app si fermasse.
     */
    private fun settings(change: () -> Unit) {
        Snapshot.withMutableSnapshot(change)
        Snapshot.sendApplyNotifications()
    }

    @Before
    fun setUp() {
        AppSettings.init(ApplicationProvider.getApplicationContext())
        before = Triple(AppSettings.dateStyle, AppSettings.timeFormat, AppSettings.language)
        settings {
            AppSettings.dateStyle = DateStyle.DAY_MONTH_YEAR_SLASH
            AppSettings.language = AppLanguage.ENGLISH
        }
    }

    @After
    fun tearDown() {
        settings {
            AppSettings.dateStyle = before.first
            AppSettings.timeFormat = before.second
            AppSettings.language = before.third
        }
    }

    @Test
    fun twentyFourHours() {
        settings { AppSettings.timeFormat = TimeFormatKind.H24 }
        assertEquals("20/10/2026 15:07", Formats.databaseDateTimeFormatter().format(at))
        assertEquals("15:07", Formats.timeFormatter().format(at))
    }

    @Test
    fun twelveHours() {
        settings { AppSettings.timeFormat = TimeFormatKind.H12 }
        assertEquals("20/10/2026 3:07 PM", Formats.databaseDateTimeFormatter().format(at))
        assertEquals("3 PM", Formats.hourLabelFormatter().format(at))
    }

    @Test
    fun hiddenOnlyInDatabaseDates() {
        settings { AppSettings.timeFormat = TimeFormatKind.HIDDEN }
        assertEquals("20/10/2026", Formats.databaseDateTimeFormatter().format(at))
        // Dove l'ora è quello che si legge (cronologia, "Created"…) resta.
        assertEquals("15:07", Formats.timeFormatter().format(at))
    }
}

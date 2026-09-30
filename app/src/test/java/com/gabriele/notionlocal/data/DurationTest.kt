package com.gabriele.notionlocal.data

import com.gabriele.notionlocal.data.entity.DurationParts
import com.gabriele.notionlocal.data.entity.DurationUnit
import com.gabriele.notionlocal.data.entity.durationBetween
import com.gabriele.notionlocal.data.entity.durationFormatOf
import com.gabriele.notionlocal.data.entity.durationMillisOfCell
import com.gabriele.notionlocal.data.entity.durationOfCell
import com.gabriele.notionlocal.data.entity.durationUnits
import com.gabriele.notionlocal.data.entity.encodeDateRange
import com.gabriele.notionlocal.data.settings.AppLanguage
import com.gabriele.notionlocal.ui.i18n.durationWords
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * **"Duration"** (29/09/2026): quanto passa fra un inizio e una fine, in
 * anni, mesi, giorni, ore e minuti, fino all'unità scelta.
 */
class DurationTest {

    /** L'esempio dell'utente: dal 12 febbraio 1996 00:00 al 29 settembre 2026 21:22. */
    private val example = durationBetween(LocalDateTime.of(1996, 2, 12, 0, 0), LocalDateTime.of(2026, 9, 29, 21, 22))!!

    private fun words(parts: DurationParts, format: DurationUnit, language: AppLanguage = AppLanguage.ENGLISH) =
        durationWords(durationUnits(parts, format), language)

    @Test
    fun theUsersExampleOnTheCalendar() {
        // Al 12/02/2026 sono 30 anni, al 12/09/2026 altri 7 mesi, poi 17 giorni, 21 ore e 22 minuti.
        assertEquals(DurationParts(30, 7, 17, 21, 22), example)
    }

    @Test
    fun theFiveFormatsInEnglish() {
        assertEquals("30 years", words(example, DurationUnit.YEARS))
        assertEquals("30 years and 7 months", words(example, DurationUnit.MONTHS))
        assertEquals("30 years, 7 months and 17 days", words(example, DurationUnit.DAYS))
        assertEquals("30 years, 7 months, 17 days and 21 hours", words(example, DurationUnit.HOURS))
        assertEquals("30 years, 7 months, 17 days, 21 hours and 22 minutes", words(example, DurationUnit.MINUTES))
    }

    @Test
    fun theFiveFormatsInItalian() {
        val it = AppLanguage.ITALIAN
        assertEquals("30 anni", words(example, DurationUnit.YEARS, it))
        assertEquals("30 anni e 7 mesi", words(example, DurationUnit.MONTHS, it))
        assertEquals("30 anni, 7 mesi e 17 giorni", words(example, DurationUnit.DAYS, it))
        assertEquals("30 anni, 7 mesi, 17 giorni e 21 ore", words(example, DurationUnit.HOURS, it))
        assertEquals("30 anni, 7 mesi, 17 giorni, 21 ore e 22 minuti", words(example, DurationUnit.MINUTES, it))
    }

    @Test
    fun theOtherLanguages() {
        assertEquals("30 Jahre, 7 Monate, 17 Tage, 21 Stunden und 22 Minuten", words(example, DurationUnit.MINUTES, AppLanguage.GERMAN))
        assertEquals("30 ans, 7 mois, 17 jours, 21 heures et 22 minutes", words(example, DurationUnit.MINUTES, AppLanguage.FRENCH))
        assertEquals("30 años, 7 meses, 17 días, 21 horas y 22 minutos", words(example, DurationUnit.MINUTES, AppLanguage.SPANISH))
        assertEquals("30年7个月17天21小时22分钟", words(example, DurationUnit.MINUTES, AppLanguage.CHINESE))
        assertEquals("30년 7개월 17일 21시간 22분", words(example, DurationUnit.MINUTES, AppLanguage.KOREAN))
        assertEquals("30年7か月17日21時間22分", words(example, DurationUnit.MINUTES, AppLanguage.JAPANESE))
    }

    @Test
    fun oneIsSingular() {
        val one = DurationParts(1, 1, 1, 1, 1)
        assertEquals("1 year, 1 month, 1 day, 1 hour and 1 minute", words(one, DurationUnit.MINUTES))
        assertEquals("1 anno, 1 mese, 1 giorno, 1 ora e 1 minuto", words(one, DurationUnit.MINUTES, AppLanguage.ITALIAN))
    }

    @Test
    fun zeroUnitsAreLeftOut() {
        val parts = durationBetween(LocalDateTime.of(2020, 1, 1, 0, 0), LocalDateTime.of(2021, 1, 18, 0, 0))!!
        assertEquals("1 year and 17 days", words(parts, DurationUnit.MINUTES))
        // Tutto a zero: lo zero dell'ultima unità scelta.
        assertEquals("0 years", words(DurationParts(0, 5, 0, 0, 0), DurationUnit.YEARS))
        val none = durationBetween(LocalDateTime.of(2026, 9, 29, 9, 0), LocalDateTime.of(2026, 9, 29, 9, 0))!!
        assertEquals("0 minutes", words(none, DurationUnit.MINUTES))
    }

    @Test
    fun theEndBeforeTheStartIsNotADuration() {
        assertNull(durationBetween(LocalDateTime.of(2026, 9, 29, 21, 22), LocalDateTime.of(2026, 9, 29, 21, 21)))
    }

    @Test
    fun monthEnds() {
        // Dal 31 gennaio al 29 febbraio non è ancora un mese.
        assertEquals(
            DurationParts(0, 0, 29, 0, 0),
            durationBetween(LocalDateTime.of(2000, 1, 31, 0, 0), LocalDateTime.of(2000, 2, 29, 0, 0))
        )
        // Dal 29 febbraio 2000 al 1° marzo 2001: un anno e un giorno.
        assertEquals(
            DurationParts(1, 0, 1, 0, 0),
            durationBetween(LocalDateTime.of(2000, 2, 29, 0, 0), LocalDateTime.of(2001, 3, 1, 0, 0))
        )
    }

    @Test
    fun theDayOfTheClockChangeIsStillADay() {
        // Il 29 marzo 2026 in Italia ha 23 ore: da mezzanotte a mezzanotte è comunque un giorno.
        val rome = ZoneId.of("Europe/Rome")
        val start = LocalDateTime.of(2026, 3, 29, 0, 0).atZone(rome).toInstant().toEpochMilli()
        val end = LocalDateTime.of(2026, 3, 30, 0, 0).atZone(rome).toInstant().toEpochMilli()
        val value = encodeDateRange(start, end, hasTime = true)
        assertEquals(DurationParts(0, 0, 1, 0, 0), durationOfCell(value, rome))
        assertEquals(23 * 3_600_000L, durationMillisOfCell(value))
    }

    @Test
    fun aCellNeedsBothEnds() {
        assertNull(durationOfCell("", ZoneId.of("UTC")))
        assertNull(durationOfCell(encodeDateRange(0L, null, hasTime = true), ZoneId.of("UTC")))
        assertNull(durationMillisOfCell(encodeDateRange(0L, null, hasTime = true)))
        // Una data col periodo, cambiata in Duration, dura quanto il periodo.
        assertEquals(DurationParts(0, 0, 2, 0, 0), durationOfCell(encodeDateRange(0L, 2 * 86_400_000L), ZoneId.of("UTC")))
    }

    @Test
    fun theFormatByName() {
        assertEquals(DurationUnit.MINUTES, durationFormatOf(null))
        assertEquals(DurationUnit.MINUTES, durationFormatOf("SECONDS"))
        assertEquals(DurationUnit.YEARS, durationFormatOf("YEARS"))
    }
}

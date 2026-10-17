package com.gabriele.notionlocal.data.widgets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * **Il meteo** (17/10/2026): le risposte vere di Open-Meteo (scaricate per
 * Bergamo e salvate in `resources/weather`) lette come si deve.
 */
class WeatherTest {

    private fun resource(name: String): String =
        javaClass.classLoader!!.getResourceAsStream("weather/$name")!!.bufferedReader().readText()

    /** Le 13:45 del 17/10/2026 a Bergamo (UTC+2), quando sono state scaricate. */
    private val fetchedAt = LocalDateTime.of(2026, 10, 17, 13, 45).toInstant(ZoneOffset.ofHours(2)).toEpochMilli()

    @Test
    fun theForecastOfBergamo() {
        val report = parseForecast(resource("forecast_bergamo.json"), fetchedAt)!!
        assertEquals(7200, report.utcOffsetSeconds)
        assertEquals(19.6, report.now.temperature, 0.01)
        assertEquals(2, report.now.code)
        assertTrue(report.now.isDay)
        assertEquals(64, report.now.humidity)
        assertEquals(48, report.hours.size)
        assertEquals("2026-10-17T13:00", report.hours.first().time)
        assertEquals(7, report.days.size)
        assertEquals("2026-10-17", report.days.first().date)
        assertTrue(report.days.all { it.max >= it.min })
    }

    @Test
    fun hoursAndDaysFromNow() {
        val report = parseForecast(resource("forecast_bergamo.json"), fetchedAt)!!
        // Dall'ora in corso (le 13 della città), anche a metà ora.
        assertEquals("2026-10-17T13:00", report.hoursFrom(fetchedAt).first().time)
        // Tre ore dopo, le prime tre non ci sono più.
        val later = fetchedAt + 3 * 60 * MINUTE_MS
        assertEquals("2026-10-17T16:00", report.hoursFrom(later).first().time)
        // Il giorno dopo, oggi è domani.
        val tomorrow = fetchedAt + 24 * 60 * MINUTE_MS
        assertEquals("2026-10-18", report.daysFrom(tomorrow).first().date)
    }

    @Test
    fun searchingACity() {
        val found = parseCitySearch(resource("search_bergamo.json"))
        assertEquals(1, found.size)
        assertEquals("Bergamo", found.single().name)
        assertEquals("Lombardia, Italia", found.single().region)
        assertEquals(45.696, found.single().latitude, 0.001)
        // Nessuna città: Open-Meteo non mette nemmeno "results".
        assertEquals(emptyList<CityResult>(), parseCitySearch("""{"generationtime_ms":0.2}"""))
        assertEquals(emptyList<CityResult>(), parseCitySearch("non è json"))
        assertNull(parseForecast("non è json", 0))
    }

    @Test
    fun milanoAlsoWithTheAppInEnglish() {
        // In inglese "Milano" trova solo le Milano piccole: Milano è "Milan".
        val english = parseCitySearch(resource("search_milano_en.json"))
        assertTrue(english.none { it.region.startsWith("Lombard") })
        // Insieme alla ricerca in italiano, Milano (Lombardia) viene per prima, e una volta sola.
        val merged = mergeCityResults(listOf(english, parseCitySearch(resource("search_milano_it.json"))))
        assertEquals("Milano", merged.first().name)
        assertEquals("Lombardia, Italia", merged.first().region)
        assertEquals(merged.size, merged.distinctBy { Math.round(it.latitude * 100) to Math.round(it.longitude * 100) }.size)
        // Le stesse città di tutte e due le lingue col nome della prima (l'inglese).
        assertTrue(merged.any { it.region == "Texas, United States" })
        assertFalse(merged.any { it.region == "Texas, Stati Uniti" })
    }

    @Test
    fun skiesTemperaturesAndWhenToRefresh() {
        assertEquals(WeatherSky.CLEAR, WeatherSky.of(0))
        assertEquals("🌙", WeatherSky.of(0).icon(isDay = false))
        assertEquals(WeatherSky.RAIN_SHOWERS, WeatherSky.of(81))
        assertEquals(WeatherSky.THUNDERSTORM_HAIL, WeatherSky.of(99))
        assertEquals("20°", temperatureText(19.6))
        assertEquals("0°", temperatureText(-0.4))
        assertEquals("-3°", temperatureText(-2.6))
        val report = parseForecast(resource("forecast_bergamo.json"), fetchedAt)!!
        assertFalse(report.isStale(fetchedAt + 29 * MINUTE_MS))
        assertTrue(report.isStale(fetchedAt + 30 * MINUTE_MS))
        assertTrue((null as WeatherReport?).isStale(fetchedAt))
    }
}

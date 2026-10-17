package com.gabriele.notionlocal.data.widgets

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/**
 * **Le domande a Open-Meteo**, per il widget del meteo: le previsioni di una
 * città e la ricerca delle città per nome. Gratuito, senza account né chiave.
 * Senza rete le risposte sono null, e il widget mostra quello che aveva.
 */
object WeatherApi {

    private const val TIMEOUT_MS = 10_000
    private const val MAX_BYTES = 256 * 1024

    /** Adesso, le prossime 48 ore e i prossimi sette giorni, con le ore della città. */
    suspend fun forecast(city: WeatherCity): WeatherReport? = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=${coordinate(city.latitude)}&longitude=${coordinate(city.longitude)}" +
            "&current=temperature_2m,apparent_temperature,relative_humidity_2m,is_day,weather_code,wind_speed_10m" +
            "&hourly=temperature_2m,weather_code,precipitation_probability,is_day" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
            "&timezone=auto&forecast_days=7&forecast_hours=48"
        get(url)?.let { parseForecast(it, System.currentTimeMillis()) }
    }

    /**
     * Le città che si chiamano così, coi nomi nella lingua dell'app ("Monaco
     * di Baviera"). Null senza rete; vuota se non ce n'è nessuna.
     */
    suspend fun searchCities(query: String, language: String): List<CityResult>? = withContext(Dispatchers.IO) {
        val text = query.trim()
        if (text.length < 2) return@withContext emptyList()
        val url = "https://geocoding-api.open-meteo.com/v1/search" +
            "?name=${URLEncoder.encode(text, "UTF-8")}&count=10&language=$language&format=json"
        get(url)?.let { parseCitySearch(it) }
    }

    private fun coordinate(value: Double): String = String.format(Locale.ROOT, "%.4f", value)

    private fun get(url: String): String? = runCatching {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/json")
            if (connection.responseCode !in 200..299) return@runCatching null
            val out = java.io.ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(16 * 1024)
                while (out.size() < MAX_BYTES) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    out.write(buffer, 0, read)
                }
            }
            String(out.toByteArray(), Charsets.UTF_8)
        } finally {
            connection.disconnect()
        }
    }.getOrNull()
}

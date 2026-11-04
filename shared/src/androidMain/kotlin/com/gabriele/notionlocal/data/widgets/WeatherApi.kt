package com.gabriele.notionlocal.data.widgets

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.util.Locale

/**
 * **Le domande a Open-Meteo**, per il widget del meteo: le previsioni di una
 * città e la ricerca delle città per nome. Gratuito, senza account né chiave.
 * Senza rete le risposte sono null, e il widget mostra quello che aveva.
 */
object WeatherApi {

    /** Adesso, le prossime 48 ore e i prossimi sette giorni, con le ore della città. */
    suspend fun forecast(city: WeatherCity): WeatherReport? = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=${coordinate(city.latitude)}&longitude=${coordinate(city.longitude)}" +
            "&current=temperature_2m,apparent_temperature,relative_humidity_2m,is_day,weather_code,wind_speed_10m" +
            "&hourly=temperature_2m,weather_code,precipitation_probability,is_day" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
            "&timezone=auto&forecast_days=7&forecast_hours=48"
        WidgetHttp.get(url, maxBytes = 256 * 1024)?.let { parseForecast(it, System.currentTimeMillis()) }
    }

    /**
     * Le città che si chiamano così in una di [languages] — la lingua
     * dell'app e quella del telefono —, coi nomi della prima ("Monaco di
     * Baviera"), le più grandi per prime (vedi `mergeCityResults`). Le
     * lingue si chiedono insieme. Null senza rete; vuota se non ce n'è
     * nessuna.
     */
    suspend fun searchCities(query: String, languages: List<String>): List<CityResult>? = withContext(Dispatchers.IO) {
        val text = query.trim()
        if (text.length < 2) return@withContext emptyList()
        val answers = languages.distinct().map { language ->
            async {
                val url = "https://geocoding-api.open-meteo.com/v1/search" +
                    "?name=${URLEncoder.encode(text, "UTF-8")}&count=10&language=$language&format=json"
                WidgetHttp.get(url, maxBytes = 256 * 1024)?.let { parseCitySearch(it) }
            }
        }.awaitAll()
        if (answers.all { it == null }) null else mergeCityResults(answers.filterNotNull()).take(15)
    }

    private fun coordinate(value: Double): String = String.format(Locale.ROOT, "%.4f", value)
}

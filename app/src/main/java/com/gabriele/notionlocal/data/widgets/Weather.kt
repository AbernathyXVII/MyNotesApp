package com.gabriele.notionlocal.data.widgets

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/**
 * **Il meteo** (dal 17/10/2026, chiesto dall'utente): una città principale e
 * fino ad altre nove, una per pagina, che si sfogliano. Un tocco mostra il
 * tempo ora per ora; tenendo premuto si aggiungono, cambiano e tolgono le
 * città.
 *
 * I dati vengono da **Open-Meteo** (open-meteo.com): previsioni gratuite,
 * senza account né chiave, e la ricerca delle città per nome. Servono la
 * rete; l'ultimo meteo scaricato resta salvato (`reports`) e si mostra anche
 * senza, con l'ora in cui è arrivato.
 */
@Serializable
@SerialName("weather")
data class WeatherWidget(
    override val id: String = newWidgetId(),
    /** Le città, la principale per prima. Al massimo `MAX_WEATHER_CITIES`. */
    val cities: List<WeatherCity> = emptyList(),
    /** L'ultimo meteo scaricato di ogni città, per id della città. */
    val reports: Map<String, WeatherReport> = emptyMap()
) : Widget()

/** La città principale e altre nove. */
const val MAX_WEATHER_CITIES = 10

/** Ogni quanto si riscarica il meteo di una città, con la barra aperta. */
const val WEATHER_REFRESH_MS = 30 * MINUTE_MS

@Serializable
data class WeatherCity(
    val id: String = newWidgetId(),
    val name: String,
    /** "Lombardia, Italia": dove sta, per distinguere le città con lo stesso nome. */
    val region: String = "",
    val latitude: Double,
    val longitude: Double
)

/** Il meteo di una città com'era quando è stato scaricato. */
@Serializable
data class WeatherReport(
    val fetchedAt: Long,
    /** Quanto la città è avanti rispetto a Greenwich, in secondi: le ore qui sotto sono le sue. */
    val utcOffsetSeconds: Int,
    val now: WeatherNow,
    /** Dalla sua ora di adesso, per 48 ore. */
    val hours: List<WeatherHour>,
    /** Da oggi, per sette giorni. */
    val days: List<WeatherDay>
)

@Serializable
data class WeatherNow(
    val temperature: Double,
    /** Il codice WMO del cielo: vedi `WeatherSky`. */
    val code: Int,
    val isDay: Boolean,
    val feelsLike: Double? = null,
    val humidity: Int? = null,
    /** km/h. */
    val wind: Double? = null
)

@Serializable
data class WeatherHour(
    /** "2026-10-17T14:00", l'ora della città. */
    val time: String,
    val temperature: Double,
    val code: Int,
    val isDay: Boolean = true,
    /** Probabilità di pioggia, in percentuale. */
    val rainChance: Int? = null
)

@Serializable
data class WeatherDay(
    /** "2026-10-17". */
    val date: String,
    val code: Int,
    val max: Double,
    val min: Double,
    val rainChance: Int? = null
)

/** Una città trovata cercandola per nome; `population` serve a mettere prima le città grandi. */
data class CityResult(
    val name: String,
    val region: String,
    val latitude: Double,
    val longitude: Double,
    val population: Long? = null
) {
    fun toCity(): WeatherCity = WeatherCity(name = name, region = region, latitude = latitude, longitude = longitude)
}

/**
 * **Il cielo** secondo il codice WMO che danno le previsioni: un'icona (una
 * emoji, colorata come nelle app del meteo) e il nome, tradotto altrove
 * (`WidgetStrings.skyName`).
 */
enum class WeatherSky(val dayIcon: String, val nightIcon: String = dayIcon) {
    CLEAR("☀️", "🌙"),
    MAINLY_CLEAR("🌤️", "🌙"),
    PARTLY_CLOUDY("⛅", "☁️"),
    OVERCAST("☁️"),
    FOG("🌫️"),
    DRIZZLE("🌦️", "🌧️"),
    FREEZING_DRIZZLE("🌧️"),
    RAIN("🌧️"),
    FREEZING_RAIN("🌧️"),
    SNOW("🌨️"),
    SNOW_GRAINS("🌨️"),
    RAIN_SHOWERS("🌦️", "🌧️"),
    SNOW_SHOWERS("🌨️"),
    THUNDERSTORM("⛈️"),
    THUNDERSTORM_HAIL("⛈️");

    fun icon(isDay: Boolean): String = if (isDay) dayIcon else nightIcon

    companion object {
        fun of(code: Int): WeatherSky = when (code) {
            0 -> CLEAR
            1 -> MAINLY_CLEAR
            2 -> PARTLY_CLOUDY
            3 -> OVERCAST
            45, 48 -> FOG
            51, 53, 55 -> DRIZZLE
            56, 57 -> FREEZING_DRIZZLE
            61, 63, 65 -> RAIN
            66, 67 -> FREEZING_RAIN
            71, 73, 75 -> SNOW
            77 -> SNOW_GRAINS
            80, 81, 82 -> RAIN_SHOWERS
            85, 86 -> SNOW_SHOWERS
            95 -> THUNDERSTORM
            96, 99 -> THUNDERSTORM_HAIL
            else -> OVERCAST
        }
    }
}

/** Il meteo di questa città va riscaricato: non c'è, o ha più di mezz'ora. */
fun WeatherReport?.isStale(now: Long): Boolean = this == null || now - fetchedAt >= WEATHER_REFRESH_MS

/** Una temperatura da leggere: "19°", "-3°" (arrotondata, senza "-0"). */
fun temperatureText(celsius: Double): String = "${Math.round(celsius)}°"

// --- Le risposte di Open-Meteo, lette ---

private val lenientJson = Json { ignoreUnknownKeys = true }

/** Le ore da adesso in poi: le previsioni partono dall'ora in corso, ma un meteo vecchio no. */
fun WeatherReport.hoursFrom(nowMillis: Long): List<WeatherHour> {
    val local = java.time.Instant.ofEpochMilli(nowMillis).atOffset(java.time.ZoneOffset.ofTotalSeconds(utcOffsetSeconds))
        .toLocalDateTime().withMinute(0).withSecond(0).withNano(0)
    return hours.filter { runCatching { !java.time.LocalDateTime.parse(it.time).isBefore(local) }.getOrDefault(true) }
}

/** I giorni da oggi (della città) in poi. */
fun WeatherReport.daysFrom(nowMillis: Long): List<WeatherDay> {
    val today = java.time.Instant.ofEpochMilli(nowMillis).atOffset(java.time.ZoneOffset.ofTotalSeconds(utcOffsetSeconds)).toLocalDate()
    return days.filter { runCatching { !java.time.LocalDate.parse(it.date).isBefore(today) }.getOrDefault(true) }
}

/** La risposta delle previsioni; null se non è quella che ci si aspetta. */
fun parseForecast(json: String, fetchedAt: Long): WeatherReport? = runCatching {
    val root = lenientJson.parseToJsonElement(json).jsonObject
    val current = root.obj("current") ?: return null
    val now = WeatherNow(
        temperature = current.double("temperature_2m") ?: return null,
        code = current.int("weather_code") ?: 0,
        isDay = current.int("is_day") != 0,
        feelsLike = current.double("apparent_temperature"),
        humidity = current.int("relative_humidity_2m"),
        wind = current.double("wind_speed_10m")
    )
    val hourly = root.obj("hourly")
    val hours = hourly?.let { h ->
        val times = h.array("time")
        times.indices.mapNotNull { i ->
            WeatherHour(
                time = times[i].string() ?: return@mapNotNull null,
                temperature = h.array("temperature_2m").getOrNull(i).double() ?: return@mapNotNull null,
                code = h.array("weather_code").getOrNull(i).int() ?: 0,
                isDay = h.array("is_day").getOrNull(i).int() != 0,
                rainChance = h.array("precipitation_probability").getOrNull(i).int()
            )
        }
    }.orEmpty()
    val daily = root.obj("daily")
    val days = daily?.let { d ->
        val dates = d.array("time")
        dates.indices.mapNotNull { i ->
            WeatherDay(
                date = dates[i].string() ?: return@mapNotNull null,
                code = d.array("weather_code").getOrNull(i).int() ?: 0,
                max = d.array("temperature_2m_max").getOrNull(i).double() ?: return@mapNotNull null,
                min = d.array("temperature_2m_min").getOrNull(i).double() ?: return@mapNotNull null,
                rainChance = d.array("precipitation_probability_max").getOrNull(i).int()
            )
        }
    }.orEmpty()
    WeatherReport(
        fetchedAt = fetchedAt,
        utcOffsetSeconds = root.int("utc_offset_seconds") ?: 0,
        now = now,
        hours = hours,
        days = days
    )
}.getOrNull()

/** Le città trovate; vuota se nessuna (Open-Meteo allora non mette "results"). */
fun parseCitySearch(json: String): List<CityResult> = runCatching {
    val root = lenientJson.parseToJsonElement(json).jsonObject
    root.array("results").mapNotNull { element ->
        val city = element as? JsonObject ?: return@mapNotNull null
        val name = city.string("name") ?: return@mapNotNull null
        val region = listOfNotNull(city.string("admin1"), city.string("country"))
            .filter { it.isNotBlank() && it != name }
            .joinToString(", ")
        CityResult(
            name = name,
            region = region,
            latitude = city.double("latitude") ?: return@mapNotNull null,
            longitude = city.double("longitude") ?: return@mapNotNull null,
            population = city.double("population")?.toLong()
        )
    }
}.getOrDefault(emptyList())

/**
 * **Le città trovate in più lingue, insieme** (dal 17/10/2026): si cerca
 * nella lingua dell'app e in quella del telefono — in inglese Milano si
 * chiama "Milan", e "Milano" trova solo le Milano del Texas e del Perù — e si
 * mettono insieme le risposte: la stessa città (stesso punto sulla mappa) una
 * volta sola, col nome della prima lingua, e **prima le città con più
 * abitanti**.
 */
fun mergeCityResults(lists: List<List<CityResult>>): List<CityResult> {
    fun place(c: CityResult) = Math.round(c.latitude * 100) to Math.round(c.longitude * 100)
    return lists.flatten()
        .distinctBy { place(it) }
        .sortedByDescending { it.population ?: -1 }
}

private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject
private fun JsonObject.array(key: String): List<JsonElement> = (this[key] as? JsonArray)?.jsonArray.orEmpty()
private fun JsonObject.string(key: String): String? = this[key].string()
private fun JsonObject.double(key: String): Double? = this[key].double()
private fun JsonObject.int(key: String): Int? = this[key].int()
private fun JsonElement?.string(): String? = (this as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull
private fun JsonElement?.double(): Double? = (this as? JsonPrimitive)?.doubleOrNull
private fun JsonElement?.int(): Int? = (this as? JsonPrimitive)?.let { p -> p.intOrNull ?: p.doubleOrNull?.let { Math.round(it).toInt() } }

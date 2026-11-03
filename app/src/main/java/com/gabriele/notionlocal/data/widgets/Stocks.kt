package com.gabriele.notionlocal.data.widgets

import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import kotlinx.serialization.json.jsonObject
import java.net.URLEncoder

/**
 * **La borsa** (dal 17/10/2026, chiesto dall'utente con la foto di una lista):
 * titoli, indici e cambi scelti dall'utente, nell'ordine che sceglie, con la
 * sigla, il nome, la variazione del giorno (verde o rossa) e il prezzo. Un
 * tocco su un titolo ne apre i dettagli; tenendo premuto si aggiungono, si
 * spostano e si tolgono.
 *
 * I dati vengono da **Yahoo Finance**: gratuito e senza chiave, ma non è un
 * servizio ufficiale per le app; i prezzi di Piazza Affari arrivano con circa
 * quindici minuti di ritardo. L'ultimo prezzo scaricato resta salvato.
 */
@Serializable
@SerialName("stocks")
data class StocksWidget(
    override val id: String = newWidgetId(),
    val items: List<StockItem> = emptyList(),
    /** L'ultimo prezzo scaricato, per sigla. */
    val quotes: Map<String, StockQuote> = emptyMap(),
    val fetchedAt: Long? = null
) : Widget()

const val MAX_STOCKS = 30

/** Ogni quanto si riscaricano i prezzi con la barra aperta. */
const val STOCKS_REFRESH_MS = 5 * MINUTE_MS

@Serializable
data class StockItem(
    /** La sigla per Yahoo: "LDO.MI", "FTSEMIB.MI", "EURUSD=X", "^GDAXI". */
    val symbol: String,
    val name: String
)

@Serializable
data class StockQuote(
    val price: Double,
    /** La variazione del giorno, in percentuale. */
    val changePercent: Double,
    val currency: String = "",
    /** Quante cifre dopo la virgola ha il prezzo (2 per le azioni, 4 per i cambi). */
    val decimals: Int = 2,
    val previousClose: Double? = null,
    val dayHigh: Double? = null,
    val dayLow: Double? = null,
    val yearHigh: Double? = null,
    val yearLow: Double? = null,
    /** Quando è stato fatto l'ultimo scambio, in millisecondi. */
    val time: Long? = null,
    val exchange: String = ""
)

/** Un titolo trovato cercandolo. */
data class StockResult(val symbol: String, val name: String, val exchange: String, val type: String) {
    fun toItem(): StockItem = StockItem(symbol = symbol, name = name)
}

/**
 * La sigla come si legge: senza la borsa dopo il punto e senza i segni di
 * Yahoo — "LDO.MI" → "LDO", "EURUSD=X" → "EURUSD", "^GDAXI" → "GDAXI".
 */
fun displaySymbol(symbol: String): String = symbol.removePrefix("^").substringBefore('=').substringBefore('.')

fun StocksWidget.isStale(now: Long): Boolean = fetchedAt == null || now - fetchedAt >= STOCKS_REFRESH_MS

private val lenientJson = Json { ignoreUnknownKeys = true }

/** I prezzi di una risposta "spark" (più titoli insieme), per sigla. */
fun parseSpark(json: String): Map<String, StockQuote> = runCatching {
    val root = lenientJson.parseToJsonElement(json).jsonObject
    val results = root.obj("spark")?.array("result").orEmpty()
    results.mapNotNull { element ->
        val result = element as? JsonObject ?: return@mapNotNull null
        val symbol = result.string("symbol") ?: return@mapNotNull null
        val meta = (result.array("response").firstOrNull() as? JsonObject)?.obj("meta") ?: return@mapNotNull null
        val price = meta.double("regularMarketPrice") ?: return@mapNotNull null
        val previous = meta.double("chartPreviousClose") ?: meta.double("previousClose")
        val change = meta.double("regularMarketChangePercent")
            ?: previous?.takeIf { it != 0.0 }?.let { (price - it) / it * 100 }
            ?: 0.0
        symbol to StockQuote(
            price = price,
            changePercent = change,
            currency = meta.string("currency").orEmpty(),
            decimals = (meta.int("priceHint") ?: 2).coerceIn(0, 6),
            previousClose = previous,
            dayHigh = meta.double("regularMarketDayHigh"),
            dayLow = meta.double("regularMarketDayLow"),
            yearHigh = meta.double("fiftyTwoWeekHigh"),
            yearLow = meta.double("fiftyTwoWeekLow"),
            time = meta.double("regularMarketTime")?.let { (it * 1000).toLong() },
            exchange = meta.string("fullExchangeName") ?: meta.string("exchangeName").orEmpty()
        )
    }.toMap()
}.getOrDefault(emptyMap())

/** I titoli trovati; solo azioni, indici, cambi, fondi e simili (niente notizie). */
fun parseStockSearch(json: String): List<StockResult> = runCatching {
    val root = lenientJson.parseToJsonElement(json).jsonObject
    root.array("quotes").mapNotNull { element ->
        val quote = element as? JsonObject ?: return@mapNotNull null
        val symbol = quote.string("symbol") ?: return@mapNotNull null
        val type = quote.string("quoteType").orEmpty()
        if (type !in setOf("EQUITY", "INDEX", "CURRENCY", "ETF", "MUTUALFUND", "CRYPTOCURRENCY", "FUTURE")) return@mapNotNull null
        StockResult(
            symbol = symbol,
            name = (quote.string("longname") ?: quote.string("shortname") ?: symbol).trim(),
            exchange = quote.string("exchDisp") ?: quote.string("exchange").orEmpty(),
            type = quote.string("typeDisp") ?: type
        )
    }
}.getOrDefault(emptyList())

private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject
private fun JsonObject.array(key: String): List<JsonElement> = (this[key] as? JsonArray).orEmpty()
private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
private fun JsonObject.double(key: String): Double? = (this[key] as? JsonPrimitive)?.doubleOrNull
private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.let { it.intOrNull ?: it.doubleOrNull?.toInt() }

/** **Le domande a Yahoo Finance**: i prezzi di più titoli insieme, e la ricerca per nome o sigla. */
object StocksApi {

    /** I prezzi di adesso, per sigla; null senza rete. Venti sigle per domanda, le domande insieme. */
    suspend fun quotes(symbols: List<String>): Map<String, StockQuote>? = withContext(Dispatchers.IO) {
        if (symbols.isEmpty()) return@withContext emptyMap()
        val answers = symbols.distinct().chunked(20).map { chunk ->
            async {
                val list = chunk.joinToString(",") { URLEncoder.encode(it, "UTF-8") }
                WidgetHttp.get("https://query1.finance.yahoo.com/v7/finance/spark?symbols=$list&range=1d&interval=1d")
                    ?.let { parseSpark(it) }
            }
        }.awaitAll()
        if (answers.any { it == null }) null else answers.filterNotNull().fold(emptyMap()) { all, part -> all + part }
    }

    /**
     * I titoli che si chiamano così, con borsa e tipo nella lingua
     * dell'app ([language], "en", "it"…: "Milan · Equity", "Milano ·
     * Azione"); null senza rete.
     */
    suspend fun search(query: String, language: String): List<StockResult>? = withContext(Dispatchers.IO) {
        val text = query.trim()
        if (text.isEmpty()) return@withContext emptyList()
        val url = "https://query2.finance.yahoo.com/v1/finance/search?q=${URLEncoder.encode(text, "UTF-8")}" +
            "&quotesCount=12&newsCount=0&lang=${yahooLanguage(language)}&region=IT"
        WidgetHttp.get(url, maxBytes = 512 * 1024)?.let { parseStockSearch(it) }
    }

    /** Le lingue che Yahoo conosce; le altre in inglese. */
    private fun yahooLanguage(language: String): String = when (language) {
        "it" -> "it-IT"
        "de" -> "de-DE"
        "fr" -> "fr-FR"
        "es" -> "es-ES"
        else -> "en-US"
    }
}

/** **Quando si riscaricano i prezzi**: con la barra aperta, ogni cinque minuti, o subito con "Refresh". */
object StocksUpdates {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val inFlight = mutableSetOf<String>()

    private val failedState = mutableStateOf<Set<String>>(emptySet())
    /** I widget la cui ultima domanda non ha avuto risposta. */
    val failed: Set<String> get() = failedState.value

    fun refresh(widget: StocksWidget, force: Boolean = false, now: Long = System.currentTimeMillis()) {
        if (widget.items.isEmpty() || widget.id in inFlight) return
        if (!force && !widget.isStale(now)) return
        inFlight += widget.id
        scope.launch {
            val quotes = StocksApi.quotes(widget.items.map { it.symbol })
            if (quotes != null) {
                WidgetStore.setStockQuotes(widget.id, quotes, System.currentTimeMillis())
                failedState.value = failedState.value - widget.id
            } else {
                failedState.value = failedState.value + widget.id
            }
            inFlight -= widget.id
        }
    }
}

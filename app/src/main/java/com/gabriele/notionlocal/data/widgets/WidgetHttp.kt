package com.gabriele.notionlocal.data.widgets

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * **Le domande dei widget a internet** (meteo, sport, borsa): una richiesta
 * semplice, con un limite di tempo e di grandezza. Null senza rete, se il
 * servizio risponde con un errore o se la risposta è troppo grande.
 */
internal object WidgetHttp {

    private const val TIMEOUT_MS = 10_000

    // Alcuni servizi rispondono solo a chi sembra un browser.
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0 Mobile Safari/537.36"

    fun get(url: String, maxBytes: Int = 2 * 1024 * 1024): String? = runCatching {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept", "application/json")
            if (connection.responseCode !in 200..299) return@runCatching null
            val out = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    out.write(buffer, 0, read)
                    if (out.size() > maxBytes) return@runCatching null
                }
            }
            String(out.toByteArray(), Charsets.UTF_8)
        } finally {
            connection.disconnect()
        }
    }.getOrNull()
}

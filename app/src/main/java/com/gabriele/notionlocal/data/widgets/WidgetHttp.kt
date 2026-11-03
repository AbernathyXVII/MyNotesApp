package com.gabriele.notionlocal.data.widgets

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * **Le domande dei widget a internet** (meteo, sport, borsa): una richiesta
 * semplice, con un limite di tempo e di grandezza. Null senza rete, se il
 * servizio risponde con un errore o se la risposta è troppo grande.
 *
 * La connessione **si riusa** (dal 17/10/2026): letta la risposta fino in
 * fondo, resta aperta per la domanda dopo allo stesso indirizzo, che così
 * non rifà né la ricerca dell'indirizzo (il DNS, a volte lento sul telefono)
 * né la stretta di mano sicura. Prima ogni domanda la chiudeva
 * (`disconnect`): gli stemmi, venti alla volta, se ne accorgevano.
 */
internal object WidgetHttp {

    private const val TIMEOUT_MS = 10_000

    // Alcuni servizi rispondono solo a chi sembra un browser.
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0 Mobile Safari/537.36"

    fun get(url: String, maxBytes: Int = 2 * 1024 * 1024): String? =
        bytes(url, maxBytes, accept = "application/json")?.let { String(it, Charsets.UTF_8) }

    /** La risposta così com'è: le immagini (gli stemmi dello sport). */
    fun bytes(url: String, maxBytes: Int, accept: String = "*/*"): ByteArray? {
        val connection = runCatching { URL(url).openConnection() as HttpURLConnection }.getOrNull() ?: return null
        var reusable = false
        return try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept", accept)
            if (connection.responseCode !in 200..299) {
                // Anche la risposta d'errore va letta tutta, per riusare la connessione.
                connection.errorStream?.use { it.readBytes() }
                reusable = true
                return null
            }
            val out = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    out.write(buffer, 0, read)
                    if (out.size() > maxBytes) return null
                }
            }
            reusable = true
            out.toByteArray()
        } catch (e: Exception) {
            null
        } finally {
            // Solo una connessione lasciata a metà si chiude: le altre tornano a disposizione.
            if (!reusable) connection.disconnect()
        }
    }
}

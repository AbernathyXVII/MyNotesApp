package com.gabriele.notionlocal.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/** Quello che un sito dice di sé: il titolo, la descrizione e l'immagine di anteprima. */
data class LinkPreview(val title: String?, val description: String?, val imageUrl: String?)

/**
 * **L'anteprima di un segnalibro** (dal 06/10/2026, "Web bookmark"): si legge
 * l'inizio della pagina del sito e se ne prendono il titolo, la descrizione
 * e l'immagine che il sito stesso indica per le anteprime (i tag "og:" che
 * usano anche Notion, WhatsApp e Telegram). Serve la rete; senza, il
 * segnalibro mostra solo l'indirizzo.
 */
object LinkPreviews {

    private const val MAX_PAGE_BYTES = 512 * 1024
    private const val MAX_IMAGE_BYTES = 5 * 1024 * 1024
    private const val TIMEOUT_MS = 8_000

    // Molti siti danno le anteprime solo a chi sembra un browser.
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0 Mobile Safari/537.36"

    /** Legge il sito e ne prende l'anteprima; null senza rete o se il sito non risponde. */
    suspend fun fetch(url: String): LinkPreview? = withContext(Dispatchers.IO) {
        runCatching {
            val (bytes, finalUrl, encoding) = download(url, MAX_PAGE_BYTES, acceptHtml = true) ?: return@runCatching null
            val text = runCatching { String(bytes, charset(encoding ?: "UTF-8")) }.getOrElse { String(bytes, Charsets.UTF_8) }
            parse(text, finalUrl)
        }.getOrNull()
    }

    /** Scarica l'immagine di anteprima, se non è troppo grande. */
    suspend fun downloadImage(url: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching { download(url, MAX_IMAGE_BYTES, acceptHtml = false)?.first }.getOrNull()
    }

    /** I byte (al massimo `limit`), l'indirizzo dopo i rimandi, e la codifica dichiarata. */
    private fun download(url: String, limit: Int, acceptHtml: Boolean): Triple<ByteArray, String, String?>? {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.instanceFollowRedirects = true
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("User-Agent", USER_AGENT)
            if (acceptHtml) connection.setRequestProperty("Accept", "text/html,application/xhtml+xml")
            if (connection.responseCode !in 200..299) return null
            val charset = connection.contentType?.substringAfter("charset=", "")?.trim()?.takeIf { it.isNotEmpty() }
            val out = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(16 * 1024)
                while (out.size() < limit) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    out.write(buffer, 0, read)
                }
            }
            if (!acceptHtml && out.size() >= limit) return null
            return Triple(out.toByteArray(), connection.url.toString(), charset)
        } finally {
            connection.disconnect()
        }
    }

    private val META_TAG = Regex("<meta\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val ATTRIBUTE = Regex("([a-zA-Z_:][-a-zA-Z0-9_:.]*)\\s*=\\s*(\"([^\"]*)\"|'([^']*)')")
    private val TITLE_TAG = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

    /** Titolo, descrizione e immagine dall'HTML di una pagina; `pageUrl` serve per gli indirizzi relativi. */
    fun parse(html: String, pageUrl: String): LinkPreview {
        val metas = META_TAG.findAll(html).map { tag ->
            ATTRIBUTE.findAll(tag.value).associate { m ->
                m.groupValues[1].lowercase() to (m.groups[3]?.value ?: m.groups[4]?.value.orEmpty())
            }
        }.toList()
        fun meta(vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
            metas.firstOrNull { (it["property"] ?: it["name"])?.lowercase() == key }?.get("content")
        }?.let { decodeEntities(it).trim() }?.takeIf { it.isNotEmpty() }

        val title = meta("og:title", "twitter:title")
            ?: TITLE_TAG.find(html)?.groupValues?.get(1)?.let { decodeEntities(it).replace(Regex("\\s+"), " ").trim() }?.takeIf { it.isNotEmpty() }
        val description = meta("og:description", "twitter:description", "description")
        val image = meta("og:image", "og:image:url", "twitter:image")?.let { resolve(pageUrl, it) }
        return LinkPreview(title, description, image)
    }

    private fun resolve(base: String, link: String): String? =
        runCatching { URL(URL(base), link).toString() }.getOrNull()

    private fun decodeEntities(text: String): String =
        text.replace(Regex("&#(x?)([0-9a-fA-F]+);")) { m ->
            val code = m.groupValues[2].toIntOrNull(if (m.groupValues[1].isEmpty()) 10 else 16)
            code?.let { String(Character.toChars(it)) } ?: m.value
        }
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
}

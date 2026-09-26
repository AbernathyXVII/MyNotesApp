package com.gabriele.notionlocal.ui.format

import com.gabriele.notionlocal.data.settings.AppLanguage
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.settings.DateStyle
import com.gabriele.notionlocal.data.settings.NumberStyle
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle

/**
 * Come l'app scrive date, ore e numeri.
 *
 * **Un posto solo**, e letto dalle impostazioni ogni volta: la stessa data
 * compare nelle celle dei database, nella cronologia delle modifiche, nei
 * preferiti e nel cestino, e se ognuno la scrivesse a modo suo cambiare il
 * formato nelle impostazioni ne cambierebbe metà.
 *
 * Tutto quello che legge (formato, lingua, fuso) è stato di Compose:
 * chiamata durante il disegno, una di queste funzioni fa ridisegnare chi
 * l'ha usata appena l'impostazione cambia.
 */
object Formats {

    /** Solo la data, secondo il formato scelto. */
    fun date(millis: Long): String =
        date(Instant.ofEpochMilli(millis).atZone(AppSettings.zoneId).toLocalDate())

    fun date(day: LocalDate): String = dateFormatter().format(day)

    /** Data e ora, l'ora sempre in 24 ore: "20.09.2026 19:41". */
    fun dateTime(millis: Long): String {
        val zoned = Instant.ofEpochMilli(millis).atZone(AppSettings.zoneId)
        return "${date(zoned.toLocalDate())} ${TIME.format(zoned)}"
    }

    /** Solo l'ora. */
    fun time(millis: Long): String =
        TIME.format(Instant.ofEpochMilli(millis).atZone(AppSettings.zoneId))

    private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /** La data nel formato scelto, per chi deve formattare da sé (le celle dei database). */
    fun dateFormatter(): DateTimeFormatter = formatterFor(AppSettings.dateStyle)

    /** Data e ora nel formato scelto: la data, uno spazio, l'ora in 24 ore. */
    fun dateTimeFormatter(): DateTimeFormatter = DateTimeFormatterBuilder()
        .append(dateFormatter())
        .appendLiteral(' ')
        .appendPattern("HH:mm")
        .toFormatter(AppSettings.language.locale)

    /** "Sep 20" in inglese; mese dopo il giorno nelle lingue europee, coi caratteri propri in Asia. */
    private fun shortPattern(language: AppLanguage): String = when (language) {
        AppLanguage.ENGLISH -> "MMM d"
        AppLanguage.CHINESE, AppLanguage.JAPANESE -> "M月d日"
        AppLanguage.KOREAN -> "M월 d일"
        else -> "d MMM"
    }

    /** Un giorno scritto in un certo formato, per gli esempi del menu delle impostazioni. */
    fun sample(style: DateStyle, day: LocalDate): String = formatterFor(style).format(day)

    private fun formatterFor(style: DateStyle): DateTimeFormatter {
        val language = AppSettings.language
        return when (style) {
            // La data lunga la scrive ogni lingua a modo suo: "September 20,
            // 2026", "20 settembre 2026", "2026年9月20日". Imporre l'ordine
            // inglese a tutte avrebbe dato "settembre 20, 2026".
            DateStyle.FULL -> DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(language.locale)
            DateStyle.SHORT -> DateTimeFormatter.ofPattern(shortPattern(language), language.locale)
            DateStyle.MONTH_DAY_YEAR_DOT -> DateTimeFormatter.ofPattern("MM.dd.yyyy")
            DateStyle.MONTH_DAY_YEAR_SLASH -> DateTimeFormatter.ofPattern("MM/dd/yyyy")
            DateStyle.DAY_MONTH_YEAR_DOT -> DateTimeFormatter.ofPattern("dd.MM.yyyy")
            DateStyle.DAY_MONTH_YEAR_SLASH -> DateTimeFormatter.ofPattern("dd/MM/yyyy")
            DateStyle.YEAR_MONTH_DAY_DOT -> DateTimeFormatter.ofPattern("yyyy.MM.dd")
            DateStyle.YEAR_MONTH_DAY_SLASH -> DateTimeFormatter.ofPattern("yyyy/MM/dd")
            DateStyle.CHINESE -> DateTimeFormatter.ofPattern("yyyy年MM月dd日")
        }
    }

    /**
     * Un numero scritto come l'utente l'ha salvato, rimesso in forma:
     * "1234567.5" diventa "1.234.567,5" o "1,234,567.5".
     *
     * Solo per **mostrarlo**: il valore salvato resta quello grezzo, col
     * punto per i decimali, così cambiare formato non tocca i dati e i
     * calcoli (ordinamenti, filtri) continuano a leggerlo sempre uguale.
     * Se il testo non è un numero lo si lascia com'è.
     */
    fun number(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return raw
        val value = trimmed.toBigDecimalOrNull() ?: return raw
        val symbols = DecimalFormatSymbols().apply {
            when (AppSettings.numberStyle) {
                NumberStyle.DOT_GROUPS -> { groupingSeparator = '.'; decimalSeparator = ',' }
                NumberStyle.COMMA_GROUPS -> { groupingSeparator = ','; decimalSeparator = '.' }
            }
        }
        // Tante cifre decimali quante ne aveva scritte l'utente, non di
        // più e non di meno: 2.50 resta 2,50.
        val decimals = value.scale().coerceAtLeast(0)
        val pattern = if (decimals > 0) "#,##0." + "0".repeat(decimals) else "#,##0"
        return DecimalFormat(pattern, symbols).apply { isParseBigDecimal = true }
            .format(value)
    }

    /** L'esempio fisso dei due formati di numero, per il menu delle impostazioni. */
    fun numberSample(style: NumberStyle): String = when (style) {
        NumberStyle.DOT_GROUPS -> "1.000.000"
        NumberStyle.COMMA_GROUPS -> "1,000,000"
    }

    private fun String.toBigDecimalOrNull(): BigDecimal? = runCatching { BigDecimal(this) }.getOrNull()
}

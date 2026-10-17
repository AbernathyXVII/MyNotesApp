package com.gabriele.notionlocal.ui.format

import com.gabriele.notionlocal.data.entity.TimeFormatKind
import com.gabriele.notionlocal.ui.i18n.Strings
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

    /** Data e ora, l'ora come dice Settings: "20.09.2026 19:41" o "20.09.2026 7:41 PM". */
    fun dateTime(millis: Long): String {
        val zoned = Instant.ofEpochMilli(millis).atZone(AppSettings.zoneId)
        return "${date(zoned.toLocalDate())} ${timeFormatter().format(zoned)}"
    }

    /**
     * Il giorno della settimana e la data, come sotto il titolo delle pagine
     * di OneNote: "Saturday, 26.09.2026". Il giorno nella lingua dell'app.
     */
    fun weekdayAndDate(millis: Long): String {
        val zoned = Instant.ofEpochMilli(millis).atZone(AppSettings.zoneId)
        val weekday = DateTimeFormatter.ofPattern("EEEE", AppSettings.language.locale).format(zoned)
            .replaceFirstChar { it.titlecase(AppSettings.language.locale) }
        return "$weekday, ${date(zoned.toLocalDate())}"
    }

    /** Solo l'ora. */
    fun time(millis: Long): String =
        timeFormatter().format(Instant.ofEpochMilli(millis).atZone(AppSettings.zoneId))

    /**
     * **L'ora come dice "Time format" in Settings** (dal 16/10/2026): "15:07"
     * o "3:07 PM" (nella lingua dell'app). "Hidden" toglie l'ora solo dalle
     * date dei database (`dateTimeFormatter(style)`); qui, dove l'ora è quello
     * che si vuole leggere, resta a 24 ore.
     */
    fun timeFormatter(): DateTimeFormatter =
        if (AppSettings.timeFormat == TimeFormatKind.H12) {
            DateTimeFormatter.ofPattern("h:mm a", AppSettings.language.locale)
        } else {
            DateTimeFormatter.ofPattern("HH:mm")
        }

    /** Le ore piene di una griglia (il giorno del calendario): "15" … o "3 PM". */
    fun hourLabelFormatter(): DateTimeFormatter =
        if (AppSettings.timeFormat == TimeFormatKind.H12) {
            DateTimeFormatter.ofPattern("h a", AppSettings.language.locale)
        } else {
            DateTimeFormatter.ofPattern("HH:mm")
        }

    /** La data nel formato scelto, per chi deve formattare da sé (le celle dei database). */
    fun dateFormatter(): DateTimeFormatter = formatterFor(AppSettings.dateStyle)

    /** Data e ora nel formato scelto: la data, uno spazio, l'ora come dice Settings. */
    fun dateTimeFormatter(): DateTimeFormatter = DateTimeFormatterBuilder()
        .append(dateFormatter())
        .appendLiteral(' ')
        .append(timeFormatter())
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

    /**
     * **Una data di un database con la sua ora** (dal 26/09/2026): il formato
     * della data e dell'ora di Settings, e con "Hidden" l'ora non si scrive.
     * Fino al 16/10/2026 sceglieva ogni colonna Date.
     */
    fun databaseDateTimeFormatter(): DateTimeFormatter =
        if (AppSettings.timeFormat == TimeFormatKind.HIDDEN) {
            dateFormatter()
        } else {
            dateTimeFormatter()
        }

    /** Il nome di uno stile di data: "Full date", "Day/Month/Year (.)"… */
    fun dateStyleName(style: DateStyle): String = when (style) {
        DateStyle.FULL -> Strings.dateFull
        DateStyle.SHORT -> Strings.dateShort
        DateStyle.MONTH_DAY_YEAR_DOT -> Strings.dateMonthDayYearDot
        DateStyle.MONTH_DAY_YEAR_SLASH -> Strings.dateMonthDayYearSlash
        DateStyle.DAY_MONTH_YEAR_DOT -> Strings.dateDayMonthYearDot
        DateStyle.DAY_MONTH_YEAR_SLASH -> Strings.dateDayMonthYearSlash
        DateStyle.YEAR_MONTH_DAY_DOT -> Strings.dateYearMonthDayDot
        DateStyle.YEAR_MONTH_DAY_SLASH -> Strings.dateYearMonthDaySlash
        DateStyle.CHINESE -> Strings.dateChinese
    }


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
    fun number(raw: String): String = number(raw, AppSettings.numberStyle)

    /** Come `number`, con uno stile scelto: per gli esempi del menu delle impostazioni. */
    fun number(raw: String, style: NumberStyle): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return raw
        val value = trimmed.toBigDecimalOrNull() ?: return raw
        val symbols = DecimalFormatSymbols().apply {
            // Senza separatore il segno dei gruppi non si scrive: basta
            // che non sia quello dei decimali.
            groupingSeparator = style.grouping ?: '\u00A0'
            decimalSeparator = style.decimal
        }
        // Tante cifre decimali quante ne aveva scritte l'utente, non di
        // più e non di meno: 2.50 resta 2,50.
        val decimals = value.scale().coerceAtLeast(0)
        val pattern = if (decimals > 0) "#,##0." + "0".repeat(decimals) else "#,##0"
        return DecimalFormat(pattern, symbols).apply {
            isParseBigDecimal = true
            isGroupingUsed = style.grouping != null
        }.format(value)
    }

    /** L'esempio di uno stile, per il menu delle impostazioni: "1 000 000", "1.000.000"… */
    fun numberSample(style: NumberStyle): String = number("1000000", style)

    /** Lo stesso stile con i decimali, sotto l'esempio: "1 234,56". */
    fun numberDecimalSample(style: NumberStyle): String = number("1234.56", style)

    private fun String.toBigDecimalOrNull(): BigDecimal? = runCatching { BigDecimal(this) }.getOrNull()
}

package com.gabriele.notionlocal.ui.i18n

import com.gabriele.notionlocal.data.entity.DurationUnit
import com.gabriele.notionlocal.data.settings.AppLanguage

/**
 * **Una durata a parole** ("Duration", dal 29/09/2026): "30 years, 7 months
 * and 17 days", "30 anni, 7 mesi e 17 giorni", col singolare per 1, le
 * virgole e la congiunzione prima dell'ultima. In cinese e giapponese i
 * pezzi stanno attaccati ("30年7か月17日"), in coreano separati da spazi.
 */
fun durationWords(units: List<Pair<DurationUnit, Long>>, language: AppLanguage): String {
    val pieces = units.map { (unit, n) -> "$n" + unitWord(unit, n, language) }
    return when (language) {
        AppLanguage.CHINESE, AppLanguage.JAPANESE -> pieces.joinToString("")
        AppLanguage.KOREAN -> pieces.joinToString(" ")
        else -> {
            val and = when (language) {
                AppLanguage.ITALIAN -> "e"
                AppLanguage.GERMAN -> "und"
                AppLanguage.FRENCH -> "et"
                AppLanguage.SPANISH -> "y"
                else -> "and"
            }
            if (pieces.size == 1) pieces.single() else pieces.dropLast(1).joinToString(", ") + " $and " + pieces.last()
        }
    }
}

/** Il nome dell'unità, con lo spazio davanti dove ci va: "1 year", "2 years", "30年". */
private fun unitWord(unit: DurationUnit, n: Long, language: AppLanguage): String {
    // In francese anche lo zero va al singolare ("0 an").
    val one = if (language == AppLanguage.FRENCH) n <= 1 else n == 1L
    fun pick(singular: String, plural: String) = " " + if (one) singular else plural
    return when (language) {
        AppLanguage.ENGLISH -> when (unit) {
            DurationUnit.YEARS -> pick("year", "years")
            DurationUnit.MONTHS -> pick("month", "months")
            DurationUnit.DAYS -> pick("day", "days")
            DurationUnit.HOURS -> pick("hour", "hours")
            DurationUnit.MINUTES -> pick("minute", "minutes")
        }
        AppLanguage.ITALIAN -> when (unit) {
            DurationUnit.YEARS -> pick("anno", "anni")
            DurationUnit.MONTHS -> pick("mese", "mesi")
            DurationUnit.DAYS -> pick("giorno", "giorni")
            DurationUnit.HOURS -> pick("ora", "ore")
            DurationUnit.MINUTES -> pick("minuto", "minuti")
        }
        AppLanguage.GERMAN -> when (unit) {
            DurationUnit.YEARS -> pick("Jahr", "Jahre")
            DurationUnit.MONTHS -> pick("Monat", "Monate")
            DurationUnit.DAYS -> pick("Tag", "Tage")
            DurationUnit.HOURS -> pick("Stunde", "Stunden")
            DurationUnit.MINUTES -> pick("Minute", "Minuten")
        }
        AppLanguage.FRENCH -> when (unit) {
            DurationUnit.YEARS -> pick("an", "ans")
            DurationUnit.MONTHS -> pick("mois", "mois")
            DurationUnit.DAYS -> pick("jour", "jours")
            DurationUnit.HOURS -> pick("heure", "heures")
            DurationUnit.MINUTES -> pick("minute", "minutes")
        }
        AppLanguage.SPANISH -> when (unit) {
            DurationUnit.YEARS -> pick("año", "años")
            DurationUnit.MONTHS -> pick("mes", "meses")
            DurationUnit.DAYS -> pick("día", "días")
            DurationUnit.HOURS -> pick("hora", "horas")
            DurationUnit.MINUTES -> pick("minuto", "minutos")
        }
        AppLanguage.CHINESE -> when (unit) {
            DurationUnit.YEARS -> "年"
            DurationUnit.MONTHS -> "个月"
            DurationUnit.DAYS -> "天"
            DurationUnit.HOURS -> "小时"
            DurationUnit.MINUTES -> "分钟"
        }
        AppLanguage.KOREAN -> when (unit) {
            DurationUnit.YEARS -> "년"
            DurationUnit.MONTHS -> "개월"
            DurationUnit.DAYS -> "일"
            DurationUnit.HOURS -> "시간"
            DurationUnit.MINUTES -> "분"
        }
        AppLanguage.JAPANESE -> when (unit) {
            DurationUnit.YEARS -> "年"
            DurationUnit.MONTHS -> "か月"
            DurationUnit.DAYS -> "日"
            DurationUnit.HOURS -> "時間"
            DurationUnit.MINUTES -> "分"
        }
    }
}

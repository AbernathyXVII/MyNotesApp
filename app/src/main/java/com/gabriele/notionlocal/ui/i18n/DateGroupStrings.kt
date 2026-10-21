package com.gabriele.notionlocal.ui.i18n

import com.gabriele.notionlocal.data.entity.DateUnit

/** Le scritte di "Date by", i raggruppamenti per data (dal 21/10/2026). */
object DateGroupStrings {

    private fun t(en: String, it: String, de: String, fr: String, es: String, zh: String, ko: String, ja: String) =
        Strings.t(en, it, de, fr, es, zh, ko, ja)

    val dateBy get() = t("Date by", "Date per", "Datum nach", "Dates par", "Fechas por", "日期分组", "날짜 기준", "日付の単位")
    val newestFirst get() = t(
        "Newest first", "Prima le più recenti", "Neueste zuerst", "Plus récentes d'abord",
        "Más recientes primero", "最新的在前", "최신순", "新しい順"
    )

    fun unitName(unit: DateUnit): String = when (unit) {
        DateUnit.DAY -> t("Day", "Giorno", "Tag", "Jour", "Día", "天", "일", "日")
        DateUnit.WEEK -> t("Week", "Settimana", "Woche", "Semaine", "Semana", "周", "주", "週")
        DateUnit.MONTH -> t("Month", "Mese", "Monat", "Mois", "Mes", "月", "월", "月")
        DateUnit.YEAR -> t("Year", "Anno", "Jahr", "Année", "Año", "年", "년", "年")
    }
}

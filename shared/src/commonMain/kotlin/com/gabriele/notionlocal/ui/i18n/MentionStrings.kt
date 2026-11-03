package com.gabriele.notionlocal.ui.i18n

import com.gabriele.notionlocal.data.settings.AppSettings
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Le scritte delle menzioni "@" e dei backlink (dal 03/11/2026). */
object MentionStrings {

    private fun t(en: String, it: String, de: String, fr: String, es: String, zh: String, ko: String, ja: String) =
        Strings.t(en, it, de, fr, es, zh, ko, ja)

    val pages get() = t("Pages", "Pagine", "Seiten", "Pages", "Páginas", "页面", "페이지", "ページ")
    val dates get() = t("Dates", "Date", "Daten", "Dates", "Fechas", "日期", "날짜", "日付")
    val today get() = t("Today", "Oggi", "Heute", "Aujourd'hui", "Hoy", "今天", "오늘", "今日")
    val tomorrow get() = t("Tomorrow", "Domani", "Morgen", "Demain", "Mañana", "明天", "내일", "明日")
    val yesterday get() = t("Yesterday", "Ieri", "Gestern", "Hier", "Ayer", "昨天", "어제", "昨日")
    val pickDate get() = t("Pick a date…", "Scegli una data…", "Datum wählen…", "Choisir une date…", "Elegir una fecha…", "选择日期…", "날짜 선택…", "日付を選ぶ…")
    val noPages get() = t("No pages with that name", "Nessuna pagina con quel nome", "Keine Seite mit diesem Namen", "Aucune page avec ce nom", "Ninguna página con ese nombre", "没有这个名字的页面", "그 이름의 페이지가 없어요", "その名前のページはありません")
    val backlinks get() = t("Backlinks", "Backlink", "Rückverweise", "Rétroliens", "Retroenlaces", "反向链接", "백링크", "バックリンク")
    val linkToPage get() = t("Link to page", "Link alla pagina", "Link zur Seite", "Lien vers la page", "Enlace a la página", "链接到页面", "페이지 링크", "ページへのリンク")
    val pageGone get() = t(
        "This page is no longer there",
        "Questa pagina non c'è più",
        "Diese Seite gibt es nicht mehr",
        "Cette page n'existe plus",
        "Esta página ya no existe",
        "这个页面已不存在",
        "이 페이지는 더 이상 없어요",
        "このページはもうありません"
    )

    /** Il giorno scritto per intero nella lingua dell'app: "3 novembre 2026". */
    fun dateText(date: LocalDate): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(AppSettings.language.locale).format(date)
}

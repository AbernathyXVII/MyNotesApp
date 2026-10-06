package com.gabriele.notionlocal.ui.i18n

/**
 * Le voci dei blocchi nuovi del 06/10/2026 — riquadro, citazione, codice,
 * immagine, video, audio, file, segnalibro, link a una pagina, database a
 * pagina intera. Stessa regola di `Strings`: ogni voce con le sue otto
 * traduzioni, e solo testo dell'app.
 */
object BlockStrings {

    private fun t(en: String, it: String, de: String, fr: String, es: String, zh: String, ko: String, ja: String) =
        Strings.t(en, it, de, fr, es, zh, ko, ja)

    // --- Riquadro e citazione ---

    /** Il suggerimento grigio di una citazione vuota, come su Notion. */
    val emptyQuote get() = t("Empty quote", "Citazione vuota", "Leeres Zitat", "Citation vide", "Cita vacía", "空引用", "빈 인용", "空の引用")
    /** Il campo per scrivere un'emoji qualsiasi come icona del riquadro. */
    val otherEmoji get() = t("Other emoji", "Altra emoji", "Anderes Emoji", "Autre émoji", "Otro emoji", "其他表情", "다른 이모지", "その他の絵文字")
    val useEmoji get() = t("Use", "Usa", "Verwenden", "Utiliser", "Usar", "使用", "사용", "使う")
    /** La famiglia di emoji dei riquadri, come la chiama Notion. */
    val calloutEmojis get() = t("Callout", "Riquadro", "Hinweisbox", "Encadré", "Destacado", "标注", "콜아웃", "コールアウト")
}

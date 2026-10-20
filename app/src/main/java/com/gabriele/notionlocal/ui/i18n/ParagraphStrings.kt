package com.gabriele.notionlocal.ui.i18n

/** Le scritte del "Paragraph" (dal 21/10/2026): lo spazio fra i blocchi e dopo i marcatori degli elenchi. */
object ParagraphStrings {

    private fun t(en: String, it: String, de: String, fr: String, es: String, zh: String, ko: String, ja: String) =
        Strings.t(en, it, de, fr, es, zh, ko, ja)

    val paragraph get() = t("Paragraph", "Paragrafo", "Absatz", "Paragraphe", "Párrafo", "段落", "단락", "段落")
    val spaceBetweenBlocks get() = t(
        "Space between blocks", "Spazio fra i blocchi", "Abstand zwischen Blöcken", "Espace entre les blocs",
        "Espacio entre bloques", "块之间的间距", "블록 사이 간격", "ブロックの間隔"
    )
    val spaceBetweenBlocksNote get() = t(
        "Added between one block and the next, like the spacing before and after a paragraph in Word. 0 pt is how pages have always looked.",
        "Si aggiunge fra un blocco e l'altro, come lo spazio prima e dopo un paragrafo su Word. 0 pt è com'erano sempre le pagine.",
        "Wird zwischen zwei Blöcken eingefügt, wie der Abstand vor und nach einem Absatz in Word. 0 pt ist das bisherige Aussehen.",
        "S'ajoute entre deux blocs, comme l'espacement avant et après un paragraphe dans Word. 0 pt correspond à l'aspect habituel.",
        "Se añade entre un bloque y el siguiente, como el espaciado antes y después de un párrafo en Word. 0 pt es el aspecto de siempre.",
        "加在一个块和下一个块之间，就像 Word 中段落前后的间距。0 pt 是页面一直以来的样子。",
        "블록과 블록 사이에 더해집니다. Word의 단락 앞뒤 간격과 같습니다. 0 pt는 지금까지의 모습입니다.",
        "ブロックとブロックの間に加わります。Word の段落前後の間隔と同じです。0 pt はこれまでどおりの見た目です。"
    )
    val markerGap get() = t(
        "Space after number, bullet or checkbox", "Spazio dopo numero, pallino o casella",
        "Abstand nach Nummer, Aufzählungszeichen oder Kontrollkästchen", "Espace après le numéro, la puce ou la case",
        "Espacio después del número, la viñeta o la casilla", "编号、项目符号或复选框后的间距",
        "번호, 글머리 기호 또는 체크박스 뒤 간격", "番号・行頭記号・チェックボックスの後の間隔"
    )
    val markerGapNote get() = t(
        "Between the number, the bullet or the checkbox of a list and the first character. Default is how lists have always looked.",
        "Fra il numero, il pallino o la casella di un elenco e il primo carattere. Default è com'erano sempre gli elenchi.",
        "Zwischen Nummer, Aufzählungszeichen oder Kontrollkästchen einer Liste und dem ersten Zeichen. Default ist das bisherige Aussehen.",
        "Entre le numéro, la puce ou la case d'une liste et le premier caractère. Default correspond à l'aspect habituel.",
        "Entre el número, la viñeta o la casilla de una lista y el primer carácter. Default es el aspecto de siempre.",
        "列表的编号、项目符号或复选框与第一个字符之间。Default 是列表一直以来的样子。",
        "목록의 번호, 글머리 기호 또는 체크박스와 첫 글자 사이입니다. Default는 지금까지의 모습입니다.",
        "リストの番号・行頭記号・チェックボックスと最初の文字の間です。Default はこれまでどおりの見た目です。"
    )
    val sameForLists get() = t(
        "Same for numbered, bulleted and to-do lists", "Uguale per elenchi numerati, puntati e to-do",
        "Gleich für nummerierte Listen, Aufzählungen und To-do-Listen", "Identique pour les listes numérotées, à puces et de tâches",
        "Igual para listas numeradas, con viñetas y de tareas", "编号、项目符号和待办列表相同",
        "번호, 글머리 기호, 할 일 목록에 동일하게", "番号付き・箇条書き・ToDo リストで共通"
    )
    val allLists get() = t("All lists", "Tutti gli elenchi", "Alle Listen", "Toutes les listes", "Todas las listas", "所有列表", "모든 목록", "すべてのリスト")
    val numbered get() = t("Numbered list", "Elenco numerato", "Nummerierte Liste", "Liste numérotée", "Lista numerada", "编号列表", "번호 목록", "番号付きリスト")
    val bulleted get() = t("Bulleted list", "Elenco puntato", "Aufzählung", "Liste à puces", "Lista con viñetas", "项目符号列表", "글머리 기호 목록", "箇条書きリスト")
    val todo get() = t("To-do list", "Elenco to-do", "To-do-Liste", "Liste de tâches", "Lista de tareas", "待办列表", "할 일 목록", "ToDo リスト")
    val default get() = t("Default", "Default", "Default", "Default", "Default", "Default", "Default", "Default")
    fun points(n: Int) = "$n pt"
    val less get() = t("Less space", "Meno spazio", "Weniger Abstand", "Moins d'espace", "Menos espacio", "减小间距", "간격 줄이기", "間隔を狭く")
    val more get() = t("More space", "Più spazio", "Mehr Abstand", "Plus d'espace", "Más espacio", "增大间距", "간격 늘리기", "間隔を広く")

    val settingsNote get() = t(
        "For every page that doesn't choose otherwise from its ⋯ menu.",
        "Per tutte le pagine che non scelgono altro dal loro menu ⋯.",
        "Für alle Seiten, die in ihrem ⋯-Menü nichts anderes wählen.",
        "Pour toutes les pages qui ne choisissent pas autre chose dans leur menu ⋯.",
        "Para todas las páginas que no eligen otra cosa en su menú ⋯.",
        "适用于所有未在 ⋯ 菜单中另行选择的页面。",
        "⋯ 메뉴에서 따로 고르지 않은 모든 페이지에 적용됩니다.",
        "⋯ メニューで別の設定をしていないすべてのページに適用されます。"
    )
    val pageNote get() = t(
        "\"As in Settings\" follows Settings; \"Only this page\" counts here only.",
        "\"Come in Settings\" segue Settings; \"Solo questa pagina\" vale solo qui.",
        "\"Wie in Settings\" folgt Settings; \"Nur diese Seite\" gilt nur hier.",
        "« Comme dans Settings » suit Settings ; « Seulement cette page » ne vaut qu'ici.",
        "\"Como en Settings\" sigue Settings; \"Solo esta página\" vale solo aquí.",
        "“与 Settings 相同”跟随 Settings；“仅此页面”只在这里生效。",
        "\"Settings와 동일\"은 Settings를 따르고, \"이 페이지만\"은 여기에서만 적용됩니다.",
        "「Settings と同じ」は Settings に従い、「このページのみ」はここだけに適用されます。"
    )
    val linesNote get() = t(
        "For the lines you chose. New lines made with Enter keep it, like in Word.",
        "Per le righe scelte. Le righe nuove fatte con Invio lo tengono, come su Word.",
        "Für die gewählten Zeilen. Neue Zeilen per Eingabetaste übernehmen es, wie in Word.",
        "Pour les lignes choisies. Les nouvelles lignes créées avec Entrée le gardent, comme dans Word.",
        "Para las líneas elegidas. Las líneas nuevas creadas con Intro lo conservan, como en Word.",
        "适用于所选的行。按回车新建的行会沿用，和 Word 一样。",
        "선택한 줄에 적용됩니다. Enter로 만든 새 줄도 그대로 이어받습니다. Word와 같습니다.",
        "選んだ行に適用されます。Enter で作った新しい行も引き継ぎます。Word と同じです。"
    )
    fun asInSettings(value: String) = t(
        "As in Settings ($value)", "Come in Settings ($value)", "Wie in Settings ($value)", "Comme dans Settings ($value)",
        "Como en Settings ($value)", "与 Settings 相同（$value）", "Settings와 동일 ($value)", "Settings と同じ（$value）"
    )
    val onlyThisPage get() = t("Only this page", "Solo questa pagina", "Nur diese Seite", "Seulement cette page", "Solo esta página", "仅此页面", "이 페이지만", "このページのみ")
    fun asInPage(value: String) = t(
        "As in the page ($value)", "Come nella pagina ($value)", "Wie auf der Seite ($value)", "Comme dans la page ($value)",
        "Como en la página ($value)", "与页面相同（$value）", "페이지와 동일 ($value)", "ページと同じ（$value）"
    )
    val onlyTheseLines get() = t("Only these lines", "Solo queste righe", "Nur diese Zeilen", "Seulement ces lignes", "Solo estas líneas", "仅这些行", "이 줄만", "これらの行のみ")
    fun lines(n: Int) = when (n) {
        1 -> t("1 line", "1 riga", "1 Zeile", "1 ligne", "1 línea", "1 行", "1줄", "1 行")
        else -> t("$n lines", "$n righe", "$n Zeilen", "$n lignes", "$n líneas", "$n 行", "${n}줄", "$n 行")
    }
    val blocksShort get() = t("Blocks", "Blocchi", "Blöcke", "Blocs", "Bloques", "块", "블록", "ブロック")
    val listsShort get() = t("Lists", "Elenchi", "Listen", "Listes", "Listas", "列表", "목록", "リスト")
    val perType get() = t("per type", "per tipo", "je Typ", "par type", "por tipo", "按类型", "유형별", "種類別")
}

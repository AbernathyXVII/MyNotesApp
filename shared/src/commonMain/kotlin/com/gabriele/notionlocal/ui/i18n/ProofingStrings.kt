package com.gabriele.notionlocal.ui.i18n

/** Le scritte del correttore (dal 08/10/2026). */
object ProofingStrings {

    private fun t(en: String, it: String, de: String, fr: String, es: String, zh: String, ko: String, ja: String) =
        Strings.t(en, it, de, fr, es, zh, ko, ja)

    val proofing get() = t("Proofing", "Controllo ortografico", "Rechtschreibprüfung", "Vérification orthographique", "Revisión ortográfica", "拼写检查", "맞춤법 검사", "スペルチェック")
    val checkSpelling get() = t("Check spelling", "Controlla l'ortografia", "Rechtschreibung prüfen", "Vérifier l'orthographe", "Revisar la ortografía", "检查拼写", "맞춤법 검사하기", "スペルをチェック")
    val checkSpellingNote get() = t(
        "Underlines in red the words the dictionary doesn't know, in the pages of the Workspace and of the Notebooks.",
        "Sottolinea in rosso le parole che il dizionario non conosce, nelle pagine del Workspace e dei Notebooks.",
        "Unterstreicht in den Seiten des Workspace und der Notebooks rot die Wörter, die das Wörterbuch nicht kennt.",
        "Souligne en rouge les mots que le dictionnaire ne connaît pas, dans les pages du Workspace et des Notebooks.",
        "Subraya en rojo las palabras que el diccionario no conoce, en las páginas del Workspace y de los Notebooks.",
        "在 Workspace 和 Notebooks 的页面中，用红线标出词典不认识的单词。",
        "Workspace와 Notebooks 페이지에서 사전에 없는 단어에 빨간 밑줄을 긋습니다.",
        "Workspace と Notebooks のページで、辞書にない単語に赤い下線を引きます。"
    )
    val language get() = t("Language", "Lingua", "Sprache", "Langue", "Idioma", "语言", "언어", "言語")
    val notAvailable get() = t("Not available yet", "Non ancora disponibile", "Noch nicht verfügbar", "Pas encore disponible", "Aún no disponible", "暂不可用", "아직 사용할 수 없음", "まだ利用できません")
    val myDictionary get() = t("My dictionary", "Il mio dizionario", "Mein Wörterbuch", "Mon dictionnaire", "Mi diccionario", "我的词典", "내 사전", "マイ辞書")
    val myDictionaryNote get() = t(
        "The words you added with \"Add to dictionary\". They count in every language.",
        "Le parole aggiunte con \"Add to dictionary\". Valgono in tutte le lingue.",
        "Die mit \"Add to dictionary\" hinzugefügten Wörter. Sie gelten in allen Sprachen.",
        "Les mots ajoutés avec « Add to dictionary ». Ils valent dans toutes les langues.",
        "Las palabras añadidas con \"Add to dictionary\". Valen en todos los idiomas.",
        "用“Add to dictionary”添加的单词，适用于所有语言。",
        "\"Add to dictionary\"로 추가한 단어입니다. 모든 언어에 적용됩니다.",
        "「Add to dictionary」で追加した単語です。すべての言語で有効です。"
    )
    val myDictionaryEmpty get() = t("No words yet.", "Ancora nessuna parola.", "Noch keine Wörter.", "Aucun mot pour l'instant.", "Aún no hay palabras.", "还没有单词。", "아직 단어가 없습니다.", "まだ単語はありません。")
    fun wordCount(n: Int) = when (n) {
        1 -> t("1 word", "1 parola", "1 Wort", "1 mot", "1 palabra", "1 个单词", "1개 단어", "1 語")
        else -> t("$n words", "$n parole", "$n Wörter", "$n mots", "$n palabras", "$n 个单词", "${n}개 단어", "$n 語")
    }
    val removeWord get() = t("Remove", "Togli", "Entfernen", "Retirer", "Quitar", "移除", "삭제", "削除")
    val addToDictionary get() = t("Add to dictionary", "Aggiungi al dizionario", "Zum Wörterbuch hinzufügen", "Ajouter au dictionnaire", "Añadir al diccionario", "添加到词典", "사전에 추가", "辞書に追加")
    val ignore get() = t("Ignore", "Ignora", "Ignorieren", "Ignorer", "Ignorar", "忽略", "무시", "無視")
    val noSuggestions get() = t("No suggestions", "Nessun suggerimento", "Keine Vorschläge", "Aucune suggestion", "Sin sugerencias", "没有建议", "추천 없음", "候補はありません")
    val on get() = t("On", "Attivo", "Ein", "Activé", "Activado", "开", "켬", "オン")
    val off get() = t("Off", "Spento", "Aus", "Désactivé", "Desactivado", "关", "끔", "オフ")
    fun default(what: String) = t("Default ($what)", "Predefinito ($what)", "Standard ($what)", "Par défaut ($what)", "Predeterminado ($what)", "默认（$what）", "기본값 ($what)", "デフォルト（$what）")
    val pageNote get() = t(
        "Only for this page. \"Default\" follows Settings.",
        "Solo per questa pagina. \"Predefinito\" segue le impostazioni.",
        "Nur für diese Seite. \"Standard\" folgt den Einstellungen.",
        "Seulement pour cette page. « Par défaut » suit les Réglages.",
        "Solo para esta página. \"Predeterminado\" sigue los Ajustes.",
        "仅限此页面。“默认”跟随设置。",
        "이 페이지에만 적용됩니다. \"기본값\"은 설정을 따릅니다.",
        "このページだけに適用されます。「デフォルト」は設定に従います。"
    )
    val ignoredWords get() = t("Ignored words", "Parole ignorate", "Ignorierte Wörter", "Mots ignorés", "Palabras ignoradas", "已忽略的单词", "무시한 단어", "無視した単語")
    val ignoredWordsNote get() = t(
        "Words you chose to ignore in this page only.",
        "Le parole che hai scelto di ignorare solo in questa pagina.",
        "Wörter, die du nur auf dieser Seite ignorierst.",
        "Les mots que vous ignorez seulement dans cette page.",
        "Las palabras que ignoras solo en esta página.",
        "仅在此页面中忽略的单词。",
        "이 페이지에서만 무시하는 단어입니다.",
        "このページだけで無視する単語です。"
    )
    val stopIgnoring get() = t("Check again", "Controlla di nuovo", "Wieder prüfen", "Vérifier à nouveau", "Revisar de nuevo", "重新检查", "다시 검사", "再びチェック")
    val loadingDictionary get() = t("Loading the dictionary…", "Carico il dizionario…", "Wörterbuch wird geladen…", "Chargement du dictionnaire…", "Cargando el diccionario…", "正在加载词典…", "사전을 불러오는 중…", "辞書を読み込み中…")
}

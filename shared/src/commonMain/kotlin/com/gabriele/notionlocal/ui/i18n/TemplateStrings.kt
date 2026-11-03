package com.gabriele.notionlocal.ui.i18n

/** Le scritte dei template delle pagine e delle proprietà in cima alla pagina di una riga (dal 26/09/2026). */
object TemplateStrings {

    private fun t(en: String, it: String, de: String, fr: String, es: String, zh: String, ko: String, ja: String) =
        Strings.t(en, it, de, fr, es, zh, ko, ja)

    val templates get() = t("Templates", "Template", "Vorlagen", "Modèles", "Plantillas", "模板", "템플릿", "テンプレート")
    val newTemplate get() = t("New template", "Nuovo template", "Neue Vorlage", "Nouveau modèle", "Nueva plantilla", "新建模板", "새 템플릿", "新しいテンプレート")
    val addExistingTemplate get() = t(
        "Add existing template", "Aggiungi un template esistente", "Vorhandene Vorlage hinzufügen",
        "Ajouter un modèle existant", "Añadir una plantilla existente", "添加现有模板", "기존 템플릿 추가",
        "既存のテンプレートを追加"
    )
    val pickerHint get() = t(
        "Tap anywhere to continue with an empty page, or pick a template",
        "Tocca un punto qualsiasi per continuare con una pagina vuota, o scegli un template",
        "Tippe irgendwo, um mit einer leeren Seite fortzufahren, oder wähle eine Vorlage",
        "Touchez n'importe où pour continuer avec une page vide, ou choisissez un modèle",
        "Toca en cualquier lugar para seguir con una página vacía, o elige una plantilla",
        "点按任意位置以继续使用空白页面，或选择一个模板",
        "아무 곳이나 탭하여 빈 페이지로 계속하거나 템플릿을 선택하세요",
        "どこかをタップして空白のページで続けるか、テンプレートを選択してください"
    )
    val noTemplates get() = t("No templates yet", "Ancora nessun template", "Noch keine Vorlagen", "Aucun modèle pour l'instant", "Todavía no hay plantillas", "还没有模板", "아직 템플릿이 없습니다", "テンプレートはまだありません")
    val noTemplatesHint get() = t(
        "What you write in a template can be added to any new page of a database, in one tap.",
        "Quello che scrivi in un template si può aggiungere con un tocco a ogni pagina nuova di un database.",
        "Was du in einer Vorlage schreibst, kannst du mit einem Tipp jeder neuen Seite einer Datenbank hinzufügen.",
        "Ce que vous écrivez dans un modèle peut être ajouté d'un geste à toute nouvelle page d'une base de données.",
        "Lo que escribes en una plantilla se puede añadir con un toque a cualquier página nueva de una base de datos.",
        "模板中写的内容，只需轻点一下即可添加到数据库的任何新页面中。",
        "템플릿에 쓴 내용은 한 번 탭하여 데이터베이스의 새 페이지에 추가할 수 있습니다.",
        "テンプレートに書いた内容は、ワンタップでデータベースの新しいページに追加できます。"
    )
    val editingTemplate get() = t(
        "You're editing a template", "Stai modificando un template", "Du bearbeitest eine Vorlage",
        "Vous modifiez un modèle", "Estás editando una plantilla", "你正在编辑模板", "템플릿을 편집하고 있습니다",
        "テンプレートを編集しています"
    )
    val untitledTemplate get() = t("Untitled template", "Template senza nome", "Unbenannte Vorlage", "Modèle sans titre", "Plantilla sin título", "未命名模板", "제목 없는 템플릿", "無題のテンプレート")
    val options get() = t("Options", "Opzioni", "Optionen", "Options", "Opciones", "选项", "옵션", "オプション")

    // --- Le proprietà in cima alla pagina di una riga ---

    val addAProperty get() = t("Add a property", "Aggiungi una proprietà", "Eigenschaft hinzufügen", "Ajouter une propriété", "Añadir una propiedad", "添加属性", "속성 추가", "プロパティを追加")
    val hiddenInView get() = t("Hidden in the table", "Nascosta nella tabella", "In der Tabelle ausgeblendet", "Masquée dans le tableau", "Oculta en la tabla", "在表格中隐藏", "표에서 숨겨짐", "テーブルで非表示")
}

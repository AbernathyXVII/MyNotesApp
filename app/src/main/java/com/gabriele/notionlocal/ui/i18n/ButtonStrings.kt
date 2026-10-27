package com.gabriele.notionlocal.ui.i18n

import com.gabriele.notionlocal.data.buttons.ButtonProblem
import com.gabriele.notionlocal.data.buttons.EditOp
import com.gabriele.notionlocal.data.buttons.InsertPlace

/**
 * Le scritte dei bottoni (dal 27/10/2026): il blocco "Button", la proprietà
 * "Button" e la finestra delle azioni. Stessa regola di `Strings`: ogni voce
 * con le sue otto traduzioni.
 */
object ButtonStrings {

    private fun t(en: String, it: String, de: String, fr: String, es: String, zh: String, ko: String, ja: String) =
        Strings.t(en, it, de, fr, es, zh, ko, ja)

    // --- Il bottone ---

    /** Il nome di un bottone che non ne ha ancora uno, come su Notion. */
    val newButton get() = t("New button", "Nuovo pulsante", "Neue Schaltfläche", "Nouveau bouton", "Nuevo botón", "新按钮", "새 버튼", "新しいボタン")
    val editButton get() = t("Edit button", "Modifica pulsante", "Schaltfläche bearbeiten", "Modifier le bouton", "Editar botón", "编辑按钮", "버튼 편집", "ボタンを編集")
    val buttonName get() = t("Button name", "Nome del pulsante", "Name der Schaltfläche", "Nom du bouton", "Nombre del botón", "按钮名称", "버튼 이름", "ボタン名")
    val icon get() = t("Icon", "Icona", "Symbol", "Icône", "Icono", "图标", "아이콘", "アイコン")
    val noIcon get() = t("No icon", "Nessuna icona", "Kein Symbol", "Aucune icône", "Sin icono", "无图标", "아이콘 없음", "アイコンなし")
    val whenTapped get() = t("When tapped", "Quando lo tocchi", "Beim Antippen", "Quand on appuie", "Al tocarlo", "点击时", "탭하면", "タップしたとき")
    val newAction get() = t("New action", "Nuova azione", "Neue Aktion", "Nouvelle action", "Nueva acción", "新操作", "새 작업", "新しいアクション")
    val noActionsYet get() = t(
        "Nothing to do yet: add the first action.",
        "Ancora niente da fare: aggiungi la prima azione.",
        "Noch nichts zu tun: füge die erste Aktion hinzu.",
        "Rien à faire pour l'instant : ajoutez la première action.",
        "Todavía nada que hacer: añade la primera acción.",
        "还没有要做的事：添加第一个操作。",
        "아직 할 일이 없어요. 첫 작업을 추가하세요.",
        "まだ何もしません。最初のアクションを追加してください。"
    )
    val moveUp get() = t("Move up", "Sposta su", "Nach oben", "Monter", "Subir", "上移", "위로 이동", "上へ移動")
    val moveDown get() = t("Move down", "Sposta giù", "Nach unten", "Descendre", "Bajar", "下移", "아래로 이동", "下へ移動")
    val options get() = t("Options", "Opzioni", "Optionen", "Options", "Opciones", "选项", "옵션", "オプション")
    val remove get() = t("Remove", "Rimuovi", "Entfernen", "Retirer", "Quitar", "移除", "제거", "削除")

    // --- Le azioni ---

    val insertBlocks get() = t("Insert blocks", "Inserisci blocchi", "Blöcke einfügen", "Insérer des blocs", "Insertar bloques", "插入块", "블록 삽입", "ブロックを挿入")
    val addPageTo get() = t("Add page to", "Aggiungi pagina a", "Seite hinzufügen zu", "Ajouter une page à", "Añadir página a", "添加页面到", "페이지 추가 위치", "ページを追加")
    val editPagesIn get() = t("Edit pages in", "Modifica pagine in", "Seiten bearbeiten in", "Modifier les pages de", "Editar páginas en", "编辑页面于", "페이지 편집 위치", "ページを編集")
    val showConfirmation get() = t("Show confirmation", "Chiedi conferma", "Bestätigung anzeigen", "Demander une confirmation", "Pedir confirmación", "显示确认", "확인 표시", "確認を表示")
    val openPageOrUrl get() = t("Open page or URL", "Apri pagina o link", "Seite oder URL öffnen", "Ouvrir une page ou une URL", "Abrir página o URL", "打开页面或网址", "페이지 또는 URL 열기", "ページまたはURLを開く")
    val sendNotification get() = t("Send notification", "Manda una notifica", "Benachrichtigung senden", "Envoyer une notification", "Enviar notificación", "发送通知", "알림 보내기", "通知を送信")

    fun placeName(place: InsertPlace): String = when (place) {
        InsertPlace.ABOVE_BUTTON -> t("Above button", "Sopra il pulsante", "Über der Schaltfläche", "Au-dessus du bouton", "Encima del botón", "按钮上方", "버튼 위", "ボタンの上")
        InsertPlace.BELOW_BUTTON -> t("Below button", "Sotto il pulsante", "Unter der Schaltfläche", "Sous le bouton", "Debajo del botón", "按钮下方", "버튼 아래", "ボタンの下")
        InsertPlace.TOP_OF_PAGE -> t("At top of page", "In cima alla pagina", "Oben auf der Seite", "En haut de la page", "Al principio de la página", "页面顶部", "페이지 맨 위", "ページの先頭")
        InsertPlace.BOTTOM_OF_PAGE -> t("At bottom of page", "In fondo alla pagina", "Unten auf der Seite", "En bas de la page", "Al final de la página", "页面底部", "페이지 맨 아래", "ページの末尾")
    }

    val addBlock get() = t("Add block", "Aggiungi blocco", "Block hinzufügen", "Ajouter un bloc", "Añadir bloque", "添加块", "블록 추가", "ブロックを追加")
    val blockTextHint get() = t("Text of the block", "Testo del blocco", "Text des Blocks", "Texte du bloc", "Texto del bloque", "块的文字", "블록 텍스트", "ブロックのテキスト")

    val database get() = t("Database", "Database", "Datenbank", "Base de données", "Base de datos", "数据库", "데이터베이스", "データベース")
    val chooseDatabase get() = t("Choose a database", "Scegli un database", "Datenbank wählen", "Choisir une base de données", "Elegir una base de datos", "选择数据库", "데이터베이스 선택", "データベースを選択")
    val thisPage get() = t("This page", "Questa pagina", "Diese Seite", "Cette page", "Esta página", "此页面", "이 페이지", "このページ")
    val thisPageNote get() = t(
        "The row of the button, or the row whose page holds it.",
        "La riga del pulsante, o quella di cui è la pagina che lo contiene.",
        "Die Zeile der Schaltfläche oder die Zeile, deren Seite sie enthält.",
        "La ligne du bouton, ou celle dont la page le contient.",
        "La fila del botón, o aquella cuya página lo contiene.",
        "按钮所在的行，或包含它的页面所属的行。",
        "버튼이 있는 행, 또는 버튼이 든 페이지의 행이에요.",
        "ボタンの行、またはボタンがあるページの行です。"
    )
    val newPageName get() = t("Name of the new page", "Nome della pagina nuova", "Name der neuen Seite", "Nom de la nouvelle page", "Nombre de la nueva página", "新页面名称", "새 페이지 이름", "新しいページの名前")
    val openAfterwards get() = t("Open it afterwards", "Aprila alla fine", "Danach öffnen", "L'ouvrir ensuite", "Abrirla después", "完成后打开", "끝나면 열기", "最後に開く")
    val allPages get() = t("All pages", "Tutte le pagine", "Alle Seiten", "Toutes les pages", "Todas las páginas", "所有页面", "모든 페이지", "すべてのページ")
    val onlyPagesWhere get() = t("Only pages where", "Solo le pagine in cui", "Nur Seiten, bei denen", "Seulement les pages où", "Solo las páginas donde", "仅限以下页面", "다음 조건의 페이지만", "次の条件のページのみ")
    val isWord get() = t("is", "è", "ist", "est", "es", "是", "값", "が")
    val filterValueHint get() = t("Value", "Valore", "Wert", "Valeur", "Valor", "值", "값", "値")
    val setProperty get() = t("Edit a property", "Cambia una proprietà", "Eigenschaft ändern", "Modifier une propriété", "Cambiar una propiedad", "修改属性", "속성 변경", "プロパティを変更")
    val chooseProperty get() = t("Choose a property", "Scegli una proprietà", "Eigenschaft wählen", "Choisir une propriété", "Elegir una propiedad", "选择属性", "속성 선택", "プロパティを選択")
    val name get() = t("Name", "Nome", "Name", "Nom", "Nombre", "名称", "이름", "名前")
    val chooseTag get() = t("Choose", "Scegli", "Wählen", "Choisir", "Elegir", "选择", "선택", "選択")
    val checked get() = t("Checked", "Spuntata", "Angehakt", "Cochée", "Marcada", "已勾选", "체크됨", "チェックあり")
    val unchecked get() = t("Unchecked", "Non spuntata", "Nicht angehakt", "Non cochée", "Sin marcar", "未勾选", "체크 안 됨", "チェックなし")

    fun opName(op: EditOp): String = when (op) {
        EditOp.SET -> t("Set to", "Imposta a", "Setzen auf", "Définir sur", "Establecer en", "设为", "다음으로 설정", "次に設定")
        EditOp.CLEAR -> t("Clear", "Svuota", "Leeren", "Vider", "Vaciar", "清空", "비우기", "クリア")
        EditOp.CHECK -> t("Check", "Spunta", "Abhaken", "Cocher", "Marcar", "勾选", "체크", "チェックする")
        EditOp.UNCHECK -> t("Uncheck", "Togli la spunta", "Haken entfernen", "Décocher", "Desmarcar", "取消勾选", "체크 해제", "チェックを外す")
        EditOp.TOGGLE -> t("Toggle", "Inverti", "Umschalten", "Inverser", "Alternar", "切换", "반전", "切り替え")
        EditOp.ADD -> t("Add", "Aggiungi", "Hinzufügen", "Ajouter", "Añadir", "添加", "추가", "追加")
        EditOp.REMOVE -> t("Remove", "Togli", "Entfernen", "Retirer", "Quitar", "移除", "제거", "削除")
        EditOp.NOW -> t("Now", "Adesso", "Jetzt", "Maintenant", "Ahora", "现在", "지금", "今")
        EditOp.TODAY -> t("Today", "Oggi", "Heute", "Aujourd'hui", "Hoy", "今天", "오늘", "今日")
    }

    /** Per un numero, "Add" e "Remove" sono "aumenta di" e "diminuisci di". */
    fun numberOpName(op: EditOp): String = when (op) {
        EditOp.ADD -> t("Increase by", "Aumenta di", "Erhöhen um", "Augmenter de", "Aumentar en", "增加", "다음만큼 증가", "増やす")
        EditOp.REMOVE -> t("Decrease by", "Diminuisci di", "Verringern um", "Diminuer de", "Disminuir en", "减少", "다음만큼 감소", "減らす")
        else -> opName(op)
    }

    val message get() = t("Message", "Messaggio", "Nachricht", "Message", "Mensaje", "消息", "메시지", "メッセージ")
    val areYouSure get() = t("Are you sure?", "Sei sicuro?", "Bist du sicher?", "Êtes-vous sûr ?", "¿Estás seguro?", "确定吗？", "확실한가요?", "よろしいですか？")
    val continueLabel get() = t("Continue", "Continua", "Weiter", "Continuer", "Continuar", "继续", "계속", "続ける")
    val page get() = t("Page", "Pagina", "Seite", "Page", "Página", "页面", "페이지", "ページ")
    val link get() = t("Link", "Link", "Link", "Lien", "Enlace", "链接", "링크", "リンク")
    val choosePage get() = t("Choose a page", "Scegli una pagina", "Seite wählen", "Choisir une page", "Elegir una página", "选择页面", "페이지 선택", "ページを選択")
    val title get() = t("Title", "Titolo", "Titel", "Titre", "Título", "标题", "제목", "タイトル")
    val text get() = t("Text", "Testo", "Text", "Texte", "Texto", "文本", "텍스트", "テキスト")
    val titleHint get() = t(
        "Empty: the name of the button",
        "Vuoto: il nome del pulsante",
        "Leer: der Name der Schaltfläche",
        "Vide : le nom du bouton",
        "Vacío: el nombre del botón",
        "留空：按钮名称",
        "비우면 버튼 이름",
        "空欄：ボタン名"
    )

    // --- Quello che manca per salvare ---

    val missingDatabase get() = t("Choose the database", "Scegli il database", "Wähle die Datenbank", "Choisissez la base de données", "Elige la base de datos", "请选择数据库", "데이터베이스를 선택하세요", "データベースを選んでください")
    val missingPageOrLink get() = t("Choose a page or write a link", "Scegli una pagina o scrivi un link", "Wähle eine Seite oder gib einen Link ein", "Choisissez une page ou écrivez un lien", "Elige una página o escribe un enlace", "请选择页面或输入链接", "페이지를 고르거나 링크를 입력하세요", "ページを選ぶかリンクを入力してください")
    val missingProperty get() = t("Choose the property and its value", "Scegli la proprietà e il suo valore", "Wähle die Eigenschaft und ihren Wert", "Choisissez la propriété et sa valeur", "Elige la propiedad y su valor", "请选择属性及其值", "속성과 값을 선택하세요", "プロパティと値を選んでください")
    val missingEdits get() = t("Add a property to change", "Aggiungi una proprietà da cambiare", "Füge eine zu ändernde Eigenschaft hinzu", "Ajoutez une propriété à modifier", "Añade una propiedad que cambiar", "请添加要修改的属性", "바꿀 속성을 추가하세요", "変更するプロパティを追加してください")
    val missingFilter get() = t("Choose the value of the filter", "Scegli il valore del filtro", "Wähle den Wert des Filters", "Choisissez la valeur du filtre", "Elige el valor del filtro", "请选择筛选值", "필터 값을 선택하세요", "フィルターの値を選んでください")
    val missingBlocks get() = t("Add a block to insert", "Aggiungi un blocco da inserire", "Füge einen Block hinzu", "Ajoutez un bloc à insérer", "Añade un bloque que insertar", "请添加要插入的块", "삽입할 블록을 추가하세요", "挿入するブロックを追加してください")
    val fixActions get() = t("Some actions are incomplete", "Alcune azioni sono incomplete", "Einige Aktionen sind unvollständig", "Certaines actions sont incomplètes", "Algunas acciones están incompletas", "部分操作不完整", "완성되지 않은 작업이 있어요", "未完成のアクションがあります")

    // --- Com'è andato un tocco ---

    fun problem(problem: ButtonProblem): String = when (problem) {
        ButtonProblem.NO_DATABASE -> t("The database is gone", "Il database non c'è più", "Die Datenbank gibt es nicht mehr", "La base de données n'existe plus", "La base de datos ya no existe", "数据库已不存在", "데이터베이스가 없어요", "データベースがありません")
        ButtonProblem.LOCKED_DATABASE -> t("The database is locked", "Il database è protetto", "Die Datenbank ist gesperrt", "La base de données est verrouillée", "La base de datos está bloqueada", "数据库已锁定", "데이터베이스가 잠겨 있어요", "データベースはロックされています")
        ButtonProblem.NO_THIS_PAGE -> t(
            "\"This page\" works in a database row or in the page of a row",
            "\"This page\" funziona nella riga di un database o nella pagina di una riga",
            "„Diese Seite“ funktioniert in einer Datenbankzeile oder auf der Seite einer Zeile",
            "« Cette page » fonctionne dans une ligne de base de données ou dans la page d'une ligne",
            "\"Esta página\" funciona en una fila de base de datos o en la página de una fila",
            "“此页面”只适用于数据库行或行的页面",
            "'이 페이지'는 데이터베이스 행이나 행의 페이지에서만 돼요",
            "「このページ」はデータベースの行か行のページで使えます"
        )
        ButtonProblem.NO_PAGE -> t("The page is gone", "La pagina non c'è più", "Die Seite gibt es nicht mehr", "La page n'existe plus", "La página ya no existe", "页面已不存在", "페이지가 없어요", "ページがありません")
        ButtonProblem.LOCKED_PAGE -> t("The page is locked", "La pagina è bloccata", "Die Seite ist gesperrt", "La page est verrouillée", "La página está bloqueada", "页面已锁定", "페이지가 잠겨 있어요", "ページはロックされています")
        ButtonProblem.NO_PROPERTY -> t("A property is gone", "Una proprietà non c'è più", "Eine Eigenschaft gibt es nicht mehr", "Une propriété n'existe plus", "Una propiedad ya no existe", "某个属性已不存在", "속성 하나가 없어요", "プロパティがありません")
        ButtonProblem.BAD_VALUE -> t("A value doesn't fit its property", "Un valore non va bene per la sua proprietà", "Ein Wert passt nicht zur Eigenschaft", "Une valeur ne convient pas à sa propriété", "Un valor no encaja con su propiedad", "某个值与属性不符", "속성에 맞지 않는 값이 있어요", "プロパティに合わない値があります")
        ButtonProblem.NO_FILTER -> t(
            "The filter's property is gone: nothing changed",
            "La proprietà del filtro non c'è più: non è cambiato niente",
            "Die Eigenschaft des Filters fehlt: nichts geändert",
            "La propriété du filtre n'existe plus : rien n'a changé",
            "La propiedad del filtro ya no existe: no cambió nada",
            "筛选属性已不存在：未做任何修改",
            "필터 속성이 없어서 아무것도 바꾸지 않았어요",
            "フィルターのプロパティがないため何も変更していません"
        )
        ButtonProblem.NOTIFICATIONS_OFF -> t("Notifications are off", "Le notifiche sono spente", "Benachrichtigungen sind aus", "Les notifications sont désactivées", "Las notificaciones están desactivadas", "通知已关闭", "알림이 꺼져 있어요", "通知がオフです")
    }

    /** Le righe nate in un database, dette alla fine del tocco. */
    fun pagesAdded(count: Int): String = if (count == 1) {
        t("1 page added", "1 pagina aggiunta", "1 Seite hinzugefügt", "1 page ajoutée", "1 página añadida", "已添加 1 个页面", "페이지 1개 추가됨", "1 ページ追加しました")
    } else {
        t("$count pages added", "$count pagine aggiunte", "$count Seiten hinzugefügt", "$count pages ajoutées", "$count páginas añadidas", "已添加 $count 个页面", "페이지 ${count}개 추가됨", "$count ページ追加しました")
    }

    /** Le righe cambiate in un database scelto (non "This page", che si vede). */
    fun pagesEdited(count: Int): String = if (count == 1) {
        t("1 page edited", "1 pagina modificata", "1 Seite bearbeitet", "1 page modifiée", "1 página editada", "已编辑 1 个页面", "페이지 1개 편집됨", "1 ページ編集しました")
    } else {
        t("$count pages edited", "$count pagine modificate", "$count Seiten bearbeitet", "$count pages modifiées", "$count páginas editadas", "已编辑 $count 个页面", "페이지 ${count}개 편집됨", "$count ページ編集しました")
    }

    // --- I collegamenti ---

    val linkInAnotherPage get() = t("Link in another page", "Collega in un'altra pagina", "In einer anderen Seite verlinken", "Lier dans une autre page", "Enlazar en otra página", "在其他页面中链接", "다른 페이지에 연결", "別のページにリンク")
    val linkInAnotherPageHint get() = t(
        "A linked copy goes at the bottom of the page you choose: it runs the same actions, and editing one edits both.",
        "In fondo alla pagina che scegli va una copia collegata: esegue le stesse azioni, e modificandone una cambiano tutte e due.",
        "Am Ende der gewählten Seite erscheint eine verknüpfte Kopie: Sie führt dieselben Aktionen aus, und Änderungen gelten für beide.",
        "Une copie liée s'ajoute en bas de la page choisie : elle lance les mêmes actions, et en modifier une modifie les deux.",
        "Al final de la página que elijas va una copia enlazada: ejecuta las mismas acciones, y editar una cambia las dos.",
        "所选页面底部会出现一个链接副本：它执行相同的操作，编辑其中一个会同时修改两个。",
        "고른 페이지 맨 아래에 연결된 복사본이 생겨요. 같은 작업을 하고, 하나를 고치면 둘 다 바뀌어요.",
        "選んだページの末尾にリンクされたコピーが追加されます。同じアクションを実行し、片方を編集すると両方が変わります。"
    )
    val linkedTo get() = t("Linked copy added", "Copia collegata aggiunta", "Verknüpfte Kopie hinzugefügt", "Copie liée ajoutée", "Copia enlazada añadida", "已添加链接副本", "연결된 복사본을 추가했어요", "リンクされたコピーを追加しました")
    val goToOriginal get() = t("Go to original", "Vai all'originale", "Zum Original", "Aller à l'original", "Ir al original", "转到原始按钮", "원본으로 이동", "元のボタンへ移動")
    val originalDeleted get() = t("Deleted button", "Pulsante eliminato", "Gelöschte Schaltfläche", "Bouton supprimé", "Botón eliminado", "已删除的按钮", "삭제된 버튼", "削除されたボタン")
    val originalDeletedNote get() = t(
        "The original button is gone: this linked copy no longer does anything.",
        "Il pulsante originale non c'è più: questa copia collegata non fa più niente.",
        "Die ursprüngliche Schaltfläche gibt es nicht mehr: Diese Kopie tut nichts mehr.",
        "Le bouton d'origine n'existe plus : cette copie liée ne fait plus rien.",
        "El botón original ya no existe: esta copia enlazada ya no hace nada.",
        "原始按钮已不存在：此链接副本不再执行任何操作。",
        "원본 버튼이 없어서 이 연결된 복사본은 아무것도 하지 않아요.",
        "元のボタンがないため、このリンクされたコピーは何もしません。"
    )
    val linkedNote get() = t(
        "Linked copy: changes apply to the original and to all its copies.",
        "Copia collegata: le modifiche valgono per l'originale e per tutte le sue copie.",
        "Verknüpfte Kopie: Änderungen gelten für das Original und alle Kopien.",
        "Copie liée : les modifications s'appliquent à l'original et à toutes ses copies.",
        "Copia enlazada: los cambios se aplican al original y a todas sus copias.",
        "链接副本：修改会应用到原始按钮及其所有副本。",
        "연결된 복사본: 바꾸면 원본과 모든 복사본에 적용돼요.",
        "リンクされたコピー：変更は元のボタンとすべてのコピーに反映されます。"
    )
    val linkToButton get() = t("Link to button", "Collegamento a pulsante", "Link zu Schaltfläche", "Lien vers un bouton", "Enlace a botón", "链接到按钮", "버튼 링크", "ボタンへのリンク")
    val linkToButtonHint get() = t(
        "Choose the button: here goes a linked copy that runs its actions.",
        "Scegli il pulsante: qui va una copia collegata che esegue le sue azioni.",
        "Wähle die Schaltfläche: Hier entsteht eine verknüpfte Kopie, die ihre Aktionen ausführt.",
        "Choisissez le bouton : une copie liée qui lance ses actions va ici.",
        "Elige el botón: aquí va una copia enlazada que ejecuta sus acciones.",
        "选择按钮：这里会放一个执行其操作的链接副本。",
        "버튼을 고르세요. 여기에 그 작업을 하는 연결된 복사본이 생겨요.",
        "ボタンを選んでください。ここにそのアクションを実行するリンクされたコピーが入ります。"
    )
    val noButtonsYet get() = t(
        "There are no buttons yet: create one with \"Button\".",
        "Non ci sono ancora pulsanti: creane uno con \"Button\".",
        "Es gibt noch keine Schaltflächen: Erstelle eine mit „Button“.",
        "Il n'y a pas encore de boutons : créez-en un avec « Button ».",
        "Aún no hay botones: crea uno con \"Button\".",
        "还没有按钮：用“Button”创建一个。",
        "아직 버튼이 없어요. \"Button\"으로 만드세요.",
        "まだボタンがありません。「Button」で作成してください。"
    )

    // --- La proprietà Button ---

    val buttonLabel get() = t("Button label", "Scritta sul pulsante", "Beschriftung", "Libellé du bouton", "Texto del botón", "按钮文字", "버튼 레이블", "ボタンのラベル")
    val editActions get() = t("Edit actions", "Modifica azioni", "Aktionen bearbeiten", "Modifier les actions", "Editar acciones", "编辑操作", "작업 편집", "アクションを編集")
    fun actionsCount(count: Int): String = when (count) {
        0 -> t("No actions", "Nessuna azione", "Keine Aktionen", "Aucune action", "Sin acciones", "无操作", "작업 없음", "アクションなし")
        1 -> t("1 action", "1 azione", "1 Aktion", "1 action", "1 acción", "1 个操作", "작업 1개", "アクション 1 件")
        else -> t("$count actions", "$count azioni", "$count Aktionen", "$count actions", "$count acciones", "$count 个操作", "작업 ${count}개", "アクション $count 件")
    }
}

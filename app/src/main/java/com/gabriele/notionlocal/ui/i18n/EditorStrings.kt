package com.gabriele.notionlocal.ui.i18n

/**
 * Le voci dell'editor delle pagine: i tipi di blocco (menu "/" e "+"), la
 * barra sopra la tastiera, le immagini, il menu degli elenchi numerati, il
 * selettore dei colori e la cronologia. Stessa regola di `Strings`: ogni
 * voce con le sue otto traduzioni, e solo testo dell'app.
 */
object EditorStrings {

    private fun t(en: String, it: String, de: String, fr: String, es: String, zh: String, ko: String, ja: String) =
        Strings.t(en, it, de, fr, es, zh, ko, ja)

    // --- Font e corpo della pagina, nella barra Aa ---

    /** La voce del font quando la pagina non ne ha scelto uno: quello di sistema. */
    val defaultFont get() = t("Default", "Predefinito", "Standard", "Par défaut", "Predeterminado", "默认", "기본", "デフォルト")
    val font get() = t("Font", "Carattere", "Schriftart", "Police", "Fuente", "字体", "글꼴", "フォント")
    val fontSize get() = t("Font size", "Dimensione del carattere", "Schriftgröße", "Taille de police", "Tamaño de fuente", "字号", "글꼴 크기", "フォントサイズ")
    fun fontSizeRange(min: Int, max: Int) = t("From $min to $max", "Da $min a $max", "Von $min bis $max", "De $min à $max", "De $min a $max", "$min 到 $max", "$min~$max", "$min～$max")

    // --- Il menu di un blocco (sei puntini nella barra, o il dito tenuto su una pagina) ---

    val blockOptions get() = t("Block options", "Opzioni del blocco", "Blockoptionen", "Options du bloc", "Opciones del bloque", "块选项", "블록 옵션", "ブロックのオプション")
    val turnInto get() = t("Turn into", "Trasforma in", "Umwandeln in", "Transformer en", "Convertir en", "转换为", "전환", "変換")
    val color get() = t("Color", "Colore", "Farbe", "Couleur", "Color", "颜色", "색상", "カラー")
    val editIcon get() = t("Edit icon", "Modifica icona", "Symbol bearbeiten", "Modifier l'icône", "Editar icono", "编辑图标", "아이콘 편집", "アイコンを編集")
    val addToFavorites get() = t("Add to favorites", "Aggiungi ai preferiti", "Zu Favoriten hinzufügen", "Ajouter aux favoris", "Añadir a favoritos", "添加到收藏", "즐겨찾기에 추가", "お気に入りに追加")
    val removeFromFavorites get() = t("Remove from favorites", "Togli dai preferiti", "Aus Favoriten entfernen", "Retirer des favoris", "Quitar de favoritos", "从收藏中移除", "즐겨찾기에서 제거", "お気に入りから削除")
    val rename get() = t("Rename", "Rinomina", "Umbenennen", "Renommer", "Cambiar nombre", "重命名", "이름 바꾸기", "名前を変更")
    val turnIntoSimpleDatabase get() = t("Turn into simple database", "Trasforma in database semplice", "In einfache Datenbank umwandeln", "Convertir en base de données simple", "Convertir en base de datos simple", "转换为简单数据库", "단순 데이터베이스로 전환", "シンプルなデータベースに変換")
    val turnIntoComplexDatabase get() = t("Turn into complex database", "Trasforma in database complesso", "In komplexe Datenbank umwandeln", "Convertir en base de données complexe", "Convertir en base de datos compleja", "转换为复杂数据库", "복합 데이터베이스로 전환", "複合データベースに変換")
    val lockDatabase get() = t("Lock database", "Blocca il database", "Datenbank sperren", "Verrouiller la base de données", "Bloquear base de datos", "锁定数据库", "데이터베이스 잠금", "データベースをロック")
    val openAsPage get() = t("Open as page", "Apri come pagina", "Als Seite öffnen", "Ouvrir en tant que page", "Abrir como página", "以页面打开", "페이지로 열기", "ページとして開く")
    val simpleDatabaseLossTitle get() = t("Turn into simple database?", "Trasformare in database semplice?", "In einfache Datenbank umwandeln?", "Convertir en base de données simple ?", "¿Convertir en base de datos simple?", "转换为简单数据库？", "단순 데이터베이스로 전환할까요?", "シンプルなデータベースに変換しますか？")

    /** Quante pagine di riga con del contenuto "Turn into simple database" cancellerebbe per sempre. */
    fun simpleDatabaseLossText(n: Int) = if (n == 1) {
        t(
            "1 row page has content: it will be deleted forever, with everything inside it. Row names and properties stay.",
            "1 pagina di riga ha del contenuto: verrà cancellata per sempre, con tutto quello che c'è dentro. I nomi delle righe e le proprietà restano.",
            "1 Zeilenseite hat Inhalt: Sie wird endgültig gelöscht, mit allem, was darin ist. Zeilennamen und Eigenschaften bleiben erhalten.",
            "1 page de ligne a du contenu : elle sera supprimée définitivement, avec tout ce qu'elle contient. Les noms des lignes et les propriétés restent.",
            "1 página de fila tiene contenido: se eliminará para siempre, con todo lo que contiene. Los nombres de las filas y las propiedades se mantienen.",
            "1 个行页面有内容：它将被永久删除，连同其中的所有内容。行名称和属性会保留。",
            "행 페이지 1개에 내용이 있습니다. 안의 내용과 함께 영구적으로 삭제됩니다. 행 이름과 속성은 유지됩니다.",
            "内容のある行ページが 1 件あります。中身ごと完全に削除されます。行の名前とプロパティは残ります。"
        )
    } else {
        t(
            "$n row pages have content: they will be deleted forever, with everything inside them. Row names and properties stay.",
            "$n pagine di riga hanno del contenuto: verranno cancellate per sempre, con tutto quello che c'è dentro. I nomi delle righe e le proprietà restano.",
            "$n Zeilenseiten haben Inhalt: Sie werden endgültig gelöscht, mit allem, was darin ist. Zeilennamen und Eigenschaften bleiben erhalten.",
            "$n pages de ligne ont du contenu : elles seront supprimées définitivement, avec tout ce qu'elles contiennent. Les noms des lignes et les propriétés restent.",
            "$n páginas de fila tienen contenido: se eliminarán para siempre, con todo lo que contienen. Los nombres de las filas y las propiedades se mantienen.",
            "$n 个行页面有内容：它们将被永久删除，连同其中的所有内容。行名称和属性会保留。",
            "행 페이지 ${n}개에 내용이 있습니다. 안의 내용과 함께 영구적으로 삭제됩니다. 행 이름과 속성은 유지됩니다.",
            "内容のある行ページが $n 件あります。中身ごと完全に削除されます。行の名前とプロパティは残ります。"
        )
    }

    /**
     * Il nome di un tipo di blocco nella lingua dell'app, partendo da quello
     * inglese. I cataloghi dei menu restano scritti in inglese — è il nome
     * con cui li si riconosce nel codice e con cui si cerca nel menu "/" —
     * e si traducono solo quando si mostrano.
     */
    fun blockType(english: String): String = when (english) {
        "Text" -> t("Text", "Testo", "Text", "Texte", "Texto", "文本", "텍스트", "テキスト")
        "Bulleted list" -> t("Bulleted list", "Elenco puntato", "Aufzählung", "Liste à puces", "Lista con viñetas", "项目符号列表", "글머리 기호 목록", "箇条書きリスト")
        "Numbered list" -> t("Numbered list", "Elenco numerato", "Nummerierte Liste", "Liste numérotée", "Lista numerada", "编号列表", "번호 매기기 목록", "番号付きリスト")
        "To-do list" -> t("To-do list", "Elenco di cose da fare", "To-do-Liste", "Liste de tâches", "Lista de tareas", "待办清单", "할 일 목록", "ToDoリスト")
        "Toggle list" -> t("Toggle list", "Elenco a scomparsa", "Aufklappliste", "Liste dépliante", "Lista desplegable", "折叠列表", "토글 목록", "トグルリスト")
        "Table" -> t("Table", "Tabella", "Tabelle", "Tableau", "Tabla", "表格", "표", "テーブル")
        "Page" -> t("Page", "Pagina", "Seite", "Page", "Página", "页面", "페이지", "ページ")
        "Database" -> t("Database", "Database", "Datenbank", "Base de données", "Base de datos", "数据库", "데이터베이스", "データベース")
        "Divider" -> t("Divider", "Divisore", "Trennlinie", "Séparateur", "Divisor", "分割线", "구분선", "区切り線")
        "Callout" -> t("Callout", "Riquadro", "Hinweisbox", "Encadré", "Destacado", "标注", "콜아웃", "コールアウト")
        "Quote" -> t("Quote", "Citazione", "Zitat", "Citation", "Cita", "引用", "인용", "引用")
        "Link to page" -> t("Link to page", "Link a una pagina", "Link zu einer Seite", "Lien vers une page", "Enlace a una página", "链接到页面", "페이지 링크", "ページへのリンク")
        "Image" -> t("Image", "Immagine", "Bild", "Image", "Imagen", "图片", "이미지", "画像")
        "Video" -> t("Video", "Video", "Video", "Vidéo", "Vídeo", "视频", "동영상", "動画")
        "Audio" -> t("Audio", "Audio", "Audio", "Audio", "Audio", "音频", "오디오", "音声")
        "Code" -> t("Code", "Codice", "Code", "Code", "Código", "代码", "코드", "コード")
        "File" -> t("File", "File", "Datei", "Fichier", "Archivo", "文件", "파일", "ファイル")
        "Web bookmark" -> t("Web bookmark", "Segnalibro web", "Web-Lesezeichen", "Signet web", "Marcador web", "网页书签", "웹 북마크", "Webブックマーク")
        "Table view" -> t("Table view", "Vista tabella", "Tabellenansicht", "Vue tableau", "Vista de tabla", "表格视图", "표 보기", "テーブルビュー")
        "Board view" -> t("Board view", "Vista bacheca", "Board-Ansicht", "Vue Kanban", "Vista de tablero", "看板视图", "보드 보기", "ボードビュー")
        "Gallery view" -> t("Gallery view", "Vista galleria", "Galerieansicht", "Vue galerie", "Vista de galería", "画廊视图", "갤러리 보기", "ギャラリービュー")
        "List view" -> t("List view", "Vista elenco", "Listenansicht", "Vue liste", "Vista de lista", "列表视图", "목록 보기", "リストビュー")
        "Calendar view" -> t("Calendar view", "Vista calendario", "Kalenderansicht", "Vue calendrier", "Vista de calendario", "日历视图", "캘린더 보기", "カレンダービュー")
        "Timeline view" -> t("Timeline view", "Vista linea temporale", "Zeitleistenansicht", "Vue chronologie", "Vista de cronograma", "时间线视图", "타임라인 보기", "タイムラインビュー")
        "Database - Inline" -> t("Database - Inline", "Database - In linea", "Datenbank – Inline", "Base de données – Intégrée", "Base de datos – En línea", "数据库 - 内嵌", "데이터베이스 - 인라인", "データベース - インライン")
        "Database - Full page" -> t("Database - Full page", "Database - Pagina intera", "Datenbank – Ganze Seite", "Base de données – Pleine page", "Base de datos – Página completa", "数据库 - 整页", "데이터베이스 - 전체 페이지", "データベース - フルページ")
        "Simple database" -> t("Simple database", "Database semplice", "Einfache Datenbank", "Base de données simple", "Base de datos simple", "简单数据库", "간단한 데이터베이스", "シンプルなデータベース")
        "Linked view of data source" -> t("Linked view of data source", "Vista collegata di un'origine dati", "Verknüpfte Ansicht einer Datenquelle", "Vue liée d'une source de données", "Vista vinculada de una fuente de datos", "数据源的关联视图", "데이터 소스의 연결된 보기", "データソースのリンクビュー")
        "Basic blocks" -> t("Basic blocks", "Blocchi di base", "Basisblöcke", "Blocs de base", "Bloques básicos", "基础块", "기본 블록", "基本ブロック")
        "Media" -> t("Media", "Media", "Medien", "Médias", "Multimedia", "媒体", "미디어", "メディア")
        else -> english
    }

    val nothingWithThatName get() = t("Nothing with that name", "Niente con questo nome", "Nichts mit diesem Namen", "Rien avec ce nom", "Nada con ese nombre", "没有匹配的项目", "해당 이름의 항목이 없습니다", "該当する項目はありません")

    // --- La barra sopra la tastiera ---

    val spoiler get() = t("Spoiler", "Spoiler", "Spoiler", "Spoiler", "Spoiler", "剧透", "스포일러", "ネタバレ")
    val outdent get() = t("Outdent", "Riduci rientro", "Einzug verkleinern", "Diminuer le retrait", "Reducir sangría", "减少缩进", "내어쓰기", "インデントを減らす")
    val indent get() = t("Indent", "Aumenta rientro", "Einzug vergrößern", "Augmenter le retrait", "Aumentar sangría", "增加缩进", "들여쓰기", "インデントを増やす")
    val moveUp get() = t("Move up", "Sposta su", "Nach oben", "Monter", "Subir", "上移", "위로 이동", "上へ移動")
    val moveDown get() = t("Move down", "Sposta giù", "Nach unten", "Descendre", "Bajar", "下移", "아래로 이동", "下へ移動")
    val addOrChangeBlock get() = t("Add or change block type", "Aggiungi o cambia tipo di blocco", "Blocktyp hinzufügen oder ändern", "Ajouter ou changer le type de bloc", "Añadir o cambiar el tipo de bloque", "添加或更改块类型", "블록 유형 추가 또는 변경", "ブロックの種類を追加・変更")
    val textAndBackgroundColor get() = t("Text and background color", "Colore del testo e dello sfondo", "Text- und Hintergrundfarbe", "Couleur du texte et du fond", "Color del texto y del fondo", "文字和背景颜色", "글자 및 배경 색상", "文字と背景の色")
    val undo get() = t("Undo", "Annulla", "Rückgängig", "Annuler", "Deshacer", "撤销", "실행 취소", "元に戻す")
    val redo get() = t("Redo", "Ripeti", "Wiederholen", "Rétablir", "Rehacer", "重做", "다시 실행", "やり直す")
    val deleteEmptyBlock get() = t("Delete empty block", "Elimina il blocco vuoto", "Leeren Block löschen", "Supprimer le bloc vide", "Eliminar el bloque vacío", "删除空块", "빈 블록 삭제", "空のブロックを削除")

    // --- Icona e copertina ---

    val icon get() = t("Icon", "Icona", "Symbol", "Icône", "Icono", "图标", "아이콘", "アイコン")
    val cover get() = t("Cover", "Copertina", "Titelbild", "Couverture", "Portada", "封面", "커버", "カバー")
    val addCover get() = t("Add cover", "Aggiungi copertina", "Titelbild hinzufügen", "Ajouter une couverture", "Añadir portada", "添加封面", "커버 추가", "カバーを追加")
    val addIcon get() = t("Add icon", "Aggiungi icona", "Symbol hinzufügen", "Ajouter une icône", "Añadir icono", "添加图标", "아이콘 추가", "アイコンを追加")
    val upload get() = t("Upload", "Carica", "Hochladen", "Importer", "Subir", "上传", "업로드", "アップロード")
    val link get() = t("Link", "Link", "Link", "Lien", "Enlace", "链接", "링크", "リンク")
    val downloading get() = t("Downloading…", "Download in corso…", "Wird geladen…", "Téléchargement…", "Descargando…", "正在下载…", "다운로드 중…", "ダウンロード中…")
    val pasteImageLink get() = t("Paste an image link", "Incolla il link di un'immagine", "Bildlink einfügen", "Collez le lien d'une image", "Pega el enlace de una imagen", "粘贴图片链接", "이미지 링크 붙여넣기", "画像のリンクを貼り付け")
    val reposition get() = t("Reposition", "Riposiziona", "Neu positionieren", "Repositionner", "Reposicionar", "调整位置", "위치 조정", "位置を調整")
    val remove get() = t("Remove", "Rimuovi", "Entfernen", "Retirer", "Quitar", "移除", "제거", "削除")
    val dragToMove get() = t("Drag to move · pinch to zoom", "Trascina per spostare · pizzica per ingrandire", "Ziehen zum Verschieben · Zwei Finger zum Zoomen", "Glissez pour déplacer · pincez pour zoomer", "Arrastra para mover · pellizca para ampliar", "拖动以移动 · 双指缩放", "드래그하여 이동 · 핀치하여 확대", "ドラッグで移動・ピンチで拡大")
    val save get() = t("Save", "Salva", "Speichern", "Enregistrer", "Guardar", "保存", "저장", "保存")
    val imageReadFailed get() = t("Couldn't read the image.", "Non sono riuscito a leggere l'immagine.", "Das Bild konnte nicht gelesen werden.", "Impossible de lire l'image.", "No se pudo leer la imagen.", "无法读取图片。", "이미지를 읽을 수 없습니다.", "画像を読み込めませんでした。")
    val linkFailed get() = t("The link didn't work.", "Il collegamento non ha funzionato.", "Der Link hat nicht funktioniert.", "Le lien n'a pas fonctionné.", "El enlace no funcionó.", "链接无效。", "링크가 작동하지 않았습니다.", "リンクが機能しませんでした。")

    // --- Blocchi e finestre dell'editor ---

    val emptyToggle get() = t(
        "Empty toggle. Tap to add a block inside.",
        "Toggle vuoto. Tocca per aggiungere un blocco.",
        "Leerer Toggle. Tippe, um einen Block hinzuzufügen.",
        "Liste dépliante vide. Touchez pour ajouter un bloc.",
        "Desplegable vacío. Toca para añadir un bloque.",
        "空的折叠列表。点按以在其中添加块。",
        "빈 토글입니다. 탭하여 블록을 추가하세요.",
        "空のトグルです。タップしてブロックを追加します。"
    )
    val removeLink get() = t("Remove link", "Rimuovi collegamento", "Link entfernen", "Supprimer le lien", "Quitar enlace", "移除链接", "링크 제거", "リンクを削除")
    val deleteDividerTitle get() = t("Delete this divider?", "Eliminare questo divisore?", "Diese Trennlinie löschen?", "Supprimer ce séparateur ?", "¿Eliminar este divisor?", "删除此分割线？", "이 구분선을 삭제할까요?", "この区切り線を削除しますか？")
    val deleteTable get() = t("Delete table", "Elimina tabella", "Tabelle löschen", "Supprimer le tableau", "Eliminar tabla", "删除表格", "표 삭제", "テーブルを削除")
    val deleteDatabaseTitle get() = t("Delete this database?", "Eliminare questo database?", "Diese Datenbank löschen?", "Supprimer cette base de données ?", "¿Eliminar esta base de datos?", "删除此数据库？", "이 데이터베이스를 삭제할까요?", "このデータベースを削除しますか？")
    val deleteDatabaseText get() = t(
        "The database, its properties and all its pages will be permanently deleted. This can't be undone.",
        "Il database, le sue proprietà e tutte le sue pagine verranno eliminati definitivamente. Non si può annullare.",
        "Die Datenbank, ihre Eigenschaften und alle ihre Seiten werden endgültig gelöscht. Das kann nicht rückgängig gemacht werden.",
        "La base de données, ses propriétés et toutes ses pages seront supprimées définitivement. Action irréversible.",
        "La base de datos, sus propiedades y todas sus páginas se eliminarán definitivamente. No se puede deshacer.",
        "该数据库、其属性及其所有页面都将被永久删除，且无法撤销。",
        "데이터베이스와 속성, 모든 페이지가 영구 삭제됩니다. 되돌릴 수 없습니다.",
        "データベースとそのプロパティ、すべてのページが完全に削除されます。元に戻すことはできません。"
    )

    // --- Il menu degli elenchi numerati ---

    val beginNewList get() = t("Begin a new list here", "Inizia qui un nuovo elenco", "Hier eine neue Liste beginnen", "Commencer une nouvelle liste ici", "Empezar una lista nueva aquí", "从这里开始新列表", "여기서 새 목록 시작", "ここから新しいリストを開始")
    val renumberContinuation get() = t("Renumber as continuation of previous list", "Rinumera come continuazione dell'elenco precedente", "Als Fortsetzung der vorherigen Liste nummerieren", "Renuméroter à la suite de la liste précédente", "Renumerar como continuación de la lista anterior", "接续上一个列表编号", "이전 목록에 이어서 번호 매기기", "前のリストの続きとして番号を振り直す")
    val removeNumber get() = t("Remove number", "Rimuovi il numero", "Nummer entfernen", "Supprimer le numéro", "Quitar el número", "移除编号", "번호 제거", "番号を削除")
    val changeToBullet get() = t("Change to bullet", "Cambia in elenco puntato", "In Aufzählungspunkt ändern", "Changer en puce", "Cambiar a viñeta", "改为项目符号", "글머리 기호로 변경", "箇条書きに変更")

    // --- Il pennello ---

    val colorText get() = t("Text", "Testo", "Text", "Texte", "Texto", "文字", "글자", "文字")
    val colorBackground get() = t("Background", "Sfondo", "Hintergrund", "Fond", "Fondo", "背景", "배경", "背景")
    val textColor get() = t("Text color", "Colore del testo", "Textfarbe", "Couleur du texte", "Color del texto", "文字颜色", "글자 색상", "文字の色")
    val backgroundColor get() = t("Background color", "Colore dello sfondo", "Hintergrundfarbe", "Couleur du fond", "Color del fondo", "背景颜色", "배경 색상", "背景の色")
    val red get() = t("Red", "Rosso", "Rot", "Rouge", "Rojo", "红", "빨강", "赤")
    val green get() = t("Green", "Verde", "Grün", "Vert", "Verde", "绿", "초록", "緑")
    val blue get() = t("Blue", "Blu", "Blau", "Bleu", "Azul", "蓝", "파랑", "青")
    val noColor get() = t("No color", "Nessun colore", "Keine Farbe", "Aucune couleur", "Sin color", "无颜色", "색상 없음", "色なし")
    val apply get() = t("Apply", "Applica", "Anwenden", "Appliquer", "Aplicar", "应用", "적용", "適用")

    // --- La cronologia ("Updates") ---

    fun noChangesYet(title: String) = t(
        "No changes recorded for \"$title\" yet.\n\nWhatever is written or corrected from now on shows up here: one line per edit, not one per keystroke.",
        "Nessuna modifica registrata per \"$title\".\n\nQuello che scrivi o correggi da ora in poi compare qui: una riga per modifica, non una per tasto premuto.",
        "Für „$title“ wurden noch keine Änderungen aufgezeichnet.\n\nAlles, was ab jetzt geschrieben oder korrigiert wird, erscheint hier: eine Zeile pro Änderung, nicht pro Tastendruck.",
        "Aucune modification enregistrée pour « $title » pour l'instant.\n\nTout ce qui est écrit ou corrigé à partir de maintenant apparaîtra ici : une ligne par modification, pas une par touche.",
        "Todavía no hay cambios registrados en «$title».\n\nTodo lo que escribas o corrijas a partir de ahora aparecerá aquí: una línea por cambio, no una por tecla.",
        "“$title”还没有记录任何修改。\n\n从现在起写入或更正的内容都会显示在这里：每次修改一行，而不是每次按键一行。",
        "\"$title\"에 기록된 변경 사항이 아직 없습니다.\n\n지금부터 작성하거나 수정한 내용이 여기에 표시됩니다. 키 입력마다가 아니라 수정마다 한 줄씩 기록됩니다.",
        "「$title」の変更履歴はまだありません。\n\nこれから書いたり修正したりした内容がここに表示されます。キー入力ごとではなく、変更ごとに1行ずつ記録されます。"
    )
    val thisPage get() = t("this page", "questa pagina", "diese Seite", "cette page", "esta página", "此页面", "이 페이지", "このページ")
    val editAdded get() = t("added", "aggiunto", "hinzugefügt", "ajouté", "añadido", "新增", "추가됨", "追加")
    val editDeleted get() = t("deleted", "eliminato", "gelöscht", "supprimé", "eliminado", "删除", "삭제됨", "削除")
    val editTitle get() = t("title", "titolo", "Titel", "titre", "título", "标题", "제목", "タイトル")
}

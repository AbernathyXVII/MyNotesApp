package com.gabriele.notionlocal.ui.i18n

import com.gabriele.notionlocal.data.entity.CalendarMode
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseLayout
import com.gabriele.notionlocal.data.entity.GalleryCardPreview
import com.gabriele.notionlocal.data.entity.GalleryCardSize
import com.gabriele.notionlocal.data.entity.RecurrenceFreq
import com.gabriele.notionlocal.data.entity.TimelineZoom
import com.gabriele.notionlocal.data.settings.AppLanguage
import com.gabriele.notionlocal.data.settings.AppSettings
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Le voci dei database: tipi di proprietà, viste, filtri, ordinamento,
 * tag, date che si ripetono, impostazioni. Stessa regola di `Strings`.
 */
object DbStrings {

    private fun t(en: String, it: String, de: String, fr: String, es: String, zh: String, ko: String, ja: String) =
        Strings.t(en, it, de, fr, es, zh, ko, ja)

    // --- Nomi di tipi, viste, zoom ---

    /** Il nome inglese di un tipo: è quello che si salva quando una proprietà nasce senza nome. */
    fun englishTypeName(type: ColumnType): String = when (type) {
        ColumnType.TEXT -> "Text"
        ColumnType.NUMBER -> "Number"
        ColumnType.SELECT -> "Select"
        ColumnType.MULTI_SELECT -> "Multi-select"
        ColumnType.DATE -> "Date"
        ColumnType.CHECKBOX -> "Checkbox"
        ColumnType.URL -> "URL"
        ColumnType.EMAIL -> "Email"
        ColumnType.PHONE -> "Phone"
        ColumnType.CREATED_TIME -> "Created time"
        ColumnType.LAST_EDITED_TIME -> "Last edited time"
    }

    fun typeName(type: ColumnType): String = when (type) {
        ColumnType.TEXT -> t("Text", "Testo", "Text", "Texte", "Texto", "文本", "텍스트", "テキスト")
        ColumnType.NUMBER -> t("Number", "Numero", "Zahl", "Nombre", "Número", "数字", "숫자", "数値")
        ColumnType.SELECT -> t("Select", "Selezione", "Auswahl", "Sélection", "Selección", "单选", "선택", "セレクト")
        ColumnType.MULTI_SELECT -> t("Multi-select", "Selezione multipla", "Mehrfachauswahl", "Sélection multiple", "Selección múltiple", "多选", "다중 선택", "マルチセレクト")
        ColumnType.DATE -> t("Date", "Data", "Datum", "Date", "Fecha", "日期", "날짜", "日付")
        ColumnType.CHECKBOX -> t("Checkbox", "Casella", "Kontrollkästchen", "Case à cocher", "Casilla", "复选框", "체크박스", "チェックボックス")
        ColumnType.URL -> t("URL", "URL", "URL", "URL", "URL", "网址", "URL", "URL")
        ColumnType.EMAIL -> t("Email", "Email", "E-Mail", "E-mail", "Correo", "电子邮件", "이메일", "メール")
        ColumnType.PHONE -> t("Phone", "Telefono", "Telefon", "Téléphone", "Teléfono", "电话", "전화", "電話")
        ColumnType.CREATED_TIME -> t("Created time", "Data di creazione", "Erstellt am", "Date de création", "Fecha de creación", "创建时间", "생성 일시", "作成日時")
        ColumnType.LAST_EDITED_TIME -> t("Last edited time", "Ultima modifica", "Zuletzt bearbeitet", "Dernière modification", "Última edición", "上次编辑时间", "마지막 편집 일시", "最終更新日時")
    }

    /**
     * Il nome di una proprietà come si mostra.
     *
     * Una proprietà lasciata senza nome prende quello inglese del suo tipo
     * ("Number", "Date"): quel nome l'ha scritto l'app, non l'utente, e si
     * traduce. Qualunque altro nome è dell'utente e resta com'è — anche se
     * per caso fosse "Date" scritto a mano: non c'è modo di distinguerli,
     * ed è lo stesso nome che l'app avrebbe dato.
     */
    fun columnName(stored: String): String =
        ColumnType.entries.firstOrNull { englishTypeName(it) == stored }?.let { typeName(it) } ?: stored

    fun layoutName(layout: DatabaseLayout): String = when (layout) {
        DatabaseLayout.TABLE -> t("Table", "Tabella", "Tabelle", "Tableau", "Tabla", "表格", "표", "テーブル")
        DatabaseLayout.BOARD -> t("Board", "Bacheca", "Board", "Kanban", "Tablero", "看板", "보드", "ボード")
        DatabaseLayout.CALENDAR -> t("Calendar", "Calendario", "Kalender", "Calendrier", "Calendario", "日历", "캘린더", "カレンダー")
        DatabaseLayout.TIMELINE -> t("Timeline", "Linea temporale", "Zeitleiste", "Chronologie", "Cronograma", "时间线", "타임라인", "タイムライン")
        DatabaseLayout.LIST -> t("List", "Elenco", "Liste", "Liste", "Lista", "列表", "목록", "リスト")
        DatabaseLayout.GALLERY -> t("Gallery", "Galleria", "Galerie", "Galerie", "Galería", "画廊", "갤러리", "ギャラリー")
    }

    fun galleryPreviewName(preview: GalleryCardPreview): String = when (preview) {
        GalleryCardPreview.NONE -> none
        GalleryCardPreview.PAGE_COVER -> t("Page cover", "Copertina della pagina", "Titelbild der Seite", "Couverture de la page", "Portada de la página", "页面封面", "페이지 커버", "ページカバー")
        GalleryCardPreview.PAGE_CONTENT -> t("Page content", "Contenuto della pagina", "Seiteninhalt", "Contenu de la page", "Contenido de la página", "页面内容", "페이지 콘텐츠", "ページコンテンツ")
    }

    // Al femminile dove la lingua lo chiede: si parla di "scheda",
    // "carte", "tarjeta".
    fun gallerySizeName(size: GalleryCardSize): String = when (size) {
        GalleryCardSize.SMALL -> t("Small", "Piccola", "Klein", "Petite", "Pequeña", "小", "작게", "小")
        GalleryCardSize.MEDIUM -> t("Medium", "Media", "Mittel", "Moyenne", "Mediana", "中", "중간", "中")
        GalleryCardSize.LARGE -> t("Large", "Grande", "Groß", "Grande", "Grande", "大", "크게", "大")
    }

    fun calendarModeName(mode: CalendarMode): String = when (mode) {
        CalendarMode.YEAR -> year
        CalendarMode.MONTH -> month
        CalendarMode.WEEK -> week
    }

    fun zoomName(zoom: TimelineZoom): String = when (zoom) {
        TimelineZoom.HOURS -> t("Hours", "Ore", "Stunden", "Heures", "Horas", "小时", "시간", "時間")
        TimelineZoom.DAY -> day
        TimelineZoom.WEEK -> week
        TimelineZoom.BI_WEEK -> t("Bi-week", "Due settimane", "Zwei Wochen", "Deux semaines", "Dos semanas", "双周", "2주", "2週間")
        TimelineZoom.MONTH -> month
        TimelineZoom.QUARTER -> t("Quarter", "Trimestre", "Quartal", "Trimestre", "Trimestre", "季度", "분기", "四半期")
        TimelineZoom.YEAR -> year
    }

    val year get() = t("Year", "Anno", "Jahr", "Année", "Año", "年", "년", "年")
    val month get() = t("Month", "Mese", "Monat", "Mois", "Mes", "月", "월", "月")
    val week get() = t("Week", "Settimana", "Woche", "Semaine", "Semana", "周", "주", "週")
    val day get() = t("Day", "Giorno", "Tag", "Jour", "Día", "日", "일", "日")

    /** Il nome di un colore dei tag. */
    fun tagColorName(english: String): String = when (english) {
        "Default" -> t("Default", "Predefinito", "Standard", "Par défaut", "Predeterminado", "默认", "기본", "デフォルト")
        "Gray" -> t("Gray", "Grigio", "Grau", "Gris", "Gris", "灰色", "회색", "グレー")
        "Brown" -> t("Brown", "Marrone", "Braun", "Marron", "Marrón", "棕色", "갈색", "ブラウン")
        "Orange" -> t("Orange", "Arancione", "Orange", "Orange", "Naranja", "橙色", "주황색", "オレンジ")
        "Yellow" -> t("Yellow", "Giallo", "Gelb", "Jaune", "Amarillo", "黄色", "노란색", "イエロー")
        "Green" -> t("Green", "Verde", "Grün", "Vert", "Verde", "绿色", "초록색", "グリーン")
        "Blue" -> t("Blue", "Blu", "Blau", "Bleu", "Azul", "蓝色", "파란색", "ブルー")
        "Purple" -> t("Purple", "Viola", "Lila", "Violet", "Morado", "紫色", "보라색", "パープル")
        "Pink" -> t("Pink", "Rosa", "Rosa", "Rose", "Rosa", "粉色", "분홍색", "ピンク")
        "Red" -> t("Red", "Rosso", "Rot", "Rouge", "Rojo", "红色", "빨간색", "レッド")
        else -> english
    }

    // --- Contenuto ---

    val name get() = t("Name", "Nome", "Name", "Nom", "Nombre", "名称", "이름", "名前")
    val none get() = t("None", "Nessuno", "Keine", "Aucun", "Ninguno", "无", "없음", "なし")
    val newPage get() = t("New page", "Nuova pagina", "Neue Seite", "Nouvelle page", "Nueva página", "新建页面", "새 페이지", "新規ページ")
    val newShort get() = t("New", "Nuova", "Neu", "Nouvelle", "Nueva", "新建", "새로 만들기", "新規")
    val open get() = t("OPEN", "APRI", "ÖFFNEN", "OUVRIR", "ABRIR", "打开", "열기", "開く")
    val deletePageTitle get() = t("Delete this page?", "Eliminare questa pagina?", "Diese Seite löschen?", "Supprimer cette page ?", "¿Eliminar esta página?", "删除此页面？", "이 페이지를 삭제할까요?", "このページを削除しますか？")
    val deleteRowText get() = t(
        "The row and its values will be deleted. This can't be undone.",
        "La riga e i suoi valori verranno eliminati. Non si può annullare.",
        "Die Zeile und ihre Werte werden gelöscht. Das kann nicht rückgängig gemacht werden.",
        "La ligne et ses valeurs seront supprimées. Action irréversible.",
        "Se eliminarán la fila y sus valores. No se puede deshacer.",
        "该行及其值将被删除，且无法撤销。",
        "행과 값이 삭제됩니다. 되돌릴 수 없습니다.",
        "行とその値が削除されます。元に戻すことはできません。"
    )
    val expandGroup get() = t("Expand group", "Espandi gruppo", "Gruppe aufklappen", "Déplier le groupe", "Expandir grupo", "展开分组", "그룹 펼치기", "グループを展開")
    val collapseGroup get() = t("Collapse group", "Comprimi gruppo", "Gruppe zuklappen", "Replier le groupe", "Contraer grupo", "折叠分组", "그룹 접기", "グループを折りたたむ")
    fun noValue(property: String) = t("No $property", "Senza $property", "Ohne $property", "Sans $property", "Sin $property", "无$property", "$property 없음", "$property なし")
    val checked get() = t("Checked", "Spuntata", "Abgehakt", "Cochée", "Marcada", "已勾选", "선택됨", "チェック済み")
    val unchecked get() = t("Unchecked", "Non spuntata", "Nicht abgehakt", "Non cochée", "Sin marcar", "未勾选", "선택 안 됨", "未チェック")
    val previous get() = t("Previous", "Precedente", "Zurück", "Précédent", "Anterior", "上一个", "이전", "前へ")
    val next get() = t("Next", "Successivo", "Weiter", "Suivant", "Siguiente", "下一个", "다음", "次へ")
    val today get() = t("Today", "Oggi", "Heute", "Aujourd'hui", "Hoy", "今天", "오늘", "今日")
    fun noDate(count: Int) = t("No date ($count)", "Senza data ($count)", "Ohne Datum ($count)", "Sans date ($count)", "Sin fecha ($count)", "无日期（$count）", "날짜 없음 ($count)", "日付なし（$count）")

    val boardNeedsSelect get() = t(
        "A board puts each page in a column, so it needs a Select property to group by.",
        "Una bacheca mette ogni pagina in una colonna, quindi le serve una proprietà Selezione per raggruppare.",
        "Ein Board ordnet jede Seite einer Spalte zu und braucht dafür eine Auswahl-Eigenschaft zum Gruppieren.",
        "Un tableau Kanban place chaque page dans une colonne : il lui faut une propriété Sélection pour regrouper.",
        "Un tablero coloca cada página en una columna, así que necesita una propiedad Selección para agrupar.",
        "看板会把每个页面放进一列，因此需要一个“单选”属性来分组。",
        "보드는 각 페이지를 열에 배치하므로 그룹화할 선택 속성이 필요합니다.",
        "ボードは各ページを列に配置するため、グループ化に使うセレクトプロパティが必要です。"
    )
    val addSelectProperty get() = t("Add a Select property", "Aggiungi una proprietà Selezione", "Auswahl-Eigenschaft hinzufügen", "Ajouter une propriété Sélection", "Añadir una propiedad Selección", "添加单选属性", "선택 속성 추가", "セレクトプロパティを追加")
    val timelineNeedsDate get() = t(
        "A timeline stretches each page over the days it covers, so it needs a Date property.",
        "Una linea temporale distende ogni pagina sui giorni che copre, quindi le serve una proprietà Data.",
        "Eine Zeitleiste zieht jede Seite über die Tage, die sie umfasst, und braucht daher eine Datumseigenschaft.",
        "Une chronologie étale chaque page sur les jours qu'elle couvre : il lui faut une propriété Date.",
        "Un cronograma extiende cada página por los días que abarca, así que necesita una propiedad Fecha.",
        "时间线会把每个页面铺开到它覆盖的日期上，因此需要一个日期属性。",
        "타임라인은 각 페이지를 해당 기간에 걸쳐 표시하므로 날짜 속성이 필요합니다.",
        "タイムラインは各ページを期間にわたって表示するため、日付プロパティが必要です。"
    )
    val addDateProperty get() = t("Add a Date property", "Aggiungi una proprietà Data", "Datumseigenschaft hinzufügen", "Ajouter une propriété Date", "Añadir una propiedad Fecha", "添加日期属性", "날짜 속성 추가", "日付プロパティを追加")
    val noPageHasDate get() = t(
        "No page has a date yet. Give one a date and it will show up here as a bar.",
        "Nessuna pagina ha ancora una data. Dagliene una e comparirà qui come una barra.",
        "Noch keine Seite hat ein Datum. Gib einer Seite ein Datum, dann erscheint sie hier als Balken.",
        "Aucune page n'a encore de date. Donnez-en une à une page et elle apparaîtra ici sous forme de barre.",
        "Ninguna página tiene fecha todavía. Ponle una a alguna y aparecerá aquí como una barra.",
        "还没有页面设置日期。为页面设置日期后，它会在这里显示为一条横条。",
        "아직 날짜가 있는 페이지가 없습니다. 페이지에 날짜를 지정하면 여기에 막대로 표시됩니다.",
        "日付のあるページはまだありません。日付を設定すると、ここにバーとして表示されます。"
    )
    val selectZoomLevel get() = t("Select a zoom level", "Scegli un livello di zoom", "Zoomstufe wählen", "Choisir un niveau de zoom", "Elegir nivel de zoom", "选择缩放级别", "확대 수준 선택", "ズームレベルを選択")

    // --- Barra della vista ---

    val changeLayout get() = t("Change layout", "Cambia vista", "Ansicht ändern", "Changer de vue", "Cambiar vista", "更改视图", "보기 변경", "ビューを変更")
    val filter get() = t("Filter", "Filtro", "Filter", "Filtre", "Filtro", "筛选", "필터", "フィルター")
    val sort get() = t("Sort", "Ordina", "Sortieren", "Trier", "Ordenar", "排序", "정렬", "並べ替え")
    val openFullPage get() = t("Open as full page", "Apri a pagina intera", "Als ganze Seite öffnen", "Ouvrir en pleine page", "Abrir a página completa", "以整页打开", "전체 페이지로 열기", "フルページで開く")
    val databaseSettings get() = t("Database settings", "Impostazioni del database", "Datenbankeinstellungen", "Paramètres de la base de données", "Ajustes de la base de datos", "数据库设置", "데이터베이스 설정", "データベースの設定")
    val removeSort get() = t("Remove sort", "Rimuovi ordinamento", "Sortierung entfernen", "Supprimer le tri", "Quitar orden", "移除排序", "정렬 제거", "並べ替えを解除")
    val sortByAnother get() = t("Sort by another property", "Ordina per un'altra proprietà", "Nach anderer Eigenschaft sortieren", "Trier par une autre propriété", "Ordenar por otra propiedad", "按其他属性排序", "다른 속성으로 정렬", "別のプロパティで並べ替え")

    // --- Filtro ---

    val removeFilter get() = t("Remove filter", "Rimuovi filtro", "Filter entfernen", "Supprimer le filtre", "Quitar filtro", "移除筛选", "필터 제거", "フィルターを解除")
    val filterNeedsProperty get() = t(
        "Filtering picks the pages whose property has a value you choose, so it needs a Select, Multi-select, Date, Number or Checkbox property.",
        "Il filtro sceglie le pagine la cui proprietà ha un valore a tua scelta, quindi serve una proprietà Selezione, Selezione multipla, Data, Numero o Casella.",
        "Der Filter wählt Seiten, deren Eigenschaft einen gewählten Wert hat, und braucht daher eine Eigenschaft vom Typ Auswahl, Mehrfachauswahl, Datum, Zahl oder Kontrollkästchen.",
        "Le filtre retient les pages dont la propriété a la valeur choisie : il faut donc une propriété Sélection, Sélection multiple, Date, Nombre ou Case à cocher.",
        "El filtro elige las páginas cuya propiedad tiene el valor que eliges, así que necesita una propiedad Selección, Selección múltiple, Fecha, Número o Casilla.",
        "筛选会挑出属性等于所选值的页面，因此需要单选、多选、日期、数字或复选框属性。",
        "필터는 속성 값이 선택한 값인 페이지를 고르므로 선택, 다중 선택, 날짜, 숫자 또는 체크박스 속성이 필요합니다.",
        "フィルターは、プロパティが選んだ値のページを抽出します。セレクト、マルチセレクト、日付、数値、チェックボックスのいずれかのプロパティが必要です。"
    )
    fun noOptionsYet(property: String) = t(
        "\"$property\" has no options yet, so there is nothing to filter by.",
        "\"$property\" non ha ancora opzioni, quindi non c'è niente da filtrare.",
        "„$property“ hat noch keine Optionen, daher gibt es nichts zu filtern.",
        "« $property » n'a pas encore d'options : il n'y a rien à filtrer.",
        "«$property» todavía no tiene opciones, así que no hay nada que filtrar.",
        "“$property”还没有选项，因此无可筛选。",
        "\"$property\"에 아직 옵션이 없어 필터링할 항목이 없습니다.",
        "「$property」にはまだ選択肢がないため、絞り込めるものがありません。"
    )
    val pagesWithAny get() = t("Pages with any of these", "Pagine con almeno uno di questi", "Seiten mit einem dieser Werte", "Pages ayant l'une de ces valeurs", "Páginas con alguno de estos", "包含其中任一项的页面", "다음 중 하나가 있는 페이지", "いずれかを含むページ")
    val pagesWithNumberRange get() = t("Pages with a number in this range", "Pagine con un numero in questo intervallo", "Seiten mit einer Zahl in diesem Bereich", "Pages dont le nombre est dans cet intervalle", "Páginas con un número en este rango", "数字在此范围内的页面", "이 범위의 숫자가 있는 페이지", "この範囲の数値を持つページ")
    val from get() = t("From", "Da", "Von", "De", "Desde", "从", "시작", "開始")
    val to get() = t("To", "A", "Bis", "À", "Hasta", "到", "끝", "終了")
    val numberRangeHint get() = t(
        "Leave one side empty for \"from here on\" or \"up to here\". Pages without a number never pass.",
        "Lascia vuoto un lato per \"da qui in poi\" o \"fino a qui\". Le pagine senza numero non passano mai.",
        "Lass eine Seite leer für „ab hier“ oder „bis hier“. Seiten ohne Zahl werden nie angezeigt.",
        "Laissez un côté vide pour « à partir d'ici » ou « jusqu'ici ». Les pages sans nombre ne sont jamais retenues.",
        "Deja un lado vacío para «desde aquí» o «hasta aquí». Las páginas sin número nunca pasan.",
        "留空一侧表示“从此开始”或“到此为止”。没有数字的页面永远不会显示。",
        "\"여기부터\" 또는 \"여기까지\"로 하려면 한쪽을 비워 두세요. 숫자가 없는 페이지는 표시되지 않습니다.",
        "「ここから」や「ここまで」にするには片方を空欄にします。数値のないページは表示されません。"
    )
    val pagesWithBox get() = t("Pages with the box like this", "Pagine con la casella così", "Seiten mit so gesetztem Kästchen", "Pages avec la case ainsi", "Páginas con la casilla así", "复选框为此状态的页面", "체크박스가 이 상태인 페이지", "チェックボックスがこの状態のページ")
    val pagesOnDays get() = t("Pages on these days", "Pagine in questi giorni", "Seiten an diesen Tagen", "Pages de ces jours", "Páginas en estos días", "这些日期的页面", "이 날짜의 페이지", "この日付のページ")
    val pickADay get() = t("Pick a day", "Scegli un giorno", "Tag wählen", "Choisir un jour", "Elige un día", "选择日期", "날짜 선택", "日を選択")
    val sameDay get() = t("Same day", "Stesso giorno", "Gleicher Tag", "Même jour", "Mismo día", "同一天", "같은 날", "同じ日")
    val filterSingleDay get() = t("Filter a single day", "Filtra un giorno solo", "Nur einen Tag filtern", "Filtrer un seul jour", "Filtrar un solo día", "只筛选一天", "하루만 필터", "1日だけ絞り込む")

    // --- Proprietà ---

    val editProperty get() = t("Edit property", "Modifica proprietà", "Eigenschaft bearbeiten", "Modifier la propriété", "Editar propiedad", "编辑属性", "속성 편집", "プロパティを編集")
    val alignLeft get() = t("Align left", "Allinea a sinistra", "Linksbündig", "Aligner à gauche", "Alinear a la izquierda", "左对齐", "왼쪽 정렬", "左揃え")
    val center get() = t("Center", "Centra", "Zentrieren", "Centrer", "Centrar", "居中", "가운데 정렬", "中央揃え")
    val hide get() = t("Hide", "Nascondi", "Ausblenden", "Masquer", "Ocultar", "隐藏", "숨기기", "非表示")
    val addProperty get() = t("Add property", "Aggiungi proprietà", "Eigenschaft hinzufügen", "Ajouter une propriété", "Añadir propiedad", "添加属性", "속성 추가", "プロパティを追加")
    val newProperty get() = t("New property", "Nuova proprietà", "Neue Eigenschaft", "Nouvelle propriété", "Nueva propiedad", "新属性", "새 속성", "新しいプロパティ")
    val add get() = t("Add", "Aggiungi", "Hinzufügen", "Ajouter", "Añadir", "添加", "추가", "追加")
    val propertyName get() = t("Property name", "Nome della proprietà", "Name der Eigenschaft", "Nom de la propriété", "Nombre de la propiedad", "属性名称", "속성 이름", "プロパティ名")
    val type get() = t("Type", "Tipo", "Typ", "Type", "Tipo", "类型", "유형", "種類")
    val moveLeft get() = t("Move to left","Sposta a sinistra", "Nach links", "Déplacer à gauche", "Mover a la izquierda", "左移", "왼쪽으로 이동", "左へ移動")
    val moveRight get() = t("Move to right","Sposta a destra", "Nach rechts", "Déplacer à droite", "Mover a la derecha", "右移", "오른쪽으로 이동", "右へ移動")
    val deleteProperty get() = t("Delete property", "Elimina proprietà", "Eigenschaft löschen", "Supprimer la propriété", "Eliminar propiedad", "删除属性", "속성 삭제", "プロパティを削除")
    val noPropertiesYet get() = t("This database has no properties yet.", "Questo database non ha ancora proprietà.", "Diese Datenbank hat noch keine Eigenschaften.", "Cette base de données n'a pas encore de propriétés.", "Esta base de datos aún no tiene propiedades.", "此数据库还没有属性。", "이 데이터베이스에는 아직 속성이 없습니다.", "このデータベースにはまだプロパティがありません。")

    // --- Tag ---

    val tagName get() = t("Tag name", "Nome del tag", "Tag-Name", "Nom du tag", "Nombre de la etiqueta", "标签名称", "태그 이름", "タグ名")
    val colors get() = t("Colors", "Colori", "Farben", "Couleurs", "Colores", "颜色", "색상", "色")
    val selected get() = t("Selected", "Selezionato", "Ausgewählt", "Sélectionné", "Seleccionado", "已选择", "선택됨", "選択済み")
    val createATag get() = t("Create a tag", "Crea un tag", "Tag erstellen", "Créer un tag", "Crear una etiqueta", "创建标签", "태그 만들기", "タグを作成")
    val create get() = t("Create", "Crea", "Erstellen", "Créer", "Crear", "创建", "만들기", "作成")
    val selectExistingOption get() = t("Select an existing option", "Scegli un'opzione esistente", "Vorhandene Option wählen", "Choisir une option existante", "Elegir una opción existente", "选择现有选项", "기존 옵션 선택", "既存の選択肢を選ぶ")
    val noTagsYet get() = t(
        "No tags yet — write above to create the first one.",
        "Ancora nessun tag: scrivi qui sopra per creare il primo.",
        "Noch keine Tags – schreib oben, um den ersten zu erstellen.",
        "Aucun tag pour l'instant — écrivez ci-dessus pour créer le premier.",
        "Aún no hay etiquetas: escribe arriba para crear la primera.",
        "还没有标签——在上方输入即可创建第一个。",
        "아직 태그가 없습니다. 위에 입력해 첫 태그를 만드세요.",
        "タグはまだありません。上に入力して最初のタグを作成します。"
    )
    val noTagMatches get() = t("No tag matches what you wrote.", "Nessun tag corrisponde a quello che hai scritto.", "Kein Tag passt zu deiner Eingabe.", "Aucun tag ne correspond à ce que vous avez écrit.", "Ninguna etiqueta coincide con lo que escribiste.", "没有与输入内容匹配的标签。", "입력한 내용과 일치하는 태그가 없습니다.", "入力内容に一致するタグはありません。")
    val editTag get() = t("Edit tag", "Modifica tag", "Tag bearbeiten", "Modifier le tag", "Editar etiqueta", "编辑标签", "태그 편집", "タグを編集")

    // --- Data e ripetizioni ---

    val includeTime get() = t("Include time", "Includi l'ora", "Uhrzeit einschließen", "Inclure l'heure", "Incluir hora", "包含时间", "시간 포함", "時刻を含める")
    val startTime get() = t("Start time", "Ora di inizio", "Startzeit", "Heure de début", "Hora de inicio", "开始时间", "시작 시간", "開始時刻")
    val endTime get() = t("End time", "Ora di fine", "Endzeit", "Heure de fin", "Hora de fin", "结束时间", "종료 시간", "終了時刻")
    val repeat get() = t("Repeat", "Ripeti", "Wiederholen", "Répéter", "Repetir", "重复", "반복", "繰り返し")
    val clear get() = t("Clear", "Cancella", "Löschen", "Effacer", "Borrar", "清除", "지우기", "クリア")
    val ok get() = t("OK", "OK", "OK", "OK", "Aceptar", "确定", "확인", "OK")
    val doesNotRepeat get() = t("Does not repeat", "Non si ripete", "Keine Wiederholung", "Ne se répète pas", "No se repite", "不重复", "반복 안 함", "繰り返さない")
    val daily get() = t("Daily", "Ogni giorno", "Täglich", "Tous les jours", "Diariamente", "每天", "매일", "毎日")
    fun weeklyOn(days: String) = t("Weekly on $days", "Ogni settimana di $days", "Wöchentlich am $days", "Chaque semaine le $days", "Cada semana el $days", "每周的$days", "매주 $days", "毎週$days")
    fun monthlyOn(what: String) = t("Monthly on $what", "Ogni mese $what", "Monatlich $what", "Chaque mois $what", "Cada mes $what", "每月$what", "매월 $what", "毎月$what")
    fun annuallyOn(date: String) = t("Annually on $date", "Ogni anno il $date", "Jährlich am $date", "Chaque année le $date", "Cada año el $date", "每年的$date", "매년 $date", "毎年$date")
    val everyWeekday get() = t("Every weekday (Monday to Friday)", "Ogni giorno feriale (dal lunedì al venerdì)", "Jeden Werktag (Montag bis Freitag)", "Tous les jours ouvrés (du lundi au vendredi)", "Todos los días laborables (de lunes a viernes)", "每个工作日（周一至周五）", "매 평일 (월요일~금요일)", "毎平日（月曜〜金曜）")
    val everyWeekdayShort get() = t("every weekday", "ogni giorno feriale", "jeden Werktag", "tous les jours ouvrés", "todos los días laborables", "每个工作日", "평일", "平日")
    val custom get() = t("Custom…", "Personalizzata…", "Benutzerdefiniert…", "Personnalisée…", "Personalizada…", "自定义…", "사용자 지정…", "カスタム…")
    val customRecurrence get() = t("Custom recurrence", "Ripetizione personalizzata", "Benutzerdefinierte Wiederholung", "Répétition personnalisée", "Repetición personalizada", "自定义重复", "사용자 지정 반복", "カスタムの繰り返し")
    val repeatEvery get() = t("Repeat every", "Ripeti ogni", "Wiederholen alle", "Répéter tous les", "Repetir cada", "重复间隔", "반복 주기", "繰り返す間隔")
    val changeUnit get() = t("Change unit", "Cambia unità", "Einheit ändern", "Changer d'unité", "Cambiar unidad", "更改单位", "단위 변경", "単位を変更")
    val repeatOn get() = t("Repeat on", "Ripeti di", "Wiederholen am", "Répéter le", "Repetir el", "重复于", "반복 요일", "繰り返す曜日")
    val ends get() = t("Ends", "Termina", "Endet", "Se termine", "Termina", "结束", "종료", "終了")
    val never get() = t("Never", "Mai", "Nie", "Jamais", "Nunca", "永不", "없음", "なし")
    val on get() = t("On", "Il", "Am", "Le", "El", "于", "날짜", "日付")
    val after get() = t("After", "Dopo", "Nach", "Après", "Después de", "重复", "반복", "回数")
    val times get() = t("times", "volte", "Mal", "fois", "veces", "次后", "회 후", "回")
    val everyDay get() = t("Every day", "Ogni giorno", "Jeden Tag", "Tous les jours", "Todos los días", "每天", "매일", "毎日")
    fun everyNDays(n: Int) = t("Every $n days", "Ogni $n giorni", "Alle $n Tage", "Tous les $n jours", "Cada $n días", "每${n}天", "${n}일마다", "${n}日ごと")
    fun everyNWeeksOn(n: Int, days: String) = t("Every $n weeks on $days", "Ogni $n settimane di $days", "Alle $n Wochen am $days", "Toutes les $n semaines le $days", "Cada $n semanas el $days", "每${n}周的$days", "${n}주마다 $days", "${n}週ごとの$days")
    fun everyNMonthsOn(n: Int, what: String) = t("Every $n months on $what", "Ogni $n mesi $what", "Alle $n Monate $what", "Tous les $n mois $what", "Cada $n meses $what", "每${n}个月$what", "${n}개월마다 $what", "${n}か月ごと$what")
    fun everyNYearsOn(n: Int, date: String) = t("Every $n years on $date", "Ogni $n anni il $date", "Alle $n Jahre am $date", "Tous les $n ans le $date", "Cada $n años el $date", "每${n}年的$date", "${n}년마다 $date", "${n}年ごとの$date")
    fun until(date: String) = t(", until $date", ", fino al $date", ", bis $date", ", jusqu'au $date", ", hasta el $date", "，直到$date", ", ${date}까지", "、${date}まで")
    fun nTimes(n: Int) = t(", $n times", ", $n volte", ", $n Mal", ", $n fois", ", $n veces", "，共${n}次", ", ${n}회", "、${n}回")

    /** "il giorno 5": il giorno del mese dentro una frase sulle ripetizioni. */
    fun dayOfMonth(n: Int) = t("day $n", "il giorno $n", "am $n.", "le $n", "el día $n", "${n}日", "${n}일", "${n}日")

    /** "On day 5": la stessa cosa come voce da scegliere. */
    fun onDayOfMonth(n: Int) = t("On day $n", "Il giorno $n", "Am $n.", "Le $n", "El día $n", "${n}日", "${n}일", "${n}日")

    /**
     * "il primo lunedì", "l'ultima domenica": quale settimana del mese e che
     * giorno. In italiano la domenica è femminile e vuole "la prima", non
     * "il primo".
     */
    fun nthWeekday(date: LocalDate, dayName: String, capitalized: Boolean = false): String {
        val last = date.plusWeeks(1).month != date.month
        val index = if (last) 4 else ((date.dayOfMonth - 1) / 7).coerceAtMost(3)
        val feminine = date.dayOfWeek == DayOfWeek.SUNDAY
        val phrase = when (AppSettings.language) {
            AppLanguage.ENGLISH -> "the ${listOf("first", "second", "third", "fourth", "last")[index]} $dayName"
            AppLanguage.ITALIAN -> if (feminine) {
                "la ${listOf("prima", "seconda", "terza", "quarta", "ultima")[index]} $dayName"
            } else {
                "il ${listOf("primo", "secondo", "terzo", "quarto", "ultimo")[index]} $dayName"
            }
            AppLanguage.GERMAN -> "am ${listOf("ersten", "zweiten", "dritten", "vierten", "letzten")[index]} $dayName"
            AppLanguage.FRENCH -> "le ${listOf("premier", "deuxième", "troisième", "quatrième", "dernier")[index]} $dayName"
            AppLanguage.SPANISH -> "el ${listOf("primer", "segundo", "tercer", "cuarto", "último")[index]} $dayName"
            AppLanguage.CHINESE -> "${listOf("第一个", "第二个", "第三个", "第四个", "最后一个")[index]}$dayName"
            AppLanguage.KOREAN -> "${listOf("첫째", "둘째", "셋째", "넷째", "마지막")[index]} $dayName"
            AppLanguage.JAPANESE -> "${listOf("第1", "第2", "第3", "第4", "最終")[index]}$dayName"
        }
        return if (capitalized) phrase.replaceFirstChar { it.uppercase(AppSettings.language.locale) } else phrase
    }

    /** L'unità di una ripetizione, al singolare o al plurale. */
    fun unit(freq: RecurrenceFreq, plural: Boolean): String = when (freq) {
        RecurrenceFreq.DAILY -> if (plural) t("days", "giorni", "Tage", "jours", "días", "天", "일", "日") else t("day", "giorno", "Tag", "jour", "día", "天", "일", "日")
        RecurrenceFreq.WEEKLY -> if (plural) t("weeks", "settimane", "Wochen", "semaines", "semanas", "周", "주", "週") else t("week", "settimana", "Woche", "semaine", "semana", "周", "주", "週")
        RecurrenceFreq.MONTHLY -> if (plural) t("months", "mesi", "Monate", "mois", "meses", "个月", "개월", "か月") else t("month", "mese", "Monat", "mois", "mes", "个月", "개월", "か月")
        RecurrenceFreq.YEARLY -> if (plural) t("years", "anni", "Jahre", "ans", "años", "年", "년", "年") else t("year", "anno", "Jahr", "an", "año", "年", "년", "年")
    }

    // --- Azioni sulla riga ---

    val actions get() = t("Actions", "Azioni", "Aktionen", "Actions", "Acciones", "操作", "작업", "操作")
    fun created(date: String) = t("Created $date", "Creata il $date", "Erstellt am $date", "Créée le $date", "Creada el $date", "创建于 $date", "$date 생성", "$date に作成")
    fun lastEdited(date: String) = t("Last edited $date", "Ultima modifica il $date", "Zuletzt bearbeitet am $date", "Modifiée le $date", "Editada el $date", "最后编辑于 $date", "$date 마지막 편집", "$date に最終更新")

    // --- Impostazioni del database ---

    val editProperties get() = t("Edit properties", "Modifica le proprietà", "Eigenschaften bearbeiten", "Modifier les propriétés", "Editar propiedades", "编辑属性", "속성 편집", "プロパティを編集")
    val layout get() = t("Layout", "Vista", "Layout", "Disposition", "Diseño", "布局", "레이아웃", "レイアウト")
    val groupBy get() = t("Group by", "Raggruppa per", "Gruppieren nach", "Regrouper par", "Agrupar por", "分组依据", "그룹화 기준", "グループ化")
    val group get() = t("Group", "Gruppo", "Gruppe", "Groupe", "Grupo", "分组", "그룹", "グループ")
    val propertyVisibility get() = t("Property visibility", "Visibilità delle proprietà", "Sichtbarkeit der Eigenschaften", "Visibilité des propriétés", "Visibilidad de propiedades", "属性可见性", "속성 표시", "プロパティの表示")
    val dateProperty get() = t("Date property", "Proprietà data", "Datumseigenschaft", "Propriété de date", "Propiedad de fecha", "日期属性", "날짜 속성", "日付プロパティ")
    val hideEmptyGroups get() = t("Hide empty groups", "Nascondi i gruppi vuoti", "Leere Gruppen ausblenden", "Masquer les groupes vides", "Ocultar grupos vacíos", "隐藏空分组", "빈 그룹 숨기기", "空のグループを非表示")
    val allShown get() = t("All shown", "Tutte visibili", "Alle sichtbar", "Toutes affichées", "Todas visibles", "全部显示", "모두 표시", "すべて表示")
    fun nHidden(n: Int) = t("$n hidden", "$n nascoste", "$n ausgeblendet", "$n masquées", "$n ocultas", "已隐藏 $n 个", "${n}개 숨김", "${n}件非表示")
    val change get() = t("Change", "Cambia", "Ändern", "Changer", "Cambiar", "更改", "변경", "変更")
    val showTitle get() = t("Show title", "Mostra il titolo", "Titel anzeigen", "Afficher le titre", "Mostrar título", "显示标题", "제목 표시", "タイトルを表示")
    val cardPreview get() = t("Card preview", "Anteprima della scheda", "Kartenvorschau", "Aperçu de la carte", "Vista previa de la tarjeta", "卡片预览", "카드 미리보기", "カードプレビュー")
    val cardSize get() = t("Card size", "Dimensione della scheda", "Kartengröße", "Taille de la carte", "Tamaño de la tarjeta", "卡片大小", "카드 크기", "カードサイズ")
    val calendarNeedsDate get() = t(
        "A calendar places rows by a Date property, and this database doesn't have one yet.",
        "Un calendario colloca le righe in base a una proprietà Data, e questo database non ne ha ancora una.",
        "Ein Kalender ordnet Zeilen nach einer Datumseigenschaft an, und diese Datenbank hat noch keine.",
        "Un calendrier place les lignes selon une propriété Date, et cette base de données n'en a pas encore.",
        "Un calendario coloca las filas según una propiedad Fecha, y esta base de datos aún no tiene ninguna.",
        "日历会按日期属性排列各行，而此数据库还没有日期属性。",
        "캘린더는 날짜 속성으로 행을 배치하는데, 이 데이터베이스에는 아직 날짜 속성이 없습니다.",
        "カレンダーは日付プロパティで行を配置しますが、このデータベースにはまだありません。"
    )
    val groupingNeedsProperty get() = t(
        "Grouping puts pages with the same value together, so it needs a property to look at.",
        "Il raggruppamento mette insieme le pagine con lo stesso valore, quindi serve una proprietà da guardare.",
        "Beim Gruppieren werden Seiten mit gleichem Wert zusammengefasst; dafür braucht es eine Eigenschaft.",
        "Le regroupement réunit les pages ayant la même valeur : il faut donc une propriété à examiner.",
        "Agrupar junta las páginas con el mismo valor, así que necesita una propiedad en la que fijarse.",
        "分组会把值相同的页面放在一起，因此需要一个可参照的属性。",
        "그룹화는 값이 같은 페이지를 모으므로 기준이 될 속성이 필요합니다.",
        "グループ化は同じ値のページをまとめるため、基準となるプロパティが必要です。"
    )
    val boardNeedsSelectSettings get() = t(
        "A board groups rows by a Select property, and this database doesn't have one yet.",
        "Una bacheca raggruppa le righe in base a una proprietà Selezione, e questo database non ne ha ancora una.",
        "Ein Board gruppiert Zeilen nach einer Auswahl-Eigenschaft, und diese Datenbank hat noch keine.",
        "Un tableau Kanban regroupe les lignes selon une propriété Sélection, et cette base de données n'en a pas encore.",
        "Un tablero agrupa las filas según una propiedad Selección, y esta base de datos aún no tiene ninguna.",
        "看板会按单选属性分组各行，而此数据库还没有单选属性。",
        "보드는 선택 속성으로 행을 그룹화하는데, 이 데이터베이스에는 아직 선택 속성이 없습니다.",
        "ボードはセレクトプロパティで行をグループ化しますが、このデータベースにはまだありません。"
    )
    val shownInTable get() = t("Shown in table", "Visibili nella tabella", "In der Tabelle sichtbar", "Affichées dans le tableau", "Visibles en la tabla", "在表格中显示", "표에 표시됨", "テーブルに表示")
    val hideAll get() = t("Hide all", "Nascondi tutte", "Alle ausblenden", "Tout masquer", "Ocultar todas", "全部隐藏", "모두 숨기기", "すべて非表示")
    val hiddenInTable get() = t("Hidden in table", "Nascoste nella tabella", "In der Tabelle ausgeblendet", "Masquées dans le tableau", "Ocultas en la tabla", "在表格中隐藏", "표에서 숨김", "テーブルで非表示")
    val showAll get() = t("Show all", "Mostra tutte", "Alle einblenden", "Tout afficher", "Mostrar todas", "全部显示", "모두 표시", "すべて表示")
    val nothingHidden get() = t(
        "Nothing is hidden. Holding a column header in the table hides it from here on.",
        "Non è nascosto niente. Tenendo premuta l'intestazione di una colonna nella tabella la si nasconde da qui in poi.",
        "Nichts ist ausgeblendet. Langes Drücken auf eine Spaltenüberschrift in der Tabelle blendet sie ab jetzt aus.",
        "Rien n'est masqué. Un appui long sur l'en-tête d'une colonne du tableau la masque désormais.",
        "No hay nada oculto. Mantener pulsado el encabezado de una columna en la tabla la oculta a partir de entonces.",
        "没有隐藏的内容。在表格中长按列标题即可将其隐藏。",
        "숨겨진 항목이 없습니다. 표에서 열 머리글을 길게 누르면 숨길 수 있습니다.",
        "非表示の項目はありません。テーブルで列見出しを長押しすると非表示になります。"
    )
    fun showInTable(property: String) = t("Show $property in the table", "Mostra $property nella tabella", "$property in der Tabelle anzeigen", "Afficher $property dans le tableau", "Mostrar $property en la tabla", "在表格中显示$property", "표에 $property 표시", "テーブルに${property}を表示")
    fun hideFromTable(property: String) = t("Hide $property from the table", "Nascondi $property dalla tabella", "$property in der Tabelle ausblenden", "Masquer $property du tableau", "Ocultar $property de la tabla", "在表格中隐藏$property", "표에서 $property 숨기기", "テーブルで${property}を非表示")
}

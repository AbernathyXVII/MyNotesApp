package com.gabriele.notionlocal.ui.i18n

/**
 * Le voci dei widget della barra laterale. Stessa regola di `Strings`:
 * ogni voce con le sue otto traduzioni, e solo testo dell'app.
 */
object WidgetStrings {

    private fun t(en: String, it: String, de: String, fr: String, es: String, zh: String, ko: String, ja: String) =
        Strings.t(en, it, de, fr, es, zh, ko, ja)

    // --- La sezione ---

    val hideWidgets get() = t("Hide widgets", "Nascondi i widget", "Widgets ausblenden", "Masquer les widgets", "Ocultar widgets", "隐藏小组件", "위젯 숨기기", "ウィジェットを隠す")
    val showWidgets get() = t("Show widgets", "Mostra i widget", "Widgets anzeigen", "Afficher les widgets", "Mostrar widgets", "显示小组件", "위젯 보기", "ウィジェットを表示")
    val addWidget get() = t("Add widget", "Aggiungi widget", "Widget hinzufügen", "Ajouter un widget", "Añadir widget", "添加小组件", "위젯 추가", "ウィジェットを追加")
    val widgetOptions get() = t("Widget options", "Opzioni del widget", "Widget-Optionen", "Options du widget", "Opciones del widget", "小组件选项", "위젯 옵션", "ウィジェットのオプション")
    val moveUp get() = t("Move up", "Sposta su", "Nach oben", "Monter", "Subir", "上移", "위로 이동", "上へ移動")
    val moveDown get() = t("Move down", "Sposta giù", "Nach unten", "Descendre", "Bajar", "下移", "아래로 이동", "下へ移動")
    val remove get() = t("Remove", "Rimuovi", "Entfernen", "Retirer", "Quitar", "移除", "제거", "削除")
    val removeWidgetTitle get() = t("Remove this widget?", "Rimuovere questo widget?", "Dieses Widget entfernen?", "Retirer ce widget ?", "¿Quitar este widget?", "移除此小组件？", "이 위젯을 제거할까요?", "このウィジェットを削除しますか？")
    val removeWidgetText get() = t(
        "What it contains goes away with it: clocks, dates, the counter's number, the timer.",
        "Se ne va con quello che contiene: orologi, date, il numero del contatore, il timer.",
        "Sein Inhalt geht mit: Uhren, Daten, der Zählerstand, der Timer.",
        "Son contenu part avec lui : horloges, dates, le nombre du compteur, le minuteur.",
        "Se va con lo que contiene: relojes, fechas, el número del contador, el temporizador.",
        "其中的内容会一起删除：时钟、日期、计数器的数字、计时器。",
        "안에 있는 내용도 함께 사라집니다: 시계, 날짜, 카운터 숫자, 타이머.",
        "中身も一緒に消えます：時計、日付、カウンターの数、タイマー。"
    )

    fun kindName(kind: com.gabriele.notionlocal.data.widgets.WidgetKind): String = when (kind) {
        com.gabriele.notionlocal.data.widgets.WidgetKind.TIME_ZONES -> timeZones
        com.gabriele.notionlocal.data.widgets.WidgetKind.LIFE_PROGRESS -> lifeProgress
        com.gabriele.notionlocal.data.widgets.WidgetKind.COUNTER -> counter
        com.gabriele.notionlocal.data.widgets.WidgetKind.POMODORO -> pomodoro
    }
    val timeZones get() = t("Time zones", "Fusi orari", "Zeitzonen", "Fuseaux horaires", "Zonas horarias", "时区", "시간대", "タイムゾーン")
    val lifeProgress get() = t("Life progress", "Avanzamento", "Fortschritt", "Progression", "Progreso", "进度", "진행률", "進捗")
    val counter get() = t("Counter", "Contatore", "Zähler", "Compteur", "Contador", "计数器", "카운터", "カウンター")
    val pomodoro get() = t("Pomodoro", "Pomodoro", "Pomodoro", "Pomodoro", "Pomodoro", "番茄钟", "뽀모도로", "ポモドーロ")

    // --- Fusi orari ---

    val addClock get() = t("Add clock", "Aggiungi orologio", "Uhr hinzufügen", "Ajouter une horloge", "Añadir reloj", "添加时钟", "시계 추가", "時計を追加")
    val addToList get() = t("Add to the list", "Aggiungi all'elenco", "Zur Liste hinzufügen", "Ajouter à la liste", "Añadir a la lista", "添加到列表", "목록에 추가", "リストに追加")
    val analog get() = t("Analog", "Analogico", "Analog", "Analogique", "Analógico", "指针", "아날로그", "アナログ")
    val digital get() = t("Digital", "Digitale", "Digital", "Numérique", "Digital", "数字", "디지털", "デジタル")
    val changeTimeZone get() = t("Change time zone", "Cambia fuso orario", "Zeitzone ändern", "Changer de fuseau", "Cambiar zona horaria", "更改时区", "시간대 변경", "タイムゾーンを変更")
    val chooseTimeZone get() = t("Choose a time zone", "Scegli un fuso orario", "Zeitzone wählen", "Choisir un fuseau horaire", "Elegir zona horaria", "选择时区", "시간대 선택", "タイムゾーンを選択")
    val searchCity get() = t("Search a city or a region", "Cerca una città o una regione", "Stadt oder Region suchen", "Rechercher une ville ou une région", "Buscar ciudad o región", "搜索城市或地区", "도시 또는 지역 검색", "都市や地域を検索")
    val day get() = t("Day", "Giorno", "Tag", "Jour", "Día", "白天", "낮", "昼")
    val night get() = t("Night", "Notte", "Nacht", "Nuit", "Noche", "夜晚", "밤", "夜")

    // --- Avanzamento ---

    val year get() = t("Year", "Anno", "Jahr", "Année", "Año", "年", "연", "年")
    val month get() = t("Month", "Mese", "Monat", "Mois", "Mes", "月", "월", "月")
    val week get() = t("Week", "Settimana", "Woche", "Semaine", "Semana", "周", "주", "週")
    val dayOfToday get() = t("Day", "Giorno", "Tag", "Jour", "Día", "日", "일", "日")
    val addDate get() = t("Add a date", "Aggiungi una data", "Datum hinzufügen", "Ajouter une date", "Añadir una fecha", "添加日期", "날짜 추가", "日付を追加")
    val editDate get() = t("Edit", "Modifica", "Bearbeiten", "Modifier", "Editar", "编辑", "편집", "編集")
    val name get() = t("Name", "Nome", "Name", "Nom", "Nombre", "名称", "이름", "名前")
    val from get() = t("From", "Da", "Von", "Du", "Desde", "从", "시작", "開始")
    val to get() = t("To", "A", "Bis", "Au", "Hasta", "到", "끝", "終了")
    val endBeforeStart get() = t("The end comes before the start.", "La fine viene prima dell'inizio.", "Das Ende liegt vor dem Anfang.", "La fin est avant le début.", "El final es anterior al inicio.", "结束早于开始。", "끝이 시작보다 빠릅니다.", "終了が開始より前です。")
    val barColor get() = t("Color", "Colore", "Farbe", "Couleur", "Color", "颜色", "색상", "色")

    // --- Contatore ---

    val increase get() = t("Plus one", "Più uno", "Plus eins", "Plus un", "Más uno", "加一", "하나 더하기", "1つ増やす")
    val decrease get() = t("Minus one", "Meno uno", "Minus eins", "Moins un", "Menos uno", "减一", "하나 빼기", "1つ減らす")
    val reset get() = t("Reset", "Azzera", "Zurücksetzen", "Réinitialiser", "Reiniciar", "重置", "초기화", "リセット")
    val rename get() = EditorStrings.rename

    // --- Pomodoro ---

    val session get() = t("Session", "Sessione", "Sitzung", "Session", "Sesión", "专注", "세션", "セッション")
    val breakTime get() = t("Break", "Pausa", "Pause", "Pause", "Descanso", "休息", "휴식", "休憩")
    val sessionLength get() = t("Session length", "Durata della sessione", "Sitzungsdauer", "Durée de la session", "Duración de la sesión", "专注时长", "세션 길이", "セッションの長さ")
    val breakLength get() = t("Break length", "Durata della pausa", "Pausendauer", "Durée de la pause", "Duración del descanso", "休息时长", "휴식 길이", "休憩の長さ")
    val minutesShort get() = t("min", "min", "Min.", "min", "min", "分钟", "분", "分")
    val minutes get() = t("Minutes", "Minuti", "Minuten", "Minutes", "Minutos", "分钟", "분", "分")
    val seconds get() = t("Seconds", "Secondi", "Sekunden", "Secondes", "Segundos", "秒", "초", "秒")
    val start get() = t("Start", "Avvia", "Starten", "Démarrer", "Iniciar", "开始", "시작", "開始")
    val pause get() = t("Pause", "Pausa", "Pausieren", "Pause", "Pausar", "暂停", "일시정지", "一時停止")
    val timerSettings get() = t("Timer settings", "Impostazioni del timer", "Timer-Einstellungen", "Réglages du minuteur", "Ajustes del temporizador", "计时器设置", "타이머 설정", "タイマー設定")

    /** Il suono di fine fase del pomodoro, nell'ingranaggio. */
    val sound get() = t("Sound", "Suono", "Ton", "Son", "Sonido", "声音", "소리", "サウンド")
    val setTime get() = t("Set the time", "Imposta il tempo", "Zeit einstellen", "Régler le temps", "Ajustar el tiempo", "设置时间", "시간 설정", "時間を設定")
    val sessionName get() = t("Session name", "Nome della sessione", "Name der Sitzung", "Nom de la session", "Nombre de la sesión", "专注名称", "세션 이름", "セッション名")
}

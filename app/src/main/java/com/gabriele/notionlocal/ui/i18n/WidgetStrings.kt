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
    val showDecimals get() = t("Show decimals", "Mostra i decimali", "Dezimalstellen zeigen", "Afficher les décimales", "Mostrar decimales", "显示小数", "소수점 표시", "小数を表示")
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
        com.gabriele.notionlocal.data.widgets.WidgetKind.STOPWATCH -> stopwatch
        com.gabriele.notionlocal.data.widgets.WidgetKind.TIMER -> timer
        com.gabriele.notionlocal.data.widgets.WidgetKind.WEATHER -> weather
    }
    val weather get() = t("Weather", "Meteo", "Wetter", "Météo", "Tiempo", "天气", "날씨", "天気")

    // --- Meteo (dal 17/10/2026) ---

    val addCity get() = t("Add a city", "Aggiungi una città", "Stadt hinzufügen", "Ajouter une ville", "Añadir una ciudad", "添加城市", "도시 추가", "都市を追加")
    val weatherCities get() = t("Cities", "Città", "Städte", "Villes", "Ciudades", "城市", "도시", "都市")
    val weatherCitiesNote get() = t(
        "The first one is the main city. Up to 10.",
        "La prima è la città principale. Fino a 10.",
        "Die erste ist die Hauptstadt. Bis zu 10.",
        "La première est la ville principale. Jusqu'à 10.",
        "La primera es la ciudad principal. Hasta 10.",
        "第一个是主要城市。最多 10 个。",
        "첫 번째가 기본 도시예요. 최대 10개.",
        "最初がメインの都市です。最大10件。"
    )
    val mainCity get() = t("Main", "Principale", "Haupt", "Principale", "Principal", "主要", "기본", "メイン")
    val changeCity get() = t("Change city", "Cambia città", "Stadt ändern", "Changer de ville", "Cambiar ciudad", "更换城市", "도시 변경", "都市を変更")
    val makeMainCity get() = t("Make main city", "Rendi principale", "Zur Hauptstadt machen", "Définir comme principale", "Hacer principal", "设为主要城市", "기본 도시로 설정", "メインにする")
    val removeCity get() = t("Remove city", "Togli la città", "Stadt entfernen", "Retirer la ville", "Quitar ciudad", "移除城市", "도시 제거", "都市を削除")
    val searchWeatherCity get() = t("Search a city", "Cerca una città", "Stadt suchen", "Rechercher une ville", "Buscar una ciudad", "搜索城市", "도시 검색", "都市を検索")
    val noCityFound get() = t("No city with this name", "Nessuna città con questo nome", "Keine Stadt mit diesem Namen", "Aucune ville de ce nom", "Ninguna ciudad con este nombre", "没有这个名字的城市", "이 이름의 도시가 없어요", "この名前の都市はありません")
    val noConnection get() = t("No connection", "Nessuna connessione", "Keine Verbindung", "Pas de connexion", "Sin conexión", "无网络连接", "연결 없음", "接続なし")
    val loadingWeather get() = t("Loading…", "Caricamento…", "Wird geladen…", "Chargement…", "Cargando…", "正在加载…", "불러오는 중…", "読み込み中…")
    val refresh get() = t("Refresh", "Aggiorna", "Aktualisieren", "Actualiser", "Actualizar", "刷新", "새로고침", "更新")
    fun updatedAt(time: String) = t("Updated $time", "Aggiornato alle $time", "Aktualisiert $time", "Mis à jour à $time", "Actualizado a las $time", "更新于 $time", "$time 업데이트", "$time 更新")
    fun maxMin(max: String, min: String) = t("H $max · L $min", "Max $max · Min $min", "H $max · T $min", "Max $max · Min $min", "Máx $max · Mín $min", "最高 $max · 最低 $min", "최고 $max · 최저 $min", "最高 $max · 最低 $min")
    fun feelsLike(value: String) = t("Feels like $value", "Percepita $value", "Gefühlt $value", "Ressenti $value", "Sensación $value", "体感 $value", "체감 $value", "体感 $value")
    fun humidity(value: Int) = t("Humidity $value%", "Umidità $value%", "Feuchte $value%", "Humidité $value%", "Humedad $value%", "湿度 $value%", "습도 $value%", "湿度 $value%")
    fun wind(kmh: Int) = t("Wind $kmh km/h", "Vento $kmh km/h", "Wind $kmh km/h", "Vent $kmh km/h", "Viento $kmh km/h", "风速 $kmh km/h", "바람 $kmh km/h", "風 $kmh km/h")
    val hourByHour get() = t("Hour by hour", "Ora per ora", "Stündlich", "Heure par heure", "Hora a hora", "逐小时", "시간별", "1時間ごと")
    val today get() = t("Today", "Oggi", "Heute", "Aujourd'hui", "Hoy", "今天", "오늘", "今日")
    val tomorrow get() = t("Tomorrow", "Domani", "Morgen", "Demain", "Mañana", "明天", "내일", "明日")
    val weatherSource get() = t("Data: Open-Meteo", "Dati: Open-Meteo", "Daten: Open-Meteo", "Données : Open-Meteo", "Datos: Open-Meteo", "数据：Open-Meteo", "데이터: Open-Meteo", "データ：Open-Meteo")

    fun skyName(sky: com.gabriele.notionlocal.data.widgets.WeatherSky): String = when (sky) {
        com.gabriele.notionlocal.data.widgets.WeatherSky.CLEAR -> t("Clear", "Sereno", "Klar", "Dégagé", "Despejado", "晴", "맑음", "快晴")
        com.gabriele.notionlocal.data.widgets.WeatherSky.MAINLY_CLEAR -> t("Mostly clear", "Quasi sereno", "Überwiegend klar", "Plutôt dégagé", "Mayormente despejado", "大部晴朗", "대체로 맑음", "晴れ")
        com.gabriele.notionlocal.data.widgets.WeatherSky.PARTLY_CLOUDY -> t("Partly cloudy", "Poco nuvoloso", "Teilweise bewölkt", "Partiellement nuageux", "Parcialmente nublado", "局部多云", "구름 조금", "所により曇り")
        com.gabriele.notionlocal.data.widgets.WeatherSky.OVERCAST -> t("Cloudy", "Nuvoloso", "Bewölkt", "Couvert", "Nublado", "阴", "흐림", "曇り")
        com.gabriele.notionlocal.data.widgets.WeatherSky.FOG -> t("Fog", "Nebbia", "Nebel", "Brouillard", "Niebla", "雾", "안개", "霧")
        com.gabriele.notionlocal.data.widgets.WeatherSky.DRIZZLE -> t("Drizzle", "Pioviggine", "Nieselregen", "Bruine", "Llovizna", "毛毛雨", "이슬비", "霧雨")
        com.gabriele.notionlocal.data.widgets.WeatherSky.FREEZING_DRIZZLE -> t("Freezing drizzle", "Pioviggine gelata", "Gefrierender Niesel", "Bruine verglaçante", "Llovizna helada", "冻毛毛雨", "어는 이슬비", "着氷性の霧雨")
        com.gabriele.notionlocal.data.widgets.WeatherSky.RAIN -> t("Rain", "Pioggia", "Regen", "Pluie", "Lluvia", "雨", "비", "雨")
        com.gabriele.notionlocal.data.widgets.WeatherSky.FREEZING_RAIN -> t("Freezing rain", "Pioggia gelata", "Gefrierender Regen", "Pluie verglaçante", "Lluvia helada", "冻雨", "어는 비", "着氷性の雨")
        com.gabriele.notionlocal.data.widgets.WeatherSky.SNOW -> t("Snow", "Neve", "Schnee", "Neige", "Nieve", "雪", "눈", "雪")
        com.gabriele.notionlocal.data.widgets.WeatherSky.SNOW_GRAINS -> t("Snow grains", "Granelli di neve", "Schneegriesel", "Neige en grains", "Granos de nieve", "米雪", "싸락눈", "霧雪")
        com.gabriele.notionlocal.data.widgets.WeatherSky.RAIN_SHOWERS -> t("Showers", "Rovesci", "Schauer", "Averses", "Chubascos", "阵雨", "소나기", "にわか雨")
        com.gabriele.notionlocal.data.widgets.WeatherSky.SNOW_SHOWERS -> t("Snow showers", "Rovesci di neve", "Schneeschauer", "Averses de neige", "Chubascos de nieve", "阵雪", "소낙눈", "にわか雪")
        com.gabriele.notionlocal.data.widgets.WeatherSky.THUNDERSTORM -> t("Thunderstorm", "Temporale", "Gewitter", "Orage", "Tormenta", "雷雨", "뇌우", "雷雨")
        com.gabriele.notionlocal.data.widgets.WeatherSky.THUNDERSTORM_HAIL -> t("Thunderstorm with hail", "Temporale con grandine", "Gewitter mit Hagel", "Orage avec grêle", "Tormenta con granizo", "雷雨伴冰雹", "우박을 동반한 뇌우", "雹を伴う雷雨")
    }
    val stopwatch get() = t("Stopwatch", "Cronometro", "Stoppuhr", "Chronomètre", "Cronómetro", "秒表", "스톱워치", "ストップウォッチ")
    val timer get() = t("Timer", "Timer", "Timer", "Minuteur", "Temporizador", "计时器", "타이머", "タイマー")
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
    val breakLength get() = t("Break length", "Durata della pausa", "Pausendauer", "Durée de la pause", "Duración del descanso", "休息时长", "휴식 길이", "休憩の長さ")
    val minutesShort get() = t("min", "min", "Min.", "min", "min", "分钟", "분", "分")
    val minutes get() = t("Minutes", "Minuti", "Minuten", "Minutes", "Minutos", "分钟", "분", "分")
    val seconds get() = t("Seconds", "Secondi", "Sekunden", "Secondes", "Segundos", "秒", "초", "秒")
    val start get() = t("Start", "Avvia", "Starten", "Démarrer", "Iniciar", "开始", "시작", "開始")
    val pause get() = t("Pause", "Pausa", "Pausieren", "Pause", "Pausar", "暂停", "일시정지", "一時停止")
    val timerSettings get() = t("Timer settings", "Impostazioni del timer", "Timer-Einstellungen", "Réglages du minuteur", "Ajustes del temporizador", "计时器设置", "타이머 설정", "タイマー設定")

    /** Il suono di fine fase del pomodoro, nell'ingranaggio. */
    val sound get() = t("Sound", "Suono", "Ton", "Son", "Sonido", "声音", "소리", "サウンド")
    /** Il suono di quando ricomincia la sessione di studio (dal 28/09/2026). */
    val studySound get() = t("Study sound", "Suono dello studio", "Lernton", "Son de l'étude", "Sonido del estudio", "学习提示音", "공부 알림음", "勉強のサウンド")
    /** Il suono di quando comincia la pausa (dal 28/09/2026). */
    val breakSound get() = t("Break sound", "Suono della pausa", "Pausenton", "Son de la pause", "Sonido del descanso", "休息提示音", "휴식 알림음", "休憩のサウンド")
    val setTime get() = t("Set the time", "Imposta il tempo", "Zeit einstellen", "Régler le temps", "Ajustar el tiempo", "设置时间", "시간 설정", "時間を設定")
    val sessionName get() = t("Session name", "Nome della sessione", "Name der Sitzung", "Nom de la session", "Nombre de la sesión", "专注名称", "세션 이름", "セッション名")

    // --- Cronometro e timer (dal 25/09/2026) ---

    val lap get() = t("Lap", "Giro", "Runde", "Tour", "Vuelta", "计次", "랩", "ラップ")
    val lapTimes get() = t("Lap times", "Tempo del giro", "Rundenzeit", "Temps du tour", "Tiempo de vuelta", "计次时间", "구간 기록", "ラップタイム")
    val overallTime get() = t("Overall time", "Tempo totale", "Gesamtzeit", "Temps total", "Tiempo total", "总时间", "전체 시간", "合計時間")
    val stop get() = t("Stop", "Stop", "Stopp", "Arrêter", "Detener", "停止", "중지", "停止")
    val resume get() = t("Resume", "Riprendi", "Fortsetzen", "Reprendre", "Reanudar", "继续", "계속", "再開")
    val hours get() = t("Hours", "Ore", "Stunden", "Heures", "Horas", "小时", "시간", "時間")
    val addTimer get() = t("Add timer", "Aggiungi timer", "Timer hinzufügen", "Ajouter un minuteur", "Añadir temporizador", "添加计时器", "타이머 추가", "タイマーを追加")
    val deleteTimer get() = t("Delete timer", "Elimina timer", "Timer löschen", "Supprimer le minuteur", "Eliminar temporizador", "删除计时器", "타이머 삭제", "タイマーを削除")
    val resetTimer get() = t("Reset timer", "Azzera il timer", "Timer zurücksetzen", "Réinitialiser le minuteur", "Reiniciar temporizador", "重置计时器", "타이머 초기화", "タイマーをリセット")
    val timerEndsAt get() = t("Ends at", "Finisce alle", "Endet um", "Se termine à", "Termina a las", "结束于", "종료 시각", "終了時刻")
}

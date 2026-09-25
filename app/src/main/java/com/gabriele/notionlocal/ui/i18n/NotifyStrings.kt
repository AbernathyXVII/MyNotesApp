package com.gabriele.notionlocal.ui.i18n

/** Le scritte delle notifiche (dal 25/09/2026): canali, timer, pomodoro, date. */
object NotifyStrings {

    private fun t(en: String, it: String, de: String, fr: String, es: String, zh: String, ko: String, ja: String) =
        Strings.t(en, it, de, fr, es, zh, ko, ja)

    // --- I canali, come li mostra Android nelle impostazioni delle notifiche ---

    val channelReminders get() = t("Reminders", "Promemoria", "Erinnerungen", "Rappels", "Recordatorios", "提醒", "리마인더", "リマインダー")
    val channelTimers get() = t("Timer and pomodoro", "Timer e pomodoro", "Timer und Pomodoro", "Minuteur et pomodoro", "Temporizador y pomodoro", "计时器和番茄钟", "타이머와 뽀모도로", "タイマーとポモドーロ")

    /** Il suggerimento di una riga-notifica senza testo, che mostrerà il nome della pagina. */
    val textHint get() = t("Notification (page title)", "Notifica (titolo della pagina)", "Benachrichtigung (Seitentitel)", "Notification (titre de la page)", "Notificación (título de la página)", "通知（页面标题）", "알림(페이지 제목)", "通知（ページのタイトル）")

    val tomorrow get() = t("Tomorrow", "Domani", "Morgen", "Demain", "Mañana", "明天", "내일", "明日")
    val alreadyPassed get() = t(
        "This time has already passed: the notification won't ring.",
        "Quest'ora è già passata: la notifica non suonerà.",
        "Diese Uhrzeit ist schon vorbei: Die Benachrichtigung wird nicht klingeln.",
        "Cette heure est déjà passée : la notification ne sonnera pas.",
        "Esta hora ya ha pasado: la notificación no sonará.",
        "这个时间已经过了：通知不会响。",
        "이미 지난 시간이에요. 알림이 울리지 않아요.",
        "この時刻はもう過ぎています。通知は鳴りません。"
    )

    // --- Timer e pomodoro ---

    val timeIsUp get() = t("Time's up", "Tempo scaduto", "Die Zeit ist um", "Temps écoulé", "Se acabó el tiempo", "时间到了", "시간이 다 됐어요", "時間です")
    fun timerOf(duration: String) = t("Timer · $duration", "Timer · $duration", "Timer · $duration", "Minuteur · $duration", "Temporizador · $duration", "计时器 · $duration", "타이머 · $duration", "タイマー · $duration")
    fun breakTime(minutes: Int) = t(
        "Time for a break · $minutes min",
        "È ora della pausa · $minutes min",
        "Zeit für eine Pause · $minutes Min.",
        "C'est l'heure de la pause · $minutes min",
        "Hora del descanso · $minutes min",
        "该休息了 · $minutes 分钟",
        "휴식 시간 · ${minutes}분",
        "休憩の時間 · ${minutes}分"
    )
    val breakOver get() = t("Break's over", "Pausa finita", "Pause vorbei", "Pause terminée", "Se acabó el descanso", "休息结束", "휴식 끝", "休憩終了")
    fun backTo(session: String, minutes: Int) = t(
        "Back to $session · $minutes min",
        "Si torna a $session · $minutes min",
        "Zurück zu $session · $minutes Min.",
        "Retour à $session · $minutes min",
        "De vuelta a $session · $minutes min",
        "回到$session · $minutes 分钟",
        "$session(으)로 돌아가기 · ${minutes}분",
        "$session に戻る · ${minutes}分"
    )

    // --- Le date dei database con inizio e fine ---

    val starts get() = t("Starts", "Inizio", "Beginn", "Début", "Inicio", "开始", "시작", "開始")
    val ends get() = t("Ends", "Fine", "Ende", "Fin", "Fin", "结束", "종료", "終了")
}

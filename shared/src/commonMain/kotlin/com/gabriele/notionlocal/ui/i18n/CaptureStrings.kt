package com.gabriele.notionlocal.ui.i18n

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Le scritte della registrazione dell'audio e della scansione dei documenti (dal 03/11/2026). */
object CaptureStrings {

    private fun t(en: String, it: String, de: String, fr: String, es: String, zh: String, ko: String, ja: String) =
        Strings.t(en, it, de, fr, es, zh, ko, ja)

    val recordAudio get() = t("Record audio", "Registra audio", "Audio aufnehmen", "Enregistrer de l'audio", "Grabar audio", "录音", "오디오 녹음", "音声を録音")
    val recording get() = t("Recording…", "Registrazione…", "Aufnahme…", "Enregistrement…", "Grabando…", "正在录音…", "녹음 중…", "録音中…")
    val recordingTitle get() = t("Recording audio", "Registrazione audio", "Audioaufnahme", "Enregistrement audio", "Grabación de audio", "正在录音", "오디오 녹음 중", "音声を録音中")
    val stop get() = t("Stop", "Ferma", "Stopp", "Arrêter", "Detener", "停止", "중지", "停止")
    val discard get() = t("Discard", "Butta via", "Verwerfen", "Supprimer", "Descartar", "丢弃", "삭제", "破棄")
    val channel get() = t("Recordings", "Registrazioni", "Aufnahmen", "Enregistrements", "Grabaciones", "录音", "녹음", "録音")
    val micDenied get() = t(
        "The app needs the microphone to record: allow it in the phone settings",
        "Per registrare l'app ha bisogno del microfono: permettilo nelle impostazioni del telefono",
        "Zum Aufnehmen braucht die App das Mikrofon: Erlaube es in den Telefoneinstellungen",
        "L'app a besoin du micro pour enregistrer : autorisez-le dans les réglages du téléphone",
        "La app necesita el micrófono para grabar: permítelo en los ajustes del teléfono",
        "应用需要麦克风才能录音：请在手机设置中允许",
        "녹음하려면 마이크가 필요해요. 휴대폰 설정에서 허용해 주세요",
        "録音にはマイクが必要です。端末の設定で許可してください"
    )
    val alreadyRecording get() = t(
        "Another recording is running: stop it first",
        "C'è già una registrazione in corso: fermala prima",
        "Es läuft bereits eine Aufnahme: Beende sie zuerst",
        "Un enregistrement est déjà en cours : arrêtez-le d'abord",
        "Ya hay una grabación en curso: detenla primero",
        "已有录音正在进行：请先停止",
        "이미 녹음 중이에요. 먼저 중지하세요",
        "別の録音が進行中です。先に停止してください"
    )
    val cannotRecord get() = t(
        "The microphone can't be used right now",
        "Il microfono adesso non si può usare",
        "Das Mikrofon kann gerade nicht verwendet werden",
        "Le micro ne peut pas être utilisé pour l'instant",
        "El micrófono no se puede usar ahora",
        "现在无法使用麦克风",
        "지금은 마이크를 사용할 수 없어요",
        "現在マイクを使用できません"
    )

    val scanDocument get() = t("Scan document", "Scansiona documento", "Dokument scannen", "Numériser un document", "Escanear documento", "扫描文档", "문서 스캔", "書類をスキャン")
    val scanUnavailable get() = t(
        "The scanner isn't available: it needs Google Play services",
        "Lo scanner non è disponibile: serve Google Play Services",
        "Der Scanner ist nicht verfügbar: Er braucht die Google Play-Dienste",
        "Le scanner n'est pas disponible : il nécessite les services Google Play",
        "El escáner no está disponible: necesita los servicios de Google Play",
        "扫描仪不可用：需要 Google Play 服务",
        "스캐너를 사용할 수 없어요. Google Play 서비스가 필요해요",
        "スキャナーを使用できません。Google Play 開発者サービスが必要です"
    )
    val scanFailed get() = t(
        "The scanned pages couldn't be saved",
        "Le pagine scansionate non si sono potute salvare",
        "Die gescannten Seiten konnten nicht gespeichert werden",
        "Les pages numérisées n'ont pas pu être enregistrées",
        "No se pudieron guardar las páginas escaneadas",
        "无法保存扫描的页面",
        "스캔한 페이지를 저장하지 못했어요",
        "スキャンしたページを保存できませんでした"
    )

    private val stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH.mm")

    /** Il nome di una registrazione, quando la si salva fuori: "Recording 2026-11-03 14.05.m4a". */
    fun recordingName(at: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        t("Recording", "Registrazione", "Aufnahme", "Enregistrement", "Grabación", "录音", "녹음", "録音") +
            " " + stamp.format(Instant.ofEpochMilli(at).atZone(zone)) + ".m4a"

    /** Il nome di una pagina scansionata: "Scan 2026-11-03 14.05 (2).jpg". */
    fun scanName(at: Long, page: Int, pages: Int, zone: ZoneId = ZoneId.systemDefault()): String =
        t("Scan", "Scansione", "Scan", "Numérisation", "Escaneo", "扫描", "스캔", "スキャン") +
            " " + stamp.format(Instant.ofEpochMilli(at).atZone(zone)) + (if (pages > 1) " ($page)" else "") + ".jpg"
}

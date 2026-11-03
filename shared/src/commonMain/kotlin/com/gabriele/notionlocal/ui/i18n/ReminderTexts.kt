package com.gabriele.notionlocal.ui.i18n

import com.gabriele.notionlocal.data.entity.ReminderUnit
import com.gabriele.notionlocal.data.entity.reminderParts
import com.gabriele.notionlocal.data.settings.AppLanguage
import com.gabriele.notionlocal.data.settings.AppSettings

/**
 * **Le notifiche in anticipo a parole** (dal 26/10/2026, vedi
 * `DateReminders.kt`): "10 hours before" nella finestra della data, "Starts
 * in 10 hours" nella notifica, e i nomi delle unità di "Custom…".
 */
object ReminderTexts {

    private fun t(en: String, it: String, de: String, fr: String, es: String, zh: String, ko: String, ja: String) =
        Strings.t(en, it, de, fr, es, zh, ko, ja)

    val addNotification get() = t("Add notification", "Aggiungi notifica", "Benachrichtigung hinzufügen", "Ajouter une notification", "Añadir notificación", "添加通知", "알림 추가", "通知を追加")
    val customTitle get() = t("Custom notification", "Notifica personalizzata", "Eigene Benachrichtigung", "Notification personnalisée", "Notificación personalizada", "自定义通知", "맞춤 알림", "カスタム通知")
    val removeNotification get() = t("Remove notification", "Togli la notifica", "Benachrichtigung entfernen", "Retirer la notification", "Quitar la notificación", "移除通知", "알림 삭제", "通知を削除")
    val tooFar get() = t("At most one year before", "Al massimo un anno prima", "Höchstens ein Jahr vorher", "Un an avant au maximum", "Como mucho un año antes", "最多提前一年", "최대 1년 전까지", "最大1年前まで")

    /** "10 hours before", "10 ore prima", "提前10小时". */
    fun before(minutes: Int, language: AppLanguage = AppSettings.language): String {
        val amount = amount(minutes, language, dative = false)
        return when (language) {
            AppLanguage.ENGLISH -> "$amount before"
            AppLanguage.ITALIAN -> "$amount prima"
            AppLanguage.GERMAN -> "$amount vorher"
            AppLanguage.FRENCH -> "$amount avant"
            AppLanguage.SPANISH -> "$amount antes"
            AppLanguage.CHINESE -> "提前$amount"
            AppLanguage.KOREAN -> "$amount 전"
            AppLanguage.JAPANESE -> "${amount}前"
        }
    }

    /** Il testo della notifica in anticipo: "Starts in 10 hours", "Inizia tra 10 ore". */
    fun startsIn(minutes: Int, language: AppLanguage = AppSettings.language): String {
        val amount = amount(minutes, language, dative = true)
        return when (language) {
            AppLanguage.ENGLISH -> "Starts in $amount"
            AppLanguage.ITALIAN -> "Inizia tra $amount"
            AppLanguage.GERMAN -> "Beginnt in $amount"
            AppLanguage.FRENCH -> "Commence dans $amount"
            AppLanguage.SPANISH -> "Empieza en $amount"
            AppLanguage.CHINESE -> "${amount}后开始"
            AppLanguage.KOREAN -> "$amount 후 시작"
            AppLanguage.JAPANESE -> "${amount}後に開始"
        }
    }

    /** Il nome dell'unità accanto al numero di "Custom…", al singolare con 1: "hours", "hour". */
    fun unitName(unit: ReminderUnit, amount: Int, language: AppLanguage = AppSettings.language): String =
        unitWord(unit, amount, language, dative = false)

    /** "10 hours", "10 ore", "10小时": il numero e l'unità più grande che lo divide. */
    private fun amount(minutes: Int, language: AppLanguage, dative: Boolean): String {
        val (n, unit) = reminderParts(minutes)
        val word = unitWord(unit, n, language, dative)
        return when (language) {
            AppLanguage.CHINESE, AppLanguage.KOREAN, AppLanguage.JAPANESE -> "$n$word"
            else -> "$n $word"
        }
    }

    /** [dative]: in tedesco "in 3 Tagen", non "Tage". */
    private fun unitWord(unit: ReminderUnit, n: Int, language: AppLanguage, dative: Boolean): String {
        // In francese anche lo zero va al singolare.
        val one = if (language == AppLanguage.FRENCH) n <= 1 else n == 1
        fun pick(singular: String, plural: String) = if (one) singular else plural
        return when (language) {
            AppLanguage.ENGLISH -> when (unit) {
                ReminderUnit.MINUTES -> pick("minute", "minutes")
                ReminderUnit.HOURS -> pick("hour", "hours")
                ReminderUnit.DAYS -> pick("day", "days")
                ReminderUnit.WEEKS -> pick("week", "weeks")
            }
            AppLanguage.ITALIAN -> when (unit) {
                ReminderUnit.MINUTES -> pick("minuto", "minuti")
                ReminderUnit.HOURS -> pick("ora", "ore")
                ReminderUnit.DAYS -> pick("giorno", "giorni")
                ReminderUnit.WEEKS -> pick("settimana", "settimane")
            }
            AppLanguage.GERMAN -> when (unit) {
                ReminderUnit.MINUTES -> pick("Minute", "Minuten")
                ReminderUnit.HOURS -> pick("Stunde", "Stunden")
                ReminderUnit.DAYS -> pick("Tag", if (dative) "Tagen" else "Tage")
                ReminderUnit.WEEKS -> pick("Woche", "Wochen")
            }
            AppLanguage.FRENCH -> when (unit) {
                ReminderUnit.MINUTES -> pick("minute", "minutes")
                ReminderUnit.HOURS -> pick("heure", "heures")
                ReminderUnit.DAYS -> pick("jour", "jours")
                ReminderUnit.WEEKS -> pick("semaine", "semaines")
            }
            AppLanguage.SPANISH -> when (unit) {
                ReminderUnit.MINUTES -> pick("minuto", "minutos")
                ReminderUnit.HOURS -> pick("hora", "horas")
                ReminderUnit.DAYS -> pick("día", "días")
                ReminderUnit.WEEKS -> pick("semana", "semanas")
            }
            AppLanguage.CHINESE -> when (unit) {
                ReminderUnit.MINUTES -> "分钟"
                ReminderUnit.HOURS -> "小时"
                ReminderUnit.DAYS -> "天"
                ReminderUnit.WEEKS -> "周"
            }
            AppLanguage.KOREAN -> when (unit) {
                ReminderUnit.MINUTES -> "분"
                ReminderUnit.HOURS -> "시간"
                ReminderUnit.DAYS -> "일"
                ReminderUnit.WEEKS -> "주"
            }
            AppLanguage.JAPANESE -> when (unit) {
                ReminderUnit.MINUTES -> "分"
                ReminderUnit.HOURS -> "時間"
                ReminderUnit.DAYS -> "日"
                ReminderUnit.WEEKS -> "週間"
            }
        }
    }
}

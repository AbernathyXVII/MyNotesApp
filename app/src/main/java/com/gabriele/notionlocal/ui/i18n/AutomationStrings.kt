package com.gabriele.notionlocal.ui.i18n

import com.gabriele.notionlocal.data.settings.AppSettings
import java.time.DayOfWeek
import java.time.format.TextStyle

/** Le scritte delle automazioni dei database (dal 03/11/2026). */
object AutomationStrings {

    private fun t(en: String, it: String, de: String, fr: String, es: String, zh: String, ko: String, ja: String) =
        Strings.t(en, it, de, fr, es, zh, ko, ja)

    val automations get() = t("Automations", "Automazioni", "Automatisierungen", "Automatisations", "Automatizaciones", "自动化", "자동화", "オートメーション")
    val newAutomation get() = t("New automation", "Nuova automazione", "Neue Automatisierung", "Nouvelle automatisation", "Nueva automatización", "新建自动化", "새 자동화", "新しいオートメーション")
    val editAutomation get() = t("Edit automation", "Modifica automazione", "Automatisierung bearbeiten", "Modifier l'automatisation", "Editar automatización", "编辑自动化", "자동화 편집", "オートメーションを編集")
    val nameHint get() = t("Name of the automation", "Nome dell'automazione", "Name der Automatisierung", "Nom de l'automatisation", "Nombre de la automatización", "自动化名称", "자동화 이름", "オートメーションの名前")
    val whenLabel get() = t("When", "Quando", "Wenn", "Quand", "Cuando", "当", "언제", "いつ")
    val thenLabel get() = t("Then", "Allora", "Dann", "Alors", "Entonces", "然后", "그러면", "そのとき")
    val pageAdded get() = t("Page added", "Pagina aggiunta", "Seite hinzugefügt", "Page ajoutée", "Página añadida", "添加页面时", "페이지 추가됨", "ページが追加されたとき")
    val propertyEdited get() = t("Property edited", "Proprietà modificata", "Eigenschaft geändert", "Propriété modifiée", "Propiedad editada", "属性被修改时", "속성 변경됨", "プロパティが変更されたとき")
    val everyDay get() = t("Every day", "Ogni giorno", "Jeden Tag", "Chaque jour", "Cada día", "每天", "매일", "毎日")
    val everyWeek get() = t("Every week", "Ogni settimana", "Jede Woche", "Chaque semaine", "Cada semana", "每周", "매주", "毎週")
    val everyMonth get() = t("Every month", "Ogni mese", "Jeden Monat", "Chaque mois", "Cada mes", "每月", "매월", "毎月")
    val anyChange get() = t("Any change", "Qualunque modifica", "Jede Änderung", "Toute modification", "Cualquier cambio", "任何更改", "모든 변경", "どの変更でも")
    val becomes get() = t("becomes", "diventa", "wird zu", "devient", "pasa a", "变为", "다음으로 바뀜", "が次になる")
    val at get() = t("at", "alle", "um", "à", "a las", "于", "시각", "時刻")
    val on get() = t("on", "di", "am", "le", "el", "在", "요일", "曜日")
    val deleteAutomation get() = t("Delete automation", "Elimina automazione", "Automatisierung löschen", "Supprimer l'automatisation", "Eliminar automatización", "删除自动化", "자동화 삭제", "オートメーションを削除")
    val noAutomations get() = t(
        "None yet. An automation does what a button does, by itself: when a page is added, when a property changes, or every day, week or month at a time you choose.",
        "Ancora nessuna. Un'automazione fa quello che fa un pulsante, da sola: quando si aggiunge una pagina, quando cambia una proprietà, o ogni giorno, settimana o mese all'ora che scegli.",
        "Noch keine. Eine Automatisierung macht, was eine Schaltfläche macht, von selbst: wenn eine Seite hinzugefügt wird, wenn sich eine Eigenschaft ändert, oder jeden Tag, jede Woche oder jeden Monat zu einer gewählten Uhrzeit.",
        "Aucune pour l'instant. Une automatisation fait ce que fait un bouton, toute seule : quand une page est ajoutée, quand une propriété change, ou chaque jour, semaine ou mois à l'heure choisie.",
        "Aún ninguna. Una automatización hace lo que hace un botón, sola: cuando se añade una página, cuando cambia una propiedad, o cada día, semana o mes a la hora que elijas.",
        "还没有。自动化会自己完成按钮的操作：添加页面时、属性变化时，或在你选择的时间每天、每周或每月执行。",
        "아직 없어요. 자동화는 버튼이 하는 일을 스스로 해요: 페이지가 추가될 때, 속성이 바뀔 때, 또는 정한 시간에 매일, 매주, 매월.",
        "まだありません。オートメーションはボタンと同じことを自動で行います：ページが追加されたとき、プロパティが変わったとき、または毎日・毎週・毎月の決めた時刻に。"
    )
    val chooseDays get() = t("Choose at least one day", "Scegli almeno un giorno", "Wähle mindestens einen Tag", "Choisissez au moins un jour", "Elige al menos un día", "请至少选择一天", "하루 이상 선택하세요", "1日以上選んでください")
    val choosePropertyFirst get() = t("Choose the property", "Scegli la proprietà", "Wähle die Eigenschaft", "Choisissez la propriété", "Elige la propiedad", "请选择属性", "속성을 선택하세요", "プロパティを選んでください")

    fun day(n: Int) = t("Day $n", "Giorno $n", "Tag $n", "Jour $n", "Día $n", "第 $n 天", "${n}일", "${n}日")

    fun lastRun(text: String) = t("Last run: $text", "Ultima volta: $text", "Zuletzt: $text", "Dernière fois : $text", "Última vez: $text", "上次：$text", "마지막 실행: $text", "前回: $text")

    /** Il nome del giorno nella lingua dell'app: "lunedì", o "lun" con [short]. */
    fun dayName(day: Int, short: Boolean = false): String =
        DayOfWeek.of(day.coerceIn(1, 7)).getDisplayName(if (short) TextStyle.SHORT else TextStyle.FULL, AppSettings.language.locale)
}

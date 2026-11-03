package com.gabriele.notionlocal.ui.i18n

/** Le scritte dei blocchi sincronizzati (dal 03/11/2026). */
object SyncStrings {

    private fun t(en: String, it: String, de: String, fr: String, es: String, zh: String, ko: String, ja: String) =
        Strings.t(en, it, de, fr, es, zh, ko, ja)

    val syncedBlock get() = t("Synced block", "Blocco sincronizzato", "Synchronisierter Block", "Bloc synchronisé", "Bloque sincronizado", "同步块", "동기화 블록", "同期ブロック")

    /** Sopra le righe: "Synced block" e, se è anche altrove, in quante pagine. */
    fun header(pages: Int): String = if (pages <= 1) {
        syncedBlock
    } else {
        syncedBlock + " · " + t(
            "$pages pages", "$pages pagine", "$pages Seiten", "$pages pages", "$pages páginas", "$pages 个页面", "페이지 ${pages}개", "${pages}ページ"
        )
    }

    val empty get() = t(
        "Empty synced block. Tap to write",
        "Blocco sincronizzato vuoto. Tocca per scrivere",
        "Leerer synchronisierter Block. Tippen zum Schreiben",
        "Bloc synchronisé vide. Touchez pour écrire",
        "Bloque sincronizado vacío. Toca para escribir",
        "空的同步块。点按即可书写",
        "빈 동기화 블록. 눌러서 작성하세요",
        "空の同期ブロック。タップして入力"
    )
    val syncToPage get() = t("Sync to another page", "Sincronizza in un'altra pagina", "Mit anderer Seite synchronisieren", "Synchroniser dans une autre page", "Sincronizar en otra página", "同步到其他页面", "다른 페이지에 동기화", "別のページに同期")
    val syncToPageHint get() = t(
        "A copy goes at the bottom of the page you choose: write in one, the others change too",
        "Una copia va in fondo alla pagina che scegli: scrivi in una, cambiano anche le altre",
        "Eine Kopie kommt ans Ende der gewählten Seite: Schreibst du in einer, ändern sich auch die anderen",
        "Une copie va en bas de la page choisie : écrivez dans l'une, les autres changent aussi",
        "Una copia va al final de la página que elijas: escribe en una y las demás cambian también",
        "副本会放在所选页面的底部：在其中一处书写，其他副本也会一起变化",
        "선택한 페이지 맨 아래에 사본이 추가돼요. 한 곳에 쓰면 다른 곳도 함께 바뀌어요",
        "選んだページの末尾にコピーが入ります。どれかに書くと、ほかも一緒に変わります"
    )
    val synced get() = t("Synced", "Sincronizzato", "Synchronisiert", "Synchronisé", "Sincronizado", "已同步", "동기화됨", "同期しました")
    val unsync get() = t("Unsync", "Togli la sincronizzazione", "Synchronisierung aufheben", "Désynchroniser", "Dejar de sincronizar", "取消同步", "동기화 해제", "同期を解除")
}

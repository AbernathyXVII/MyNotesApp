package com.gabriele.notionlocal.ui.i18n

/** Le scritte di "Condividi con PageFlow" e delle note al volo (dal 02/11/2026). */
object ShareStrings {

    private fun t(en: String, it: String, de: String, fr: String, es: String, zh: String, ko: String, ja: String) =
        Strings.t(en, it, de, fr, es, zh, ko, ja)

    val saveToNote get() = t("Save to PageFlow", "Salva in PageFlow", "In PageFlow speichern", "Enregistrer dans PageFlow", "Guardar en PageFlow", "保存到 PageFlow", "PageFlow에 저장", "PageFlow に保存")
    val title get() = t("Title", "Titolo", "Titel", "Titre", "Título", "标题", "제목", "タイトル")
    val saveTo get() = t("Save to", "Salva in", "Speichern in", "Enregistrer dans", "Guardar en", "保存到", "저장 위치", "保存先")
    val inbox get() = t("Inbox", "Inbox", "Eingang", "Boîte de réception", "Bandeja de entrada", "收件箱", "받은 편지함", "受信箱")
    val asNewPage get() = t("As a new page", "Come pagina nuova", "Als neue Seite", "Comme nouvelle page", "Como página nueva", "作为新页面", "새 페이지로", "新しいページとして")
    val atTheEnd get() = t("At the end of the page", "In fondo alla pagina", "Am Ende der Seite", "À la fin de la page", "Al final de la página", "添加到页面末尾", "페이지 끝에", "ページの最後に")
    val save get() = t("Save", "Salva", "Speichern", "Enregistrer", "Guardar", "保存", "저장", "保存")
    val saveAndOpen get() = t("Save and open", "Salva e apri", "Speichern und öffnen", "Enregistrer et ouvrir", "Guardar y abrir", "保存并打开", "저장하고 열기", "保存して開く")
    fun savedIn(page: String) = t("Saved in $page", "Salvato in $page", "Gespeichert in $page", "Enregistré dans $page", "Guardado en $page", "已保存到 $page", "$page 에 저장됨", "$page に保存しました")
    val nothingToSave get() = t(
        "There's nothing PageFlow can save here.", "Qui non c'è niente che PageFlow possa salvare.", "Hier gibt es nichts, was PageFlow speichern kann.",
        "Il n'y a rien ici que PageFlow puisse enregistrer.", "Aquí no hay nada que PageFlow pueda guardar.", "这里没有 PageFlow 可以保存的内容。",
        "PageFlow가 저장할 수 있는 내용이 없습니다.", "PageFlow で保存できるものがありません。"
    )
    val saveFailed get() = t(
        "Couldn't save it. Try again.", "Non sono riuscito a salvarlo. Riprova.", "Speichern fehlgeschlagen. Versuch es noch einmal.",
        "Impossible d'enregistrer. Réessaie.", "No se pudo guardar. Inténtalo de nuevo.", "保存失败，请重试。", "저장하지 못했습니다. 다시 시도하세요.", "保存できませんでした。もう一度お試しください。"
    )
    fun images(n: Int) = t(
        if (n == 1) "1 image" else "$n images", if (n == 1) "1 immagine" else "$n immagini", if (n == 1) "1 Bild" else "$n Bilder",
        if (n == 1) "1 image" else "$n images", if (n == 1) "1 imagen" else "$n imágenes", "$n 张图片", "이미지 ${n}개", "画像 ${n} 枚"
    )
    fun files(n: Int) = t(
        if (n == 1) "1 file" else "$n files", if (n == 1) "1 file" else "$n file", if (n == 1) "1 Datei" else "$n Dateien",
        if (n == 1) "1 fichier" else "$n fichiers", if (n == 1) "1 archivo" else "$n archivos", "$n 个文件", "파일 ${n}개", "ファイル ${n} 個"
    )

    // --- Nelle impostazioni ---
    val settingsTitle get() = t(
        "Shared items and quick notes", "Condivisi e note al volo", "Geteiltes und Schnellnotizen", "Partages et notes rapides",
        "Compartidos y notas rápidas", "分享内容和快速笔记", "공유 항목과 빠른 메모", "共有とクイックメモ"
    )
    val pickerHint get() = t(
        "What you share with PageFlow and your quick notes go into this page. You can choose another one each time you share.",
        "Quello che condividi con PageFlow e le note al volo finiscono in questa pagina. Condividendo puoi sceglierne un'altra ogni volta.",
        "Was du mit PageFlow teilst und deine Schnellnotizen landen auf dieser Seite. Beim Teilen kannst du jedes Mal eine andere wählen.",
        "Ce que tu partages avec PageFlow et tes notes rapides arrivent dans cette page. En partageant, tu peux en choisir une autre à chaque fois.",
        "Lo que compartes con PageFlow y tus notas rápidas van a esta página. Al compartir puedes elegir otra cada vez.",
        "你分享到 PageFlow 的内容和快速笔记会放在这个页面。每次分享时都可以另选页面。",
        "PageFlow로 공유한 항목과 빠른 메모가 이 페이지에 저장됩니다. 공유할 때마다 다른 페이지를 고를 수 있습니다.",
        "PageFlow に共有したものとクイックメモはこのページに入ります。共有するたびに別のページを選べます。"
    )
    val inboxNote get() = t(
        "Inbox, at the bottom of the main menu (created the first time)", "Inbox, in fondo al menu principale (nasce la prima volta)",
        "Eingang, unten im Hauptmenü (wird beim ersten Mal erstellt)", "Boîte de réception, en bas du menu principal (créée la première fois)",
        "Bandeja de entrada, al final del menú principal (se crea la primera vez)", "收件箱，位于主菜单底部（首次使用时创建）",
        "받은 편지함, 메인 메뉴 맨 아래 (처음 사용할 때 생성)", "受信箱、メインメニューの一番下（初回に作成）"
    )
}

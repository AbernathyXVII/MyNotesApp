package com.gabriele.notionlocal.ui.i18n

/**
 * Le voci dei blocchi nuovi del 06/10/2026 — riquadro, citazione, codice,
 * immagine, video, audio, file, segnalibro, link a una pagina, database a
 * pagina intera. Stessa regola di `Strings`: ogni voce con le sue otto
 * traduzioni, e solo testo dell'app.
 */
object BlockStrings {

    private fun t(en: String, it: String, de: String, fr: String, es: String, zh: String, ko: String, ja: String) =
        Strings.t(en, it, de, fr, es, zh, ko, ja)

    // --- Riquadro e citazione ---

    /** Il suggerimento grigio di una citazione vuota, come su Notion. */
    val emptyQuote get() = t("Empty quote", "Citazione vuota", "Leeres Zitat", "Citation vide", "Cita vacía", "空引用", "빈 인용", "空の引用")
    /** Il campo per scrivere un'emoji qualsiasi come icona del riquadro. */
    val otherEmoji get() = t("Other emoji", "Altra emoji", "Anderes Emoji", "Autre émoji", "Otro emoji", "其他表情", "다른 이모지", "その他の絵文字")
    val useEmoji get() = t("Use", "Usa", "Verwenden", "Utiliser", "Usar", "使用", "사용", "使う")
    /** La famiglia di emoji dei riquadri, come la chiama Notion. */
    val calloutEmojis get() = t("Callout", "Riquadro", "Hinweisbox", "Encadré", "Destacado", "标注", "콜아웃", "コールアウト")

    // --- Link a una pagina ---

    val linkToPageHint get() = t(
        "Choose the page to link to: it stays where it is.",
        "Scegli la pagina a cui rimandare: resta dov'è.",
        "Wähle die Seite, auf die verwiesen wird: Sie bleibt, wo sie ist.",
        "Choisissez la page à lier : elle reste où elle est.",
        "Elige la página a la que enlazar: se queda donde está.",
        "选择要链接的页面：它会留在原处。",
        "링크할 페이지를 고르세요. 페이지는 그대로 있어요.",
        "リンクするページを選んでください。ページはそのままの場所に残ります。"
    )
    /** Al posto del nome, quando la pagina richiamata è stata cancellata per sempre. */
    val deletedPage get() = t("Deleted page", "Pagina eliminata", "Gelöschte Seite", "Page supprimée", "Página eliminada", "已删除的页面", "삭제된 페이지", "削除されたページ")
    val inTrash get() = t("In the trash", "Nel cestino", "Im Papierkorb", "Dans la corbeille", "En la papelera", "在回收站中", "휴지통에 있음", "ゴミ箱内")
    val changePage get() = t("Change page", "Cambia pagina", "Seite ändern", "Changer de page", "Cambiar página", "更换页面", "페이지 변경", "ページを変更")
    val openPage get() = t("Open", "Apri", "Öffnen", "Ouvrir", "Abrir", "打开", "열기", "開く")

    // --- Immagini, video, audio e file ---

    val addImage get() = t("Add an image", "Aggiungi un'immagine", "Bild hinzufügen", "Ajouter une image", "Añadir una imagen", "添加图片", "이미지 추가", "画像を追加")
    val view get() = t("View", "Guarda", "Ansehen", "Voir", "Ver", "查看", "보기", "表示")
    val download get() = t("Download", "Scarica", "Herunterladen", "Télécharger", "Descargar", "下载", "다운로드", "ダウンロード")
    val replace get() = t("Replace", "Sostituisci", "Ersetzen", "Remplacer", "Reemplazar", "替换", "바꾸기", "置き換え")
    val imageSaved get() = t("Saved in Pictures/notE", "Salvata in Immagini/notE", "In Bilder/notE gespeichert", "Enregistrée dans Images/notE", "Guardada en Imágenes/notE", "已保存到 图片/notE", "사진/notE에 저장했습니다", "ピクチャ/notE に保存しました")
    val fileSaved get() = t("File saved", "File salvato", "Datei gespeichert", "Fichier enregistré", "Archivo guardado", "文件已保存", "파일을 저장했습니다", "ファイルを保存しました")
    val saveFailed get() = t("Couldn't save the file", "Non sono riuscito a salvare il file", "Datei konnte nicht gespeichert werden", "Impossible d'enregistrer le fichier", "No se pudo guardar el archivo", "无法保存文件", "파일을 저장할 수 없습니다", "ファイルを保存できませんでした")
    val moveBlockHint get() = t(
        "It goes to the bottom of the page you choose.",
        "Va in fondo alla pagina che scegli.",
        "Es kommt ans Ende der gewählten Seite.",
        "Il va en bas de la page choisie.",
        "Va al final de la página que elijas.",
        "它会移到你选择的页面底部。",
        "선택한 페이지의 맨 아래로 이동해요.",
        "選んだページの一番下に移動します。"
    )
    /** Le maniglie ai lati di un'immagine, per allargarla o stringerla. */
    val resizeImage get() = t("Resize", "Ridimensiona", "Größe ändern", "Redimensionner", "Cambiar tamaño", "调整大小", "크기 조정", "サイズを変更")

    // --- Codice ---

    val copyCode get() = t("Copy code", "Copia il codice", "Code kopieren", "Copier le code", "Copiar código", "复制代码", "코드 복사", "コードをコピー")
    val codeCopied get() = t("Code copied", "Codice copiato", "Code kopiert", "Code copié", "Código copiado", "代码已复制", "코드를 복사했습니다", "コードをコピーしました")
    val searchLanguage get() = t("Search for a language…", "Cerca un linguaggio…", "Sprache suchen…", "Rechercher un langage…", "Buscar un lenguaje…", "搜索语言…", "언어 검색…", "言語を検索…")
}

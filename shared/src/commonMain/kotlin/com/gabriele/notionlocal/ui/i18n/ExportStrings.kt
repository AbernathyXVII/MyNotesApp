package com.gabriele.notionlocal.ui.i18n

/** Le scritte di "Export", la pagina in PDF o in Markdown (dal 02/11/2026). */
object ExportStrings {

    private fun t(en: String, it: String, de: String, fr: String, es: String, zh: String, ko: String, ja: String) =
        Strings.t(en, it, de, fr, es, zh, ko, ja)

    val exportPage get() = t("Export page", "Esporta la pagina", "Seite exportieren", "Exporter la page", "Exportar la página", "导出页面", "페이지 내보내기", "ページをエクスポート")
    val pdfHint get() = t(
        "To send or print: it looks the same everywhere",
        "Per mandarla o stamparla: si vede uguale ovunque",
        "Zum Senden oder Drucken: sieht überall gleich aus",
        "Pour l'envoyer ou l'imprimer : identique partout",
        "Para enviarla o imprimirla: se ve igual en todas partes",
        "用于发送或打印:在哪里看都一样",
        "보내거나 인쇄할 때: 어디서나 똑같이 보여요",
        "送信や印刷に:どこでも同じに見えます"
    )
    val markdownHint get() = t(
        "Plain text for other notes apps; with images, subpages or databases, a .zip",
        "Testo semplice per le altre app di note; con immagini, sottopagine o database, uno .zip",
        "Reiner Text für andere Notiz-Apps; mit Bildern, Unterseiten oder Datenbanken ein .zip",
        "Texte brut pour d'autres applis de notes ; avec images, sous-pages ou bases de données, un .zip",
        "Texto sin formato para otras apps de notas; con imágenes, subpáginas o bases de datos, un .zip",
        "供其他笔记应用使用的纯文本;含图片、子页面或数据库时为 .zip",
        "다른 노트 앱용 일반 텍스트; 이미지, 하위 페이지나 데이터베이스가 있으면 .zip",
        "ほかのノートアプリ用のテキスト。画像、サブページ、データベースがあれば .zip"
    )
    val includeSubpages get() = t("Include subpages", "Includi le sottopagine", "Unterseiten einschließen", "Inclure les sous-pages", "Incluir subpáginas", "包含子页面", "하위 페이지 포함", "サブページを含める")
    val includeDrawings get() = t("Include drawings", "Includi i disegni", "Zeichnungen einschließen", "Inclure les dessins", "Incluir dibujos", "包含手绘", "그림 포함", "手書きを含める")
    val includeDrawingsHint get() = t(
        "The page keeps the phone's proportions, so they stay in place",
        "La pagina tiene le proporzioni del telefono, così restano al loro posto",
        "Die Seite behält die Proportionen des Telefons, damit sie an ihrem Platz bleiben",
        "La page garde les proportions du téléphone, pour qu'ils restent à leur place",
        "La página mantiene las proporciones del teléfono, así se quedan en su sitio",
        "页面保持手机的比例，手绘就会留在原位",
        "페이지가 휴대폰 비율을 유지해 그림이 제자리에 남아요",
        "ページはスマホの比率のままなので、手書きが元の位置に残ります"
    )
    val share get() = t("Share", "Condividi", "Teilen", "Partager", "Compartir", "分享", "공유", "共有")
    val save get() = t("Save", "Salva", "Speichern", "Enregistrer", "Guardar", "保存", "저장", "保存")
    val print get() = t("Print", "Stampa", "Drucken", "Imprimer", "Imprimir", "打印", "인쇄", "印刷")
    val saved get() = t("Saved", "Salvato", "Gespeichert", "Enregistré", "Guardado", "已保存", "저장됨", "保存しました")
    val couldNotExport get() = t(
        "Couldn't export the page",
        "Impossibile esportare la pagina",
        "Die Seite konnte nicht exportiert werden",
        "Impossible d'exporter la page",
        "No se pudo exportar la página",
        "无法导出页面",
        "페이지를 내보낼 수 없어요",
        "ページをエクスポートできませんでした"
    )
}

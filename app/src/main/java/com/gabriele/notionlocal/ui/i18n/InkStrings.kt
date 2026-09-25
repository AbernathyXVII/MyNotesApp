package com.gabriele.notionlocal.ui.i18n

import com.gabriele.notionlocal.data.entity.InkTool

/** Le scritte del disegno a mano (dal 25/09/2026): la voce nei tre puntini, la barra, le finestre degli strumenti. */
object InkStrings {

    private fun t(en: String, it: String, de: String, fr: String, es: String, zh: String, ko: String, ja: String) =
        Strings.t(en, it, de, fr, es, zh, ko, ja)

    val draw get() = t("Draw", "Disegna", "Zeichnen", "Dessiner", "Dibujar", "绘图", "그리기", "描画")
    val done get() = t("Done", "Fine", "Fertig", "Terminé", "Listo", "完成", "완료", "完了")
    val undo get() = t("Undo", "Annulla", "Rückgängig", "Annuler", "Deshacer", "撤销", "실행 취소", "元に戻す")
    val redo get() = t("Redo", "Ripeti", "Wiederholen", "Rétablir", "Rehacer", "重做", "다시 실행", "やり直し")
    val eraser get() = t("Eraser", "Gomma", "Radierer", "Gomme", "Borrador", "橡皮擦", "지우개", "消しゴム")
    val thickness get() = t("Thickness", "Spessore", "Stärke", "Épaisseur", "Grosor", "粗细", "굵기", "太さ")
    val types get() = t("Types", "Tipi", "Typen", "Types", "Tipos", "类型", "유형", "種類")
    val strokeEraser get() = t("Stroke Eraser", "Gomma per tratti", "Strich-Radierer", "Gomme de trait", "Borrador de trazos", "笔画橡皮擦", "획 지우개", "ストローク消しゴム")
    val pointEraser get() = t("Point Eraser", "Gomma a punto", "Punkt-Radierer", "Gomme point", "Borrador de puntos", "点橡皮擦", "점 지우개", "ポイント消しゴム")
    val colors get() = t("Colors", "Colori", "Farben", "Couleurs", "Colores", "颜色", "색상", "色")
    val textColor get() = t("Text color", "Colore del testo", "Textfarbe", "Couleur du texte", "Color del texto", "文字颜色", "텍스트 색", "文字の色")
    val thinner get() = t("Thinner", "Più sottile", "Dünner", "Plus fin", "Más fino", "更细", "더 얇게", "細く")
    val thicker get() = t("Thicker", "Più spesso", "Dicker", "Plus épais", "Más grueso", "更粗", "더 굵게", "太く")
    val fingerDraws get() = t("Draw with finger", "Disegna col dito", "Mit dem Finger zeichnen", "Dessiner au doigt", "Dibujar con el dedo", "用手指绘图", "손가락으로 그리기", "指で描く")
    val penDetected get() = t(
        "S Pen detected: from now on your finger scrolls the page. The hand in the bar turns finger drawing back on.",
        "S Pen rilevata: da ora il dito scorre la pagina. La mano nella barra fa tornare a disegnare col dito.",
        "S Pen erkannt: Ab jetzt scrollt der Finger die Seite. Die Hand in der Leiste schaltet das Fingerzeichnen wieder ein.",
        "S Pen détecté : désormais le doigt fait défiler la page. La main dans la barre réactive le dessin au doigt.",
        "S Pen detectado: desde ahora el dedo desplaza la página. La mano de la barra vuelve a activar el dibujo con el dedo.",
        "已检测到 S Pen：手指现在用于滚动页面。点栏中的手形图标可恢复手指绘图。",
        "S Pen이 감지되었습니다. 이제 손가락으로 페이지를 스크롤합니다. 바의 손 아이콘으로 손가락 그리기를 다시 켤 수 있습니다.",
        "S Pen を検出しました。これからは指でページをスクロールします。バーの手のアイコンで指描きに戻せます。"
    )

    fun toolName(tool: InkTool): String = when (tool) {
        InkTool.PEN -> t("Pen", "Penna", "Stift", "Stylo", "Bolígrafo", "钢笔", "펜", "ペン")
        InkTool.PENCIL -> t("Pencil", "Matita", "Bleistift", "Crayon", "Lápiz", "铅笔", "연필", "鉛筆")
        InkTool.MARKER -> t("Marker", "Pennarello", "Marker", "Marqueur", "Rotulador", "马克笔", "마커", "マーカー")
        InkTool.HIGHLIGHTER -> t("Highlighter", "Evidenziatore", "Textmarker", "Surligneur", "Resaltador", "荧光笔", "형광펜", "蛍光ペン")
    }
}

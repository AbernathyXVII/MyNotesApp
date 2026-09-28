package com.gabriele.notionlocal.ui.i18n

import com.gabriele.notionlocal.data.entity.ChartAggregation
import com.gabriele.notionlocal.data.entity.ChartHeight
import com.gabriele.notionlocal.data.entity.ChartType

/** Le scritte della vista Chart dei database (dal 28/09/2026). */
object ChartStrings {

    private fun t(en: String, it: String, de: String, fr: String, es: String, zh: String, ko: String, ja: String) =
        Strings.t(en, it, de, fr, es, zh, ko, ja)

    val chart get() = t("Chart", "Grafico", "Diagramm", "Graphique", "Gráfico", "图表", "차트", "グラフ")
    val chartType get() = t("Chart type", "Tipo di grafico", "Diagrammtyp", "Type de graphique", "Tipo de gráfico", "图表类型", "차트 유형", "グラフの種類")

    fun typeName(type: ChartType) = when (type) {
        ChartType.VERTICAL -> t("Vertical", "Verticale", "Vertikal", "Vertical", "Vertical", "垂直", "세로", "縦棒")
        ChartType.HORIZONTAL -> t("Horizontal", "Orizzontale", "Horizontal", "Horizontal", "Horizontal", "水平", "가로", "横棒")
        ChartType.LINE -> t("Line", "Linea", "Linie", "Ligne", "Línea", "折线", "선", "折れ線")
        ChartType.DONUT -> t("Donut", "Ciambella", "Ring", "Anneau", "Anillo", "环形", "도넛", "ドーナツ")
    }

    val xAxis get() = t("X axis", "Asse X", "X-Achse", "Axe X", "Eje X", "X 轴", "X축", "X 軸")
    val yAxis get() = t("Y axis", "Asse Y", "Y-Achse", "Axe Y", "Eje Y", "Y 轴", "Y축", "Y 軸")
    val data get() = t("Data", "Dati", "Daten", "Données", "Datos", "数据", "데이터", "データ")
    val style get() = t("Style", "Stile", "Stil", "Style", "Estilo", "样式", "스타일", "スタイル")
    val whatToShow get() = t("What to show", "Cosa mostrare", "Was anzeigen", "Quoi afficher", "Qué mostrar", "显示内容", "표시할 항목", "表示する内容")
    val sliceBy get() = t("Slice by", "Fette per", "Aufteilen nach", "Découper par", "Dividir por", "切片依据", "분할 기준", "分割の基準")
    val sortBy get() = t("Sort by", "Ordina per", "Sortieren nach", "Trier par", "Ordenar por", "排序方式", "정렬 기준", "並べ替え")
    val omitZeroValues get() = t("Omit zero values", "Ometti i valori zero", "Nullwerte auslassen", "Omettre les valeurs nulles", "Omitir valores cero", "省略零值", "0 값 생략", "ゼロの値を省く")
    val decimalPlaces get() = t("Decimal places", "Cifre decimali", "Dezimalstellen", "Décimales", "Decimales", "小数位数", "소수 자릿수", "小数点以下の桁数")
    val cumulative get() = t("Cumulative", "Cumulativo", "Kumulativ", "Cumulatif", "Acumulado", "累计", "누적", "累積")
    val range get() = t("Range", "Intervallo", "Bereich", "Plage", "Rango", "范围", "범위", "範囲")
    val referenceLine get() = t("Reference line", "Linea di riferimento", "Referenzlinie", "Ligne de référence", "Línea de referencia", "参考线", "기준선", "基準線")
    val color get() = t("Color", "Colore", "Farbe", "Couleur", "Color", "颜色", "색", "色")
    val moreStyleOptions get() = t("More style options", "Altre opzioni di stile", "Weitere Stiloptionen", "Plus d'options de style", "Más opciones de estilo", "更多样式选项", "추가 스타일 옵션", "その他のスタイル")
    val auto get() = t("Auto", "Automatico", "Automatisch", "Auto", "Automático", "自动", "자동", "自動")
    val default get() = t("Default", "Predefinito", "Standard", "Par défaut", "Predeterminado", "默认", "기본값", "デフォルト")
    val count get() = t("Count", "Conteggio", "Anzahl", "Nombre", "Recuento", "计数", "개수", "件数")
    val calculate get() = t("Calculate", "Calcola", "Berechnen", "Calculer", "Calcular", "计算", "계산", "計算")

    fun aggregationName(aggregation: ChartAggregation) = when (aggregation) {
        ChartAggregation.VALUES -> t("Values", "Valori", "Werte", "Valeurs", "Valores", "值", "값", "値")
        ChartAggregation.SUM -> t("Sum", "Somma", "Summe", "Somme", "Suma", "求和", "합계", "合計")
        ChartAggregation.AVERAGE -> t("Average", "Media", "Durchschnitt", "Moyenne", "Promedio", "平均值", "평균", "平均")
        ChartAggregation.MIN -> t("Min", "Minimo", "Min", "Min", "Mín", "最小值", "최솟값", "最小")
        ChartAggregation.MAX -> t("Max", "Massimo", "Max", "Max", "Máx", "最大值", "최댓값", "最大")
        ChartAggregation.MEDIAN -> t("Median", "Mediana", "Median", "Médiane", "Mediana", "中位数", "중앙값", "中央値")
        ChartAggregation.DISTINCT -> t("Distinct values", "Valori distinti", "Eindeutige Werte", "Valeurs distinctes", "Valores distintos", "不同值", "고유 값", "個別の値")
    }

    /** "Price (Values)": la proprietà e come si fa il suo numero. */
    fun valueName(column: String, aggregation: ChartAggregation) = "$column (${aggregationName(aggregation)})"

    fun sortLabelAsc(name: String) = "$name A → Z"
    fun sortLabelDesc(name: String) = "$name Z → A"
    fun sortValueAsc(name: String) = t("$name Low → High", "$name dal più basso", "$name aufsteigend", "$name croissant", "$name de menor a mayor", "$name 从低到高", "$name 낮은 순", "$name 低い順")
    fun sortValueDesc(name: String) = t("$name High → Low", "$name dal più alto", "$name absteigend", "$name décroissant", "$name de mayor a menor", "$name 从高到低", "$name 높은 순", "$name 高い順")

    val min get() = t("Min", "Minimo", "Min", "Min", "Mín", "最小", "최소", "最小")
    val max get() = t("Max", "Massimo", "Max", "Max", "Máx", "最大", "최대", "最大")
    val rangeHint get() = t(
        "Leave a field empty to let the chart choose.",
        "Lascia vuoto un campo e lo sceglie il grafico.",
        "Ein leeres Feld wählt das Diagramm selbst.",
        "Un champ vide est choisi par le graphique.",
        "Deja un campo vacío y lo elige el gráfico.",
        "留空则由图表自动选择。",
        "비워 두면 차트가 정합니다.",
        "空欄にするとグラフが決めます。"
    )
    fun linesCount(n: Int) =
        if (n == 1) t("1 line", "1 linea", "1 Linie", "1 ligne", "1 línea", "1 条线", "선 1개", "1 本")
        else t("$n lines", "$n linee", "$n Linien", "$n lignes", "$n líneas", "$n 条线", "선 ${n}개", "${n} 本")
    val addReferenceLine get() = t("Add reference line", "Aggiungi una linea di riferimento", "Referenzlinie hinzufügen", "Ajouter une ligne de référence", "Añadir línea de referencia", "添加参考线", "기준선 추가", "基準線を追加")
    val value get() = t("Value", "Valore", "Wert", "Valeur", "Valor", "值", "값", "値")
    val labelOptional get() = t("Label (optional)", "Nome (facoltativo)", "Beschriftung (optional)", "Libellé (facultatif)", "Etiqueta (opcional)", "标签（可选）", "레이블(선택)", "ラベル（任意）")
    val height get() = t("Height", "Altezza", "Höhe", "Hauteur", "Altura", "高度", "높이", "高さ")

    fun heightName(height: ChartHeight) = when (height) {
        ChartHeight.SMALL -> t("Small", "Piccola", "Klein", "Petite", "Pequeña", "小", "작게", "小")
        ChartHeight.MEDIUM -> t("Medium", "Media", "Mittel", "Moyenne", "Mediana", "中", "보통", "中")
        ChartHeight.LARGE -> t("Large", "Grande", "Groß", "Grande", "Grande", "大", "크게", "大")
        ChartHeight.EXTRA_LARGE -> t("Extra large", "Molto grande", "Sehr groß", "Très grande", "Muy grande", "特大", "매우 크게", "特大")
    }

    val gridLines get() = t("Grid lines", "Linee della griglia", "Gitternetzlinien", "Quadrillage", "Líneas de cuadrícula", "网格线", "격자선", "グリッド線")
    val dataLabels get() = t("Data labels", "Etichette dei dati", "Datenbeschriftungen", "Étiquettes de données", "Etiquetas de datos", "数据标签", "데이터 레이블", "データラベル")
    val smoothLine get() = t("Smooth line", "Linea morbida", "Geglättete Linie", "Ligne lissée", "Línea suave", "平滑线", "부드러운 선", "滑らかな線")
    val showLegend get() = t("Show legend", "Mostra la legenda", "Legende anzeigen", "Afficher la légende", "Mostrar leyenda", "显示图例", "범례 표시", "凡例を表示")
    val showTotal get() = t("Show total", "Mostra il totale", "Summe anzeigen", "Afficher le total", "Mostrar total", "显示总计", "합계 표시", "合計を表示")
    fun total(name: String) = t("Total $name", "$name in tutto", "$name gesamt", "$name total", "$name total", "$name 总计", "$name 합계", "$name 合計")

    val nothingToShow get() = t(
        "Nothing to show yet: add some pages to this database.",
        "Ancora niente da mostrare: aggiungi qualche pagina a questo database.",
        "Noch nichts anzuzeigen: Füge dieser Datenbank Seiten hinzu.",
        "Rien à afficher pour l'instant : ajoutez des pages à cette base.",
        "Nada que mostrar todavía: añade páginas a esta base de datos.",
        "暂无可显示内容：请向此数据库添加页面。",
        "아직 표시할 내용이 없습니다. 이 데이터베이스에 페이지를 추가하세요.",
        "まだ表示するものがありません。このデータベースにページを追加してください。"
    )
    val checked get() = t("Checked", "Spuntata", "Aktiviert", "Cochée", "Marcada", "已勾选", "선택됨", "チェック済み")
    val unchecked get() = t("Unchecked", "Non spuntata", "Nicht aktiviert", "Non cochée", "Sin marcar", "未勾选", "선택 안 됨", "未チェック")
    val empty get() = t("Empty", "Vuoto", "Leer", "Vide", "Vacío", "空", "비어 있음", "空")
}

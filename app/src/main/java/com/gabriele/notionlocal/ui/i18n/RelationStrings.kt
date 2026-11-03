package com.gabriele.notionlocal.ui.i18n

import com.gabriele.notionlocal.data.entity.RollupCalc

/** Le scritte di "Relation" e "Rollup" (dal 21/10/2026). */
object RelationStrings {

    private fun t(en: String, it: String, de: String, fr: String, es: String, zh: String, ko: String, ja: String) =
        Strings.t(en, it, de, fr, es, zh, ko, ja)

    val relatedTo get() = t("Related to", "Collegata a", "Verknüpft mit", "Liée à", "Relacionada con", "关联到", "연결 대상", "リレーション先")
    val chooseDatabase get() = t(
        "Choose a database", "Scegli un database", "Datenbank wählen", "Choisir une base de données",
        "Elegir una base de datos", "选择数据库", "데이터베이스 선택", "データベースを選択"
    )
    val chooseDatabaseHint get() = t(
        "The rows of this database link to the rows of the one you choose. It can also be this one.",
        "Le righe di questo database si collegano alle righe di quello che scegli. Può essere anche questo.",
        "Die Zeilen dieser Datenbank werden mit den Zeilen der gewählten verknüpft. Es kann auch diese sein.",
        "Les lignes de cette base se lient à celles de la base choisie. Ce peut être celle-ci.",
        "Las filas de esta base se vinculan con las de la que elijas. También puede ser esta.",
        "这个数据库的行会关联到你选择的数据库的行，也可以是这个数据库本身。",
        "이 데이터베이스의 행이 선택한 데이터베이스의 행과 연결됩니다. 이 데이터베이스여도 됩니다.",
        "このデータベースの行を、選んだデータベースの行とつなげます。このデータベース自身でもかまいません。"
    )
    fun showOn(database: String) = t(
        "Show on “$database”", "Mostra anche in “$database”", "Auch in „$database“ anzeigen", "Afficher aussi dans « $database »",
        "Mostrar también en “$database”", "也显示在“$database”", "“$database”에도 표시", "「$database」にも表示"
    )
    val showOnHint get() = t(
        "Two-way: the other database gets a Relation too, and both stay in sync.",
        "Nei due sensi: anche l'altro database ha una Relation, e le due restano allineate.",
        "In beide Richtungen: Auch die andere Datenbank bekommt eine Relation, beide bleiben synchron.",
        "Dans les deux sens : l'autre base reçoit aussi une Relation, et les deux restent synchronisées.",
        "En ambos sentidos: la otra base también tiene una Relation, y las dos quedan sincronizadas.",
        "双向：另一个数据库也会有一个关联，两者保持同步。",
        "양방향: 다른 데이터베이스에도 관계형이 생기고, 둘이 함께 맞춰집니다.",
        "双方向：もう一方のデータベースにもリレーションができ、両方がそろいます。"
    )
    val twinName get() = t("Name there", "Nome di là", "Name dort", "Nom là-bas", "Nombre allí", "那边的名称", "저쪽 이름", "向こうでの名前")
    val databaseMissing get() = t(
        "Database not found", "Database non trovato", "Datenbank nicht gefunden", "Base de données introuvable",
        "Base de datos no encontrada", "找不到数据库", "데이터베이스를 찾을 수 없음", "データベースが見つかりません"
    )
    fun rowsOf(database: String) = t(
        "Rows of “$database”", "Righe di “$database”", "Zeilen von „$database“", "Lignes de « $database »",
        "Filas de “$database”", "“$database”的行", "“$database”의 행", "「$database」の行"
    )
    val linked get() = t("Linked", "Collegate", "Verknüpft", "Liées", "Vinculadas", "已关联", "연결됨", "リンク済み")
    val noRows get() = t("No rows", "Nessuna riga", "Keine Zeilen", "Aucune ligne", "Sin filas", "没有行", "행 없음", "行がありません")
    val relation get() = t("Relation", "Relazione", "Relation", "Relation", "Relación", "关联", "관계형", "リレーション")
    val property get() = t("Property", "Proprietà", "Eigenschaft", "Propriété", "Propiedad", "属性", "속성", "プロパティ")
    val calculate get() = t("Calculate", "Calcola", "Berechnen", "Calculer", "Calcular", "计算", "계산", "計算")
    val needsRelation get() = t(
        "A Rollup reads the rows linked by a Relation: add a Relation to this database first.",
        "Un Rollup legge le righe collegate da una Relation: prima aggiungi una Relation a questo database.",
        "Ein Rollup liest die über eine Relation verknüpften Zeilen: Füge zuerst eine Relation hinzu.",
        "Un Rollup lit les lignes liées par une Relation : ajoutez d'abord une Relation à cette base.",
        "Un Rollup lee las filas vinculadas por una Relation: primero añade una Relation a esta base.",
        "汇总读取关联所连接的行：请先给这个数据库添加一个关联。",
        "롤업은 관계형으로 연결된 행을 읽습니다. 먼저 이 데이터베이스에 관계형을 추가하세요.",
        "ロールアップはリレーションでつながった行を読みます。先にリレーションを追加してください。"
    )
    val name get() = t("Name", "Nome", "Name", "Nom", "Nombre", "名称", "이름", "名前")

    fun calcName(calc: RollupCalc): String = when (calc) {
        RollupCalc.SHOW_ORIGINAL -> t("Show original", "Mostra i valori", "Original anzeigen", "Afficher l'original", "Mostrar original", "显示原始值", "원본 표시", "元の値を表示")
        RollupCalc.SHOW_UNIQUE -> t("Show unique values", "Mostra i valori unici", "Eindeutige Werte anzeigen", "Valeurs uniques", "Valores únicos", "显示唯一值", "고유 값 표시", "一意の値を表示")
        RollupCalc.COUNT_ALL -> t("Count all", "Conta tutte", "Alle zählen", "Tout compter", "Contar todo", "全部计数", "모두 세기", "すべてカウント")
        RollupCalc.COUNT_VALUES -> t("Count values", "Conta i valori", "Werte zählen", "Compter les valeurs", "Contar valores", "值计数", "값 세기", "値をカウント")
        RollupCalc.COUNT_UNIQUE -> t("Count unique values", "Conta i valori unici", "Eindeutige Werte zählen", "Compter les valeurs uniques", "Contar valores únicos", "唯一值计数", "고유 값 세기", "一意の値をカウント")
        RollupCalc.COUNT_EMPTY -> t("Count empty", "Conta le vuote", "Leere zählen", "Compter les vides", "Contar vacías", "空值计数", "빈 항목 세기", "空をカウント")
        RollupCalc.COUNT_NOT_EMPTY -> t("Count not empty", "Conta le piene", "Nicht leere zählen", "Compter les non vides", "Contar no vacías", "非空计数", "비어 있지 않은 항목 세기", "空でないものをカウント")
        RollupCalc.PERCENT_EMPTY -> t("Percent empty", "Percentuale vuote", "Prozent leer", "Pourcentage vide", "Porcentaje vacío", "空值百分比", "빈 항목 비율", "空の割合")
        RollupCalc.PERCENT_NOT_EMPTY -> t("Percent not empty", "Percentuale piene", "Prozent nicht leer", "Pourcentage non vide", "Porcentaje no vacío", "非空百分比", "비어 있지 않은 비율", "空でない割合")
        RollupCalc.SUM -> t("Sum", "Somma", "Summe", "Somme", "Suma", "求和", "합계", "合計")
        RollupCalc.AVERAGE -> t("Average", "Media", "Durchschnitt", "Moyenne", "Promedio", "平均值", "평균", "平均")
        RollupCalc.MEDIAN -> t("Median", "Mediana", "Median", "Médiane", "Mediana", "中位数", "중앙값", "中央値")
        RollupCalc.MIN -> t("Min", "Minimo", "Minimum", "Min", "Mínimo", "最小值", "최솟값", "最小")
        RollupCalc.MAX -> t("Max", "Massimo", "Maximum", "Max", "Máximo", "最大值", "최댓값", "最大")
        RollupCalc.RANGE -> t("Range", "Intervallo", "Spanne", "Écart", "Rango", "范围", "범위", "範囲")
        RollupCalc.EARLIEST_DATE -> t("Earliest date", "Data più vecchia", "Frühestes Datum", "Date la plus ancienne", "Fecha más antigua", "最早日期", "가장 이른 날짜", "最も早い日付")
        RollupCalc.LATEST_DATE -> t("Latest date", "Data più recente", "Spätestes Datum", "Date la plus récente", "Fecha más reciente", "最晚日期", "가장 늦은 날짜", "最も遅い日付")
        RollupCalc.DATE_RANGE -> t("Date range (days)", "Giorni fra la prima e l'ultima", "Datumsspanne (Tage)", "Écart de dates (jours)", "Rango de fechas (días)", "日期范围（天）", "날짜 범위(일)", "日付の範囲（日）")
        RollupCalc.CHECKED -> t("Checked", "Spuntate", "Angehakt", "Cochées", "Marcadas", "已勾选", "체크됨", "チェック済み")
        RollupCalc.UNCHECKED -> t("Unchecked", "Non spuntate", "Nicht angehakt", "Non cochées", "Sin marcar", "未勾选", "체크 안 됨", "未チェック")
        RollupCalc.PERCENT_CHECKED -> t("Percent checked", "Percentuale spuntate", "Prozent angehakt", "Pourcentage cochées", "Porcentaje marcadas", "已勾选百分比", "체크 비율", "チェック済みの割合")
        RollupCalc.PERCENT_UNCHECKED -> t("Percent unchecked", "Percentuale non spuntate", "Prozent nicht angehakt", "Pourcentage non cochées", "Porcentaje sin marcar", "未勾选百分比", "체크 안 된 비율", "未チェックの割合")
    }
}

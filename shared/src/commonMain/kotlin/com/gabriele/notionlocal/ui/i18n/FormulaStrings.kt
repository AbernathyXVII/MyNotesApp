package com.gabriele.notionlocal.ui.i18n

import com.gabriele.notionlocal.data.formula.FormulaError

/**
 * Le scritte delle formule (dal 28/10/2026): la finestra in cui si scrive
 * la formula, gli errori e le spiegazioni delle funzioni. Stessa regola di
 * `Strings`: ogni voce con le sue otto traduzioni. I nomi delle funzioni
 * restano in inglese, come su Notion: sono quelli che si scrivono.
 */
object FormulaStrings {

    private fun t(en: String, it: String, de: String, fr: String, es: String, zh: String, ko: String, ja: String) =
        Strings.t(en, it, de, fr, es, zh, ko, ja)

    val editFormula get() = t("Edit formula", "Modifica formula", "Formel bearbeiten", "Modifier la formule", "Editar fórmula", "编辑公式", "수식 편집", "数式を編集")
    val noFormula get() = t("No formula yet", "Ancora nessuna formula", "Noch keine Formel", "Pas encore de formule", "Aún no hay fórmula", "还没有公式", "아직 수식이 없어요", "まだ数式がありません")
    val placeholder get() = t(
        "Write a formula, e.g. prop(\"Price\") * prop(\"Quantity\")",
        "Scrivi una formula, es. prop(\"Prezzo\") * prop(\"Quantità\")",
        "Schreibe eine Formel, z. B. prop(\"Preis\") * prop(\"Menge\")",
        "Écrivez une formule, ex. prop(\"Prix\") * prop(\"Quantité\")",
        "Escribe una fórmula, p. ej. prop(\"Precio\") * prop(\"Cantidad\")",
        "输入公式，例如 prop(\"价格\") * prop(\"数量\")",
        "수식을 입력하세요. 예: prop(\"가격\") * prop(\"수량\")",
        "数式を入力（例: prop(\"価格\") * prop(\"数量\")）"
    )
    fun resultFor(title: String) = t(
        "Result for “$title”", "Risultato per «$title»", "Ergebnis für „$title“", "Résultat pour « $title »",
        "Resultado para «$title»", "“$title”的结果", "“$title”의 결과", "「$title」の結果"
    )
    val noRows get() = t(
        "Add a row to see the result here.",
        "Aggiungi una riga per vedere qui il risultato.",
        "Füge eine Zeile hinzu, um hier das Ergebnis zu sehen.",
        "Ajoutez une ligne pour voir le résultat ici.",
        "Añade una fila para ver aquí el resultado.",
        "添加一行即可在此看到结果。",
        "행을 추가하면 여기에 결과가 표시돼요.",
        "行を追加すると、ここに結果が表示されます。"
    )
    val empty get() = t("Empty", "Vuoto", "Leer", "Vide", "Vacío", "空", "비어 있음", "空")
    val checked get() = t("Checked", "Spuntata", "Angehakt", "Cochée", "Marcada", "已勾选", "체크됨", "チェック済み")
    val unchecked get() = t("Unchecked", "Non spuntata", "Nicht angehakt", "Non cochée", "Sin marcar", "未勾选", "체크 안 됨", "未チェック")
    val error get() = t("Error", "Errore", "Fehler", "Erreur", "Error", "错误", "오류", "エラー")
    val properties get() = t("Properties", "Proprietà", "Eigenschaften", "Propriétés", "Propiedades", "属性", "속성", "プロパティ")
    val operators get() = t("Operators", "Operatori", "Operatoren", "Opérateurs", "Operadores", "运算符", "연산자", "演算子")
    val examples get() = t("Examples", "Esempi", "Beispiele", "Exemples", "Ejemplos", "示例", "예시", "例")
    val logic get() = t("Logic", "Logica", "Logik", "Logique", "Lógica", "逻辑", "논리", "論理")
    val numbers get() = t("Numbers", "Numeri", "Zahlen", "Nombres", "Números", "数字", "숫자", "数値")
    val text get() = t("Text", "Testo", "Text", "Texte", "Texto", "文本", "텍스트", "テキスト")
    val dates get() = t("Dates", "Date", "Daten", "Dates", "Fechas", "日期", "날짜", "日付")
    val units get() = t(
        "Units: \"years\", \"quarters\", \"months\", \"weeks\", \"days\", \"hours\", \"minutes\"",
        "Unità: \"years\", \"quarters\", \"months\", \"weeks\", \"days\", \"hours\", \"minutes\" (o \"anni\", \"mesi\", \"settimane\", \"giorni\", \"ore\", \"minuti\")",
        "Einheiten: \"years\", \"quarters\", \"months\", \"weeks\", \"days\", \"hours\", \"minutes\"",
        "Unités : \"years\", \"quarters\", \"months\", \"weeks\", \"days\", \"hours\", \"minutes\"",
        "Unidades: \"years\", \"quarters\", \"months\", \"weeks\", \"days\", \"hours\", \"minutes\"",
        "单位：\"years\"、\"quarters\"、\"months\"、\"weeks\"、\"days\"、\"hours\"、\"minutes\"",
        "단위: \"years\", \"quarters\", \"months\", \"weeks\", \"days\", \"hours\", \"minutes\"",
        "単位: \"years\"、\"quarters\"、\"months\"、\"weeks\"、\"days\"、\"hours\"、\"minutes\""
    )

    // --- Gli esempi ---

    val exampleDaysLeft get() = t("Days left", "Giorni che mancano", "Verbleibende Tage", "Jours restants", "Días que faltan", "剩余天数", "남은 일수", "残り日数")
    val examplePriceTimesQuantity get() = t("Price × quantity", "Prezzo × quantità", "Preis × Menge", "Prix × quantité", "Precio × cantidad", "价格 × 数量", "가격 × 수량", "価格 × 数量")
    val exampleIfChecked get() = t("If it's checked…", "Se è spuntato…", "Wenn angehakt…", "Si c'est coché…", "Si está marcado…", "如果已勾选…", "체크되어 있으면…", "チェックされていたら…")
    val exampleDone get() = t("Done", "Finito", "Erledigt", "Terminé", "Hecho", "完成", "완료", "完了")
    val exampleToDo get() = t("To do", "Da fare", "Zu erledigen", "À faire", "Por hacer", "待办", "할 일", "未完了")

    // --- Gli errori ---

    /** Che cosa non va, detto a parole: sotto la formula, al posto del risultato. */
    fun errorText(e: FormulaError): String {
        val d = e.detail
        return when (e.kind) {
            FormulaError.Kind.SYNTAX, FormulaError.Kind.UNEXPECTED -> t(
                "Something is wrong near “$d”", "C'è qualcosa che non va vicino a «$d»", "Etwas stimmt nicht bei „$d“",
                "Quelque chose ne va pas près de « $d »", "Algo no va cerca de «$d»", "“$d”附近有错误", "“$d” 근처에 문제가 있어요", "「$d」の近くに誤りがあります"
            )
            FormulaError.Kind.MISSING -> t(
                "Missing “$d”", "Manca «$d»", "„$d“ fehlt", "Il manque « $d »", "Falta «$d»", "缺少“$d”", "“$d”이(가) 빠졌어요", "「$d」がありません"
            )
            FormulaError.Kind.INCOMPLETE -> t(
                "The formula stops halfway", "La formula si ferma a metà", "Die Formel hört mittendrin auf", "La formule s'arrête à mi-chemin",
                "La fórmula se queda a medias", "公式不完整", "수식이 중간에 끝났어요", "数式が途中で終わっています"
            )
            FormulaError.Kind.UNKNOWN_NAME -> t(
                "“$d” isn't a function: properties are written prop(\"$d\")",
                "«$d» non è una funzione: le proprietà si scrivono prop(\"$d\")",
                "„$d“ ist keine Funktion: Eigenschaften schreibt man prop(\"$d\")",
                "« $d » n'est pas une fonction : les propriétés s'écrivent prop(\"$d\")",
                "«$d» no es una función: las propiedades se escriben prop(\"$d\")",
                "“$d”不是函数：属性要写成 prop(\"$d\")",
                "“$d”은(는) 함수가 아니에요. 속성은 prop(\"$d\")로 써요",
                "「$d」は関数ではありません。プロパティは prop(\"$d\") と書きます"
            )
            FormulaError.Kind.PROP_NEEDS_NAME -> t(
                "prop() wants the name of a property in quotes: prop(\"Name\")",
                "prop() vuole il nome di una proprietà fra virgolette: prop(\"Name\")",
                "prop() braucht den Namen einer Eigenschaft in Anführungszeichen: prop(\"Name\")",
                "prop() veut le nom d'une propriété entre guillemets : prop(\"Name\")",
                "prop() necesita el nombre de una propiedad entre comillas: prop(\"Name\")",
                "prop() 需要带引号的属性名：prop(\"Name\")",
                "prop()에는 따옴표로 감싼 속성 이름이 필요해요: prop(\"Name\")",
                "prop() には引用符で囲んだプロパティ名が必要です: prop(\"Name\")"
            )
            FormulaError.Kind.UNKNOWN_PROPERTY -> t(
                "There's no property “$d”", "Non c'è una proprietà «$d»", "Es gibt keine Eigenschaft „$d“", "Il n'y a pas de propriété « $d »",
                "No hay ninguna propiedad «$d»", "没有属性“$d”", "“$d” 속성이 없어요", "プロパティ「$d」はありません"
            )
            FormulaError.Kind.UNKNOWN_FUNCTION -> t(
                "There's no function “$d”", "Non c'è una funzione «$d»", "Es gibt keine Funktion „$d“", "Il n'y a pas de fonction « $d »",
                "No hay ninguna función «$d»", "没有函数“$d”", "“$d” 함수가 없어요", "関数「$d」はありません"
            )
            FormulaError.Kind.WRONG_ARGUMENTS -> t(
                "$d(): too many or too few values", "$d(): troppi o troppo pochi valori", "$d(): zu viele oder zu wenige Werte",
                "$d() : trop ou trop peu de valeurs", "$d(): demasiados o muy pocos valores", "$d()：参数数量不对", "$d(): 값의 개수가 맞지 않아요", "$d(): 値の数が合いません"
            )
            FormulaError.Kind.TYPE -> t(
                "“$d” can't work with these values", "«$d» non funziona con questi valori", "„$d“ funktioniert mit diesen Werten nicht",
                "« $d » ne fonctionne pas avec ces valeurs", "«$d» no funciona con estos valores", "“$d”无法处理这些值", "“$d”은(는) 이 값으로 계산할 수 없어요", "「$d」はこれらの値では使えません"
            )
            FormulaError.Kind.CIRCULAR -> t(
                "The formula reads itself (through “$d”)", "La formula legge se stessa (passando da «$d»)", "Die Formel liest sich selbst (über „$d“)",
                "La formule se lit elle-même (via « $d »)", "La fórmula se lee a sí misma (a través de «$d»)", "公式引用了自身（通过“$d”）",
                "수식이 자기 자신을 읽고 있어요(“$d” 통해)", "数式が自分自身を参照しています（「$d」経由）"
            )
            FormulaError.Kind.DIVISION_BY_ZERO -> t(
                "Division by zero", "Divisione per zero", "Division durch null", "Division par zéro", "División entre cero", "除以零", "0으로 나눌 수 없어요", "ゼロ除算"
            )
        }
    }

    // --- Le funzioni ---

    /** Che cosa fa una funzione dell'elenco, in una riga. */
    fun describe(name: String): String = when (name) {
        "if" -> t("The first value if the condition is true, otherwise the second", "Il primo valore se la condizione è vera, se no il secondo", "Der erste Wert, wenn die Bedingung stimmt, sonst der zweite", "La première valeur si la condition est vraie, sinon la seconde", "El primer valor si la condición es cierta, si no el segundo", "条件为真取第一个值，否则取第二个", "조건이 참이면 첫 번째 값, 아니면 두 번째 값", "条件が真なら最初の値、そうでなければ2番目の値")
        "ifs" -> t("The value of the first true condition, or the last one", "Il valore della prima condizione vera, o l'ultimo", "Der Wert der ersten wahren Bedingung, sonst der letzte", "La valeur de la première condition vraie, sinon la dernière", "El valor de la primera condición cierta, o el último", "第一个为真的条件对应的值，否则取最后一个", "처음으로 참인 조건의 값, 없으면 마지막 값", "最初に真になった条件の値、なければ最後の値")
        "and" -> t("True if both are true", "Vero se lo sono entrambi", "Wahr, wenn beide wahr sind", "Vrai si les deux le sont", "Cierto si ambos lo son", "两者都为真时为真", "둘 다 참이면 참", "両方が真なら真")
        "or" -> t("True if at least one is true", "Vero se almeno uno lo è", "Wahr, wenn mindestens einer wahr ist", "Vrai si au moins un l'est", "Cierto si al menos uno lo es", "至少一个为真时为真", "하나라도 참이면 참", "少なくとも1つが真なら真")
        "not" -> t("The opposite", "Il contrario", "Das Gegenteil", "Le contraire", "Lo contrario", "取反", "반대", "反対")
        "empty" -> t("True if the value is empty", "Vero se il valore è vuoto", "Wahr, wenn der Wert leer ist", "Vrai si la valeur est vide", "Cierto si el valor está vacío", "值为空时为真", "값이 비어 있으면 참", "値が空なら真")
        "round" -> t("Rounds, keeping the given decimals", "Arrotonda, coi decimali scelti", "Rundet auf die gewünschten Nachkommastellen", "Arrondit, avec les décimales choisies", "Redondea con los decimales elegidos", "四舍五入到指定小数位", "지정한 소수 자릿수로 반올림", "指定した小数点以下の桁で四捨五入")
        "floor" -> t("Rounds down", "Arrotonda per difetto", "Rundet ab", "Arrondit vers le bas", "Redondea hacia abajo", "向下取整", "내림", "切り捨て")
        "ceil" -> t("Rounds up", "Arrotonda per eccesso", "Rundet auf", "Arrondit vers le haut", "Redondea hacia arriba", "向上取整", "올림", "切り上げ")
        "abs" -> t("Without the minus sign", "Senza il meno", "Ohne Minuszeichen", "Sans le signe moins", "Sin el signo menos", "绝对值", "절댓값", "絶対値")
        "min" -> t("The smallest", "Il più piccolo", "Der kleinste", "Le plus petit", "El más pequeño", "最小值", "가장 작은 값", "最小値")
        "max" -> t("The largest", "Il più grande", "Der größte", "Le plus grand", "El más grande", "最大值", "가장 큰 값", "最大値")
        "sum" -> t("Adds the values", "Somma i valori", "Addiert die Werte", "Additionne les valeurs", "Suma los valores", "求和", "값을 더해요", "合計")
        "mean" -> t("The average", "La media", "Der Durchschnitt", "La moyenne", "La media", "平均值", "평균", "平均")
        "sqrt" -> t("Square root", "Radice quadrata", "Quadratwurzel", "Racine carrée", "Raíz cuadrada", "平方根", "제곱근", "平方根")
        "pow" -> t("Raises to a power (like ^)", "Elevamento a potenza (come ^)", "Potenz (wie ^)", "Puissance (comme ^)", "Potencia (como ^)", "乘方（同 ^）", "거듭제곱(^와 같음)", "べき乗（^ と同じ）")
        "mod" -> t("The remainder of a division (like %)", "Il resto di una divisione (come %)", "Rest einer Division (wie %)", "Le reste d'une division (comme %)", "El resto de una división (como %)", "取余（同 %）", "나머지(%와 같음)", "割り算の余り（% と同じ）")
        "toNumber" -> t("A text, a checkbox or a date as a number", "Un testo, una casella o una data come numero", "Text, Kontrollkästchen oder Datum als Zahl", "Un texte, une case ou une date en nombre", "Un texto, una casilla o una fecha como número", "把文本、复选框或日期转成数字", "텍스트·체크박스·날짜를 숫자로", "テキスト・チェックボックス・日付を数値に")
        "concat" -> t("Joins texts", "Unisce dei testi", "Verbindet Texte", "Assemble des textes", "Une textos", "拼接文本", "텍스트를 이어 붙여요", "テキストを連結")
        "join" -> t("Joins the non-empty values with a separator", "Unisce i valori non vuoti con un separatore", "Verbindet die nicht leeren Werte mit einem Trennzeichen", "Assemble les valeurs non vides avec un séparateur", "Une los valores no vacíos con un separador", "用分隔符连接非空值", "비어 있지 않은 값을 구분자로 이어요", "空でない値を区切り文字で連結")
        "length" -> t("How many characters", "Quanti caratteri", "Wie viele Zeichen", "Combien de caractères", "Cuántos caracteres", "字符数", "글자 수", "文字数")
        "lower" -> t("In lowercase", "In minuscolo", "In Kleinbuchstaben", "En minuscules", "En minúsculas", "转小写", "소문자로", "小文字に")
        "upper" -> t("In uppercase", "In maiuscolo", "In Großbuchstaben", "En majuscules", "En mayúsculas", "转大写", "대문자로", "大文字に")
        "contains" -> t("True if the text contains the piece", "Vero se il testo contiene il pezzo", "Wahr, wenn der Text das Stück enthält", "Vrai si le texte contient le morceau", "Cierto si el texto contiene el fragmento", "文本包含该片段时为真", "텍스트에 그 조각이 있으면 참", "テキストにその部分が含まれていれば真")
        "replace" -> t("Replaces the first match", "Sostituisce la prima occorrenza", "Ersetzt den ersten Treffer", "Remplace la première occurrence", "Reemplaza la primera coincidencia", "替换第一个匹配项", "처음 일치하는 부분을 바꿔요", "最初の一致を置換")
        "replaceAll" -> t("Replaces every match", "Sostituisce tutte le occorrenze", "Ersetzt alle Treffer", "Remplace toutes les occurrences", "Reemplaza todas las coincidencias", "替换所有匹配项", "일치하는 모든 부분을 바꿔요", "すべての一致を置換")
        "slice" -> t("A piece of the text, from start to end", "Un pezzo del testo, dall'inizio alla fine indicati", "Ein Stück Text, von Anfang bis Ende", "Un morceau du texte, du début à la fin", "Un fragmento del texto, de inicio a fin", "截取文本的一段", "텍스트의 일부(시작~끝)", "テキストの一部（開始〜終了）")
        "format" -> t("The value as text", "Il valore come testo", "Der Wert als Text", "La valeur en texte", "El valor como texto", "把值转成文本", "값을 텍스트로", "値をテキストに")
        "now" -> t("Now, with the time", "Adesso, con l'ora", "Jetzt, mit Uhrzeit", "Maintenant, avec l'heure", "Ahora, con la hora", "现在（含时间）", "지금(시간 포함)", "現在（時刻付き）")
        "today" -> t("Today, without the time", "Oggi, senza l'ora", "Heute, ohne Uhrzeit", "Aujourd'hui, sans l'heure", "Hoy, sin la hora", "今天（不含时间）", "오늘(시간 없음)", "今日（時刻なし）")
        "dateAdd" -> t("The date moved forward", "La data spostata avanti", "Das Datum nach vorne verschoben", "La date avancée", "La fecha movida hacia adelante", "日期向后推", "날짜를 뒤로 미뤄요", "日付を先に進める")
        "dateSubtract" -> t("The date moved back", "La data spostata indietro", "Das Datum nach hinten verschoben", "La date reculée", "La fecha movida hacia atrás", "日期向前推", "날짜를 앞으로 당겨요", "日付を前に戻す")
        "dateBetween" -> t("How much from the second date to the first", "Quanto manca dalla seconda data alla prima", "Wie viel von der zweiten bis zur ersten Datum", "Combien de la seconde date à la première", "Cuánto hay de la segunda fecha a la primera", "从第二个日期到第一个日期的间隔", "두 번째 날짜에서 첫 번째 날짜까지", "2番目の日付から1番目の日付まで")
        "formatDate" -> t("The date written your way: DD/MM/YYYY, dddd, HH:mm…", "La data scritta a modo tuo: DD/MM/YYYY, dddd, HH:mm…", "Das Datum nach Wunsch: DD/MM/YYYY, dddd, HH:mm…", "La date à votre façon : DD/MM/YYYY, dddd, HH:mm…", "La fecha a tu manera: DD/MM/YYYY, dddd, HH:mm…", "按格式写日期：DD/MM/YYYY、dddd、HH:mm…", "원하는 형식의 날짜: DD/MM/YYYY, dddd, HH:mm…", "好きな形式の日付: DD/MM/YYYY、dddd、HH:mm…")
        "year" -> t("The year", "L'anno", "Das Jahr", "L'année", "El año", "年", "연도", "年")
        "month" -> t("The month, 1–12", "Il mese, 1–12", "Der Monat, 1–12", "Le mois, 1–12", "El mes, 1–12", "月份（1–12）", "월(1–12)", "月（1–12）")
        "date" -> t("The day of the month", "Il giorno del mese", "Der Tag im Monat", "Le jour du mois", "El día del mes", "当月第几天", "일(날짜)", "日にち")
        "day" -> t("The day of the week, 1 = Monday … 7 = Sunday", "Il giorno della settimana, 1 = lunedì … 7 = domenica", "Der Wochentag, 1 = Montag … 7 = Sonntag", "Le jour de la semaine, 1 = lundi … 7 = dimanche", "El día de la semana, 1 = lunes … 7 = domingo", "星期几，1 = 周一 … 7 = 周日", "요일, 1 = 월요일 … 7 = 일요일", "曜日、1 = 月曜 … 7 = 日曜")
        "week" -> t("The week of the year", "La settimana dell'anno", "Die Kalenderwoche", "La semaine de l'année", "La semana del año", "一年中的第几周", "연중 몇째 주", "年の週番号")
        "hour" -> t("The hour", "L'ora", "Die Stunde", "L'heure", "La hora", "小时", "시", "時")
        "minute" -> t("The minutes", "I minuti", "Die Minuten", "Les minutes", "Los minutos", "分钟", "분", "分")
        "dateStart" -> t("The start of a date range", "L'inizio di un periodo", "Der Beginn eines Zeitraums", "Le début d'une période", "El inicio de un periodo", "日期范围的开始", "기간의 시작", "期間の開始")
        "dateEnd" -> t("The end of a date range", "La fine di un periodo", "Das Ende eines Zeitraums", "La fin d'une période", "El final de un periodo", "日期范围的结束", "기간의 끝", "期間の終了")
        else -> ""
    }
}

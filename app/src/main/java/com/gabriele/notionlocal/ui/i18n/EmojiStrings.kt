package com.gabriele.notionlocal.ui.i18n

import com.gabriele.notionlocal.data.EmojiCategory

/**
 * Le scritte del **selettore delle icone** (dal 07/10/2026): le schede, la
 * ricerca e le categorie delle emoji, coi nomi di Notion.
 */
object EmojiStrings {

    private fun t(en: String, it: String, de: String, fr: String, es: String, zh: String, ko: String, ja: String) =
        Strings.t(en, it, de, fr, es, zh, ko, ja)

    val emojiTab get() = t("Emoji", "Emoji", "Emoji", "Emoji", "Emoji", "表情", "이모지", "絵文字")
    val iconsTab get() = t("Icons", "Icone", "Symbole", "Icônes", "Iconos", "图标", "아이콘", "アイコン")
    val iconColor get() = t("Icon color", "Colore delle icone", "Symbolfarbe", "Couleur des icônes", "Color de los iconos", "图标颜色", "아이콘 색상", "アイコンの色")
    val uploadTab get() = t("Upload", "Carica", "Hochladen", "Importer", "Subir", "上传", "업로드", "アップロード")
    val filter get() = t("Filter…", "Filtra…", "Filtern…", "Filtrer…", "Filtrar…", "筛选…", "필터…", "フィルター…")
    val random get() = t("Random", "Casuale", "Zufällig", "Aléatoire", "Aleatorio", "随机", "무작위", "ランダム")
    val skinTone get() = t("Skin tone", "Colore della pelle", "Hautfarbe", "Teint", "Tono de piel", "肤色", "피부색", "肌の色")
    val noResults get() = t("No results", "Nessun risultato", "Keine Ergebnisse", "Aucun résultat", "Sin resultados", "没有结果", "결과 없음", "結果なし")
    val uploadImage get() = t("Upload an image", "Carica un'immagine", "Bild hochladen", "Importer une image", "Subir una imagen", "上传图片", "이미지 업로드", "画像をアップロード")
    val uploadHint get() = t(
        "Choose an image from the phone. Square images look best.",
        "Scegli un'immagine dal telefono. Quelle quadrate vengono meglio.",
        "Wähle ein Bild vom Telefon. Quadratische Bilder sehen am besten aus.",
        "Choisissez une image sur le téléphone. Les images carrées rendent le mieux.",
        "Elige una imagen del teléfono. Las cuadradas quedan mejor.",
        "从手机中选择图片。方形图片效果最好。",
        "휴대폰에서 이미지를 선택하세요. 정사각형 이미지가 가장 잘 어울려요.",
        "スマートフォンから画像を選びます。正方形の画像がいちばんきれいです。"
    )
    val addCustom get() = t("Add an image", "Aggiungi un'immagine", "Bild hinzufügen", "Ajouter une image", "Añadir una imagen", "添加图片", "이미지 추가", "画像を追加")
    val customEmpty get() = t(
        "The images you upload as icons appear here.",
        "Qui compaiono le immagini che carichi come icona.",
        "Hier erscheinen die Bilder, die du als Symbol hochlädst.",
        "Les images importées comme icône apparaissent ici.",
        "Aquí aparecen las imágenes que subes como icono.",
        "你上传为图标的图片会显示在这里。",
        "아이콘으로 업로드한 이미지가 여기에 나타나요.",
        "アイコンとしてアップロードした画像がここに表示されます。"
    )
    val useTyped get() = t("Use this emoji", "Usa questa emoji", "Dieses Emoji verwenden", "Utiliser cet emoji", "Usar este emoji", "使用此表情", "이 이모지 사용", "この絵文字を使う")

    val recent get() = t("Recent", "Recenti", "Zuletzt verwendet", "Récents", "Recientes", "最近使用", "최근", "最近")
    val custom get() = t("Custom", "Personalizzate", "Benutzerdefiniert", "Personnalisés", "Personalizados", "自定义", "사용자 지정", "カスタム")

    fun category(category: EmojiCategory): String = when (category) {
        EmojiCategory.PEOPLE -> t("People", "Persone", "Personen", "Personnes", "Personas", "人物", "사람", "人")
        EmojiCategory.NATURE -> t("Animals and nature", "Animali e natura", "Tiere und Natur", "Animaux et nature", "Animales y naturaleza", "动物与自然", "동물 및 자연", "動物と自然")
        EmojiCategory.FOOD -> t("Food and drink", "Cibo e bevande", "Essen und Trinken", "Nourriture et boissons", "Comida y bebida", "食物与饮料", "음식 및 음료", "食べ物と飲み物")
        EmojiCategory.ACTIVITY -> t("Activity", "Attività", "Aktivitäten", "Activités", "Actividades", "活动", "활동", "アクティビティ")
        EmojiCategory.TRAVEL -> t("Travel and places", "Viaggi e luoghi", "Reisen und Orte", "Voyages et lieux", "Viajes y lugares", "旅行与地点", "여행 및 장소", "旅行と場所")
        EmojiCategory.OBJECTS -> t("Objects", "Oggetti", "Objekte", "Objets", "Objetos", "物品", "사물", "物")
        EmojiCategory.SYMBOLS -> t("Symbols", "Simboli", "Symbole", "Symboles", "Símbolos", "符号", "기호", "記号")
        EmojiCategory.FLAGS -> t("Flags", "Bandiere", "Flaggen", "Drapeaux", "Banderas", "旗帜", "깃발", "旗")
    }
}

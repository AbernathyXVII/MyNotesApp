package com.gabriele.notionlocal.ui.i18n

import com.gabriele.notionlocal.data.settings.AppLanguage
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.format.Formats

/**
 * Le voci dell'app nelle otto lingue delle impostazioni.
 *
 * **Ogni voce con tutte le sue traduzioni in un posto solo.** Con i file
 * di risorse di Android le otto versioni della stessa frase starebbero in
 * otto file diversi, e aggiungerne una vorrebbe dire aprirli tutti; qui
 * una voce nuova si scrive una volta, e una traduzione mancante non
 * compila.
 *
 * Si traduce solo quello che scrive l'app — menu, impostazioni, nomi dei
 * tipi di blocco e di proprietà — mai quello che scrive l'utente.
 *
 * La lingua letta qui è uno stato di Compose: cambiandola nelle
 * impostazioni, ogni schermata che mostra una di queste voci si ridisegna
 * da sola nella lingua nuova.
 */
object Strings {

    internal fun t(
        en: String,
        it: String,
        de: String,
        fr: String,
        es: String,
        zh: String,
        ko: String,
        ja: String
    ): String = when (AppSettings.language) {
        AppLanguage.ENGLISH -> en
        AppLanguage.ITALIAN -> it
        AppLanguage.GERMAN -> de
        AppLanguage.FRENCH -> fr
        AppLanguage.SPANISH -> es
        AppLanguage.CHINESE -> zh
        AppLanguage.KOREAN -> ko
        AppLanguage.JAPANESE -> ja
    }

    // --- Comuni ---

    val cancel get() = t("Cancel", "Annulla", "Abbrechen", "Annuler", "Cancelar", "取消", "취소", "キャンセル")
    val delete get() = t("Delete", "Elimina", "Löschen", "Supprimer", "Eliminar", "删除", "삭제", "削除")
    val back get() = t("Back", "Indietro", "Zurück", "Retour", "Atrás", "返回", "뒤로", "戻る")
    val untitled get() = t("Untitled", "Senza titolo", "Ohne Titel", "Sans titre", "Sin título", "无标题", "제목 없음", "無題")
    val done get() = t("Done", "Fatto", "Fertig", "Terminé", "Listo", "完成", "완료", "完了")

    // --- Menu dei tre puntini ---

    val pageOptions get() = t("Page options", "Opzioni della pagina", "Seitenoptionen", "Options de la page", "Opciones de página", "页面选项", "페이지 옵션", "ページのオプション")
    val favorite get() = t("Favorite", "Preferita", "Favorit", "Favori", "Favorito", "收藏", "즐겨찾기", "お気に入り")
    val duplicate get() = t("Duplicate", "Duplica", "Duplizieren", "Dupliquer", "Duplicar", "复制", "복제", "複製")
    val moveTo get() = t("Move to", "Sposta in", "Verschieben nach", "Déplacer vers", "Mover a", "移动到", "이동", "移動")
    val turnIntoDatabase get() = t("Turn into database", "Trasforma in database", "In Datenbank umwandeln", "Convertir en base de données", "Convertir en base de datos", "转换为数据库", "데이터베이스로 전환", "データベースに変換")
    val turnIntoPage get() = t("Turn into page", "Trasforma in pagina", "In Seite umwandeln", "Convertir en page", "Convertir en página", "转换为页面", "페이지로 전환", "ページに変換")
    val moveToTrash get() = t("Move to trash", "Sposta nel cestino", "In den Papierkorb", "Mettre à la corbeille", "Mover a la papelera", "移到回收站", "휴지통으로 이동", "ゴミ箱に移動")
    val lockView get() = t("Lock view", "Blocca la vista", "Ansicht sperren", "Verrouiller la vue", "Bloquear vista", "锁定视图", "보기 잠금", "ビューをロック")
    val lockPage get() = t("Lock page", "Blocca la pagina", "Seite sperren", "Verrouiller la page", "Bloquear página", "锁定页面", "페이지 잠금", "ページをロック")
    val updates get() = t("Updates", "Modifiche", "Änderungen", "Modifications", "Cambios", "更新记录", "변경 내역", "更新履歴")

    // --- Conteggio del testo, in fondo al menu dei tre puntini ---

    /** "2.652 words": il numero col separatore delle migliaia scelto nelle impostazioni. */
    fun wordCount(n: Int): String {
        val number = Formats.number(n.toString())
        return if (n == 1) {
            t("1 word", "1 parola", "1 Wort", "1 mot", "1 palabra", "1 个词", "단어 1개", "1 語")
        } else {
            t("$number words", "$number parole", "$number Wörter", "$number mots", "$number palabras", "$number 个词", "단어 ${number}개", "$number 語")
        }
    }
    val statWords get() = t("Words", "Parole", "Wörter", "Mots", "Palabras", "词数", "단어", "単語数")
    val statLetters get() = t("Letters", "Lettere", "Buchstaben", "Lettres", "Letras", "字母", "알파벳", "アルファベット")
    val statNumbers get() = t("Numbers", "Numeri", "Ziffern", "Chiffres", "Números", "数字", "숫자", "数字")
    val statLettersAndNumbers get() = t("Letters and numbers", "Lettere e numeri", "Buchstaben und Ziffern", "Lettres et chiffres", "Letras y números", "字母和数字", "알파벳과 숫자", "アルファベットと数字")
    val statCharactersNoSpaces get() = t("Characters (no spaces)", "Caratteri (senza spazi)", "Zeichen (ohne Leerzeichen)", "Caractères (sans espaces)", "Caracteres (sin espacios)", "字符数（不计空格）", "문자 수(공백 제외)", "文字数（スペースを含めない）")
    val statCharactersWithSpaces get() = t("Characters (with spaces)", "Caratteri (con spazi)", "Zeichen (mit Leerzeichen)", "Caractères (avec espaces)", "Caracteres (con espacios)", "字符数（计空格）", "문자 수(공백 포함)", "文字数（スペースを含める）")
    val statLines get() = t("Lines", "Righe", "Zeilen", "Lignes", "Líneas", "行数", "줄", "行数")
    val statJapanese get() = t("Japanese characters", "Caratteri giapponesi", "Japanische Zeichen", "Caractères japonais", "Caracteres japoneses", "日文字符", "일본어 문자", "日本語の文字")
    val statChinese get() = t("Chinese characters", "Caratteri cinesi", "Chinesische Zeichen", "Caractères chinois", "Caracteres chinos", "中文字符", "중국어 문자", "中国語の文字")
    // "Lettere coreane" è la parola dell'utente: l'hangul si scrive per
    // sillabe, ma per chi lo studia ognuna è una lettera.
    val statKorean get() = t("Korean characters", "Lettere coreane", "Koreanische Zeichen", "Caractères coréens", "Caracteres coreanos", "韩文字符", "한글 글자", "韓国語の文字")
    val noOtherPage get() = t(
        "There is no other page to move it into.",
        "Non c'è un'altra pagina in cui spostarla.",
        "Es gibt keine andere Seite, in die sie verschoben werden kann.",
        "Il n'y a aucune autre page où la déplacer.",
        "No hay otra página a la que moverla.",
        "没有其他可移入的页面。",
        "옮길 수 있는 다른 페이지가 없습니다.",
        "移動先のページが他にありません。"
    )

    // --- Duplicare ---

    val duplicateWhere get() = t("Where should the copy go?", "Dove mettere la copia?", "Wohin soll die Kopie?", "Où placer la copie ?", "¿Dónde poner la copia?", "副本放在哪里？", "사본을 어디에 둘까요?", "コピーをどこに置きますか？")
    val nextToOriginal get() = t("Next to the original", "Accanto all'originale", "Neben dem Original", "À côté de l'original", "Junto al original", "放在原页面旁边", "원본 옆에", "元のページの隣")
    val nextToOriginalPageHint get() = t("Right below it, in the same place", "Subito sotto, nello stesso posto", "Direkt darunter, am selben Ort", "Juste en dessous, au même endroit", "Justo debajo, en el mismo lugar", "紧接在其下方，位置相同", "바로 아래, 같은 위치에", "すぐ下、同じ場所に")
    val nextToOriginalRowHint get() = t("As a new row of the same database", "Come nuova riga dello stesso database", "Als neue Zeile derselben Datenbank", "Comme nouvelle ligne de la même base de données", "Como una fila nueva de la misma base de datos", "作为同一数据库中的新行", "같은 데이터베이스의 새 행으로", "同じデータベースの新しい行として")
    val endOfMainMenu get() = t("End of the main menu", "In fondo al menu principale", "Ende des Hauptmenüs", "Fin du menu principal", "Final del menú principal", "主菜单末尾", "메인 메뉴 끝", "メインメニューの末尾")
    val endOfMainMenuHint get() = t("As a new page after the last one", "Come nuova pagina dopo l'ultima", "Als neue Seite nach der letzten", "Comme nouvelle page après la dernière", "Como página nueva después de la última", "作为最后一个页面之后的新页面", "마지막 페이지 다음에 새 페이지로", "最後のページの後に新しいページとして")
    val insidePage get() = t("Inside a page", "Dentro una pagina", "In einer Seite", "Dans une page", "Dentro de una página", "放入某个页面", "페이지 안에", "ページの中に")
    val insidePageHint get() = t(
        "Any page, including the pages of every database",
        "Qualsiasi pagina, anche quelle di tutti i database",
        "Jede Seite, auch die Seiten aller Datenbanken",
        "N'importe quelle page, y compris celles de toutes les bases de données",
        "Cualquier página, incluidas las de todas las bases de datos",
        "任意页面，包括所有数据库中的页面",
        "모든 데이터베이스의 페이지를 포함한 아무 페이지",
        "すべてのデータベースのページを含む、任意のページ"
    )
    val duplicateInto get() = t("Duplicate into", "Duplica in", "Duplizieren in", "Dupliquer dans", "Duplicar en", "复制到", "복제할 위치", "複製先")
    val duplicateIntoHint get() = t(
        "Tap a page to put the copy at its end. Open a database to see its pages.",
        "Tocca una pagina per mettere la copia in fondo. Apri un database per vederne le pagine.",
        "Tippe auf eine Seite, um die Kopie an ihr Ende zu setzen. Öffne eine Datenbank, um ihre Seiten zu sehen.",
        "Touchez une page pour placer la copie à la fin. Ouvrez une base de données pour voir ses pages.",
        "Toca una página para poner la copia al final. Abre una base de datos para ver sus páginas.",
        "点按页面，副本会放在其末尾。展开数据库可查看其中的页面。",
        "페이지를 누르면 사본이 그 끝에 놓입니다. 데이터베이스를 펼치면 안의 페이지가 보입니다.",
        "ページをタップすると、その末尾にコピーを置きます。データベースを開くと中のページが表示されます。"
    )
    val searchPages get() = t("Search pages", "Cerca una pagina", "Seiten suchen", "Rechercher une page", "Buscar páginas", "搜索页面", "페이지 검색", "ページを検索")
    fun inPlace(title: String) = t("In ${title}", "In ${title}", "In ${title}", "Dans ${title}", "En ${title}", "位于 ${title}", "위치: ${title}", "場所：${title}")
    fun duplicatedInto(title: String) = t(
        "Copy placed in “${title}”",
        "Copia messa in “${title}”",
        "Kopie in „${title}“ abgelegt",
        "Copie placée dans « ${title} »",
        "Copia colocada en «${title}»",
        "副本已放入“${title}”",
        "사본을 \"${title}\"에 넣었습니다",
        "コピーを「${title}」に置きました"
    )
    /** Quello che si aggiunge fra parentesi al titolo della copia. */
    val copySuffix get() = t("copy", "copia", "Kopie", "copie", "copia", "副本", "사본", "コピー")

    // --- Buttare nel cestino ---

    fun moveToTrashText(title: String) = t(
        "\"$title\" will disappear from where it is and go to the trash, where you can restore it.",
        "\"$title\" sparirà da dove si trova e finirà nel cestino, da cui potrai ripristinarla.",
        "„$title“ verschwindet von ihrem Platz und kommt in den Papierkorb, wo du sie wiederherstellen kannst.",
        "« $title » disparaîtra de son emplacement et ira dans la corbeille, d'où vous pourrez la restaurer.",
        "«$title» desaparecerá de donde está y pasará a la papelera, desde donde podrás restaurarla.",
        "“$title”将从当前位置消失并移到回收站，你可以在那里恢复它。",
        "\"$title\" 페이지가 현재 위치에서 사라지고 휴지통으로 이동합니다. 휴지통에서 복원할 수 있습니다.",
        "「$title」は現在の場所から消えてゴミ箱に移動します。ゴミ箱から復元できます。"
    )
    val trashFateKeep get() = t(
        "It will be deleted for good after 30 days.",
        "Dopo 30 giorni verrà eliminata per sempre.",
        "Nach 30 Tagen wird sie endgültig gelöscht.",
        "Elle sera supprimée définitivement après 30 jours.",
        "Se eliminará definitivamente a los 30 días.",
        "30 天后将被永久删除。",
        "30일 후 영구 삭제됩니다.",
        "30日後に完全に削除されます。"
    )
    val trashFateEmpty get() = t(
        "It will be deleted for good the next time the app starts.",
        "Verrà eliminata per sempre al prossimo avvio dell'app.",
        "Beim nächsten Start der App wird sie endgültig gelöscht.",
        "Elle sera supprimée définitivement au prochain démarrage de l'app.",
        "Se eliminará definitivamente la próxima vez que se abra la app.",
        "下次启动应用时将被永久删除。",
        "다음에 앱을 시작할 때 영구 삭제됩니다.",
        "次にアプリを起動したときに完全に削除されます。"
    )

    // --- Barra laterale ---

    val openSidebar get() = t("Open sidebar", "Apri la barra laterale", "Seitenleiste öffnen", "Ouvrir la barre latérale", "Abrir la barra lateral", "打开侧边栏", "사이드바 열기", "サイドバーを開く")
    val widgets get() = t("Widgets", "Widget", "Widgets", "Widgets", "Widgets", "小组件", "위젯", "ウィジェット")
    val notebook get() = t("Notebook", "Quaderno", "Notizbuch", "Carnet", "Cuaderno", "笔记本", "노트북", "ノートブック")
    val mainMenu get() = t("Main menu", "Menu principale", "Hauptmenü", "Menu principal", "Menú principal", "主菜单", "메인 메뉴", "メインメニュー")
    val favoritePages get() = t("Favorite pages", "Pagine preferite", "Favorisierte Seiten", "Pages favorites", "Páginas favoritas", "收藏的页面", "즐겨찾는 페이지", "お気に入りのページ")
    val pages get() = t("Pages", "Pagine", "Seiten", "Pages", "Páginas", "页面", "페이지", "ページ")
    val search get() = t("Search", "Cerca", "Suchen", "Rechercher", "Buscar", "搜索", "검색", "検索")
    val startupWindow get() = t("Startup window", "Finestra di avvio", "Startfenster", "Fenêtre de démarrage", "Ventana de inicio", "启动窗口", "시작 화면", "起動画面")
    val backup get() = t("Backup", "Backup", "Sicherung", "Sauvegarde", "Copia de seguridad", "备份", "백업", "バックアップ")
    val notifications get() = t("Notifications", "Notifiche", "Benachrichtigungen", "Notifications", "Notificaciones", "通知", "알림", "通知")
    val trash get() = t("Trash", "Cestino", "Papierkorb", "Corbeille", "Papelera", "回收站", "휴지통", "ゴミ箱")
    val import get() = t("Import", "Importa", "Importieren", "Importer", "Importar", "导入", "가져오기", "インポート")
    val export get() = t("Export", "Esporta", "Exportieren", "Exporter", "Exportar", "导出", "내보내기", "エクスポート")
    val connections get() = t("Connections", "Connessioni", "Verbindungen", "Connexions", "Conexiones", "连接", "연결", "接続")
    val settings get() = t("Settings", "Impostazioni", "Einstellungen", "Paramètres", "Ajustes", "设置", "설정", "設定")
    val expand get() = t("Expand", "Espandi", "Aufklappen", "Déplier", "Expandir", "展开", "펼치기", "展開")
    val collapse get() = t("Collapse", "Comprimi", "Zuklappen", "Replier", "Contraer", "折叠", "접기", "折りたたむ")
    val noPagesInside get() = t("No pages inside", "Nessuna pagina dentro", "Keine Seiten darin", "Aucune page à l'intérieur", "No hay páginas dentro", "里面没有页面", "안에 페이지가 없습니다", "中にページはありません")

    // --- Preferiti ---

    val sortBy get() = t("Sort by", "Ordina per", "Sortieren nach", "Trier par", "Ordenar por", "排序方式", "정렬 기준", "並べ替え")
    val dateAdded get() = t("Date added", "Data di aggiunta", "Hinzugefügt am", "Date d'ajout", "Fecha de adición", "添加日期", "추가한 날짜", "追加日")
    val alphabetical get() = t("Alphabetical", "Alfabetico", "Alphabetisch", "Alphabétique", "Alfabético", "按名称", "이름순", "名前順")
    val ascending get() = t("Ascending", "Crescente", "Aufsteigend", "Croissant", "Ascendente", "升序", "오름차순", "昇順")
    val descending get() = t("Descending", "Decrescente", "Absteigend", "Décroissant", "Descendente", "降序", "내림차순", "降順")
    fun addedOn(date: String) = t("Added $date", "Aggiunta il $date", "Hinzugefügt am $date", "Ajoutée le $date", "Añadida el $date", "添加于 $date", "$date 에 추가됨", "$date に追加")
    val dateAddedUnknown get() = t("Date added unknown", "Data di aggiunta sconosciuta", "Hinzufügedatum unbekannt", "Date d'ajout inconnue", "Fecha de adición desconocida", "添加日期未知", "추가한 날짜 알 수 없음", "追加日不明")
    val noFavorites get() = t(
        "No favorite pages yet. Add one from the ⋯ menu of a page.",
        "Nessuna pagina preferita. Aggiungine una dal menu ⋯ di una pagina.",
        "Noch keine Favoriten. Füge eine Seite über ihr ⋯-Menü hinzu.",
        "Aucune page favorite. Ajoutez-en une depuis le menu ⋯ d'une page.",
        "Aún no hay páginas favoritas. Añade una desde el menú ⋯ de una página.",
        "还没有收藏的页面。可在页面的 ⋯ 菜单中添加。",
        "즐겨찾는 페이지가 없습니다. 페이지의 ⋯ 메뉴에서 추가하세요.",
        "お気に入りのページはまだありません。ページの ⋯ メニューから追加できます。"
    )

    // --- Cestino ---

    fun deletedOn(date: String) = t("Deleted $date", "Eliminata il $date", "Gelöscht am $date", "Supprimée le $date", "Eliminada el $date", "删除于 $date", "$date 에 삭제됨", "$date に削除")
    val restore get() = t("Restore", "Ripristina", "Wiederherstellen", "Restaurer", "Restaurar", "恢复", "복원", "復元")
    val deletePermanently get() = t("Delete permanently", "Elimina definitivamente", "Endgültig löschen", "Supprimer définitivement", "Eliminar definitivamente", "永久删除", "영구 삭제", "完全に削除")
    val trashIsEmpty get() = t("The trash is empty", "Il cestino è vuoto", "Der Papierkorb ist leer", "La corbeille est vide", "La papelera está vacía", "回收站是空的", "휴지통이 비어 있습니다", "ゴミ箱は空です")
    val emptyTrash get() = t("Empty trash", "Svuota cestino", "Papierkorb leeren", "Vider la corbeille", "Vaciar papelera", "清空回收站", "휴지통 비우기", "ゴミ箱を空にする")
    val keepFor30Days get() = t("Keep deleted pages for 30 days", "Conserva le pagine eliminate per 30 giorni", "Gelöschte Seiten 30 Tage aufbewahren", "Conserver les pages supprimées 30 jours", "Conservar las páginas eliminadas 30 días", "将删除的页面保留 30 天", "삭제한 페이지를 30일 동안 보관", "削除したページを30日間保存")
    val keepFor30DaysDetail get() = t(
        "Pages are deleted for good 30 days after being moved here.",
        "Le pagine vengono eliminate per sempre 30 giorni dopo essere finite qui.",
        "Seiten werden 30 Tage nach dem Verschieben hierher endgültig gelöscht.",
        "Les pages sont supprimées définitivement 30 jours après leur arrivée ici.",
        "Las páginas se eliminan definitivamente 30 días después de llegar aquí.",
        "页面移到这里 30 天后将被永久删除。",
        "페이지는 이곳으로 옮겨진 지 30일 후 영구 삭제됩니다.",
        "ページはここに移動してから30日後に完全に削除されます。"
    )
    val emptyAutomatically get() = t("Empty trash automatically", "Svuota automaticamente il cestino", "Papierkorb automatisch leeren", "Vider automatiquement la corbeille", "Vaciar la papelera automáticamente", "自动清空回收站", "휴지통 자동 비우기", "ゴミ箱を自動的に空にする")
    val emptyAutomaticallyDetail get() = t(
        "Pages are deleted for good the next time the app starts.",
        "Le pagine vengono eliminate per sempre al prossimo avvio dell'app.",
        "Seiten werden beim nächsten Start der App endgültig gelöscht.",
        "Les pages sont supprimées définitivement au prochain démarrage de l'app.",
        "Las páginas se eliminan definitivamente la próxima vez que se abra la app.",
        "页面将在下次启动应用时被永久删除。",
        "페이지는 다음에 앱을 시작할 때 영구 삭제됩니다.",
        "ページは次にアプリを起動したときに完全に削除されます。"
    )
    fun daysLeft(days: Int) = if (days == 1) {
        t("1 day left", "Manca 1 giorno", "Noch 1 Tag", "Encore 1 jour", "Queda 1 día", "剩余 1 天", "1일 남음", "残り1日")
    } else {
        t("$days days left", "Mancano $days giorni", "Noch $days Tage", "Encore $days jours", "Quedan $days días", "剩余 $days 天", "${days}일 남음", "残り${days}日")
    }
    val deletedAtNextStart get() = t("Deleted at next start", "Eliminata al prossimo avvio", "Wird beim nächsten Start gelöscht", "Supprimée au prochain démarrage", "Se eliminará al próximo inicio", "下次启动时删除", "다음 시작 시 삭제", "次回起動時に削除")
    val deleteForeverTitle get() = t("Delete this page permanently?", "Eliminare definitivamente questa pagina?", "Diese Seite endgültig löschen?", "Supprimer définitivement cette page ?", "¿Eliminar definitivamente esta página?", "永久删除此页面？", "이 페이지를 영구 삭제할까요?", "このページを完全に削除しますか？")
    val deleteForeverText get() = t(
        "The page and the pages inside it will be deleted. This can't be undone.",
        "La pagina e le pagine che contiene verranno eliminate. Non si può annullare.",
        "Die Seite und die Seiten darin werden gelöscht. Das kann nicht rückgängig gemacht werden.",
        "La page et les pages qu'elle contient seront supprimées. Action irréversible.",
        "Se eliminarán la página y las páginas que contiene. No se puede deshacer.",
        "此页面及其中的页面将被删除，且无法撤销。",
        "이 페이지와 안에 있는 페이지가 삭제됩니다. 되돌릴 수 없습니다.",
        "このページと中のページが削除されます。元に戻すことはできません。"
    )
    val emptyTrashTitle get() = t("Empty the trash?", "Svuotare il cestino?", "Papierkorb leeren?", "Vider la corbeille ?", "¿Vaciar la papelera?", "清空回收站？", "휴지통을 비울까요?", "ゴミ箱を空にしますか？")
    val emptyTrashText get() = t(
        "All pages in the trash will be deleted permanently. This can't be undone.",
        "Tutte le pagine nel cestino verranno eliminate definitivamente. Non si può annullare.",
        "Alle Seiten im Papierkorb werden endgültig gelöscht. Das kann nicht rückgängig gemacht werden.",
        "Toutes les pages de la corbeille seront supprimées définitivement. Action irréversible.",
        "Todas las páginas de la papelera se eliminarán definitivamente. No se puede deshacer.",
        "回收站中的所有页面都将被永久删除，且无法撤销。",
        "휴지통의 모든 페이지가 영구 삭제됩니다. 되돌릴 수 없습니다.",
        "ゴミ箱のすべてのページが完全に削除されます。元に戻すことはできません。"
    )
    val pageInTrash get() = t("This page is in the trash.", "Questa pagina è nel cestino.", "Diese Seite ist im Papierkorb.", "Cette page est dans la corbeille.", "Esta página está en la papelera.", "此页面在回收站中。", "이 페이지는 휴지통에 있습니다.", "このページはゴミ箱にあります。")
    val restoredToMainMenu get() = t("Restored at the end of the main menu", "Ripristinata in fondo al menu principale", "Am Ende des Hauptmenüs wiederhergestellt", "Restaurée à la fin du menu principal", "Restaurada al final del menú principal", "已恢复到主菜单末尾", "메인 메뉴 끝에 복원됨", "メインメニューの末尾に復元しました")

    // --- Ricerca ---

    val searchHint get() = t("Search in all pages", "Cerca in tutte le pagine", "Alle Seiten durchsuchen", "Rechercher dans toutes les pages", "Buscar en todas las páginas", "在所有页面中搜索", "모든 페이지에서 검색", "すべてのページを検索")
    val searchOptions get() = t("Search options", "Opzioni di ricerca", "Suchoptionen", "Options de recherche", "Opciones de búsqueda", "搜索选项", "검색 옵션", "検索オプション")
    val searchInContent get() = t("Search in content", "Cerca nel contenuto", "Im Inhalt suchen", "Rechercher dans le contenu", "Buscar en el contenido", "搜索内容", "내용에서 검색", "本文を検索")
    val searchInTitles get() = t("Search in titles", "Cerca nei titoli", "In Titeln suchen", "Rechercher dans les titres", "Buscar en los títulos", "搜索标题", "제목에서 검색", "タイトルを検索")
    val searchInTags get() = t("Search in tags", "Cerca nei tag", "In Tags suchen", "Rechercher dans les tags", "Buscar en las etiquetas", "搜索标签", "태그에서 검색", "タグを検索")
    val searchInAttachments get() = t("Search in attachments", "Cerca negli allegati", "In Anhängen suchen", "Rechercher dans les pièces jointes", "Buscar en los adjuntos", "搜索附件", "첨부 파일에서 검색", "添付ファイルを検索")
    val searchInArchived get() = t("Search in archived notes", "Cerca nelle note archiviate", "In archivierten Notizen suchen", "Rechercher dans les notes archivées", "Buscar en las notas archivadas", "搜索已归档的笔记", "보관된 노트에서 검색", "アーカイブしたノートを検索")
    val searchInTrash get() = t("Search in trash", "Cerca nel cestino", "Im Papierkorb suchen", "Rechercher dans la corbeille", "Buscar en la papelera", "搜索回收站", "휴지통에서 검색", "ゴミ箱を検索")
    val caseSensitive get() = t("Case sensitive", "Distingui maiuscole e minuscole", "Groß-/Kleinschreibung beachten", "Respecter la casse", "Distinguir mayúsculas y minúsculas", "区分大小写", "대소문자 구분", "大文字と小文字を区別")
    val wholeWords get() = t("Match whole words", "Cerca parole intere", "Nur ganze Wörter", "Mots entiers uniquement", "Solo palabras completas", "全字匹配", "전체 단어 일치", "単語単位で検索")
    val autoIndexing get() = t("Automatic indexing", "Indicizzazione automatica", "Automatische Indizierung", "Indexation automatique", "Indexación automática", "自动索引", "자동 색인", "自動インデックス作成")
    val ocrSearch get() = t("OCR search in images/PDF", "Ricerca OCR nelle immagini/PDF", "OCR-Suche in Bildern/PDF", "Recherche OCR dans les images/PDF", "Búsqueda OCR en imágenes/PDF", "在图片/PDF 中进行 OCR 搜索", "이미지/PDF OCR 검색", "画像/PDFのOCR検索")
    val searchPrompt get() = t("Search across all your pages", "Cerca in tutte le tue pagine", "Durchsuche alle deine Seiten", "Recherchez dans toutes vos pages", "Busca en todas tus páginas", "搜索你的所有页面", "모든 페이지에서 검색하세요", "すべてのページを検索できます")
    val noResults get() = t("No results", "Nessun risultato", "Keine Ergebnisse", "Aucun résultat", "Sin resultados", "没有结果", "결과 없음", "結果なし")
    val inTrash get() = t("In trash", "Nel cestino", "Im Papierkorb", "Dans la corbeille", "En la papelera", "在回收站中", "휴지통에 있음", "ゴミ箱内")

    // --- Finestra di avvio ---

    val startupDescription get() = t(
        "Choose the page you see when you open the app.",
        "Scegli la pagina che vedi quando apri l'app.",
        "Wähle die Seite, die beim Öffnen der App erscheint.",
        "Choisissez la page affichée à l'ouverture de l'app.",
        "Elige la página que ves al abrir la app.",
        "选择打开应用时显示的页面。",
        "앱을 열 때 표시할 페이지를 선택하세요.",
        "アプリを開いたときに表示するページを選択します。"
    )
    val lastVisitedPage get() = t("Last visited page", "Ultima pagina visitata", "Zuletzt besuchte Seite", "Dernière page visitée", "Última página visitada", "上次访问的页面", "마지막으로 방문한 페이지", "最後に開いたページ")
    val specificPage get() = t("A specific page", "Una pagina specifica", "Eine bestimmte Seite", "Une page précise", "Una página concreta", "指定页面", "특정 페이지", "特定のページ")
    val choosePage get() = t("Choose page", "Scegli la pagina", "Seite auswählen", "Choisir la page", "Elegir página", "选择页面", "페이지 선택", "ページを選択")
    val noPageChosen get() = t("No page chosen yet", "Nessuna pagina scelta", "Noch keine Seite gewählt", "Aucune page choisie", "Ninguna página elegida", "尚未选择页面", "선택한 페이지 없음", "ページが選択されていません")

    // --- Impostazioni ---

    val font get() = t("Font", "Carattere", "Schriftart", "Police", "Fuente", "字体", "글꼴", "フォント")
    val fontSize get() = t("Font size", "Dimensione del carattere", "Schriftgröße", "Taille de police", "Tamaño de fuente", "字号", "글꼴 크기", "文字サイズ")
    val theme get() = t("Theme", "Tema", "Design", "Thème", "Tema", "主题", "테마", "テーマ")
    val themeDark get() = t("Dark", "Scuro", "Dunkel", "Sombre", "Oscuro", "深色", "어둡게", "ダーク")
    val themeLight get() = t("Light", "Chiaro", "Hell", "Clair", "Claro", "浅色", "밝게", "ライト")
    val themeSystem get() = t("System", "Sistema", "System", "Système", "Sistema", "跟随系统", "시스템", "システム")
    val appSounds get() = t("App sounds", "Suoni dell'app", "App-Töne", "Sons de l'app", "Sonidos de la app", "应用声音", "앱 소리", "アプリのサウンド")
    val notificationSound get() = t("Notification sound", "Suono delle notifiche", "Benachrichtigungston", "Son des notifications", "Sonido de notificación", "通知声音", "알림 소리", "通知音")
    val defaultSound get() = t("Default", "Predefinito", "Standard", "Par défaut", "Predeterminado", "默认", "기본값", "デフォルト")
    val chooseFromFiles get() = t("Choose from files", "Scegli dai file", "Aus Dateien wählen", "Choisir dans les fichiers", "Elegir de los archivos", "从文件中选择", "파일에서 선택", "ファイルから選択")
    val useDefaultSound get() = t("Use default sound", "Usa il suono predefinito", "Standardton verwenden", "Utiliser le son par défaut", "Usar el sonido predeterminado", "使用默认声音", "기본 소리 사용", "デフォルトの音を使用")
    val playSound get() = t("Play", "Riproduci", "Abspielen", "Lire", "Reproducir", "播放", "재생", "再生")
    val turnOffNotifications get() = t("Turn off notifications", "Disattiva le notifiche", "Benachrichtigungen ausschalten", "Désactiver les notifications", "Desactivar las notificaciones", "关闭通知", "알림 끄기", "通知をオフにする")
    val turnOffForAWhile get() = t("Turn off for a while", "Disattiva per un periodo", "Vorübergehend ausschalten", "Désactiver pendant un moment", "Desactivar durante un tiempo", "暂时关闭", "일정 시간 동안 끄기", "一定時間オフにする")
    val forOneHour get() = t("For 1 hour", "Per 1 ora", "Für 1 Stunde", "Pendant 1 heure", "Durante 1 hora", "1 小时", "1시간 동안", "1時間")
    val forEightHours get() = t("For 8 hours", "Per 8 ore", "Für 8 Stunden", "Pendant 8 heures", "Durante 8 horas", "8 小时", "8시간 동안", "8時間")
    val forOneDay get() = t("For 1 day", "Per 1 giorno", "Für 1 Tag", "Pendant 1 jour", "Durante 1 día", "1 天", "1일 동안", "1日")
    val forOneWeek get() = t("For 1 week", "Per 1 settimana", "Für 1 Woche", "Pendant 1 semaine", "Durante 1 semana", "1 周", "1주일 동안", "1週間")
    fun offUntil(date: String) = t("Off until $date", "Disattivate fino al $date", "Aus bis $date", "Désactivées jusqu'au $date", "Desactivadas hasta el $date", "关闭至 $date", "$date 까지 꺼짐", "$date までオフ")
    val turnBackOn get() = t("Turn back on", "Riattiva", "Wieder einschalten", "Réactiver", "Volver a activar", "重新开启", "다시 켜기", "オンに戻す")
    val notificationsNotYet get() = t(
        "Notifications aren't available yet: these choices will apply once they are.",
        "Le notifiche non ci sono ancora: queste scelte varranno quando arriveranno.",
        "Benachrichtigungen gibt es noch nicht: Diese Einstellungen gelten, sobald sie verfügbar sind.",
        "Les notifications ne sont pas encore disponibles : ces choix s'appliqueront dès qu'elles le seront.",
        "Las notificaciones aún no están disponibles: estas opciones se aplicarán cuando lo estén.",
        "通知功能尚未推出：这些设置将在推出后生效。",
        "알림 기능은 아직 없습니다. 기능이 추가되면 이 설정이 적용됩니다.",
        "通知はまだ利用できません。利用可能になるとこの設定が適用されます。"
    )
    val animations get() = t("Animations", "Animazioni", "Animationen", "Animations", "Animaciones", "动画", "애니메이션", "アニメーション")
    val animationsOn get() = t("On", "Attive", "An", "Activées", "Activadas", "开启", "켜기", "オン")
    val animationsReduced get() = t("Reduced", "Ridotte", "Reduziert", "Réduites", "Reducidas", "减少", "줄이기", "控えめ")
    val animationsOff get() = t("Off", "Disattivate", "Aus", "Désactivées", "Desactivadas", "关闭", "끄기", "オフ")
    val numberFormat get() = t("Number format", "Formato dei numeri", "Zahlenformat", "Format des nombres", "Formato de números", "数字格式", "숫자 형식", "数値の形式")
    val dateFormat get() = t("Date format", "Formato della data", "Datumsformat", "Format de date", "Formato de fecha", "日期格式", "날짜 형식", "日付の形式")
    val dateFull get() = t("Full date", "Data completa", "Vollständiges Datum", "Date complète", "Fecha completa", "完整日期", "전체 날짜", "完全な日付")
    val dateShort get() = t("Short date", "Data breve", "Kurzes Datum", "Date courte", "Fecha corta", "简短日期", "짧은 날짜", "短い日付")
    val dateMonthDayYearDot get() = t("Month.Day.Year", "Mese.Giorno.Anno", "Monat.Tag.Jahr", "Mois.Jour.Année", "Mes.Día.Año", "月.日.年", "월.일.년", "月.日.年")
    val dateMonthDayYearSlash get() = t("Month/Day/Year", "Mese/Giorno/Anno", "Monat/Tag/Jahr", "Mois/Jour/Année", "Mes/Día/Año", "月/日/年", "월/일/년", "月/日/年")
    val dateDayMonthYearDot get() = t("Day.Month.Year", "Giorno.Mese.Anno", "Tag.Monat.Jahr", "Jour.Mois.Année", "Día.Mes.Año", "日.月.年", "일.월.년", "日.月.年")
    val dateDayMonthYearSlash get() = t("Day/Month/Year", "Giorno/Mese/Anno", "Tag/Monat/Jahr", "Jour/Mois/Année", "Día/Mes/Año", "日/月/年", "일/월/년", "日/月/年")
    val dateYearMonthDayDot get() = t("Year.Month.Day", "Anno.Mese.Giorno", "Jahr.Monat.Tag", "Année.Mois.Jour", "Año.Mes.Día", "年.月.日", "년.월.일", "年.月.日")
    val dateYearMonthDaySlash get() = t("Year/Month/Day", "Anno/Mese/Giorno", "Jahr/Monat/Tag", "Année/Mois/Jour", "Año/Mes/Día", "年/月/日", "년/월/일", "年/月/日")
    val dateChinese get() = t("Chinese date", "Data cinese", "Chinesisches Datum", "Date chinoise", "Fecha china", "中文日期", "중국식 날짜", "中国式の日付")
    val language get() = t("Language", "Lingua", "Sprache", "Langue", "Idioma", "语言", "언어", "言語")
    val languageNote get() = t(
        "Translates the app's own labels, not what you write.",
        "Traduce le voci dell'app, non quello che scrivi tu.",
        "Übersetzt die Beschriftungen der App, nicht deine eigenen Texte.",
        "Traduit les libellés de l'app, pas ce que vous écrivez.",
        "Traduce las etiquetas de la app, no lo que escribes tú.",
        "只翻译应用自身的文字，不翻译你写的内容。",
        "앱의 기본 문구만 번역하며 직접 작성한 내용은 번역하지 않습니다.",
        "アプリの表示のみを翻訳し、あなたが書いた内容は翻訳しません。"
    )
    val timeZone get() = t("Time zone", "Fuso orario", "Zeitzone", "Fuseau horaire", "Zona horaria", "时区", "시간대", "タイムゾーン")
    val searchTimeZones get() = t("Search time zones", "Cerca un fuso orario", "Zeitzonen suchen", "Rechercher un fuseau", "Buscar zona horaria", "搜索时区", "시간대 검색", "タイムゾーンを検索")
}

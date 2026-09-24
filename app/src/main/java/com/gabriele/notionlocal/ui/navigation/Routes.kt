package com.gabriele.notionlocal.ui.navigation

/**
 * Rotte di navigazione dell'app. pageId è passato come argomento di
 * navigazione per sapere quale pagina aprire nell'editor o nella
 * vista database. La rotta HOME apre semplicemente PageEditorScreen
 * puntata su PageEntity.ROOT_PAGE_ID — non esiste più una schermata
 * Home separata.
 */
object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val PAGE_EDITOR = "page/{pageId}"
    const val DATABASE_VIEW = "database/{pageId}"

    /** La cronologia delle modifiche di una pagina: la voce "Updates" del menu. */
    const val UPDATES = "updates/{pageId}"

    // Le voci della barra laterale che aprono una schermata a sé.
    const val FAVORITES = "favorites"
    const val TRASH = "trash"
    const val STARTUP = "startup"
    const val SETTINGS = "settings"

    fun pageEditor(pageId: String) = "page/$pageId"
    fun databaseView(pageId: String) = "database/$pageId"
    fun updates(pageId: String) = "updates/$pageId"

    /** La rotta di una pagina, che sia testo o database. */
    fun forPage(pageId: String, isDatabase: Boolean) =
        if (isDatabase) databaseView(pageId) else pageEditor(pageId)

    /** L'id della pagina dentro una rotta "page/…" o "database/…", o null. */
    fun pageIdOf(route: String): String? = when {
        route.startsWith("page/") -> route.removePrefix("page/")
        route.startsWith("database/") -> route.removePrefix("database/")
        else -> null
    }
}

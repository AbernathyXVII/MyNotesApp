package com.gabriele.notionlocal.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.settings.AnimationLevel
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.settings.StartupMode
import com.gabriele.notionlocal.data.settings.TrashPolicy
import com.gabriele.notionlocal.ui.screen.DatabaseViewScreen
import com.gabriele.notionlocal.ui.screen.FavoritesScreen
import com.gabriele.notionlocal.ui.screen.NoInsets
import com.gabriele.notionlocal.ui.screen.PageEditorScreen
import com.gabriele.notionlocal.ui.screen.SearchScreen
import com.gabriele.notionlocal.ui.screen.SettingsScreen
import com.gabriele.notionlocal.ui.screen.SidebarActions
import com.gabriele.notionlocal.ui.screen.SidebarContent
import com.gabriele.notionlocal.ui.screen.StartupScreen
import com.gabriele.notionlocal.ui.screen.TrashScreen
import com.gabriele.notionlocal.ui.screen.UpdatesScreen
import com.gabriele.notionlocal.ui.theme.SidebarBackground
import com.gabriele.notionlocal.viewmodel.SidebarViewModel
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

/**
 * Grafo di navigazione dell'app. Non esiste più una schermata Home
 * separata: la rotta HOME apre semplicemente PageEditorScreen puntata
 * sulla pagina radice fissa (PageEntity.ROOT_PAGE_ID) — creata al volo
 * al primo avvio.
 *
 * **La barra laterale sta sopra a tutto**, non dentro una schermata: si
 * apre dal pulsante con le tre lineette di ogni pagina e scorre da
 * sinistra sopra quella aperta. Toccando una pagina nella barra ci si va
 * dritti, **senza passare dalle pagine che la contengono**: sotto resta
 * solo il menu principale, così "indietro" riporta lì e non a una fila di
 * pagine mai aperte davvero.
 */
@Composable
fun AppNavHost(factory: ViewModelFactory) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val sidebar: SidebarViewModel = viewModel(factory = factory)
    val treeRoot by sidebar.root.collectAsStateWithLifecycle()
    val treeChildren by sidebar.children.collectAsStateWithLifecycle()
    val treeExpanded by sidebar.expanded.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val animations = AppSettings.animations

    // La pagina aperta adesso, per evidenziarla nell'albero.
    val currentPageId = when (backStackEntry?.destination?.route) {
        Routes.HOME -> PageEntity.ROOT_PAGE_ID
        Routes.PAGE_EDITOR, Routes.DATABASE_VIEW -> backStackEntry?.arguments?.getString("pageId")
        else -> null
    }

    fun openSidebar() {
        sidebar.refresh()
        scope.launch {
            if (animations == AnimationLevel.OFF) drawerState.snapTo(DrawerValue.Open) else drawerState.open()
        }
    }

    fun closeSidebar() {
        scope.launch {
            if (animations == AnimationLevel.OFF) drawerState.snapTo(DrawerValue.Closed) else drawerState.close()
        }
    }

    /** Dritti a una pagina, con sotto solo il menu principale. */
    fun jumpTo(route: String) {
        navController.navigate(route) {
            popUpTo(Routes.HOME)
            launchSingleTop = true
        }
    }

    fun goHome() {
        navController.popBackStack(Routes.HOME, inclusive = false)
    }

    // --- All'avvio: pulizia del cestino e pagina da cui partire ---
    //
    // Una volta sola per avvio vero: il flag sopravvive a una rotazione o a
    // un cambio di tema di sistema, che ricreano tutto questo da capo — e
    // senza, ogni volta si ricomincerebbe dalla pagina d'avvio.
    var startupHandled by rememberSaveable { mutableStateOf(false) }
    // Letta subito, prima che il registro dell'ultima pagina la sovrascriva
    // col menu principale da cui si parte.
    val lastRouteAtLaunch = remember { AppSettings.lastVisitedRoute }
    LaunchedEffect(Unit) {
        if (startupHandled) return@LaunchedEffect
        startupHandled = true
        val repository = factory.pageRepository
        repository.purgeTrash(
            emptyAll = AppSettings.trashPolicy == TrashPolicy.EMPTY_AUTOMATICALLY,
            retentionDays = AppSettings.TRASH_RETENTION_DAYS
        ).forEach { factory.imageStore.delete(it) }

        val targetId = when (AppSettings.startupMode) {
            StartupMode.HOME -> null
            StartupMode.LAST_VISITED -> lastRouteAtLaunch?.let { Routes.pageIdOf(it) }
            StartupMode.SPECIFIC_PAGE -> AppSettings.startupPageId
        }
        // Solo se la pagina c'è ancora e non sta nel cestino: altrimenti si
        // resta sul menu principale, che è meglio di una pagina vuota.
        val target = targetId
            ?.takeIf { it != PageEntity.ROOT_PAGE_ID }
            ?.let { repository.getPage(it) }
            ?.takeIf { it.trashedAt == null }
        if (target != null) navController.navigate(Routes.forPage(target.id, target.isDatabase))
    }

    // L'ultima pagina aperta, per la finestra di avvio "Last visited page".
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            val id = entry.arguments?.getString("pageId")
            when (entry.destination.route) {
                Routes.HOME -> AppSettings.lastVisitedRoute = Routes.HOME
                Routes.PAGE_EDITOR -> id?.let { AppSettings.lastVisitedRoute = Routes.pageEditor(it) }
                Routes.DATABASE_VIEW -> id?.let { AppSettings.lastVisitedRoute = Routes.databaseView(it) }
            }
        }
    }

    BackHandler(enabled = drawerState.isOpen) { closeSidebar() }

    val actions = SidebarActions(
        onMainMenu = { closeSidebar(); goHome() },
        onFavorites = { closeSidebar(); navController.navigate(Routes.FAVORITES) { launchSingleTop = true } },
        onOpenNode = { node ->
            closeSidebar()
            val pageId = node.pageId
            val rowId = node.rowId
            when {
                pageId == PageEntity.ROOT_PAGE_ID -> goHome()
                pageId != null -> jumpTo(Routes.forPage(pageId, node.isDatabase))
                rowId != null -> sidebar.openRow(rowId) { id -> jumpTo(Routes.pageEditor(id)) }
            }
        },
        onToggleNode = { sidebar.toggle(it) },
        onSearch = { closeSidebar(); navController.navigate(Routes.SEARCH) { launchSingleTop = true } },
        onStartup = { closeSidebar(); navController.navigate(Routes.STARTUP) { launchSingleTop = true } },
        onTrash = { closeSidebar(); navController.navigate(Routes.TRASH) { launchSingleTop = true } },
        onSettings = { closeSidebar(); navController.navigate(Routes.SETTINGS) { launchSingleTop = true } }
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        // Il trascinamento serve solo a chiuderla: aperta col dito dal bordo
        // sinistro litigherebbe col gesto "indietro" del telefono, che parte
        // proprio da lì.
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = SidebarBackground,
                windowInsets = NoInsets,
                modifier = Modifier.width(SIDEBAR_WIDTH)
            ) {
                SidebarContent(
                    root = treeRoot,
                    children = treeChildren,
                    expanded = treeExpanded,
                    currentPageId = currentPageId,
                    actions = actions,
                    // Aperta o mentre si apre: da chiusa la barra resta
                    // disegnata dietro alla pagina, e gli orologi non
                    // devono ridisegnarsi ogni secondo per nessuno.
                    widgetsActive = drawerState.isOpen || drawerState.targetValue == DrawerValue.Open
                )
            }
        }
    ) {
        AppRoutes(
            navController = navController,
            factory = factory,
            animations = animations,
            onOpenSidebar = ::openSidebar,
            onOpenRowPage = { rowId, then ->
                scope.launch { factory.pageRepository.ensureRowPage(rowId)?.let(then) }
            }
        )
    }
}

private val SIDEBAR_WIDTH = 300.dp

@Composable
private fun AppRoutes(
    navController: NavHostController,
    factory: ViewModelFactory,
    animations: AnimationLevel,
    onOpenSidebar: () -> Unit,
    onOpenRowPage: (rowId: String, then: (String) -> Unit) -> Unit
) {
    // Le dissolvenze fra una schermata e l'altra seguono l'impostazione
    // delle animazioni: piene come di serie, brevi, o nessuna.
    val enter: EnterTransition = when (animations) {
        AnimationLevel.FULL -> fadeIn(tween(FULL_FADE_MS))
        AnimationLevel.REDUCED -> fadeIn(tween(REDUCED_FADE_MS))
        AnimationLevel.OFF -> EnterTransition.None
    }
    val exit: ExitTransition = when (animations) {
        AnimationLevel.FULL -> fadeOut(tween(FULL_FADE_MS))
        AnimationLevel.REDUCED -> fadeOut(tween(REDUCED_FADE_MS))
        AnimationLevel.OFF -> ExitTransition.None
    }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = { enter },
        exitTransition = { exit },
        popEnterTransition = { enter },
        popExitTransition = { exit }
    ) {

        composable(Routes.HOME) {
            PageEditorScreen(
                pageId = PageEntity.ROOT_PAGE_ID,
                factory = factory,
                onBack = { navController.popBackStack() },
                onOpenSearch = { navController.navigate(Routes.SEARCH) },
                onOpenUpdates = { id -> navController.navigate(Routes.updates(id)) },
                onNavigateToPage = { pageId -> navController.navigate(Routes.pageEditor(pageId)) },
                onNavigateToDatabase = { pageId -> navController.navigate(Routes.databaseView(pageId)) },
                onOpenSidebar = onOpenSidebar
            )
        }

        composable(
            route = Routes.PAGE_EDITOR,
            arguments = listOf(navArgument("pageId") { type = NavType.StringType })
        ) { backStackEntry ->
            val pageId = backStackEntry.arguments?.getString("pageId") ?: return@composable
            PageEditorScreen(
                pageId = pageId,
                factory = factory,
                onBack = { navController.popBackStack() },
                onOpenSearch = { navController.navigate(Routes.SEARCH) },
                onOpenUpdates = { id -> navController.navigate(Routes.updates(id)) },
                onNavigateToPage = { linkedId -> navController.navigate(Routes.pageEditor(linkedId)) },
                onNavigateToDatabase = { linkedId -> navController.navigate(Routes.databaseView(linkedId)) },
                onOpenSidebar = onOpenSidebar
            )
        }

        composable(
            route = Routes.UPDATES,
            arguments = listOf(navArgument("pageId") { type = NavType.StringType })
        ) { backStackEntry ->
            val pageId = backStackEntry.arguments?.getString("pageId") ?: return@composable
            UpdatesScreen(
                pageId = pageId,
                factory = factory,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.DATABASE_VIEW,
            arguments = listOf(navArgument("pageId") { type = NavType.StringType })
        ) { backStackEntry ->
            val pageId = backStackEntry.arguments?.getString("pageId") ?: return@composable
            DatabaseViewScreen(
                pageId = pageId,
                factory = factory,
                onBack = { navController.popBackStack() },
                onOpenRowPage = { rowPageId -> navController.navigate(Routes.pageEditor(rowPageId)) },
                onOpenSearch = { navController.navigate(Routes.SEARCH) },
                onOpenUpdates = { id -> navController.navigate(Routes.updates(id)) },
                onOpenPage = { id -> navController.navigate(Routes.pageEditor(id)) },
                onOpenDatabase = { id -> navController.navigate(Routes.databaseView(id)) },
                onOpenSidebar = onOpenSidebar
            )
        }

        composable(Routes.SEARCH) {
            SearchScreen(
                factory = factory,
                onBack = { navController.popBackStack() },
                onOpenHit = { hit ->
                    // Via la ricerca prima di aprire la pagina, così
                    // "indietro" dalla pagina torna da dove si era partiti
                    // e non alla ricerca vuota.
                    val pageId = hit.pageId
                    val rowId = hit.rowId
                    if (pageId != null) {
                        navController.popBackStack()
                        navController.navigate(Routes.forPage(pageId, hit.isDatabase))
                    } else if (rowId != null) {
                        onOpenRowPage(rowId) { id ->
                            navController.popBackStack()
                            navController.navigate(Routes.pageEditor(id))
                        }
                    }
                }
            )
        }

        composable(Routes.FAVORITES) {
            FavoritesScreen(
                factory = factory,
                onBack = { navController.popBackStack() },
                onOpenPage = { page -> navController.navigate(Routes.forPage(page.id, page.isDatabase)) }
            )
        }

        composable(Routes.TRASH) {
            TrashScreen(
                factory = factory,
                onBack = { navController.popBackStack() },
                onOpenPage = { page -> navController.navigate(Routes.forPage(page.id, page.isDatabase)) }
            )
        }

        composable(Routes.STARTUP) {
            StartupScreen(factory = factory, onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}

// Piena è la dissolvenza di serie della navigazione, quella che l'app ha
// sempre avuto; ridotta è un sesto.
private const val FULL_FADE_MS = 700
private const val REDUCED_FADE_MS = 120

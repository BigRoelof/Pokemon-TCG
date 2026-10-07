package com.example.pokemontcg.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.pokemontcg.ui.binders.BinderPickerScreen
import com.example.pokemontcg.ui.binders.BinderPickerViewModel
import com.example.pokemontcg.ui.binders.BinderScreen
import com.example.pokemontcg.ui.binders.BinderViewModel
import com.example.pokemontcg.ui.binders.BindersScreen
import com.example.pokemontcg.ui.chase.ChaseListScreen
import com.example.pokemontcg.ui.collection.CollectionScreen
import com.example.pokemontcg.ui.components.TopLevelSection
import com.example.pokemontcg.ui.details.DetailsScreen
import com.example.pokemontcg.ui.search.SearchScreen
import com.example.pokemontcg.ui.search.SearchViewModel

object Routes {
    const val COLLECTION = "collection"
    const val CHASE = "chase"
    const val SEARCH = "search?${SearchViewModel.SET_ID_KEY}={${SearchViewModel.SET_ID_KEY}}"
    const val DETAILS = "details/{cardId}"
    const val BINDERS = "binders"
    const val BINDER = "binder/{${BinderViewModel.BINDER_ID_KEY}}"
    const val BINDER_ADD = "binder/{${BinderViewModel.BINDER_ID_KEY}}/add?${BinderPickerViewModel.POCKET_KEY}={${BinderPickerViewModel.POCKET_KEY}}"

    fun forSection(section: TopLevelSection) = when (section) {
        TopLevelSection.COLLECTION -> COLLECTION
        TopLevelSection.CHASE -> CHASE
        TopLevelSection.BINDERS -> BINDERS
    }

    /** Search, optionally opened inside one set. */
    fun createSearchRoute(setId: String? = null) =
        if (setId == null) "search" else "search?${SearchViewModel.SET_ID_KEY}=${Uri.encode(setId)}"

    fun createDetailsRoute(cardId: String) = "details/$cardId"

    fun createBinderRoute(binderId: Long) = "binder/$binderId"

    /** The binder's card picker; picked cards fill the empty pockets from [fromPocket] on. */
    fun createBinderAddRoute(binderId: Long, fromPocket: Int) =
        "binder/$binderId/add?${BinderPickerViewModel.POCKET_KEY}=$fromPocket"
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    factory: ViewModelProvider.Factory
) {
    val openSearch = { navController.navigate(Routes.createSearchRoute()) }
    val openDetails = { cardId: String -> navController.navigate(Routes.createDetailsRoute(cardId)) }
    // Standard bottom-bar behaviour: one copy of each section, each keeping its scroll and filters
    val openSection = { section: TopLevelSection ->
        navController.navigate(Routes.forSection(section)) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(navController = navController, startDestination = Routes.COLLECTION) {
        composable(Routes.COLLECTION) {
            CollectionScreen(
                onNavigateToSearch = openSearch,
                onNavigateToDetails = openDetails,
                onSelectSection = openSection,
                factory = factory
            )
        }

        composable(Routes.CHASE) {
            ChaseListScreen(
                onNavigateToSearch = openSearch,
                onNavigateToDetails = openDetails,
                onSelectSection = openSection,
                factory = factory
            )
        }

        composable(Routes.BINDERS) {
            BindersScreen(
                onOpenBinder = { binderId -> navController.navigate(Routes.createBinderRoute(binderId)) },
                onSelectSection = openSection,
                factory = factory
            )
        }

        composable(
            route = Routes.BINDER,
            arguments = listOf(navArgument(BinderViewModel.BINDER_ID_KEY) { type = NavType.LongType })
        ) { backStackEntry ->
            val binderId = backStackEntry.arguments?.getLong(BinderViewModel.BINDER_ID_KEY) ?: return@composable
            BinderScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetails = openDetails,
                onAddCards = { fromPocket -> navController.navigate(Routes.createBinderAddRoute(binderId, fromPocket)) },
                factory = factory
            )
        }

        composable(
            route = Routes.BINDER_ADD,
            arguments = listOf(
                navArgument(BinderViewModel.BINDER_ID_KEY) { type = NavType.LongType },
                navArgument(BinderPickerViewModel.POCKET_KEY) {
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ) {
            BinderPickerScreen(
                onNavigateBack = { navController.popBackStack() },
                factory = factory
            )
        }

        composable(
            route = Routes.SEARCH,
            arguments = listOf(
                navArgument(SearchViewModel.SET_ID_KEY) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetails = openDetails,
                factory = factory
            )
        }

        composable(
            route = Routes.DETAILS,
            arguments = listOf(navArgument("cardId") { type = NavType.StringType })
        ) { backStackEntry ->
            val cardId = backStackEntry.arguments?.getString("cardId") ?: return@composable
            DetailsScreen(
                cardId = cardId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSet = { setId -> navController.navigate(Routes.createSearchRoute(setId)) },
                factory = factory
            )
        }
    }
}

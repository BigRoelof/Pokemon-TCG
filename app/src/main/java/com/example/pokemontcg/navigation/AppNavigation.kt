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

    fun forSection(section: TopLevelSection) = when (section) {
        TopLevelSection.COLLECTION -> COLLECTION
        TopLevelSection.CHASE -> CHASE
    }

    /** Search, optionally opened inside one set. */
    fun createSearchRoute(setId: String? = null) =
        if (setId == null) "search" else "search?${SearchViewModel.SET_ID_KEY}=${Uri.encode(setId)}"

    fun createDetailsRoute(cardId: String) = "details/$cardId"
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

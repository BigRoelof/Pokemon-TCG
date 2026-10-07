package com.example.pokemontcg.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.lifecycle.ViewModelProvider
import com.example.pokemontcg.ui.details.DetailsScreen
import com.example.pokemontcg.ui.home.HomeScreen
import com.example.pokemontcg.ui.search.SearchScreen
import com.example.pokemontcg.ui.search.SearchViewModel

object Routes {
    const val HOME = "home"
    const val SEARCH = "search?${SearchViewModel.SET_ID_KEY}={${SearchViewModel.SET_ID_KEY}}"
    const val DETAILS = "details/{cardId}"

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
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToSearch = { navController.navigate(Routes.createSearchRoute()) },
                onNavigateToDetails = { cardId -> 
                    navController.navigate(Routes.createDetailsRoute(cardId))
                },
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
                onNavigateToDetails = { cardId ->
                    navController.navigate(Routes.createDetailsRoute(cardId))
                },
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

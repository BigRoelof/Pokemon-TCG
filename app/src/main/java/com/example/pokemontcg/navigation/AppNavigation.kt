package com.example.pokemontcg.navigation

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

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val DETAILS = "details/{cardId}"

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
                onNavigateToSearch = { navController.navigate(Routes.SEARCH) },
                onNavigateToDetails = { cardId -> 
                    navController.navigate(Routes.createDetailsRoute(cardId))
                },
                factory = factory
            )
        }
        
        composable(Routes.SEARCH) {
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
                factory = factory
            )
        }
    }
}

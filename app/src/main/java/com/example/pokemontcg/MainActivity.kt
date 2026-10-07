package com.example.pokemontcg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.example.pokemontcg.navigation.AppNavigation
import com.example.pokemontcg.ui.theme.PokemonTCGChaseListTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModelFactory = (application as PokemonTcgApplication).container.viewModelFactory

        setContent {
            PokemonTCGChaseListTheme {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    factory = viewModelFactory
                )
            }
        }
    }
}

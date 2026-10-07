package com.example.pokemontcg

import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.pokemontcg.navigation.AppNavigation
import com.example.pokemontcg.ui.theme.PokemonTCGChaseListTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Every screen has a red header behind the status bar, so its icons are always light
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
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

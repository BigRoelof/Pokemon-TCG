package com.example.pokemontcg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.example.pokemontcg.data.api.PokemonApi
import com.example.pokemontcg.data.database.PokemonDatabase
import com.example.pokemontcg.data.repository.PokemonRepository
import com.example.pokemontcg.navigation.AppNavigation
import com.example.pokemontcg.ui.ViewModelFactory
import com.example.pokemontcg.ui.theme.PokemonTCGChaseListTheme
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val okHttpClient = okhttp3.OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                    .build()
                var response = chain.proceed(request)
                var tryCount = 0
                val maxLimit = 2
                while (!response.isSuccessful && (response.code == 500 || response.code == 429 || response.code == 503) && tryCount < maxLimit) {
                    tryCount++
                    response.close()
                    try {
                        Thread.sleep(1000L * tryCount)
                    } catch (e: InterruptedException) {
                        break
                    }
                    response = chain.proceed(request)
                }
                response
            }
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val api = Retrofit.Builder()
            .baseUrl(PokemonApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PokemonApi::class.java)

        val database = PokemonDatabase.getDatabase(applicationContext)
        val dao = database.pokemonDao()

        val repository = PokemonRepository(api, dao)
        val viewModelFactory = ViewModelFactory(repository)

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

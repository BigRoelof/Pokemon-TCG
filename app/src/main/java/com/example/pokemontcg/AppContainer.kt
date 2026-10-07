package com.example.pokemontcg

import android.content.Context
import com.example.pokemontcg.data.api.PokemonApi
import com.example.pokemontcg.data.database.PokemonDatabase
import com.example.pokemontcg.data.repository.PokemonRepository
import com.example.pokemontcg.ui.ViewModelFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Manual dependency injection: app-wide singletons, created once per process. */
class AppContainer(context: Context) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val api: PokemonApi = Retrofit.Builder()
        .baseUrl(PokemonApi.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(PokemonApi::class.java)

    private val database = PokemonDatabase.getDatabase(context)

    val repository = PokemonRepository(api, database.pokemonDao())

    val viewModelFactory = ViewModelFactory(repository)
}

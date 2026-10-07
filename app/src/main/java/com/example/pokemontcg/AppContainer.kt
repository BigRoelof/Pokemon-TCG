package com.example.pokemontcg

import android.content.Context
import com.example.pokemontcg.data.catalog.CatalogApi
import com.example.pokemontcg.data.catalog.CatalogSync
import com.example.pokemontcg.data.database.PokemonDatabase
import com.example.pokemontcg.data.preferences.UserPreferences
import com.example.pokemontcg.data.prices.PriceApi
import com.example.pokemontcg.data.prices.PriceService
import com.example.pokemontcg.data.repository.PokemonRepository
import com.example.pokemontcg.ui.createViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Manual dependency injection: app-wide singletons, created once per process. */
class AppContainer(context: Context) {

    /** For work that outlives any screen, like the catalog sync. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val catalogApi: CatalogApi = Retrofit.Builder()
        .baseUrl(CatalogApi.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(CatalogApi::class.java)

    private val priceApi: PriceApi = Retrofit.Builder()
        .baseUrl(PriceApi.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(PriceApi::class.java)

    private val database = PokemonDatabase.getDatabase(context)

    private val catalogSync = CatalogSync(catalogApi, database.catalogDao(), appScope)

    private val priceService = PriceService(priceApi, database.priceDao(), database.catalogDao())

    val repository = PokemonRepository(
        dao = database.pokemonDao(),
        collectionDao = database.collectionDao(),
        catalogDao = database.catalogDao(),
        catalogSync = catalogSync,
        priceDao = database.priceDao(),
        priceService = priceService
    )

    private val preferences = UserPreferences(context)

    val viewModelFactory = createViewModelFactory(repository, preferences)
}

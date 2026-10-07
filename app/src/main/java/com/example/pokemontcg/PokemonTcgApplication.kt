package com.example.pokemontcg

import android.app.Application

class PokemonTcgApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // Fetch new and changed sets in the background; cheap when nothing changed
        container.repository.syncCatalog()
    }
}

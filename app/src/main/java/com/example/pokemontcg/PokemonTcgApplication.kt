package com.example.pokemontcg

import android.app.Application

class PokemonTcgApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }
}

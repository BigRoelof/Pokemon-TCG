package com.example.pokemontcg.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pokemontcg.data.preferences.UserPreferences
import com.example.pokemontcg.data.repository.PokemonRepository
import com.example.pokemontcg.ui.chase.ChaseListViewModel
import com.example.pokemontcg.ui.collection.CollectionViewModel
import com.example.pokemontcg.ui.details.DetailsViewModel
import com.example.pokemontcg.ui.search.SearchViewModel

/** Creates every ViewModel in the app. Add an `initializer` here for each new ViewModel. */
fun createViewModelFactory(
    repository: PokemonRepository,
    preferences: UserPreferences
): ViewModelProvider.Factory = viewModelFactory {
    initializer { CollectionViewModel(repository, preferences) }
    initializer { ChaseListViewModel(repository, preferences) }
    initializer { SearchViewModel(repository, createSavedStateHandle()) }
    initializer { DetailsViewModel(repository) }
}

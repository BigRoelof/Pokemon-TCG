package com.example.pokemontcg.ui.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokemontcg.data.model.CardSort
import com.example.pokemontcg.data.preferences.UserPreferences
import com.example.pokemontcg.data.repository.PokemonRepository
import com.example.pokemontcg.ui.lists.CardListState
import com.example.pokemontcg.ui.lists.buildCardListState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CollectionViewModel(
    private val repository: PokemonRepository,
    private val preferences: UserPreferences
) : ViewModel() {

    private val setName = MutableStateFlow<String?>(null)

    /** Null while loading. */
    val uiState: StateFlow<CardListState?> =
        combine(repository.collection, setName, preferences.collectionSort, repository.catalogSets, repository.prices) {
                collection, set, sort, catalogSets, prices ->
            buildCardListState(collection, sort, set, catalogSets, prices, ownedCards = collection)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        // Prices are cached for a day; this only fetches missing or stale ones
        viewModelScope.launch { repository.refreshTrackedPrices() }
    }

    fun onSortSelected(sort: CardSort) = preferences.setCollectionSort(sort)

    fun onSetSelected(name: String?) {
        setName.value = name
    }
}

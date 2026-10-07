package com.example.pokemontcg.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokemontcg.data.catalog.SyncState
import com.example.pokemontcg.data.model.Card
import com.example.pokemontcg.data.repository.PokemonRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

sealed class SearchUiState {
    object Idle : SearchUiState()
    data class Success(val query: String, val results: List<Card>) : SearchUiState()
}

data class CatalogStatus(val cardCount: Int, val sync: SyncState)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val repository: PokemonRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val catalogStatus: StateFlow<CatalogStatus> =
        combine(repository.catalogCardCount, repository.catalogSyncState, ::CatalogStatus)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CatalogStatus(0, SyncState.Idle))

    // Searching is local and fast, so a short debounce is enough. Re-runs when the catalog
    // grows, so results fill in while the first download is still running.
    val uiState: StateFlow<SearchUiState> =
        combine(
            _searchQuery.debounce { if (it.isBlank()) 0 else 250 },
            repository.catalogCardCount
        ) { query, _ -> query }
            .mapLatest { query ->
                if (query.isBlank()) SearchUiState.Idle
                else SearchUiState.Success(query, repository.searchCards(query))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState.Idle)

    /** Ids of cards already on the chase list, to tag them in the results. */
    val savedCardIds: StateFlow<Set<String>> = repository.chaseCards
        .map { cards -> cards.mapTo(HashSet()) { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun onQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun retrySync() = repository.syncCatalog()
}

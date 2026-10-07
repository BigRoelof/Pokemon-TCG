package com.example.pokemontcg.ui.chase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokemontcg.data.database.ChaseCardEntity
import com.example.pokemontcg.data.model.CardSort
import com.example.pokemontcg.data.preferences.UserPreferences
import com.example.pokemontcg.data.repository.PokemonRepository
import com.example.pokemontcg.ui.lists.CardListState
import com.example.pokemontcg.ui.lists.buildCardListState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A card just moved to the collection; [original] restores it on Undo. */
data class CaughtEvent(val original: ChaseCardEntity)

class ChaseListViewModel(
    private val repository: PokemonRepository,
    private val preferences: UserPreferences
) : ViewModel() {

    private val setName = MutableStateFlow<String?>(null)
    private val options = combine(setName, preferences.chaseSort) { set, sort -> set to sort }

    /** Null while loading. */
    val uiState: StateFlow<CardListState?> =
        combine(repository.chaseList, repository.collection, options, repository.catalogSets, repository.prices) {
                chase, collection, (set, sort), catalogSets, prices ->
            buildCardListState(chase, sort, set, catalogSets, prices, ownedCards = collection)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _caughtEvents = Channel<CaughtEvent>(Channel.BUFFERED)
    val caughtEvents: Flow<CaughtEvent> = _caughtEvents.receiveAsFlow()

    init {
        // Prices are cached for a day; this only fetches missing or stale ones
        viewModelScope.launch { repository.refreshTrackedPrices() }
    }

    fun onSortSelected(sort: CardSort) = preferences.setChaseSort(sort)

    fun onSetSelected(name: String?) {
        setName.value = name
    }

    fun catchCard(cardId: String) {
        viewModelScope.launch {
            repository.catchCard(cardId)?.let { _caughtEvents.send(CaughtEvent(it)) }
        }
    }

    fun undoCatch(event: CaughtEvent) {
        viewModelScope.launch { repository.undoCatch(event.original) }
    }
}

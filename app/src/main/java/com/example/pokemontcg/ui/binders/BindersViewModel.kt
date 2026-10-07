package com.example.pokemontcg.ui.binders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokemontcg.data.repository.PokemonRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BindersViewModel(
    private val repository: PokemonRepository
) : ViewModel() {

    /** Null while loading. */
    val uiState: StateFlow<List<BinderSummary>?> =
        combine(repository.binders, repository.binderCards, repository.prices, ::buildBinderSummaries)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _createdBinders = Channel<Long>(Channel.BUFFERED)

    /** Ids of binders just created here, to open them. */
    val createdBinders: Flow<Long> = _createdBinders.receiveAsFlow()

    init {
        // Binder cards are collection cards; prices are cached for a day
        viewModelScope.launch { repository.refreshTrackedPrices() }
    }

    fun createBinder(name: String) {
        viewModelScope.launch { _createdBinders.send(repository.createBinder(name)) }
    }
}

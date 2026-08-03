package com.example.pokemontcg.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokemontcg.data.database.ChaseCardEntity
import com.example.pokemontcg.data.repository.PokemonRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val cards: List<ChaseCardEntity>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel(private val repository: PokemonRepository) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = repository.chaseCards
        .map { cards -> HomeUiState.Success(cards) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState.Loading
        )

    fun toggleObtainedStatus(cardId: String, currentStatus: Boolean) {
        viewModelScope.launch {
            repository.updateCardObtainedStatus(cardId, !currentStatus)
        }
    }
}

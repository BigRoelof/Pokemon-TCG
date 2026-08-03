package com.example.pokemontcg.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokemontcg.data.api.model.CardDto
import com.example.pokemontcg.data.repository.PokemonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class DetailsUiState {
    object Loading : DetailsUiState()
    data class Success(val card: CardDto) : DetailsUiState()
    data class Error(val message: String) : DetailsUiState()
}

class DetailsViewModel(
    private val repository: PokemonRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailsUiState>(DetailsUiState.Loading)
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    private var currentCardId: String? = null

    fun loadCard(cardId: String) {
        if (currentCardId == cardId) return
        currentCardId = cardId
        
        viewModelScope.launch {
            _uiState.value = DetailsUiState.Loading
            
            // Collect isSaved status
            launch {
                repository.isCardInChaseList(cardId).collect { saved ->
                    _isSaved.value = saved
                }
            }

            val result = repository.getCardDetails(cardId)
            result.onSuccess { card ->
                _uiState.value = DetailsUiState.Success(card)
            }.onFailure { error ->
                _uiState.value = DetailsUiState.Error(error.message ?: "Failed to load card")
            }
        }
    }

    fun toggleChaseList() {
        val state = _uiState.value
        if (state is DetailsUiState.Success) {
            viewModelScope.launch {
                if (_isSaved.value) {
                    repository.removeCardFromChaseList(state.card.id)
                } else {
                    repository.addCardToChaseList(state.card)
                }
            }
        }
    }
}

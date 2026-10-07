package com.example.pokemontcg.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokemontcg.data.model.Card
import com.example.pokemontcg.data.repository.PokemonRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DetailsUiState {
    object Loading : DetailsUiState()
    data class Success(val card: Card) : DetailsUiState()
    object NotFound : DetailsUiState()
}

class DetailsViewModel(
    private val repository: PokemonRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailsUiState>(DetailsUiState.Loading)
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    private val _isObtained = MutableStateFlow(false)
    val isObtained: StateFlow<Boolean> = _isObtained.asStateFlow()

    private var currentCardId: String? = null
    private var savedStatusJob: Job? = null

    fun loadCard(cardId: String) {
        if (currentCardId == cardId) return
        currentCardId = cardId

        savedStatusJob?.cancel()
        savedStatusJob = viewModelScope.launch {
            repository.observeSavedCard(cardId).collect { saved ->
                _isSaved.value = saved != null
                _isObtained.value = saved?.obtained == true
            }
        }
        viewModelScope.launch {
            _uiState.value = repository.getCard(cardId)?.let { DetailsUiState.Success(it) }
                ?: DetailsUiState.NotFound
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

    fun setObtained(obtained: Boolean) {
        val cardId = currentCardId ?: return
        viewModelScope.launch {
            repository.setObtained(cardId, obtained)
        }
    }
}

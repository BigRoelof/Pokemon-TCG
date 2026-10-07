package com.example.pokemontcg.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokemontcg.data.api.model.CardDto
import com.example.pokemontcg.data.repository.PokemonRepository
import com.example.pokemontcg.ui.toUserMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _isObtained = MutableStateFlow(false)
    val isObtained: StateFlow<Boolean> = _isObtained.asStateFlow()

    private var currentCardId: String? = null
    private var savedStatusJob: Job? = null
    private var loadJob: Job? = null

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
        fetchCard(cardId)
    }

    fun retry() {
        currentCardId?.let(::fetchCard)
    }

    private fun fetchCard(cardId: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            // Show the locally saved copy right away so saved cards also work offline,
            // then refresh it with the full data from the API.
            val savedCard = repository.getSavedCard(cardId)
            _uiState.value = savedCard?.let { DetailsUiState.Success(it) } ?: DetailsUiState.Loading

            repository.getCardDetails(cardId)
                .onSuccess { card ->
                    _uiState.value = DetailsUiState.Success(card)
                }.onFailure { error ->
                    if (savedCard == null) {
                        _uiState.value = DetailsUiState.Error(error.toUserMessage())
                    }
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

    fun setObtained(obtained: Boolean) {
        val cardId = currentCardId ?: return
        viewModelScope.launch {
            repository.setObtained(cardId, obtained)
        }
    }
}

package com.example.pokemontcg.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokemontcg.data.api.model.CardDto
import com.example.pokemontcg.data.repository.PokemonRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(val results: List<CardDto>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

class SearchViewModel(
    private val repository: PokemonRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.value = SearchUiState.Idle
            return
        }

        searchJob = viewModelScope.launch {
            delay(800) // debounce
            _uiState.value = SearchUiState.Loading
            val result = repository.searchCards(query)
            result.onSuccess { cards ->
                _uiState.value = SearchUiState.Success(cards)
            }.onFailure { error ->
                _uiState.value = SearchUiState.Error(error.message ?: "An error occurred")
            }
        }
    }
}

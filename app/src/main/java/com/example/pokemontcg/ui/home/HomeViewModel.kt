package com.example.pokemontcg.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokemontcg.data.database.ChaseCardEntity
import com.example.pokemontcg.data.repository.PokemonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ChaseFilter(val label: String) {
    ALL("All"),
    CHASING("Chasing"),
    OBTAINED("Caught")
}

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        /** Cards matching [filter]. */
        val cards: List<ChaseCardEntity>,
        val filter: ChaseFilter,
        val obtainedCount: Int,
        val totalCount: Int
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

fun buildHomeState(cards: List<ChaseCardEntity>, filter: ChaseFilter) = HomeUiState.Success(
    cards = when (filter) {
        ChaseFilter.ALL -> cards
        ChaseFilter.CHASING -> cards.filterNot { it.obtained }
        ChaseFilter.OBTAINED -> cards.filter { it.obtained }
    },
    filter = filter,
    obtainedCount = cards.count { it.obtained },
    totalCount = cards.size
)

class HomeViewModel(private val repository: PokemonRepository) : ViewModel() {

    private val filter = MutableStateFlow(ChaseFilter.ALL)

    val uiState: StateFlow<HomeUiState> = combine(repository.chaseCards, filter, ::buildHomeState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState.Loading
        )

    fun onFilterSelected(newFilter: ChaseFilter) {
        filter.value = newFilter
    }

    fun setObtained(cardId: String, obtained: Boolean) {
        viewModelScope.launch {
            repository.setObtained(cardId, obtained)
        }
    }
}

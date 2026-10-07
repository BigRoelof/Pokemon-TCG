package com.example.pokemontcg.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokemontcg.data.database.CardPriceEntity
import com.example.pokemontcg.data.database.CardSetWithCount
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
        /** Cards matching [setName] and [filter]. */
        val cards: List<ChaseCardEntity>,
        val filter: ChaseFilter,
        /** Counts cover the chosen set (or the whole list), regardless of [filter]. */
        val obtainedCount: Int,
        val totalCount: Int,
        /** The set the list is narrowed to, or null for all sets. */
        val setName: String? = null,
        /**
         * Sets on the chase list, newest first. The chase list stores set names, not ids, so
         * [CardSetWithCount.id] holds the set name here; [CardSetWithCount.cardCount] is the
         * number of chase-list cards in that set.
         */
        val sets: List<CardSetWithCount> = emptyList(),
        /** Cardmarket prices (EUR) by card id, for the cards that have one. */
        val prices: Map<String, Double> = emptyMap(),
        /** Total price of the cards still to catch, within the chosen set. */
        val valueToChase: Double = 0.0
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

fun buildHomeState(
    cards: List<ChaseCardEntity>,
    filter: ChaseFilter,
    setName: String? = null,
    catalogSets: List<CardSetWithCount> = emptyList(),
    prices: Map<String, CardPriceEntity> = emptyMap()
): HomeUiState.Success {
    val knownPrices = cards.mapNotNull { card -> prices[card.id]?.price?.let { card.id to it } }.toMap()
    // A set whose last card was removed no longer filters anything
    val activeSet = setName?.takeIf { name -> cards.any { it.setName == name } }
    val inSet = if (activeSet == null) cards else cards.filter { it.setName == activeSet }
    return HomeUiState.Success(
        cards = when (filter) {
            ChaseFilter.ALL -> inSet
            ChaseFilter.CHASING -> inSet.filterNot { it.obtained }
            ChaseFilter.OBTAINED -> inSet.filter { it.obtained }
        },
        filter = filter,
        obtainedCount = inSet.count { it.obtained },
        totalCount = inSet.size,
        setName = activeSet,
        sets = chaseListSets(cards, catalogSets),
        prices = knownPrices,
        valueToChase = inSet.filterNot { it.obtained }.sumOf { knownPrices[it.id] ?: 0.0 }
    )
}

private fun chaseListSets(cards: List<ChaseCardEntity>, catalogSets: List<CardSetWithCount>): List<CardSetWithCount> {
    val catalogByName = catalogSets.associateBy { it.name }
    return cards.groupingBy { it.setName }.eachCount().map { (name, count) ->
        val known = catalogByName[name]
        CardSetWithCount(
            id = name,
            name = name,
            series = known?.series,
            releaseDate = known?.releaseDate,
            symbolUrl = known?.symbolUrl,
            ptcgoCode = known?.ptcgoCode,
            cardCount = count
        )
    }.sortedWith(compareByDescending<CardSetWithCount> { it.releaseDate.orEmpty() }.thenBy { it.name })
}

class HomeViewModel(private val repository: PokemonRepository) : ViewModel() {

    private val filter = MutableStateFlow(ChaseFilter.ALL)
    private val setName = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> =
        combine(repository.chaseCards, filter, setName, repository.catalogSets, repository.prices, ::buildHomeState)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = HomeUiState.Loading
            )

    init {
        // Prices are cached for a day; this only fetches missing or stale ones
        viewModelScope.launch { repository.refreshChaseListPrices() }
    }

    fun onFilterSelected(newFilter: ChaseFilter) {
        filter.value = newFilter
    }

    fun onSetSelected(name: String?) {
        setName.value = name
    }

    fun setObtained(cardId: String, obtained: Boolean) {
        viewModelScope.launch {
            repository.setObtained(cardId, obtained)
        }
    }
}

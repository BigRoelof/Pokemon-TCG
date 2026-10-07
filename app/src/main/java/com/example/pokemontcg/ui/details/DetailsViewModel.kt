package com.example.pokemontcg.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokemontcg.data.database.CardPriceEntity
import com.example.pokemontcg.data.model.Card
import com.example.pokemontcg.data.model.TrackedCard
import com.example.pokemontcg.data.repository.PokemonRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class DetailsUiState {
    object Loading : DetailsUiState()
    data class Success(val card: Card) : DetailsUiState()
    object NotFound : DetailsUiState()
}

/** [price] is the cached row (null until first fetched); [loading]/[failed] describe the latest refresh. */
data class PriceUiState(
    val price: CardPriceEntity? = null,
    val loading: Boolean = false,
    val failed: Boolean = false
)

class DetailsViewModel(
    private val repository: PokemonRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailsUiState>(DetailsUiState.Loading)
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    /** The card's place in the user's lists: in the collection (owned), on the chase list, or null. */
    private val _tracked = MutableStateFlow<TrackedCard?>(null)
    val tracked: StateFlow<TrackedCard?> = _tracked.asStateFlow()

    private val _price = MutableStateFlow(PriceUiState())
    val price: StateFlow<PriceUiState> = _price.asStateFlow()

    private var currentCardId: String? = null
    private var savedStatusJob: Job? = null
    private var priceJob: Job? = null

    fun loadCard(cardId: String) {
        if (currentCardId == cardId) return
        currentCardId = cardId

        savedStatusJob?.cancel()
        savedStatusJob = viewModelScope.launch {
            repository.observeTrackedCard(cardId).collect { _tracked.value = it }
        }
        viewModelScope.launch {
            _uiState.value = repository.getCard(cardId)?.let { DetailsUiState.Success(it) }
                ?: DetailsUiState.NotFound
        }

        priceJob?.cancel()
        priceJob = viewModelScope.launch {
            repository.observePrice(cardId).collect { row -> _price.update { it.copy(price = row) } }
        }
        refreshPrice()
    }

    fun refreshPrice() {
        val cardId = currentCardId ?: return
        viewModelScope.launch {
            _price.update { it.copy(loading = true, failed = false) }
            val failed = runCatching { repository.refreshPrice(cardId) }.isFailure
            _price.update { it.copy(loading = false, failed = failed) }
        }
    }

    fun addToCollection() = withCard { repository.addCardToCollection(it) }

    fun addToChaseList() = withCard { repository.addCardToChaseList(it) }

    /** Moves the card from the chase list into the collection. */
    fun catchCard() = withCard { repository.catchCard(it.id) }

    /** Removes the card from whichever list it's on. */
    fun removeCard() = withCard { repository.removeCard(it.id) }

    private fun withCard(action: suspend (Card) -> Unit) {
        val card = (_uiState.value as? DetailsUiState.Success)?.card ?: return
        viewModelScope.launch { action(card) }
    }
}

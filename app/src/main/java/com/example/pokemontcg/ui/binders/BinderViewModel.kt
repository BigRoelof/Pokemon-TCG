package com.example.pokemontcg.ui.binders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokemontcg.data.model.TrackedCard
import com.example.pokemontcg.data.repository.PokemonRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class BinderUiState {
    object Loading : BinderUiState()
    data class Ready(val binder: BinderState) : BinderUiState()
    /** Deleted, or never existed. */
    object NotFound : BinderUiState()
}

/** A card just taken out of the binder; Undo puts it back in [pocket] (or the next empty one). */
data class RemovedFromBinder(val card: TrackedCard, val pocket: Int)

class BinderViewModel(
    private val repository: PokemonRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val binderId: Long = checkNotNull(savedStateHandle[BINDER_ID_KEY]) { "Binder screen opened without a binder id" }

    val uiState: StateFlow<BinderUiState> =
        combine(repository.observeBinder(binderId), repository.observeBinderCards(binderId), repository.prices) { binder, cards, prices ->
            if (binder == null) BinderUiState.NotFound else BinderUiState.Ready(buildBinderState(binder, cards, prices))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BinderUiState.Loading)

    /** The card picked up to move (long-pressed), or null. */
    val movingCardId: StateFlow<String?> = savedStateHandle.getStateFlow(MOVING_KEY, null)

    private val _removed = Channel<RemovedFromBinder>(Channel.BUFFERED)
    val removedCards: Flow<RemovedFromBinder> = _removed.receiveAsFlow()

    fun startMoving(cardId: String) {
        savedStateHandle[MOVING_KEY] = cardId
    }

    fun cancelMoving() {
        savedStateHandle[MOVING_KEY] = null
    }

    /** Drops the picked-up card into [pocket], swapping with a card already there. */
    fun moveTo(pocket: Int) {
        val cardId = movingCardId.value ?: return
        cancelMoving()
        viewModelScope.launch { repository.moveCardInBinder(binderId, cardId, pocket) }
    }

    /** Takes the picked-up card out of the binder; it stays in the collection. */
    fun removeMovingCard() {
        val cardId = movingCardId.value ?: return
        cancelMoving()
        val binder = (uiState.value as? BinderUiState.Ready)?.binder ?: return
        val (pocket, card) = binder.pockets.entries.firstOrNull { it.value.id == cardId } ?: return
        viewModelScope.launch {
            repository.removeCardFromBinder(binderId, cardId)
            _removed.send(RemovedFromBinder(card, pocket))
        }
    }

    fun undoRemove(removed: RemovedFromBinder) {
        viewModelScope.launch { repository.addCardsToBinder(binderId, listOf(removed.card.id), fromPocket = removed.pocket) }
    }

    fun rename(name: String) {
        viewModelScope.launch { repository.renameBinder(binderId, name) }
    }

    fun delete() {
        viewModelScope.launch { repository.deleteBinder(binderId) }
    }

    /** Moves every card up so no empty pockets are left between them. */
    fun closeGaps() = arrange { binder -> binder.pockets.toSortedMap().values.map { it.id } }

    fun sortBySetAndNumber() = arrange { binder -> setAndNumberOrder(binder.pockets.values, repository.catalogSets.first()) }

    private fun arrange(order: suspend (BinderState) -> List<String>) {
        val binder = (uiState.value as? BinderUiState.Ready)?.binder ?: return
        cancelMoving()
        viewModelScope.launch { repository.arrangeBinder(binderId, order(binder)) }
    }

    companion object {
        const val BINDER_ID_KEY = "binderId"
        private const val MOVING_KEY = "movingCardId"
    }
}

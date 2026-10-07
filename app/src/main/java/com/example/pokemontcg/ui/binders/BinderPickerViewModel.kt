package com.example.pokemontcg.ui.binders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokemontcg.data.database.CardSetWithCount
import com.example.pokemontcg.data.model.CardSort
import com.example.pokemontcg.data.model.TrackedCard
import com.example.pokemontcg.data.repository.PokemonRepository
import com.example.pokemontcg.ui.lists.buildCardListState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The "Add cards" screen of a binder: the collection, to pick cards from. */
data class BinderPickerState(
    val binderName: String,
    /** Collection cards matching the query and set, newest set first, then by number. */
    val cards: List<TrackedCard>,
    val collectionSize: Int,
    /** Cards already in this binder. */
    val inBinder: Set<String>,
    /** Sets in the collection; ids are set names (see `CardListState.sets`). */
    val sets: List<CardSetWithCount>,
    val setName: String?
)

class BinderPickerViewModel(
    private val repository: PokemonRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val binderId: Long = checkNotNull(savedStateHandle[BinderViewModel.BINDER_ID_KEY]) { "Picker opened without a binder id" }

    /** Picked cards go into the empty pockets from this one on. */
    private val fromPocket: Int = savedStateHandle[POCKET_KEY] ?: 0

    val query: StateFlow<String> = savedStateHandle.getStateFlow(QUERY_KEY, "")
    private val setName: StateFlow<String?> = savedStateHandle.getStateFlow(SET_KEY, null)

    /** Picked card ids, in the order they were tapped: the order they'll fill the pockets. */
    val selected: StateFlow<List<String>> = savedStateHandle.getStateFlow(SELECTED_KEY, arrayListOf())

    private val filters = combine(query, setName) { query, set -> query to set }

    /** Null while loading. */
    val uiState: StateFlow<BinderPickerState?> = combine(
        repository.collection,
        repository.observeBinder(binderId),
        repository.observeBinderCards(binderId),
        repository.catalogSets,
        filters
    ) { collection, binder, binderCards, catalogSets, (query, set) ->
        val list = buildCardListState(collection, CardSort.SET, set, catalogSets)
        BinderPickerState(
            binderName = binder?.name.orEmpty(),
            cards = list.cards.filter { matchesQuery(it, query) },
            collectionSize = collection.size,
            inBinder = binderCards.mapTo(HashSet()) { it.card.id },
            sets = list.sets,
            setName = list.setName
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _done = Channel<Unit>(Channel.BUFFERED)

    /** Emits once the picked cards are in the binder. */
    val done: Flow<Unit> = _done.receiveAsFlow()

    fun onQueryChanged(query: String) {
        savedStateHandle[QUERY_KEY] = query
    }

    fun onSetSelected(name: String?) {
        savedStateHandle[SET_KEY] = name
    }

    fun toggle(cardId: String) {
        val current = selected.value
        savedStateHandle[SELECTED_KEY] = ArrayList(if (cardId in current) current - cardId else current + cardId)
    }

    fun addSelected() {
        val cardIds = selected.value
        viewModelScope.launch {
            repository.addCardsToBinder(binderId, cardIds, fromPocket)
            _done.send(Unit)
        }
    }

    companion object {
        const val POCKET_KEY = "pocket"
        private const val QUERY_KEY = "query"
        private const val SET_KEY = "set"
        private const val SELECTED_KEY = "selected"
    }
}

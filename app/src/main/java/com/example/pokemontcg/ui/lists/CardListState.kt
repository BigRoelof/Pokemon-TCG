package com.example.pokemontcg.ui.lists

import com.example.pokemontcg.data.database.CardPriceEntity
import com.example.pokemontcg.data.database.CardSetWithCount
import com.example.pokemontcg.data.model.CardNumberOrder
import com.example.pokemontcg.data.model.CardSort
import com.example.pokemontcg.data.model.TrackedCard

/** How much of a set the user owns: [owned] of the [total] cards the catalog knows for it. */
data class SetCompletion(val setName: String, val owned: Int, val total: Int) {
    val fraction: Float get() = if (total == 0) 0f else (owned.toFloat() / total).coerceIn(0f, 1f)
}

/** What a card list screen (collection or chase list) shows. */
data class CardListState(
    /** Cards in the chosen set (or all), in [sort] order. */
    val cards: List<TrackedCard>,
    /** Size of the whole list, regardless of the set filter. */
    val totalCount: Int,
    val sort: CardSort,
    /** The set the list is narrowed to, or null for all sets. */
    val setName: String?,
    /**
     * Sets in this list, newest first. Lists store set names, not ids, so [CardSetWithCount.id]
     * holds the set name; [CardSetWithCount.cardCount] is the number of list cards in that set.
     */
    val sets: List<CardSetWithCount>,
    /** Cardmarket prices (EUR) by card id, for the cards that have one. */
    val prices: Map<String, Double>,
    /** Total price of [cards]. */
    val value: Double,
    /** Set completion for the chosen set, when the catalog knows its size. */
    val setCompletion: SetCompletion?
)

/**
 * Builds a list screen's state. [ownedCards] is the collection, used for set completion on
 * both screens (on the collection screen it's the same list as [listCards]).
 */
fun buildCardListState(
    listCards: List<TrackedCard>,
    sort: CardSort = CardSort.NEWEST,
    setName: String? = null,
    catalogSets: List<CardSetWithCount> = emptyList(),
    prices: Map<String, CardPriceEntity> = emptyMap(),
    ownedCards: List<TrackedCard> = emptyList()
): CardListState {
    val knownPrices = listCards.mapNotNull { card -> prices[card.id]?.price?.let { card.id to it } }.toMap()
    val sets = listSets(listCards, catalogSets)
    // A set whose last card left the list no longer filters anything
    val activeSet = setName?.takeIf { name -> listCards.any { it.setName == name } }
    val inScope = if (activeSet == null) listCards else listCards.filter { it.setName == activeSet }
    return CardListState(
        cards = inScope.sortedWith(cardOrder(sort, knownPrices, sets)),
        totalCount = listCards.size,
        sort = sort,
        setName = activeSet,
        sets = sets,
        prices = knownPrices,
        value = inScope.sumOf { knownPrices[it.id] ?: 0.0 },
        setCompletion = activeSet?.let { name ->
            catalogSets.firstOrNull { it.name == name }?.let { set ->
                SetCompletion(name, owned = ownedCards.count { it.setName == name }, total = set.cardCount)
            }
        }
    )
}

/** Every order ends with newest-added first, so ties and unpriced cards stay predictable. */
private fun cardOrder(sort: CardSort, prices: Map<String, Double>, sets: List<CardSetWithCount>): Comparator<TrackedCard> {
    val newest = compareByDescending<TrackedCard> { it.addedAt }
    return when (sort) {
        CardSort.NEWEST -> newest
        // Cards without a price go last in both directions
        CardSort.PRICE_HIGH -> compareBy<TrackedCard> { prices[it.id] == null }
            .thenByDescending { prices[it.id] ?: 0.0 }.then(newest)
        CardSort.PRICE_LOW -> compareBy<TrackedCard> { prices[it.id] == null }
            .thenBy { prices[it.id] ?: 0.0 }.then(newest)
        CardSort.NAME -> compareBy<TrackedCard, String>(String.CASE_INSENSITIVE_ORDER) { it.name }
            .thenBy { it.setName }.then(newest)
        CardSort.SET -> {
            // `sets` is already newest set first
            val setRank = sets.withIndex().associate { (index, set) -> set.name to index }
            compareBy<TrackedCard> { setRank[it.setName] ?: Int.MAX_VALUE }
                .thenBy(CardNumberOrder) { it.number }.then(newest)
        }
    }
}

private fun listSets(cards: List<TrackedCard>, catalogSets: List<CardSetWithCount>): List<CardSetWithCount> {
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

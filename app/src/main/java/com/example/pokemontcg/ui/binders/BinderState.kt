package com.example.pokemontcg.ui.binders

import com.example.pokemontcg.data.database.BinderEntity
import com.example.pokemontcg.data.database.BinderPocketCard
import com.example.pokemontcg.data.database.CardPriceEntity
import com.example.pokemontcg.data.database.CardSetWithCount
import com.example.pokemontcg.data.model.CardNumberOrder
import com.example.pokemontcg.data.model.POCKETS_PER_PAGE
import com.example.pokemontcg.data.model.TrackedCard
import com.example.pokemontcg.data.model.binderPageCount
import com.example.pokemontcg.data.repository.toTracked

/** A binder on the Binders screen: its cover shows the first page. */
data class BinderSummary(
    val id: Long,
    val name: String,
    val cardCount: Int,
    /** Total Cardmarket price (EUR) of the cards that have one. */
    val value: Double,
    /** Image of the card in each of the first page's pockets, null for an empty pocket. */
    val firstPage: List<String?>
)

/** One binder's pages. */
data class BinderState(
    val id: Long,
    val name: String,
    /** The cards by pocket number; missing pockets are empty. */
    val pockets: Map<Int, TrackedCard>,
    /** Pages up to the last filled pocket, at least one. */
    val pageCount: Int,
    val value: Double
) {
    val cardCount: Int get() = pockets.size
}

fun buildBinderSummaries(
    binders: List<BinderEntity>,
    cards: List<BinderPocketCard>,
    prices: Map<String, CardPriceEntity>
): List<BinderSummary> {
    val cardsByBinder = cards.groupBy { it.binderId }
    return binders.map { binder ->
        val binderCards = cardsByBinder[binder.id].orEmpty()
        val firstPage = binderCards.filter { it.pocket < POCKETS_PER_PAGE }.associate { it.pocket to it.card.imageUrl }
        BinderSummary(
            id = binder.id,
            name = binder.name,
            cardCount = binderCards.size,
            value = binderCards.sumOf { prices[it.card.id]?.price ?: 0.0 },
            firstPage = List(POCKETS_PER_PAGE) { pocket -> firstPage[pocket] }
        )
    }
}

fun buildBinderState(binder: BinderEntity, cards: List<BinderPocketCard>, prices: Map<String, CardPriceEntity>): BinderState {
    val pockets = cards.associate { it.pocket to it.toTracked() }
    return BinderState(
        id = binder.id,
        name = binder.name,
        pockets = pockets,
        pageCount = binderPageCount(pockets.keys.maxOrNull()),
        value = cards.sumOf { prices[it.card.id]?.price ?: 0.0 }
    )
}

/** Card ids by set, oldest set first (sets the catalog doesn't know go last), then by card number. */
fun setAndNumberOrder(cards: Collection<TrackedCard>, catalogSets: List<CardSetWithCount>): List<String> {
    val releaseDates = catalogSets.associate { it.name to it.releaseDate }
    return cards.sortedWith(
        compareBy<TrackedCard> { releaseDates[it.setName] == null }
            .thenBy { releaseDates[it.setName].orEmpty() }
            .thenBy { it.setName }
            .thenBy(CardNumberOrder) { it.number }
    ).map { it.id }
}

/** Every word of [query] must appear in the card's name, set name or number. */
fun matchesQuery(card: TrackedCard, query: String): Boolean {
    val words = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val haystack = "${card.name} ${card.setName} ${card.number}".lowercase()
    return words.all { it in haystack }
}

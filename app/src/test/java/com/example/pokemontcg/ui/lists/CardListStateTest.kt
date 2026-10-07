package com.example.pokemontcg.ui.lists

import com.example.pokemontcg.data.database.CardPriceEntity
import com.example.pokemontcg.data.database.CardSetWithCount
import com.example.pokemontcg.data.model.CardSort
import com.example.pokemontcg.data.model.TrackedCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CardListStateTest {

    private fun card(id: String, name: String, setName: String, number: String, added: Long = 0, owned: Boolean = false) =
        TrackedCard(id, name, setName, number, "", "", addedAt = added, owned = owned)

    private fun price(id: String, value: Double?) = CardPriceEntity(id, null, value, null, null, null, null, 0)

    private val catalogSets = listOf(
        CardSetWithCount("sv3pt5", "151", "Scarlet & Violet", "2023/09/22", "sym151", "MEW", 207),
        CardSetWithCount("sv3", "Obsidian Flames", "Scarlet & Violet", "2023/08/11", null, "OBF", 230),
        CardSetWithCount("base1", "Base", "Base", "1999/01/09", null, "BS", 102)
    )

    private val chase = listOf(
        card("base1-4", "Charizard", "Base", "4", added = 1),
        card("sv3pt5-199", "Charizard ex", "151", "199", added = 3),
        card("sv3pt5-25", "pikachu", "151", "25", added = 2),
        card("sv3-125", "Arcanine", "Obsidian Flames", "125", added = 4)
    )

    private val collection = listOf(
        card("sv3pt5-1", "Bulbasaur", "151", "1", owned = true),
        card("sv3pt5-4", "Charmander", "151", "4", owned = true),
        card("base1-58", "Pikachu", "Base", "58", owned = true)
    )

    private val prices = mapOf(
        "base1-4" to price("base1-4", 500.0),
        "sv3pt5-199" to price("sv3pt5-199", 395.75),
        "sv3-125" to price("sv3-125", 3.5),
        "sv3pt5-25" to price("sv3pt5-25", null) // no price on Cardmarket
    )

    private fun order(sort: CardSort) =
        buildCardListState(chase, sort, catalogSets = catalogSets, prices = prices).cards.map { it.id }

    @Test
    fun setFilterNarrowsCardsAndValueButNotTheListSize() {
        val state = buildCardListState(chase, setName = "151", catalogSets = catalogSets, prices = prices)
        assertEquals(setOf("sv3pt5-199", "sv3pt5-25"), state.cards.map { it.id }.toSet())
        assertEquals(395.75, state.value, 0.0)
        assertEquals(4, state.totalCount)
        assertEquals("151", state.setName)
    }

    @Test
    fun setCompletionCountsOwnedCardsAgainstTheCatalogSet() {
        val state = buildCardListState(chase, setName = "151", catalogSets = catalogSets, ownedCards = collection)
        assertEquals(SetCompletion("151", owned = 2, total = 207), state.setCompletion)
        assertNull(buildCardListState(chase, catalogSets = catalogSets, ownedCards = collection).setCompletion)
    }

    @Test
    fun setNoLongerInTheListIsIgnored() {
        val state = buildCardListState(chase, setName = "Evolving Skies", catalogSets = catalogSets)
        assertNull(state.setName)
        assertEquals(4, state.cards.size)
    }

    @Test
    fun setsInTheListAreNewestFirstWithListCounts() {
        val sets = buildCardListState(chase, catalogSets = catalogSets).sets
        assertEquals(listOf("151", "Obsidian Flames", "Base"), sets.map { it.name })
        assertEquals(listOf(2, 1, 1), sets.map { it.cardCount })
        assertEquals("sym151", sets.first().symbolUrl)
    }

    @Test
    fun valueSumsKnownPricesAndListsThem() {
        val state = buildCardListState(chase, prices = prices)
        assertEquals(899.25, state.value, 0.001)
        assertEquals(setOf("base1-4", "sv3pt5-199", "sv3-125"), state.prices.keys)
    }

    @Test
    fun newestAddedFirstByDefault() {
        assertEquals(listOf("sv3-125", "sv3pt5-199", "sv3pt5-25", "base1-4"), order(CardSort.NEWEST))
    }

    @Test
    fun priceSortsPutCardsWithoutAPriceLast() {
        assertEquals(listOf("base1-4", "sv3pt5-199", "sv3-125", "sv3pt5-25"), order(CardSort.PRICE_HIGH))
        assertEquals(listOf("sv3-125", "sv3pt5-199", "base1-4", "sv3pt5-25"), order(CardSort.PRICE_LOW))
    }

    @Test
    fun nameSortIgnoresCase() {
        assertEquals(listOf("sv3-125", "base1-4", "sv3pt5-199", "sv3pt5-25"), order(CardSort.NAME))
    }

    @Test
    fun setSortIsNewestSetFirstThenCardNumber() {
        assertEquals(listOf("sv3pt5-25", "sv3pt5-199", "sv3-125", "base1-4"), order(CardSort.SET))
    }

    @Test
    fun completionFractionIsClamped() {
        assertEquals(1f, SetCompletion("x", owned = 5, total = 3).fraction)
        assertEquals(0f, SetCompletion("x", owned = 0, total = 0).fraction)
    }
}

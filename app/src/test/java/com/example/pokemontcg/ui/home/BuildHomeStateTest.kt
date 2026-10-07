package com.example.pokemontcg.ui.home

import com.example.pokemontcg.data.database.CardPriceEntity
import com.example.pokemontcg.data.database.CardSetWithCount
import com.example.pokemontcg.data.model.ChaseSort
import com.example.pokemontcg.data.model.TrackedCard
import org.junit.Assert.assertEquals
import org.junit.Test

class BuildHomeStateTest {

    private fun card(id: String, obtained: Boolean, setName: String = "Set") = TrackedCard(
        id = id, name = id, setName = setName, number = "1",
        imageUrl = "", largeImageUrl = "", addedAt = 0, owned = obtained
    )

    private val cards = listOf(card("a", obtained = true), card("b", obtained = false), card("c", obtained = false))

    @Test
    fun allFilterKeepsEveryCardInOrder() {
        assertEquals(listOf("a", "b", "c"), buildHomeState(cards, ChaseFilter.ALL).cards.map { it.id })
    }

    @Test
    fun chasingFilterShowsOnlyCardsNotObtained() {
        assertEquals(listOf("b", "c"), buildHomeState(cards, ChaseFilter.CHASING).cards.map { it.id })
    }

    @Test
    fun obtainedFilterShowsOnlyObtainedCards() {
        assertEquals(listOf("a"), buildHomeState(cards, ChaseFilter.OBTAINED).cards.map { it.id })
    }

    @Test
    fun countsCoverWholeListRegardlessOfFilter() {
        val state = buildHomeState(cards, ChaseFilter.OBTAINED)
        assertEquals(1, state.obtainedCount)
        assertEquals(3, state.totalCount)
    }

    private val mixed = listOf(
        card("a", obtained = true, setName = "Base"),
        card("b", obtained = false, setName = "Base"),
        card("c", obtained = true, setName = "151"),
        card("d", obtained = false, setName = "Obsidian Flames")
    )

    private val catalogSets = listOf(
        CardSetWithCount("sv3pt5", "151", "Scarlet & Violet", "2023/09/22", "sym151", "MEW", 207),
        CardSetWithCount("sv3", "Obsidian Flames", "Scarlet & Violet", "2023/08/11", null, "OBF", 230),
        CardSetWithCount("base1", "Base", "Base", "1999/01/09", null, "BS", 102)
    )

    @Test
    fun setFilterNarrowsCardsAndCounts() {
        val state = buildHomeState(mixed, ChaseFilter.ALL, setName = "Base")
        assertEquals(listOf("a", "b"), state.cards.map { it.id })
        assertEquals(1, state.obtainedCount)
        assertEquals(2, state.totalCount)
        assertEquals("Base", state.setName)
    }

    @Test
    fun setAndCaughtFiltersCombine() {
        assertEquals(listOf("b"), buildHomeState(mixed, ChaseFilter.CHASING, setName = "Base").cards.map { it.id })
    }

    @Test
    fun setNoLongerOnTheListIsIgnored() {
        val state = buildHomeState(mixed, ChaseFilter.ALL, setName = "Evolving Skies")
        assertEquals(null, state.setName)
        assertEquals(4, state.totalCount)
    }

    @Test
    fun setsOnTheListAreNewestFirstWithYourCardCounts() {
        val sets = buildHomeState(mixed, ChaseFilter.ALL, catalogSets = catalogSets).sets
        assertEquals(listOf("151", "Obsidian Flames", "Base"), sets.map { it.name })
        assertEquals(listOf(1, 1, 2), sets.map { it.cardCount })
        assertEquals("sym151", sets.first().symbolUrl)
    }

    @Test
    fun valueToChaseSumsKnownPricesOfCardsNotCaughtYet() {
        val prices = mapOf(
            "a" to price("a", 10.0), // caught: not counted
            "b" to price("b", 2.5),
            "d" to price("d", null) // no price on Cardmarket
        )
        val state = buildHomeState(mixed, ChaseFilter.ALL, prices = prices)
        assertEquals(2.5, state.valueToChase, 0.0)
        assertEquals(mapOf("a" to 10.0, "b" to 2.5), state.prices)
        assertEquals(2.5, buildHomeState(mixed, ChaseFilter.ALL, setName = "Base", prices = prices).valueToChase, 0.0)
        assertEquals(0.0, buildHomeState(mixed, ChaseFilter.ALL, setName = "151", prices = prices).valueToChase, 0.0)
    }

    private fun price(id: String, value: Double?) = CardPriceEntity(id, null, value, null, null, null, null, 0)

    private fun sortCard(id: String, name: String, setName: String, number: String, added: Long) =
        TrackedCard(id, name, setName, number, "", "", addedAt = added, owned = false)

    private val unsorted = listOf(
        sortCard("base1-4", "Charizard", "Base", "4", added = 1),
        sortCard("sv3pt5-199", "Charizard ex", "151", "199", added = 3),
        sortCard("sv3pt5-25", "pikachu", "151", "25", added = 2),
        sortCard("sv3-125", "Arcanine", "Obsidian Flames", "125", added = 4)
    )

    private val sortPrices = mapOf(
        "base1-4" to price("base1-4", 500.0),
        "sv3pt5-199" to price("sv3pt5-199", 395.75),
        "sv3-125" to price("sv3-125", 3.5)
        // pikachu has no price
    )

    private fun order(sort: ChaseSort) =
        buildHomeState(unsorted, ChaseFilter.ALL, catalogSets = catalogSets, prices = sortPrices, sort = sort).cards.map { it.id }

    @Test
    fun newestAddedFirstByDefault() {
        assertEquals(listOf("sv3-125", "sv3pt5-199", "sv3pt5-25", "base1-4"), order(ChaseSort.NEWEST))
        assertEquals(ChaseSort.NEWEST, buildHomeState(unsorted, ChaseFilter.ALL).sort)
    }

    @Test
    fun priceSortsPutCardsWithoutAPriceLast() {
        assertEquals(listOf("base1-4", "sv3pt5-199", "sv3-125", "sv3pt5-25"), order(ChaseSort.PRICE_HIGH))
        assertEquals(listOf("sv3-125", "sv3pt5-199", "base1-4", "sv3pt5-25"), order(ChaseSort.PRICE_LOW))
    }

    @Test
    fun nameSortIgnoresCase() {
        assertEquals(listOf("sv3-125", "base1-4", "sv3pt5-199", "sv3pt5-25"), order(ChaseSort.NAME))
    }

    @Test
    fun setSortIsNewestSetFirstThenCardNumber() {
        assertEquals(listOf("sv3pt5-25", "sv3pt5-199", "sv3-125", "base1-4"), order(ChaseSort.SET))
    }

    @Test
    fun sortingAppliesAfterFiltering() {
        val state = buildHomeState(unsorted, ChaseFilter.ALL, setName = "151", prices = sortPrices, sort = ChaseSort.PRICE_HIGH)
        assertEquals(listOf("sv3pt5-199", "sv3pt5-25"), state.cards.map { it.id })
    }
}

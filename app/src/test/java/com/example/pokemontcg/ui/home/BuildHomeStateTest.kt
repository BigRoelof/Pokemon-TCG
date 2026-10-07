package com.example.pokemontcg.ui.home

import com.example.pokemontcg.data.database.CardSetWithCount
import com.example.pokemontcg.data.database.ChaseCardEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class BuildHomeStateTest {

    private fun card(id: String, obtained: Boolean, setName: String = "Set") = ChaseCardEntity(
        id = id, name = id, setName = setName, number = "1",
        imageUrl = "", largeImageUrl = "", obtained = obtained, dateAdded = 0
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
}

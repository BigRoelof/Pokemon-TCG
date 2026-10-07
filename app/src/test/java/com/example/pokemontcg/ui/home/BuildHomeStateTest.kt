package com.example.pokemontcg.ui.home

import com.example.pokemontcg.data.database.ChaseCardEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class BuildHomeStateTest {

    private fun card(id: String, obtained: Boolean) = ChaseCardEntity(
        id = id, name = id, setName = "Set", number = "1",
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
}

package com.example.pokemontcg.ui.binders

import com.example.pokemontcg.data.database.BinderEntity
import com.example.pokemontcg.data.database.BinderPocketCard
import com.example.pokemontcg.data.database.CardPriceEntity
import com.example.pokemontcg.data.database.CardSetWithCount
import com.example.pokemontcg.data.database.CollectionCardEntity
import com.example.pokemontcg.data.model.TrackedCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BinderStateTest {

    private fun pocketCard(binderId: Long, pocket: Int, id: String, setName: String = "Obsidian Flames", number: String = "1") =
        BinderPocketCard(binderId, pocket, CollectionCardEntity(id, "Card $id", setName, number, "img-$id", "large-$id", addedAt = 0))

    private fun price(cardId: String, price: Double?) =
        CardPriceEntity(cardId, tcgdexId = null, price = price, average30 = null, low = null, cardmarketProductId = null, sourceUpdated = null, fetchedAt = 0)

    private fun tracked(id: String, setName: String, number: String) =
        TrackedCard(id, id, setName, number, "", "", addedAt = 0, owned = true)

    private fun set(name: String, releaseDate: String?) =
        CardSetWithCount(id = name, name = name, series = null, releaseDate = releaseDate, symbolUrl = null, ptcgoCode = null, cardCount = 0)

    @Test
    fun summariesShowTheFirstPageCountAndValue() {
        val binders = listOf(BinderEntity(1, "Fire", 0), BinderEntity(2, "Empty", 0))
        val cards = listOf(pocketCard(1, 0, "a"), pocketCard(1, 4, "b"), pocketCard(1, 9, "c"))
        val summaries = buildBinderSummaries(binders, cards, mapOf("a" to price("a", 2.5), "c" to price("c", 1.0), "b" to price("b", null)))

        val fire = summaries[0]
        assertEquals(3, fire.cardCount)
        assertEquals(3.5, fire.value, 0.001)
        assertEquals(9, fire.firstPage.size)
        assertEquals("img-a", fire.firstPage[0])
        assertEquals("img-b", fire.firstPage[4])
        assertNull(fire.firstPage[1])

        val empty = summaries[1]
        assertEquals(0, empty.cardCount)
        assertTrue(empty.firstPage.all { it == null })
    }

    @Test
    fun binderStateKeepsGapsAndCountsPages() {
        val state = buildBinderState(BinderEntity(1, "Fire", 0), listOf(pocketCard(1, 2, "a"), pocketCard(1, 10, "b")), emptyMap())
        assertEquals(setOf(2, 10), state.pockets.keys)
        assertEquals(2, state.cardCount)
        assertEquals(2, state.pageCount)
    }

    @Test
    fun setOrderIsOldestSetFirstThenNumber() {
        val cards = listOf(
            tracked("new-2", "Newer", "2"),
            tracked("old-10", "Older", "10"),
            tracked("unknown-1", "Unknown", "1"),
            tracked("old-2", "Older", "2"),
            tracked("old-TG1", "Older", "TG1")
        )
        val sets = listOf(set("Newer", "2024/01/01"), set("Older", "2020/01/01"))
        assertEquals(listOf("old-2", "old-10", "old-TG1", "new-2", "unknown-1"), setAndNumberOrder(cards, sets))
    }

    @Test
    fun queryMatchesNameSetAndNumberWords() {
        val card = TrackedCard("sv3-125", "Charizard ex", "Obsidian Flames", "125", "", "", 0, true)
        assertTrue(matchesQuery(card, ""))
        assertTrue(matchesQuery(card, "chari 125"))
        assertTrue(matchesQuery(card, "  obsidian  charizard "))
        assertFalse(matchesQuery(card, "pikachu"))
    }
}

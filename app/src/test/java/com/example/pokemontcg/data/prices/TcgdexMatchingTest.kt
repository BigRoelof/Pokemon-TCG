package com.example.pokemontcg.data.prices

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TcgdexMatchingTest {

    private val sets = listOf(
        TcgdexSetJson("sv03", "Obsidian Flames"),
        TcgdexSetJson("swsh12.5gg", "Crown Zenith Galarian Gallery"),
        TcgdexSetJson("hgss1", "HeartGold SoulSilver"),
        TcgdexSetJson("sm115", "Hidden Fates"),
        TcgdexSetJson("sma", "Hidden Fates Shiny Vault")
    )

    @Test
    fun setsMatchByNameIgnoringCaseAndPunctuation() {
        assertEquals("sv03", TcgdexMatching.findSetId("sv3", "Obsidian Flames", "125", sets))
        assertEquals("swsh12.5gg", TcgdexMatching.findSetId("swsh12pt5gg", "Crown Zenith Galarian Gallery", "GG55", sets))
    }

    @Test
    fun setsFallBackToTheSameId() {
        // "HeartGold & SoulSilver" vs "HeartGold SoulSilver"
        assertEquals("hgss1", TcgdexMatching.findSetId("hgss1", "HeartGold & SoulSilver", "1", sets))
    }

    @Test
    fun aliasesCoverRenamedSetsAndSubSets() {
        assertEquals("30th-c", TcgdexMatching.findSetId("me55c", "30th Celebration: Classic Collection", "1", sets))
        assertEquals("sma", TcgdexMatching.findSetId("sm115", "Hidden Fates", "SV49", sets))
        assertEquals("sm115", TcgdexMatching.findSetId("sm115", "Hidden Fates", "49", sets))
    }

    @Test
    fun unknownSetHasNoMatch() {
        assertNull(TcgdexMatching.findSetId("xx1", "Nonexistent", "1", sets))
    }

    @Test
    fun cardsMatchIgnoringZeroPaddingAndCase() {
        val cards = listOf(TcgdexCardBriefJson("sv03-001", "001"), TcgdexCardBriefJson("sv03-125", "125"), TcgdexCardBriefJson("x-GG01", "GG01"))
        assertEquals("sv03-001", TcgdexMatching.findCardId("1", cards))
        assertEquals("sv03-125", TcgdexMatching.findCardId("125", cards))
        assertEquals("x-GG01", TcgdexMatching.findCardId("gg01", cards))
        assertNull(TcgdexMatching.findCardId("999", cards))
    }

    @Test
    fun priceFallsBackFromTrendAndIgnoresZero() {
        assertEquals(3.51, TcgdexMatching.pickPrice(CardmarketPriceJson(null, 1, 3.88, 1.99, 3.51, 3.66))!!, 0.0)
        assertEquals(3.66, TcgdexMatching.pickPrice(CardmarketPriceJson(null, 1, 3.88, 1.99, 0.0, 3.66))!!, 0.0)
        assertNull(TcgdexMatching.pickPrice(CardmarketPriceJson(null, 1, null, null, 0.0, null)))
        assertNull(TcgdexMatching.pickPrice(null))
    }
}

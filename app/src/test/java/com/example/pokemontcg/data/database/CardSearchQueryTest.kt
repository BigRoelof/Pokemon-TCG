package com.example.pokemontcg.data.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CardSearchQueryTest {

    @Test
    fun blankInputHasNoQuery() {
        assertNull(CardSearchQuery.build("   "))
    }

    @Test
    fun everyWordMustAppearInTheName() {
        val sql = CardSearchQuery.build("Charizard  EX")!!
        assertEquals(2, Regex("c\\.name LIKE \\?").findAll(sql.sql.substringBefore("ORDER BY")).count())
        assertEquals(listOf("%charizard%", "%ex%"), sql.args.take(2))
    }

    @Test
    fun rankingArgsUseTheWholePhrase() {
        val sql = CardSearchQuery.build(" mr. mime ")!!
        assertEquals(listOf("%mr.%", "%mime%", "mr. mime", "mr. mime%"), sql.args)
    }

    @Test
    fun likeWildcardsInInputAreEscaped() {
        val sql = CardSearchQuery.build("100%_\\")!!
        assertEquals("%100\\%\\_\\\\%", sql.args.first())
    }

    @Test
    fun resultsAreLimited() {
        assertTrue(CardSearchQuery.build("pikachu")!!.sql.endsWith("LIMIT ${CardSearchQuery.MAX_RESULTS}"))
    }
}

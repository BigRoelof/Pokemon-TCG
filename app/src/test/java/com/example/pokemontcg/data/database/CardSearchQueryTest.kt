package com.example.pokemontcg.data.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CardSearchQueryTest {

    @Test
    fun blankInputWithoutSetHasNoQuery() {
        assertNull(CardSearchQuery.build("   "))
    }

    @Test
    fun eachWordMatchesNameSetNameSetCodeOrNumber() {
        val sql = CardSearchQuery.build("Charizard  OBF")!!
        assertEquals(
            listOf("%charizard%", "% charizard%", "charizard", "charizard", "%obf%", "% obf%", "obf", "obf"),
            sql.args.take(8)
        )
    }

    @Test
    fun rankingArgsFollowTheConditions() {
        val sql = CardSearchQuery.build(" mr. mime ")!!
        // 2 words x 4 condition args, then: exact phrase, every word in name (2), phrase prefix
        assertEquals(listOf("mr. mime", "%mr.%", "%mime%", "mr. mime%"), sql.args.drop(8))
        assertEquals(sql.args.size, sql.sql.count { it == '?' })
    }

    @Test
    fun setFilterComesFirstAndAllowsAnEmptyQuery() {
        val sql = CardSearchQuery.build("", setId = "sv3")!!
        assertEquals(listOf("sv3"), sql.args)
        assertTrue(sql.sql.contains("c.setId = ?"))
        assertEquals(sql.args.size, sql.sql.count { it == '?' })
    }

    @Test
    fun likeWildcardsInInputAreEscaped() {
        val sql = CardSearchQuery.build("100%_\\")!!
        assertEquals("%100\\%\\_\\\\%", sql.args.first())
    }

    @Test
    fun searchesAreLimitedButWholeSetsAreNot() {
        assertTrue(CardSearchQuery.build("pikachu")!!.sql.endsWith("LIMIT ${CardSearchQuery.MAX_RESULTS}"))
        assertTrue(CardSearchQuery.build("", setId = "sv3")!!.sql.endsWith("LIMIT 1000"))
    }
}

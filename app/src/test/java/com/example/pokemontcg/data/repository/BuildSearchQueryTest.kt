package com.example.pokemontcg.data.repository

import com.example.pokemontcg.data.repository.PokemonRepository.Companion.buildSearchQuery
import org.junit.Assert.assertEquals
import org.junit.Test

class BuildSearchQueryTest {

    @Test
    fun singleWordBecomesQuotedPrefixMatch() {
        assertEquals("name:\"pikachu*\"", buildSearchQuery("pikachu"))
    }

    @Test
    fun multipleWordsStayInOnePhrase() {
        assertEquals("name:\"charizard ex*\"", buildSearchQuery("charizard ex"))
    }

    @Test
    fun surroundingWhitespaceIsTrimmed() {
        assertEquals("name:\"mr. mime*\"", buildSearchQuery("  mr. mime "))
    }

    @Test
    fun quotesAndBackslashesAreEscaped() {
        assertEquals("name:\"a\\\"b\\\\c*\"", buildSearchQuery("a\"b\\c"))
    }

    @Test
    fun rawApiQueryIsPassedThrough() {
        assertEquals("set.id:sv1 rarity:rare", buildSearchQuery(" set.id:sv1 rarity:rare "))
    }
}

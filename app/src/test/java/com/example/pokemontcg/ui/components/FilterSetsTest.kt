package com.example.pokemontcg.ui.components

import com.example.pokemontcg.data.database.CardSetWithCount
import org.junit.Assert.assertEquals
import org.junit.Test

class FilterSetsTest {

    private fun set(id: String, name: String, series: String, code: String?) =
        CardSetWithCount(id, name, series, null, null, code, 10)

    private val sets = listOf(
        set("sv3", "Obsidian Flames", "Scarlet & Violet", "OBF"),
        set("sv3pt5", "151", "Scarlet & Violet", "MEW"),
        set("base1", "Base", "Base", "BS"),
        set("bw10", "Plasma Blast", "Black & White", "PLB")
    )

    private fun ids(filter: String) = filterSets(sets, filter).map { it.id }

    @Test
    fun matchesTheStartOfWordsInNameOrSeries() {
        assertEquals(listOf("sv3"), ids("flam"))
        assertEquals(listOf("sv3", "sv3pt5"), ids("scarlet"))
        assertEquals(listOf("sv3pt5"), ids("151"))
    }

    @Test
    fun doesNotMatchInsideWords() {
        // "ase" is inside "Base" and "Blast" but starts no word
        assertEquals(emptyList<String>(), ids("ase"))
    }

    @Test
    fun matchesSetCodesExactly() {
        assertEquals(listOf("sv3"), ids("obf"))
        assertEquals(listOf("sv3pt5"), ids("MEW"))
    }

    @Test
    fun blankFilterKeepsAllSets() {
        assertEquals(4, filterSets(sets, "  ").size)
    }
}

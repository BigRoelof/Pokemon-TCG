package com.example.pokemontcg.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BinderPocketsTest {

    @Test
    fun freePocketsSkipsFilledOnesFromTheStart() {
        assertEquals(listOf(1, 3, 4), freePockets(occupied = setOf(0, 2), from = 0, count = 3))
    }

    @Test
    fun freePocketsStartsAtTheTappedPocket() {
        assertEquals(listOf(5, 7), freePockets(occupied = setOf(1, 6), from = 5, count = 2))
    }

    @Test
    fun freePocketsWithNothingToPlace() {
        assertEquals(emptyList<Int>(), freePockets(occupied = setOf(0), from = 0, count = 0))
    }

    @Test
    fun pageCountCoversTheLastFilledPocket() {
        assertEquals(1, binderPageCount(null))
        assertEquals(1, binderPageCount(0))
        assertEquals(1, binderPageCount(8))
        assertEquals(2, binderPageCount(9))
        assertEquals(3, binderPageCount(20))
    }
}

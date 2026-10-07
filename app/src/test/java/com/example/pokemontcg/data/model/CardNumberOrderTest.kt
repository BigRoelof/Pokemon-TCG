package com.example.pokemontcg.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CardNumberOrderTest {

    @Test
    fun numbersNumericallyThenOthersAlphabetically() {
        val numbers = listOf("GG55", "10", "TG05", "2", "125", "gg01", "1")
        assertEquals(listOf("1", "2", "10", "125", "gg01", "GG55", "TG05"), numbers.sortedWith(CardNumberOrder))
    }
}

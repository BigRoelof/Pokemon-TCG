package com.example.pokemontcg.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class PriceFormatTest {

    // Some JDKs use a non-breaking space between symbol and amount; compare without it
    private fun plain(text: String) = text.replace('\u00A0', ' ').replace('\u202F', ' ')

    @Test
    fun usesTheLocalesMoneyFormatInEuros() {
        assertEquals("€ 3,51", plain(formatEuro(3.51, Locale.forLanguageTag("nl-NL"))))
        assertEquals("€2,026.85", plain(formatEuro(2026.85, Locale.US)))
    }
}

package com.example.pokemontcg.data.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CardDataCleanupTest {

    @Test
    fun raritySpellingsAreUnified() {
        assertEquals("Rare Holo V", CardDataCleanup.rarity("Holo Rare V"))
        assertEquals("Rare Holo VMAX", CardDataCleanup.rarity("Holo Rare VMAX"))
        assertEquals("Rare Holo EX", CardDataCleanup.rarity("Rare Holo ex"))
        assertEquals("Mega Attack Rare", CardDataCleanup.rarity("MEGA_ATTACK_RARE"))
    }

    @Test
    fun ordinaryRaritiesAreKept() {
        assertEquals("Special Illustration Rare", CardDataCleanup.rarity("Special Illustration Rare"))
        assertEquals("ACE SPEC Rare", CardDataCleanup.rarity("ACE SPEC Rare"))
        assertNull(CardDataCleanup.rarity("  "))
        assertNull(CardDataCleanup.rarity(null))
    }

    @Test
    fun typesAreDelimitedOnBothSides() {
        assertEquals(",Fire,", CardDataCleanup.types(listOf("Fire")))
        assertEquals(",Water,Psychic,", CardDataCleanup.types(listOf("Water", "Psychic")))
        assertNull(CardDataCleanup.types(emptyList()))
        assertNull(CardDataCleanup.types(null))
    }
}

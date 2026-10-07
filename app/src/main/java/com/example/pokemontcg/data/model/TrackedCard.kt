package com.example.pokemontcg.data.model

/** A card on the chase list ([owned] = false) or in the collection ([owned] = true). */
data class TrackedCard(
    val id: String,
    val name: String,
    val setName: String,
    val number: String,
    val imageUrl: String,
    val largeImageUrl: String,
    /** When it was added to its current list. */
    val addedAt: Long,
    val owned: Boolean
)

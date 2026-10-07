package com.example.pokemontcg.data.model

/** A card as the UI shows it, from the local catalog or from the chase list. */
data class Card(
    val id: String,
    val name: String,
    val number: String,
    val setName: String,
    val imageSmall: String?,
    val imageLarge: String?,
    /** Catalog set id; null for a saved card the catalog doesn't know. */
    val setId: String? = null,
    val setSeries: String? = null,
    val releaseDate: String? = null,
    val rarity: String? = null,
    val artist: String? = null,
    /** The card's Pokémon type, or "Trainer"/"Energy" for other cards. */
    val typeLabel: String? = null
)

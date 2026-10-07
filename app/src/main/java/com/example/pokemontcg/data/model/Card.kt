package com.example.pokemontcg.data.model

/** A card as the UI shows it, from the local catalog or from the chase list. */
data class Card(
    val id: String,
    val name: String,
    val number: String,
    val setName: String,
    val imageSmall: String?,
    val imageLarge: String?,
    val setSeries: String? = null,
    val releaseDate: String? = null,
    val rarity: String? = null,
    val artist: String? = null
)

package com.example.pokemontcg.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A card the user wants but doesn't own yet. A snapshot, so the list works without the catalog. */
@Entity(tableName = "chase_cards")
data class ChaseCardEntity(
    @PrimaryKey val id: String,
    val name: String,
    val setName: String,
    val number: String,
    val imageUrl: String,
    val largeImageUrl: String,
    val dateAdded: Long
)

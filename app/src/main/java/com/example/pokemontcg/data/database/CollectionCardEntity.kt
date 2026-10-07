package com.example.pokemontcg.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A card the user owns. Same snapshot fields as [ChaseCardEntity]; a card is in at most one of the two. */
@Entity(tableName = "collection_cards")
data class CollectionCardEntity(
    @PrimaryKey val id: String,
    val name: String,
    val setName: String,
    val number: String,
    val imageUrl: String,
    val largeImageUrl: String,
    /** When the card entered the collection (caught or added directly). */
    val addedAt: Long
)

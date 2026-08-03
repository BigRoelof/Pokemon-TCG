package com.example.pokemontcg.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chase_cards")
data class ChaseCardEntity(
    @PrimaryKey val id: String,
    val name: String,
    val setName: String,
    val number: String,
    val imageUrl: String,
    val largeImageUrl: String,
    val obtained: Boolean,
    val dateAdded: Long
)

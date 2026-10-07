package com.example.pokemontcg.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Every known card, synced from the open-source dataset by `CatalogSync`. */
@Entity(tableName = "catalog_cards", indices = [Index("setId")])
data class CatalogCardEntity(
    @PrimaryKey val id: String,
    val setId: String,
    val name: String,
    val number: String,
    val rarity: String?,
    val artist: String?,
    val imageSmall: String?,
    val imageLarge: String?,
    /** "Pokémon", "Trainer" or "Energy". */
    val supertype: String? = null,
    /** Pokémon types delimited on both sides, e.g. ",Fire,"; see `CardDataCleanup.types`. */
    val types: String? = null
)

@Entity(tableName = "card_sets")
data class CardSetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val series: String?,
    /** `yyyy/MM/dd`, so it sorts as text. */
    val releaseDate: String?,
    val total: Int?,
    val logoUrl: String?,
    val symbolUrl: String?,
    /** Collector code such as `OBF`; missing for some older and promo sets. */
    val ptcgoCode: String? = null
)

/** Content hash of each downloaded dataset file, so unchanged files aren't downloaded again. */
@Entity(tableName = "catalog_files")
data class CatalogFileEntity(
    @PrimaryKey val path: String,
    val sha: String
)

/** A catalog card joined with its set. */
data class CatalogCardWithSet(
    val id: String,
    val name: String,
    val number: String,
    val rarity: String?,
    val artist: String?,
    val imageSmall: String?,
    val imageLarge: String?,
    val setId: String,
    val setName: String?,
    val setSeries: String?,
    val releaseDate: String?,
    val supertype: String?,
    val types: String?
)

/** A value with how many catalog cards have it, e.g. a rarity for the rarity filter. */
data class NameCount(val name: String, val count: Int)

/** A set that has cards in the catalog, for the set picker. */
data class CardSetWithCount(
    val id: String,
    val name: String,
    val series: String?,
    val releaseDate: String?,
    val symbolUrl: String?,
    val ptcgoCode: String?,
    val cardCount: Int
)

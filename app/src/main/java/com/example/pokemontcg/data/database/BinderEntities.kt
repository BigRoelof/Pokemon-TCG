package com.example.pokemontcg.data.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A binder the user made, filled by hand with cards from the collection. */
@Entity(tableName = "binders")
data class BinderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long
)

/**
 * A collection card in one of a binder's pockets. Pockets are numbered from 0, nine to a page,
 * and may be left empty. A card can be in several binders, but only once per binder.
 */
@Entity(
    tableName = "binder_cards",
    primaryKeys = ["binderId", "cardId"],
    foreignKeys = [
        ForeignKey(entity = BinderEntity::class, parentColumns = ["id"], childColumns = ["binderId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["binderId", "pocket"], unique = true), Index("cardId")]
)
data class BinderCardEntity(
    val binderId: Long,
    val cardId: String,
    val pocket: Int,
    val addedAt: Long
)

/** A binder card with its collection snapshot. */
data class BinderPocketCard(
    val binderId: Long,
    val pocket: Int,
    @Embedded val card: CollectionCardEntity
)

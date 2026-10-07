package com.example.pokemontcg.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cached Cardmarket price (EUR) for a card on the chase list or one the user opened.
 * A row with a null [price] means TCGdex has no price for the card; it's checked again later.
 */
@Entity(tableName = "card_prices")
data class CardPriceEntity(
    @PrimaryKey val cardId: String,
    val tcgdexId: String?,
    /** Cardmarket trend price, or the best fallback. */
    val price: Double?,
    val average30: Double?,
    val low: Double?,
    val cardmarketProductId: Int?,
    /** When Cardmarket's figures were updated, as an ISO timestamp from TCGdex. */
    val sourceUpdated: String?,
    /** When we fetched this row (epoch millis); rows older than a day get refreshed. */
    val fetchedAt: Long
)

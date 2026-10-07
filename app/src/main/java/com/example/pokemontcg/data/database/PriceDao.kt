package com.example.pokemontcg.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceDao {

    @Query("SELECT * FROM card_prices")
    fun observeAll(): Flow<List<CardPriceEntity>>

    @Query("SELECT * FROM card_prices WHERE cardId = :cardId")
    fun observe(cardId: String): Flow<CardPriceEntity?>

    @Query("SELECT * FROM card_prices WHERE cardId = :cardId")
    suspend fun get(cardId: String): CardPriceEntity?

    @Upsert
    suspend fun upsert(price: CardPriceEntity)
}

package com.example.pokemontcg.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PokemonDao {

    @Query("SELECT * FROM chase_cards ORDER BY dateAdded DESC")
    fun getAllChaseCards(): Flow<List<ChaseCardEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM chase_cards WHERE id = :cardId)")
    fun isCardInChaseList(cardId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: ChaseCardEntity)

    @Query("DELETE FROM chase_cards WHERE id = :cardId")
    suspend fun deleteCardById(cardId: String)
}

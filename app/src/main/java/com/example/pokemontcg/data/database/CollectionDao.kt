package com.example.pokemontcg.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** The collection, plus moving cards between it and the chase list in one transaction. */
@Dao
interface CollectionDao {

    @Query("SELECT * FROM collection_cards ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<CollectionCardEntity>>

    @Query("SELECT * FROM collection_cards WHERE id = :cardId")
    fun observeById(cardId: String): Flow<CollectionCardEntity?>

    @Query("SELECT * FROM collection_cards WHERE id = :cardId")
    suspend fun getById(cardId: String): CollectionCardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(card: CollectionCardEntity)

    @Query("DELETE FROM collection_cards WHERE id = :cardId")
    suspend fun delete(cardId: String)

    @Query("SELECT * FROM chase_cards WHERE id = :cardId")
    suspend fun getChaseCard(cardId: String): ChaseCardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChaseCard(card: ChaseCardEntity)

    @Query("DELETE FROM chase_cards WHERE id = :cardId")
    suspend fun deleteChaseCard(cardId: String)

    /** Catches a chase-list card: it moves to the collection. Returns false if it wasn't on the chase list. */
    @Transaction
    suspend fun moveToCollection(cardId: String, now: Long): Boolean {
        val card = getChaseCard(cardId) ?: return false
        insert(CollectionCardEntity(card.id, card.name, card.setName, card.number, card.imageUrl, card.largeImageUrl, addedAt = now))
        deleteChaseCard(cardId)
        return true
    }

    /** Undoes a catch exactly: the card leaves the collection and [original] returns to the chase list. */
    @Transaction
    suspend fun restoreChaseCard(original: ChaseCardEntity) {
        delete(original.id)
        insertChaseCard(original)
    }

    /** Puts a collection card back on the chase list. */
    @Transaction
    suspend fun moveToChaseList(cardId: String, now: Long): Boolean {
        val card = getById(cardId) ?: return false
        insertChaseCard(ChaseCardEntity(card.id, card.name, card.setName, card.number, card.imageUrl, card.largeImageUrl, dateAdded = now))
        delete(cardId)
        return true
    }
}

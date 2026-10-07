package com.example.pokemontcg.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.pokemontcg.data.model.freePockets
import kotlinx.coroutines.flow.Flow

/** Binders and the cards in their pockets. Cards leave binders when they leave the collection (see [CollectionDao]). */
@Dao
interface BinderDao {

    @Query("SELECT * FROM binders ORDER BY createdAt")
    fun observeBinders(): Flow<List<BinderEntity>>

    @Query("SELECT * FROM binders WHERE id = :binderId")
    fun observeBinder(binderId: Long): Flow<BinderEntity?>

    @Query(
        "SELECT bc.binderId, bc.pocket, c.* FROM binder_cards bc JOIN collection_cards c ON c.id = bc.cardId " +
            "ORDER BY bc.binderId, bc.pocket"
    )
    fun observeAllPocketCards(): Flow<List<BinderPocketCard>>

    @Query(
        "SELECT bc.binderId, bc.pocket, c.* FROM binder_cards bc JOIN collection_cards c ON c.id = bc.cardId " +
            "WHERE bc.binderId = :binderId ORDER BY bc.pocket"
    )
    fun observePocketCards(binderId: Long): Flow<List<BinderPocketCard>>

    @Query("SELECT binderId FROM binder_cards WHERE cardId = :cardId")
    fun observeBinderIdsFor(cardId: String): Flow<List<Long>>

    @Insert
    suspend fun insertBinder(binder: BinderEntity): Long

    @Query("UPDATE binders SET name = :name WHERE id = :binderId")
    suspend fun renameBinder(binderId: Long, name: String)

    /** Its cards go with it (foreign key cascade); they stay in the collection. */
    @Query("DELETE FROM binders WHERE id = :binderId")
    suspend fun deleteBinder(binderId: Long)

    @Query("SELECT * FROM binder_cards WHERE binderId = :binderId")
    suspend fun getCards(binderId: Long): List<BinderCardEntity>

    @Query("SELECT id FROM collection_cards WHERE id IN (:cardIds)")
    suspend fun ownedIds(cardIds: List<String>): List<String>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCards(cards: List<BinderCardEntity>)

    @Query("DELETE FROM binder_cards WHERE binderId = :binderId AND cardId = :cardId")
    suspend fun removeCard(binderId: Long, cardId: String)

    @Query("UPDATE binder_cards SET pocket = :pocket WHERE binderId = :binderId AND cardId = :cardId")
    suspend fun setPocket(binderId: Long, cardId: String, pocket: Int)

    /**
     * Puts collection cards into the first empty pockets from [fromPocket] on, in the given order.
     * Cards already in the binder or not in the collection are skipped. Returns how many were added.
     */
    @Transaction
    suspend fun addCards(binderId: Long, cardIds: List<String>, fromPocket: Int, now: Long): Int {
        val existing = getCards(binderId)
        val inBinder = existing.mapTo(HashSet()) { it.cardId }
        val owned = ownedIds(cardIds).toSet()
        val newIds = cardIds.distinct().filter { it in owned && it !in inBinder }
        val pockets = freePockets(existing.mapTo(HashSet()) { it.pocket }, fromPocket, newIds.size)
        insertCards(newIds.zip(pockets) { cardId, pocket -> BinderCardEntity(binderId, cardId, pocket, addedAt = now) })
        return newIds.size
    }

    /** Moves a card to [toPocket]; a card already there swaps into the moved card's old pocket. */
    @Transaction
    suspend fun moveCard(binderId: Long, cardId: String, toPocket: Int) {
        val cards = getCards(binderId)
        val from = cards.firstOrNull { it.cardId == cardId }?.pocket ?: return
        if (from == toPocket || toPocket < 0) return
        val other = cards.firstOrNull { it.pocket == toPocket }
        // Pockets are unique per binder, so park the moved card outside the binder first
        setPocket(binderId, cardId, -1)
        if (other != null) setPocket(binderId, other.cardId, from)
        setPocket(binderId, cardId, toPocket)
    }

    /** Puts the binder's cards in pockets 0, 1, 2, … in the given order; cards not listed follow in pocket order. */
    @Transaction
    suspend fun arrange(binderId: Long, orderedCardIds: List<String>) {
        val cards = getCards(binderId)
        val listed = orderedCardIds.distinct().filter { id -> cards.any { it.cardId == id } }
        val rest = cards.filter { it.cardId !in listed }.sortedBy { it.pocket }.map { it.cardId }
        val order = listed + rest
        // Two passes so no two cards share a pocket halfway through
        order.forEachIndexed { index, id -> setPocket(binderId, id, -1 - index) }
        order.forEachIndexed { index, id -> setPocket(binderId, id, index) }
    }
}

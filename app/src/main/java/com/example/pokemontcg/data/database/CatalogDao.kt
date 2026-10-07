package com.example.pokemontcg.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {

    /** Runs a query built by [CardSearchQuery]. */
    @RawQuery
    suspend fun search(query: SupportSQLiteQuery): List<CatalogCardWithSet>

    @Query(
        "SELECT c.id, c.name, c.number, c.rarity, c.artist, c.imageSmall, c.imageLarge, " +
            "s.name AS setName, s.series AS setSeries, s.releaseDate " +
            "FROM catalog_cards c LEFT JOIN card_sets s ON s.id = c.setId WHERE c.id = :cardId"
    )
    suspend fun getCard(cardId: String): CatalogCardWithSet?

    @Query("SELECT COUNT(*) FROM catalog_cards")
    fun observeCardCount(): Flow<Int>

    @Query("SELECT * FROM catalog_files")
    suspend fun getFiles(): List<CatalogFileEntity>

    @Upsert
    suspend fun upsertSets(sets: List<CardSetEntity>)

    @Upsert
    suspend fun upsertFile(file: CatalogFileEntity)

    @Upsert
    suspend fun upsertCards(cards: List<CatalogCardEntity>)

    @Query("DELETE FROM catalog_cards WHERE setId = :setId")
    suspend fun deleteCardsInSet(setId: String)

    /** Replaces a set's cards and records the file hash in one go, so an interrupted sync redownloads it. */
    @Transaction
    suspend fun replaceSetCards(setId: String, cards: List<CatalogCardEntity>, file: CatalogFileEntity) {
        deleteCardsInSet(setId)
        upsertCards(cards)
        upsertFile(file)
    }
}

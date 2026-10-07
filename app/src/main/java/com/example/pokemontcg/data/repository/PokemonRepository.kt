package com.example.pokemontcg.data.repository

import com.example.pokemontcg.data.catalog.CatalogSync
import com.example.pokemontcg.data.catalog.SyncState
import com.example.pokemontcg.data.database.CardSearchQuery
import com.example.pokemontcg.data.database.CatalogCardWithSet
import com.example.pokemontcg.data.database.CatalogDao
import com.example.pokemontcg.data.database.ChaseCardEntity
import com.example.pokemontcg.data.database.PokemonDao
import com.example.pokemontcg.data.model.Card
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Single access point for card data. Search and card details come from the local catalog
 * (kept up to date by [CatalogSync]), so they work offline.
 */
class PokemonRepository(
    private val dao: PokemonDao,
    private val catalogDao: CatalogDao,
    private val catalogSync: CatalogSync
) {

    val chaseCards: Flow<List<ChaseCardEntity>> = dao.getAllChaseCards()

    val catalogSyncState: StateFlow<SyncState> = catalogSync.state

    val catalogCardCount: Flow<Int> = catalogDao.observeCardCount()

    fun syncCatalog() = catalogSync.sync()

    /** Emits the saved card, or null while it isn't on the chase list. */
    fun observeSavedCard(cardId: String): Flow<ChaseCardEntity?> = dao.observeCardById(cardId)

    suspend fun searchCards(query: String): List<Card> {
        val sql = CardSearchQuery.build(query) ?: return emptyList()
        return catalogDao.search(CardSearchQuery.toSqliteQuery(sql)).map { it.toCard() }
    }

    /** The card from the catalog, or the saved copy if the catalog doesn't have it (yet). */
    suspend fun getCard(cardId: String): Card? =
        catalogDao.getCard(cardId)?.toCard() ?: dao.getCardById(cardId)?.toCard()

    suspend fun addCardToChaseList(card: Card) {
        dao.insertCard(
            ChaseCardEntity(
                id = card.id,
                name = card.name,
                setName = card.setName,
                number = card.number,
                imageUrl = card.imageSmall ?: "",
                largeImageUrl = card.imageLarge ?: "",
                obtained = false,
                dateAdded = System.currentTimeMillis()
            )
        )
    }

    suspend fun setObtained(cardId: String, obtained: Boolean) = dao.setObtained(cardId, obtained)

    suspend fun removeCardFromChaseList(cardId: String) = dao.deleteCardById(cardId)
}

private fun CatalogCardWithSet.toCard() = Card(
    id = id,
    name = name,
    number = number,
    setName = setName ?: "Unknown set",
    imageSmall = imageSmall,
    imageLarge = imageLarge,
    setSeries = setSeries,
    releaseDate = releaseDate,
    rarity = rarity,
    artist = artist
)

private fun ChaseCardEntity.toCard() = Card(
    id = id,
    name = name,
    number = number,
    setName = setName,
    imageSmall = imageUrl.ifEmpty { null },
    imageLarge = largeImageUrl.ifEmpty { null }
)

package com.example.pokemontcg.data.repository

import com.example.pokemontcg.data.catalog.CatalogSync
import com.example.pokemontcg.data.catalog.SyncState
import com.example.pokemontcg.data.database.CardPriceEntity
import com.example.pokemontcg.data.database.CardSearchQuery
import com.example.pokemontcg.data.database.CardSetWithCount
import com.example.pokemontcg.data.database.CatalogCardWithSet
import com.example.pokemontcg.data.database.CatalogDao
import com.example.pokemontcg.data.database.ChaseCardEntity
import com.example.pokemontcg.data.database.NameCount
import com.example.pokemontcg.data.database.PokemonDao
import com.example.pokemontcg.data.database.PriceDao
import com.example.pokemontcg.data.model.Card
import com.example.pokemontcg.data.model.SearchFilters
import com.example.pokemontcg.data.prices.PriceService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Single access point for card data. Search and card details come from the local catalog
 * (kept up to date by [CatalogSync]), so they work offline.
 */
class PokemonRepository(
    private val dao: PokemonDao,
    private val catalogDao: CatalogDao,
    private val catalogSync: CatalogSync,
    private val priceDao: PriceDao,
    private val priceService: PriceService
) {

    val chaseCards: Flow<List<ChaseCardEntity>> = dao.getAllChaseCards()

    val catalogSyncState: StateFlow<SyncState> = catalogSync.state

    val catalogCardCount: Flow<Int> = catalogDao.observeCardCount()

    /** Sets that have cards, newest first. */
    val catalogSets: Flow<List<CardSetWithCount>> = catalogDao.observeSets()

    fun syncCatalog() = catalogSync.sync()

    /** Cached Cardmarket prices (EUR) by card id. */
    val prices: Flow<Map<String, CardPriceEntity>> = priceDao.observeAll().map { rows -> rows.associateBy { it.cardId } }

    fun observePrice(cardId: String): Flow<CardPriceEntity?> = priceDao.observe(cardId)

    /** Fetches the card's price if it's missing or a day old; throws when offline. */
    suspend fun refreshPrice(cardId: String) = priceService.refreshIfStale(cardId)

    /** Refreshes stale prices for every card on the chase list; failures are skipped. */
    suspend fun refreshChaseListPrices() {
        priceService.refreshAllIfStale(dao.getAllChaseCards().first().map { it.id })
    }

    /** Emits the saved card, or null while it isn't on the chase list. */
    fun observeSavedCard(cardId: String): Flow<ChaseCardEntity?> = dao.observeCardById(cardId)

    /** Rarities in the catalog with their card counts, most common first. */
    val catalogRarities: Flow<List<NameCount>> = catalogDao.observeRarities()

    /** Searches the catalog; with no words, lists everything matching [filters]. */
    suspend fun searchCards(query: String, filters: SearchFilters = SearchFilters()): List<Card> {
        val sql = CardSearchQuery.build(query, filters) ?: return emptyList()
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
    setId = setId,
    setSeries = setSeries,
    releaseDate = releaseDate,
    rarity = rarity,
    artist = artist,
    // Pokémon show their type; Trainer and Energy cards their category
    typeLabel = types?.trim(',')?.split(',')?.firstOrNull()?.takeIf { supertype == "Pokémon" } ?: supertype
)

private fun ChaseCardEntity.toCard() = Card(
    id = id,
    name = name,
    number = number,
    setName = setName,
    imageSmall = imageUrl.ifEmpty { null },
    imageLarge = largeImageUrl.ifEmpty { null }
)

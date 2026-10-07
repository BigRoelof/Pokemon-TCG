package com.example.pokemontcg.data.repository

import com.example.pokemontcg.data.catalog.CatalogSync
import com.example.pokemontcg.data.catalog.SyncState
import com.example.pokemontcg.data.database.BinderDao
import com.example.pokemontcg.data.database.BinderEntity
import com.example.pokemontcg.data.database.BinderPocketCard
import com.example.pokemontcg.data.database.CardPriceEntity
import com.example.pokemontcg.data.database.CardSearchQuery
import com.example.pokemontcg.data.database.CardSetWithCount
import com.example.pokemontcg.data.database.CatalogCardWithSet
import com.example.pokemontcg.data.database.CatalogDao
import com.example.pokemontcg.data.database.ChaseCardEntity
import com.example.pokemontcg.data.database.CollectionCardEntity
import com.example.pokemontcg.data.database.CollectionDao
import com.example.pokemontcg.data.database.NameCount
import com.example.pokemontcg.data.database.PokemonDao
import com.example.pokemontcg.data.database.PriceDao
import com.example.pokemontcg.data.model.Card
import com.example.pokemontcg.data.model.SearchFilters
import com.example.pokemontcg.data.model.TrackedCard
import com.example.pokemontcg.data.prices.PriceService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Single access point for card data. Search and card details come from the local catalog
 * (kept up to date by [CatalogSync]), so they work offline.
 */
class PokemonRepository(
    private val dao: PokemonDao,
    private val collectionDao: CollectionDao,
    private val binderDao: BinderDao,
    private val catalogDao: CatalogDao,
    private val catalogSync: CatalogSync,
    private val priceDao: PriceDao,
    private val priceService: PriceService
) {

    val chaseCards: Flow<List<ChaseCardEntity>> = dao.getAllChaseCards()

    val collectionCards: Flow<List<CollectionCardEntity>> = collectionDao.observeAll()

    val chaseList: Flow<List<TrackedCard>> = chaseCards.map { cards -> cards.map { it.toTracked() } }

    val collection: Flow<List<TrackedCard>> = collectionCards.map { cards -> cards.map { it.toTracked() } }

    /** Both lists together: chase-list cards (not owned) and collection cards (owned). */
    val trackedCards: Flow<List<TrackedCard>> = combine(chaseList, collection) { chase, owned -> chase + owned }

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

    /** Refreshes stale prices for every card in the collection and on the chase list; failures are skipped. */
    suspend fun refreshTrackedPrices() {
        priceService.refreshAllIfStale(trackedCards.first().map { it.id })
    }

    /** Emits the card from the collection or the chase list, or null while it's on neither. */
    fun observeTrackedCard(cardId: String): Flow<TrackedCard?> =
        combine(dao.observeCardById(cardId), collectionDao.observeById(cardId)) { chase, owned ->
            owned?.toTracked() ?: chase?.toTracked()
        }

    /** Rarities in the catalog with their card counts, most common first. */
    val catalogRarities: Flow<List<NameCount>> = catalogDao.observeRarities()

    /** Searches the catalog; with no words, lists everything matching [filters]. */
    suspend fun searchCards(query: String, filters: SearchFilters = SearchFilters()): List<Card> {
        val sql = CardSearchQuery.build(query, filters) ?: return emptyList()
        return catalogDao.search(CardSearchQuery.toSqliteQuery(sql)).map { it.toCard() }
    }

    /** The card from the catalog, or the saved copy if the catalog doesn't have it (yet). */
    suspend fun getCard(cardId: String): Card? =
        catalogDao.getCard(cardId)?.toCard()
            ?: dao.getCardById(cardId)?.toCard()
            ?: collectionDao.getById(cardId)?.toCard()

    suspend fun addCardToChaseList(card: Card) {
        dao.insertCard(
            ChaseCardEntity(
                id = card.id,
                name = card.name,
                setName = card.setName,
                number = card.number,
                imageUrl = card.imageSmall ?: "",
                largeImageUrl = card.imageLarge ?: "",
                dateAdded = System.currentTimeMillis()
            )
        )
    }

    suspend fun addCardToCollection(card: Card) {
        collectionDao.insert(
            CollectionCardEntity(
                id = card.id,
                name = card.name,
                setName = card.setName,
                number = card.number,
                imageUrl = card.imageSmall ?: "",
                largeImageUrl = card.imageLarge ?: "",
                addedAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Catches a chase-list card: it moves into the collection. Returns the chase-list entry as it
     * was, for [undoCatch], or null if the card wasn't on the chase list.
     */
    suspend fun catchCard(cardId: String): ChaseCardEntity? {
        val original = dao.getCardById(cardId) ?: return null
        return original.takeIf { collectionDao.moveToCollection(cardId, System.currentTimeMillis()) }
    }

    suspend fun undoCatch(original: ChaseCardEntity) = collectionDao.restoreChaseCard(original)

    /** Catching moves a chase-list card into the collection; un-catching moves it back. */
    suspend fun setOwned(cardId: String, owned: Boolean) {
        val now = System.currentTimeMillis()
        if (owned) collectionDao.moveToCollection(cardId, now) else collectionDao.moveToChaseList(cardId, now)
    }

    /** Removes the card from whichever list it's on (and from every binder). */
    suspend fun removeCard(cardId: String) {
        dao.deleteCardById(cardId)
        collectionDao.removeFromCollection(cardId)
    }

    /** Binders, oldest first. */
    val binders: Flow<List<BinderEntity>> = binderDao.observeBinders()

    /** The cards in every binder, by binder and pocket. */
    val binderCards: Flow<List<BinderPocketCard>> = binderDao.observeAllPocketCards()

    fun observeBinder(binderId: Long): Flow<BinderEntity?> = binderDao.observeBinder(binderId)

    fun observeBinderCards(binderId: Long): Flow<List<BinderPocketCard>> = binderDao.observePocketCards(binderId)

    /** Ids of the binders that hold the card. */
    fun observeBinderIdsFor(cardId: String): Flow<List<Long>> = binderDao.observeBinderIdsFor(cardId)

    /** Returns the new binder's id. */
    suspend fun createBinder(name: String): Long =
        binderDao.insertBinder(BinderEntity(name = name.trim(), createdAt = System.currentTimeMillis()))

    suspend fun renameBinder(binderId: Long, name: String) = binderDao.renameBinder(binderId, name.trim())

    suspend fun deleteBinder(binderId: Long) = binderDao.deleteBinder(binderId)

    /** Fills the first empty pockets from [fromPocket] on; returns how many cards were added. */
    suspend fun addCardsToBinder(binderId: Long, cardIds: List<String>, fromPocket: Int = 0): Int =
        binderDao.addCards(binderId, cardIds, fromPocket, System.currentTimeMillis())

    suspend fun removeCardFromBinder(binderId: Long, cardId: String) = binderDao.removeCard(binderId, cardId)

    suspend fun moveCardInBinder(binderId: Long, cardId: String, toPocket: Int) =
        binderDao.moveCard(binderId, cardId, toPocket)

    /** Refills the binder's pockets from the first one in the given card order. */
    suspend fun arrangeBinder(binderId: Long, orderedCardIds: List<String>) = binderDao.arrange(binderId, orderedCardIds)
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

private fun ChaseCardEntity.toTracked() =
    TrackedCard(id, name, setName, number, imageUrl, largeImageUrl, addedAt = dateAdded, owned = false)

fun BinderPocketCard.toTracked() = card.toTracked()

private fun CollectionCardEntity.toTracked() =
    TrackedCard(id, name, setName, number, imageUrl, largeImageUrl, addedAt = addedAt, owned = true)

private fun CollectionCardEntity.toCard() = Card(
    id = id,
    name = name,
    number = number,
    setName = setName,
    imageSmall = imageUrl.ifEmpty { null },
    imageLarge = largeImageUrl.ifEmpty { null }
)

private fun ChaseCardEntity.toCard() = Card(
    id = id,
    name = name,
    number = number,
    setName = setName,
    imageSmall = imageUrl.ifEmpty { null },
    imageLarge = largeImageUrl.ifEmpty { null }
)

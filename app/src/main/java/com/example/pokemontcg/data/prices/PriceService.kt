package com.example.pokemontcg.data.prices

import com.example.pokemontcg.data.database.CardPriceEntity
import com.example.pokemontcg.data.database.CatalogDao
import com.example.pokemontcg.data.database.PriceDao
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

/**
 * Fetches Cardmarket prices from TCGdex and caches them in Room. Prices are fetched per card,
 * only for cards on the chase list or opened in Details, and at most once a day per card
 * (TCGdex asks clients to cache).
 */
class PriceService(
    private val api: PriceApi,
    private val priceDao: PriceDao,
    private val catalogDao: CatalogDao,
    private val now: () -> Long = System::currentTimeMillis
) {
    // TCGdex's set list and set contents, cached for the life of the process
    private val cacheLock = Mutex()
    private var tcgdexSets: List<TcgdexSetJson>? = null
    private val setCards = mutableMapOf<String, List<TcgdexCardBriefJson>>()

    /**
     * Refreshes the card's price if it's missing or older than a day. Throws on network
     * failure, leaving any cached price in place.
     */
    suspend fun refreshIfStale(cardId: String) {
        val cached = priceDao.get(cardId)
        if (cached != null && now() - cached.fetchedAt < MAX_AGE_MS) return

        val tcgdexId = cached?.tcgdexId ?: resolveTcgdexId(cardId)
        if (tcgdexId == null) {
            priceDao.upsert(emptyPrice(cardId, tcgdexId = null))
            return
        }
        val cardmarket = api.getCard(tcgdexId).pricing?.cardmarket
        priceDao.upsert(
            CardPriceEntity(
                cardId = cardId,
                tcgdexId = tcgdexId,
                price = TcgdexMatching.pickPrice(cardmarket),
                average30 = cardmarket?.average30?.takeIf { it > 0 },
                low = cardmarket?.low?.takeIf { it > 0 },
                cardmarketProductId = cardmarket?.idProduct,
                sourceUpdated = cardmarket?.updated,
                fetchedAt = now()
            )
        )
    }

    /** Refreshes stale prices for several cards, a few at a time; individual failures are skipped. */
    suspend fun refreshAllIfStale(cardIds: Collection<String>) = coroutineScope {
        val requests = Semaphore(PARALLEL_REQUESTS)
        cardIds.map { id ->
            async {
                requests.withPermit { runCatching { refreshIfStale(id) } }
            }
        }.awaitAll()
    }

    private suspend fun resolveTcgdexId(cardId: String): String? {
        val card = catalogDao.getCard(cardId) ?: return null
        val sets = cacheLock.withLock { tcgdexSets ?: api.getSets().also { tcgdexSets = it } }
        val setId = TcgdexMatching.findSetId(card.setId, card.setName.orEmpty(), card.number, sets) ?: return null
        val cards = cacheLock.withLock {
            setCards[setId] ?: api.getSet(setId).cards.orEmpty().also { setCards[setId] = it }
        }
        return TcgdexMatching.findCardId(card.number, cards)
    }

    private fun emptyPrice(cardId: String, tcgdexId: String?) =
        CardPriceEntity(cardId, tcgdexId, null, null, null, null, null, now())

    private companion object {
        const val MAX_AGE_MS = 24 * 60 * 60 * 1000L
        const val PARALLEL_REQUESTS = 4
    }
}

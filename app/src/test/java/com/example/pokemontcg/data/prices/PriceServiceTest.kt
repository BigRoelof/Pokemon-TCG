package com.example.pokemontcg.data.prices

import androidx.sqlite.db.SupportSQLiteQuery
import com.example.pokemontcg.data.database.CardPriceEntity
import com.example.pokemontcg.data.database.CardSetEntity
import com.example.pokemontcg.data.database.CardSetWithCount
import com.example.pokemontcg.data.database.CatalogCardEntity
import com.example.pokemontcg.data.database.CatalogCardWithSet
import com.example.pokemontcg.data.database.CatalogDao
import com.example.pokemontcg.data.database.CatalogFileEntity
import com.example.pokemontcg.data.database.NameCount
import com.example.pokemontcg.data.database.PriceDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class PriceServiceTest {

    private var clock = 1_000_000_000L
    private val day = 24 * 60 * 60 * 1000L

    private val api = FakePriceApi()
    private val priceDao = FakePriceDao()
    private val service = PriceService(api, priceDao, FakeCatalogDao(), now = { clock })

    @Test
    fun fetchesAndCachesThePrice() = runBlocking {
        service.refreshIfStale("sv3-125")
        val row = priceDao.rows.getValue("sv3-125")
        assertEquals("sv03-125", row.tcgdexId)
        assertEquals(3.51, row.price!!, 0.0)
        assertEquals(725205, row.cardmarketProductId)
        assertEquals(1, api.cardRequests)
    }

    @Test
    fun freshPricesAreNotFetchedAgain() = runBlocking {
        service.refreshIfStale("sv3-125")
        clock += day - 1
        service.refreshIfStale("sv3-125")
        assertEquals(1, api.cardRequests)
    }

    @Test
    fun stalePricesAreRefreshedWithoutResolvingTheIdAgain() = runBlocking {
        service.refreshIfStale("sv3-125")
        clock += day
        api.trend = 4.0
        service.refreshIfStale("sv3-125")
        assertEquals(4.0, priceDao.rows.getValue("sv3-125").price!!, 0.0)
        assertEquals(1, api.setListRequests)
    }

    @Test
    fun failedRefreshKeepsTheCachedPrice() = runBlocking {
        service.refreshIfStale("sv3-125")
        clock += day
        api.offline = true
        try {
            service.refreshIfStale("sv3-125")
            fail("Expected a network error")
        } catch (expected: IOException) {
        }
        assertEquals(3.51, priceDao.rows.getValue("sv3-125").price!!, 0.0)
    }

    @Test
    fun cardsWithoutAMatchAreRecordedWithoutAPrice() = runBlocking {
        service.refreshIfStale("unknown-1")
        val row = priceDao.rows.getValue("unknown-1")
        assertNull(row.tcgdexId)
        assertNull(row.price)
        assertEquals(0, api.cardRequests)
    }

    @Test
    fun refreshAllSkipsFailures() = runBlocking {
        api.offline = true
        service.refreshAllIfStale(listOf("sv3-125", "unknown-1"))
        // The unknown card needs no network; the other failed and stays uncached
        assertEquals(setOf("unknown-1"), priceDao.rows.keys)
    }

    private class FakePriceApi : PriceApi {
        var offline = false
        var trend = 3.51
        var cardRequests = 0
        var setListRequests = 0

        private fun checkOnline() {
            if (offline) throw IOException("offline")
        }

        override suspend fun getSets(): List<TcgdexSetJson> {
            checkOnline(); setListRequests++
            return listOf(TcgdexSetJson("sv03", "Obsidian Flames"))
        }

        override suspend fun getSet(id: String): TcgdexSetDetailJson {
            checkOnline()
            return TcgdexSetDetailJson(id, listOf(TcgdexCardBriefJson("sv03-001", "001"), TcgdexCardBriefJson("sv03-125", "125")))
        }

        override suspend fun getCard(id: String): TcgdexCardJson {
            checkOnline(); cardRequests++
            return TcgdexCardJson(id, TcgdexPricingJson(CardmarketPriceJson("2026-10-07T09:52:36Z", 725205, 3.88, 1.99, trend, 3.66)))
        }
    }

    private class FakePriceDao : PriceDao {
        val rows = mutableMapOf<String, CardPriceEntity>()
        override fun observeAll(): Flow<List<CardPriceEntity>> = flowOf(rows.values.toList())
        override fun observe(cardId: String): Flow<CardPriceEntity?> = flowOf(rows[cardId])
        override suspend fun get(cardId: String) = rows[cardId]
        override suspend fun upsert(price: CardPriceEntity) {
            rows[price.cardId] = price
        }
    }

    private class FakeCatalogDao : CatalogDao {
        override suspend fun getCard(cardId: String): CatalogCardWithSet? = when (cardId) {
            "sv3-125" -> CatalogCardWithSet("sv3-125", "Charizard ex", "125", null, null, null, null, "sv3", "Obsidian Flames", null, null, "Pokémon", ",Fire,")
            else -> null
        }

        override suspend fun search(query: SupportSQLiteQuery) = emptyList<CatalogCardWithSet>()
        override fun observeSets(): Flow<List<CardSetWithCount>> = flowOf(emptyList())
        override fun observeCardCount(): Flow<Int> = flowOf(0)
        override fun observeRarities(): Flow<List<NameCount>> = flowOf(emptyList())
        override suspend fun getFiles() = emptyList<CatalogFileEntity>()
        override suspend fun upsertSets(sets: List<CardSetEntity>) = Unit
        override suspend fun upsertFile(file: CatalogFileEntity) = Unit
        override suspend fun upsertCards(cards: List<CatalogCardEntity>) = Unit
        override suspend fun deleteCardsInSet(setId: String) = Unit
    }
}

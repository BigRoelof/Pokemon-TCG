package com.example.pokemontcg.data.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BinderDaoTest {

    private lateinit var db: PokemonDatabase
    private lateinit var dao: BinderDao
    private lateinit var collection: CollectionDao
    private var binderId = 0L

    private fun owned(id: String) = CollectionCardEntity(id, "Card $id", "Obsidian Flames", id, "s", "l", addedAt = 0)

    /** Pocket → card id for the binder. */
    private suspend fun pockets(binder: Long = binderId): Map<Int, String> =
        dao.getCards(binder).associate { it.pocket to it.cardId }

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            PokemonDatabase::class.java
        ).build()
        dao = db.binderDao()
        collection = db.collectionDao()
        listOf("a", "b", "c", "d").forEach { collection.insert(owned(it)) }
        binderId = dao.insertBinder(BinderEntity(name = "Fire", createdAt = 1))
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun addingFillsEmptyPocketsInOrderAndSkipsDuplicatesAndUnownedCards() = runBlocking {
        assertEquals(2, dao.addCards(binderId, listOf("a", "b"), fromPocket = 0, now = 1))
        dao.moveCard(binderId, "a", 3)
        // Pockets: 1 = b, 3 = a. "a" is already in, "x" isn't owned.
        assertEquals(2, dao.addCards(binderId, listOf("c", "a", "x", "d"), fromPocket = 0, now = 2))
        assertEquals(mapOf(0 to "c", 1 to "b", 2 to "d", 3 to "a"), pockets())
    }

    @Test
    fun addingFromATappedPocketStartsThere() = runBlocking {
        dao.addCards(binderId, listOf("a", "b"), fromPocket = 4, now = 1)
        assertEquals(mapOf(4 to "a", 5 to "b"), pockets())
    }

    @Test
    fun movingToAFilledPocketSwapsTheCards() = runBlocking {
        dao.addCards(binderId, listOf("a", "b", "c"), fromPocket = 0, now = 1)
        dao.moveCard(binderId, "a", 2)
        assertEquals(mapOf(0 to "c", 1 to "b", 2 to "a"), pockets())
        dao.moveCard(binderId, "b", 12)
        assertEquals(mapOf(0 to "c", 12 to "b", 2 to "a"), pockets())
    }

    @Test
    fun arrangingRefillsFromTheFirstPocket() = runBlocking {
        dao.addCards(binderId, listOf("a", "b", "c"), fromPocket = 5, now = 1)
        dao.arrange(binderId, listOf("c", "a", "b"))
        assertEquals(mapOf(0 to "c", 1 to "a", 2 to "b"), pockets())
    }

    @Test
    fun aCardCanBeInSeveralBindersAndLeavesThemAllWithTheCollection() = runBlocking {
        val other = dao.insertBinder(BinderEntity(name = "Favourites", createdAt = 2))
        dao.addCards(binderId, listOf("a", "b"), fromPocket = 0, now = 1)
        dao.addCards(other, listOf("a"), fromPocket = 0, now = 1)
        assertEquals(listOf(binderId, other), dao.observeBinderIdsFor("a").first().sorted())

        collection.removeFromCollection("a")
        assertEquals(mapOf(1 to "b"), pockets())
        assertTrue(pockets(other).isEmpty())
    }

    @Test
    fun movingACardBackToTheChaseListTakesItOutOfBinders() = runBlocking {
        dao.addCards(binderId, listOf("a"), fromPocket = 0, now = 1)
        collection.moveToChaseList("a", now = 2)
        assertTrue(pockets().isEmpty())
    }

    @Test
    fun deletingABinderDeletesItsPocketsButKeepsTheCards() = runBlocking {
        dao.addCards(binderId, listOf("a", "b"), fromPocket = 0, now = 1)
        dao.deleteBinder(binderId)
        assertTrue(dao.getCards(binderId).isEmpty())
        assertEquals(4, collection.observeAll().first().size)
    }

    @Test
    fun pocketCardsComeWithTheirCollectionSnapshot() = runBlocking {
        dao.addCards(binderId, listOf("b"), fromPocket = 7, now = 1)
        val card = dao.observePocketCards(binderId).first().single()
        assertEquals(7, card.pocket)
        assertEquals("Card b", card.card.name)
    }
}

package com.example.pokemontcg.data.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CollectionDaoTest {

    private lateinit var db: PokemonDatabase
    private lateinit var dao: CollectionDao

    private val pikachu = ChaseCardEntity("basep-1", "Pikachu", "Wizards Black Star Promos", "1", "s", "l", dateAdded = 100)

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            PokemonDatabase::class.java
        ).build()
        dao = db.collectionDao()
        dao.insertChaseCard(pikachu)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun catchingMovesTheCardIntoTheCollection() = runBlocking {
        assertTrue(dao.moveToCollection("basep-1", now = 500))
        assertNull(dao.getChaseCard("basep-1"))
        val owned = dao.getById("basep-1")!!
        assertEquals("Pikachu", owned.name)
        assertEquals(500, owned.addedAt)
    }

    @Test
    fun movingBackPutsItOnTheChaseList() = runBlocking {
        dao.moveToCollection("basep-1", now = 500)
        assertTrue(dao.moveToChaseList("basep-1", now = 600))
        assertNull(dao.getById("basep-1"))
        assertEquals(pikachu.copy(dateAdded = 600), dao.getChaseCard("basep-1"))
    }

    @Test
    fun movingACardThatIsNotThereChangesNothing() = runBlocking {
        assertFalse(dao.moveToChaseList("basep-1", now = 500))
        assertFalse(dao.moveToCollection("unknown-1", now = 500))
        assertEquals(pikachu, dao.getChaseCard("basep-1"))
    }
}

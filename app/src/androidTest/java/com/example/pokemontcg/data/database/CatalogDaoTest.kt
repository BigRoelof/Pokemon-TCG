package com.example.pokemontcg.data.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Runs catalog searches on real SQLite, where LIKE escaping and ordering actually apply. */
@RunWith(AndroidJUnit4::class)
class CatalogDaoTest {

    private lateinit var db: PokemonDatabase
    private lateinit var dao: CatalogDao

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            PokemonDatabase::class.java
        ).build()
        dao = db.catalogDao()
        dao.upsertSets(
            listOf(
                CardSetEntity("base1", "Base", "Base", "1999/01/09", 102, null, null),
                CardSetEntity("sv3", "Obsidian Flames", "Scarlet & Violet", "2023/08/11", 230, null, null)
            )
        )
        dao.upsertCards(
            listOf(
                card("base1-4", "base1", "Charizard"),
                card("sv3-125", "sv3", "Charizard ex"),
                card("sv3-199", "sv3", "Dark Charizard ex"),
                card("base1-31", "base1", "Mr. Mime"),
                card("sv3-1", "sv3", "Pikachu_100%")
            )
        )
    }

    @After
    fun tearDown() = db.close()

    private fun card(id: String, setId: String, name: String) =
        CatalogCardEntity(id, setId, name, id.substringAfter('-'), null, null, null, null)

    private fun search(input: String) = runBlocking {
        dao.search(CardSearchQuery.toSqliteQuery(CardSearchQuery.build(input)!!)).map { it.id }
    }

    @Test
    fun exactNameFirstThenPrefixThenNewestSet() {
        assertEquals(listOf("base1-4", "sv3-125", "sv3-199"), search("charizard"))
    }

    @Test
    fun wordsCanAppearAnywhere() {
        assertEquals(listOf("sv3-199"), search("ex dark"))
        assertEquals(listOf("base1-31"), search("mr. mime"))
    }

    @Test
    fun wildcardCharactersMatchLiterally() {
        assertEquals(listOf("sv3-1"), search("_100%"))
        assertEquals(emptyList<String>(), search("pika%chu"))
    }

    @Test
    fun cardIncludesItsSet() = runBlocking {
        val card = dao.getCard("sv3-125")!!
        assertEquals("Obsidian Flames", card.setName)
        assertEquals("2023/08/11", card.releaseDate)
    }

    @Test
    fun replacingASetsCardsRecordsTheFileHash() = runBlocking {
        dao.replaceSetCards("base1", listOf(card("base1-4", "base1", "Charizard")), CatalogFileEntity("cards/en/base1.json", "abc"))
        assertEquals(listOf("base1-4"), search("charizard").filter { it.startsWith("base1") })
        assertEquals(emptyList<String>(), search("mime"))
        assertEquals(listOf(CatalogFileEntity("cards/en/base1.json", "abc")), dao.getFiles())
    }
}

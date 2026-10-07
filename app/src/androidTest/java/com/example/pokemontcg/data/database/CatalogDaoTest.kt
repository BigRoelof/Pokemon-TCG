package com.example.pokemontcg.data.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.pokemontcg.data.model.CardType
import com.example.pokemontcg.data.model.SearchFilters
import kotlinx.coroutines.flow.first
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
                CardSetEntity("base1", "Base", "Base", "1999/01/09", 102, null, null, "BS"),
                CardSetEntity("sv3", "Obsidian Flames", "Scarlet & Violet", "2023/08/11", 230, null, null, "OBF"),
                CardSetEntity("ex1", "Expedition Base Set", "E-Card", "2002/09/15", 165, null, null, "EX")
            )
        )
        dao.upsertCards(
            listOf(
                card("base1-4", "base1", "Charizard"),
                card("sv3-125", "sv3", "Charizard ex"),
                card("sv3-199", "sv3", "Dark Charizard ex"),
                card("base1-31", "base1", "Mr. Mime"),
                card("sv3-1", "sv3", "Pikachu_100%"),
                card("sv3-GG01", "sv3", "Charmander"),
                card("sv3-10", "sv3", "Charmeleon"),
                card("ex1-40", "ex1", "Charizard"),
                card("sv3-215", "sv3", "Charizard ex", rarity = "Special Illustration Rare", types = ",Darkness,"),
                card("sv3-196", "sv3", "Arven", rarity = "Ultra Rare", supertype = "Trainer"),
                card("sv3-200", "sv3", "Fire Energy", supertype = "Energy"),
                card("sv3-4", "sv3", "Charmander", rarity = "Special Illustration Rare", types = ",Fire,")
            )
        )
    }

    @After
    fun tearDown() = db.close()

    private fun card(
        id: String,
        setId: String,
        name: String,
        rarity: String? = null,
        supertype: String = "Pokémon",
        types: String? = null
    ) = CatalogCardEntity(id, setId, name, id.substringAfter('-'), rarity, null, null, null, supertype, types)

    private fun search(input: String, setId: String? = null, type: CardType? = null, rarity: String? = null) = runBlocking {
        dao.search(CardSearchQuery.toSqliteQuery(CardSearchQuery.build(input, SearchFilters(setId, type, rarity))!!)).map { it.id }
    }

    @Test
    fun exactNameFirstThenPrefixThenNewestSet() {
        assertEquals(listOf("ex1-40", "base1-4", "sv3-125", "sv3-215", "sv3-199"), search("charizard"))
    }

    @Test
    fun wordsCanNameTheSetByNameOrCode() {
        assertEquals(listOf("sv3-125", "sv3-199", "sv3-215"), search("charizard obsidian"))
        assertEquals(listOf("sv3-125", "sv3-199", "sv3-215"), search("charizard OBF"))
        assertEquals(listOf("base1-4"), search("charizard bs"))
    }

    @Test
    fun setNameWordsDoNotOutrankCardNames() {
        // "ex" also starts "Expedition", but cards named "... ex" come first
        assertEquals(listOf("sv3-125", "sv3-215", "sv3-199", "ex1-40"), search("charizard ex"))
    }

    @Test
    fun setCodeAndNumberFindOneCard() {
        assertEquals(listOf("sv3-125"), search("obf 125"))
    }

    @Test
    fun setFilterLimitsResults() {
        assertEquals(listOf("sv3-125", "sv3-215", "sv3-199"), search("charizard", setId = "sv3"))
    }

    @Test
    fun wholeSetIsListedInCardNumberOrder() {
        assertEquals(
            listOf("sv3-1", "sv3-4", "sv3-10", "sv3-125", "sv3-196", "sv3-199", "sv3-200", "sv3-215", "sv3-GG01"),
            search("", setId = "sv3")
        )
    }

    @Test
    fun setsWithCardsAreListedNewestFirst() = runBlocking {
        val sets = dao.observeSets().first()
        assertEquals(listOf("sv3", "ex1", "base1"), sets.map { it.id })
        assertEquals(9, sets.first().cardCount)
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
        assertEquals(listOf("base1-4"), search("charizard", setId = "base1"))
        assertEquals(emptyList<String>(), search("mime"))
        assertEquals(listOf(CatalogFileEntity("cards/en/base1.json", "abc")), dao.getFiles())
    }

    @Test
    fun typeFilterMatchesPokemonTypesAndCardCategories() {
        assertEquals(listOf("sv3-4"), search("", type = CardType.FIRE))
        assertEquals(listOf("sv3-196"), search("", type = CardType.TRAINER))
        assertEquals(listOf("sv3-200"), search("", type = CardType.ENERGY))
    }

    @Test
    fun rarityFilterCombinesWithWords() {
        assertEquals(listOf("sv3-4", "sv3-215"), search("", rarity = "Special Illustration Rare"))
        assertEquals(listOf("sv3-215"), search("charizard", rarity = "Special Illustration Rare"))
    }

    @Test
    fun raritiesAreListedMostCommonFirst() = runBlocking {
        val rarities = dao.observeRarities().first()
        assertEquals(listOf(NameCount("Special Illustration Rare", 2), NameCount("Ultra Rare", 1)), rarities)
    }
}

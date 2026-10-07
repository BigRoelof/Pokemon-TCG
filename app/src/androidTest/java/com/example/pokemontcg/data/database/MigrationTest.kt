package com.example.pokemontcg.data.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val testDb = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PokemonDatabase::class.java
    )

    @Test
    fun migrate1To2AddsCatalogTablesAndKeepsChaseList() {
        helper.createDatabase(testDb, 1).apply {
            execSQL(
                "INSERT INTO chase_cards (id, name, setName, number, imageUrl, largeImageUrl, obtained, dateAdded) " +
                    "VALUES ('sv3-125', 'Charizard ex', 'Obsidian Flames', '125', 'small.png', 'large.png', 1, 1)"
            )
            close()
        }
        // Validates the migrated schema against the exported schemas/2.json
        helper.runMigrationsAndValidate(testDb, 2, true, *ALL_MIGRATIONS).use { db ->
            db.query("SELECT obtained FROM chase_cards WHERE id = 'sv3-125'").use { cursor ->
                check(cursor.moveToFirst() && cursor.getInt(0) == 1) { "Saved card lost during migration" }
            }
        }
    }

    @Test
    fun migrate2To3AddsSetCodesAndRefetchesTheSetsFile() {
        helper.createDatabase(testDb, 2).apply {
            execSQL("INSERT INTO card_sets (id, name, series, releaseDate, total, logoUrl, symbolUrl) VALUES ('sv3', 'Obsidian Flames', 'Scarlet & Violet', '2023/08/11', 230, NULL, NULL)")
            execSQL("INSERT INTO catalog_files (path, sha) VALUES ('sets/en.json', 'abc'), ('cards/en/sv3.json', 'def')")
            close()
        }
        helper.runMigrationsAndValidate(testDb, 3, true, *ALL_MIGRATIONS).use { db ->
            db.query("SELECT path FROM catalog_files").use { cursor ->
                check(cursor.count == 1 && cursor.moveToFirst() && cursor.getString(0) == "cards/en/sv3.json") {
                    "Only the sets file should be marked for download again"
                }
            }
            db.query("SELECT name FROM card_sets WHERE id = 'sv3'").use { cursor ->
                check(cursor.moveToFirst()) { "Set lost during migration" }
            }
        }
    }

    @Test
    fun migrate3To4AddsThePriceCache() {
        helper.createDatabase(testDb, 3).close()
        helper.runMigrationsAndValidate(testDb, 4, true, *ALL_MIGRATIONS).use { db ->
            db.query("SELECT COUNT(*) FROM card_prices").use { cursor ->
                check(cursor.moveToFirst() && cursor.getInt(0) == 0) { "Price cache should start empty" }
            }
        }
    }

    @Test
    fun migrate4To5AddsTypesAndRedownloadsEveryCardFile() {
        helper.createDatabase(testDb, 4).apply {
            execSQL("INSERT INTO catalog_files (path, sha) VALUES ('sets/en.json', 'a'), ('cards/en/sv3.json', 'b'), ('cards/en/base1.json', 'c')")
            close()
        }
        helper.runMigrationsAndValidate(testDb, 5, true, *ALL_MIGRATIONS).use { db ->
            db.query("SELECT path FROM catalog_files").use { cursor ->
                check(cursor.count == 1 && cursor.moveToFirst() && cursor.getString(0) == "sets/en.json") {
                    "Card files should be marked for download again, the sets file kept"
                }
            }
        }
    }

    @Test
    fun migrate5To6MovesCaughtCardsIntoTheCollection() {
        helper.createDatabase(testDb, 5).apply {
            execSQL(
                "INSERT INTO chase_cards (id, name, setName, number, imageUrl, largeImageUrl, obtained, dateAdded) VALUES " +
                    "('swsh12pt5gg-GG55', 'Regigigas VSTAR', 'Crown Zenith Galarian Gallery', 'GG55', 's', 'l', 1, 111), " +
                    "('basep-1', 'Pikachu', 'Wizards Black Star Promos', '1', 's', 'l', 0, 222)"
            )
            close()
        }
        helper.runMigrationsAndValidate(testDb, 6, true, *ALL_MIGRATIONS).use { db ->
            db.query("SELECT id, addedAt FROM collection_cards").use { cursor ->
                check(cursor.count == 1 && cursor.moveToFirst()) { "Expected one collection card" }
                check(cursor.getString(0) == "swsh12pt5gg-GG55" && cursor.getLong(1) == 111L) { "Caught card or its date lost" }
            }
            db.query("SELECT id, dateAdded FROM chase_cards").use { cursor ->
                check(cursor.count == 1 && cursor.moveToFirst()) { "Expected one chase card" }
                check(cursor.getString(0) == "basep-1" && cursor.getLong(1) == 222L) { "Chase card or its date lost" }
            }
        }
    }

    @Test
    fun migrate6To7AddsEmptyBindersAndKeepsTheCollection() {
        helper.createDatabase(testDb, 6).apply {
            execSQL(
                "INSERT INTO collection_cards (id, name, setName, number, imageUrl, largeImageUrl, addedAt) " +
                    "VALUES ('sv3-125', 'Charizard ex', 'Obsidian Flames', '125', 's', 'l', 1)"
            )
            close()
        }
        helper.runMigrationsAndValidate(testDb, 7, true, *ALL_MIGRATIONS).use { db ->
            db.query("SELECT COUNT(*) FROM binders").use { cursor ->
                check(cursor.moveToFirst() && cursor.getInt(0) == 0) { "Binders should start empty" }
            }
            db.query("SELECT COUNT(*) FROM collection_cards").use { cursor ->
                check(cursor.moveToFirst() && cursor.getInt(0) == 1) { "Collection card lost during migration" }
            }
        }
    }

    /** Creates the oldest schema and checks Room can migrate it to the current version. */
    @Test
    fun migrateAllFromFirstVersion() {
        helper.createDatabase(testDb, 1).apply {
            execSQL(
                "INSERT INTO chase_cards (id, name, setName, number, imageUrl, largeImageUrl, obtained, dateAdded) " +
                    "VALUES ('sv3-125', 'Charizard ex', 'Obsidian Flames', '125', 'small.png', 'large.png', 0, 1)"
            )
            close()
        }

        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            PokemonDatabase::class.java,
            testDb
        )
            .addMigrations(*ALL_MIGRATIONS)
            .build()
            .apply {
                openHelper.writableDatabase.query("SELECT name FROM chase_cards WHERE id = 'sv3-125'").use { cursor ->
                    check(cursor.moveToFirst() && cursor.getString(0) == "Charizard ex") { "Saved card lost during migration" }
                }
                close()
            }
    }
}

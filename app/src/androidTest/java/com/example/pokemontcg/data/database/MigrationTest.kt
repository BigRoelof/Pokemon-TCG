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

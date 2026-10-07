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

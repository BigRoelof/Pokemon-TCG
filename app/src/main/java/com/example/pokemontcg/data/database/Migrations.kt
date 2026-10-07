package com.example.pokemontcg.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Every manual schema migration, in order. When changing an entity:
 * 1. Bump `version` in [PokemonDatabase] and build, so Room exports `schemas/<version>.json`.
 * 2. Add a `Migration(old, new)` here, or an `AutoMigration` on [PokemonDatabase] for simple changes
 *    such as new tables (that is how versions 2 to 5 were added). Copy table definitions from
 *    the exported schema so `MigrationTest` can validate them.
 * 3. Run `MigrationTest` on a device; it fails if any version can't be migrated to the latest.
 */
val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_5_6)

/**
 * 6: the collection gets its own table. Caught chase-list cards (`obtained = 1`) move there,
 * keeping when they were added; the chase list keeps the rest and loses the `obtained` column.
 * SQLite on older Android versions can't drop a column, so `chase_cards` is rebuilt.
 */
object MIGRATION_5_6 : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `collection_cards` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`setName` TEXT NOT NULL, `number` TEXT NOT NULL, `imageUrl` TEXT NOT NULL, " +
                "`largeImageUrl` TEXT NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "INSERT INTO collection_cards (id, name, setName, number, imageUrl, largeImageUrl, addedAt) " +
                "SELECT id, name, setName, number, imageUrl, largeImageUrl, dateAdded FROM chase_cards WHERE obtained = 1"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `chase_cards_new` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`setName` TEXT NOT NULL, `number` TEXT NOT NULL, `imageUrl` TEXT NOT NULL, " +
                "`largeImageUrl` TEXT NOT NULL, `dateAdded` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "INSERT INTO chase_cards_new (id, name, setName, number, imageUrl, largeImageUrl, dateAdded) " +
                "SELECT id, name, setName, number, imageUrl, largeImageUrl, dateAdded FROM chase_cards WHERE obtained = 0"
        )
        db.execSQL("DROP TABLE chase_cards")
        db.execSQL("ALTER TABLE chase_cards_new RENAME TO chase_cards")
    }
}

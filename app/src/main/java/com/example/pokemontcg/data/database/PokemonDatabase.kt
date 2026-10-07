package com.example.pokemontcg.data.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ChaseCardEntity::class, CatalogCardEntity::class, CardSetEntity::class, CatalogFileEntity::class,
        CardPriceEntity::class, CollectionCardEntity::class
    ],
    // 6: collection_cards; caught chase cards moved there (manual MIGRATION_5_6)
    version = 6,
    exportSchema = true,
    autoMigrations = [
        // 2: card catalog tables
        AutoMigration(from = 1, to = 2),
        // 3: set collector codes (card_sets.ptcgoCode)
        AutoMigration(from = 2, to = 3, spec = Migration2To3::class),
        // 4: Cardmarket price cache
        AutoMigration(from = 3, to = 4),
        // 5: card supertype and types, rarities cleaned up
        AutoMigration(from = 4, to = 5, spec = Migration4To5::class)
    ]
)
abstract class PokemonDatabase : RoomDatabase() {

    abstract fun pokemonDao(): PokemonDao

    abstract fun catalogDao(): CatalogDao

    abstract fun priceDao(): PriceDao

    abstract fun collectionDao(): CollectionDao

    companion object {
        const val DATABASE_NAME = "pokemon_tcg_database"

        @Volatile
        private var INSTANCE: PokemonDatabase? = null

        fun getDatabase(context: Context): PokemonDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PokemonDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

/** Forgets the sets file's hash so the next sync downloads it again and fills in the new codes. */
class Migration2To3 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.execSQL("DELETE FROM catalog_files WHERE path = 'sets/en.json'")
    }
}

/** Forgets every card file's hash, so the next sync downloads all cards again to fill the new columns. */
class Migration4To5 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.execSQL("DELETE FROM catalog_files WHERE path LIKE 'cards/%'")
    }
}

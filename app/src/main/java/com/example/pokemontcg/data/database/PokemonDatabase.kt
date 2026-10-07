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
        CardPriceEntity::class
    ],
    version = 4,
    exportSchema = true,
    autoMigrations = [
        // 2: card catalog tables
        AutoMigration(from = 1, to = 2),
        // 3: set collector codes (card_sets.ptcgoCode)
        AutoMigration(from = 2, to = 3, spec = Migration2To3::class),
        // 4: Cardmarket price cache
        AutoMigration(from = 3, to = 4)
    ]
)
abstract class PokemonDatabase : RoomDatabase() {

    abstract fun pokemonDao(): PokemonDao

    abstract fun catalogDao(): CatalogDao

    abstract fun priceDao(): PriceDao

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

package com.example.pokemontcg.data.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ChaseCardEntity::class, CatalogCardEntity::class, CardSetEntity::class, CatalogFileEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        // 2: card catalog tables
        AutoMigration(from = 1, to = 2)
    ]
)
abstract class PokemonDatabase : RoomDatabase() {

    abstract fun pokemonDao(): PokemonDao

    abstract fun catalogDao(): CatalogDao

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

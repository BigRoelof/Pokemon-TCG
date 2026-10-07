package com.example.pokemontcg.data.database

import androidx.room.migration.Migration

/**
 * Every manual schema migration, in order. When changing [ChaseCardEntity]:
 * 1. Bump `version` in [PokemonDatabase] and build, so Room exports `schemas/<version>.json`.
 * 2. Add a `Migration(old, new)` here, or an `AutoMigration` on [PokemonDatabase] for simple changes
 *    such as new tables (that is how version 2 was added).
 * 3. Run `MigrationTest` on a device; it fails if any version can't be migrated to the latest.
 */
val ALL_MIGRATIONS: Array<Migration> = arrayOf()

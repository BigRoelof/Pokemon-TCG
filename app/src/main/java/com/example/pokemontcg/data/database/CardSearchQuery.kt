package com.example.pokemontcg.data.database

import androidx.sqlite.db.SimpleSQLiteQuery
import com.example.pokemontcg.data.model.SearchFilters

/**
 * Builds the catalog search. Every word must match the card name (anywhere), the start of a
 * word in the set name, the set code (e.g. `OBF`) or the card number; so "charizard 151",
 * "charizard obf" and "obf 125" all work. [SearchFilters] narrow it to a set, type or rarity.
 */
object CardSearchQuery {

    /** Cap for searches with words. */
    const val MAX_RESULTS = 100

    /** Cap for browsing by filters alone, e.g. a whole set or every Fire card of a rarity. */
    const val MAX_BROWSE_RESULTS = 1000

    fun limitFor(input: String) = if (input.isBlank()) MAX_BROWSE_RESULTS else MAX_RESULTS

    data class Sql(val sql: String, val args: List<String>)

    /** Returns null when there's nothing to search for (no words and no filters). */
    fun build(input: String, filters: SearchFilters = SearchFilters()): Sql? {
        val words = input.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty() && filters.isEmpty) return null
        val phrase = words.joinToString(" ")
        val args = mutableListOf<String>()
        val conditions = mutableListOf<String>()

        filters.setId?.let { setId ->
            conditions += "c.setId = ?"
            args += setId
        }
        filters.type?.let { type ->
            if (type.isSupertype) {
                conditions += "c.supertype = ?"
                args += type.label
            } else {
                conditions += "c.types LIKE ?"
                args += "%,${type.label},%"
            }
        }
        filters.rarity?.let { rarity ->
            conditions += "c.rarity = ?"
            args += rarity
        }
        words.forEach { word ->
            conditions += "(c.name LIKE ? ESCAPE '\\' OR (' ' || s.name) LIKE ? ESCAPE '\\' " +
                "OR lower(s.ptcgoCode) = ? OR lower(c.number) = ?)"
            args += listOf("%${escapeLike(word)}%", "% ${escapeLike(word)}%", word, word)
        }

        val order = mutableListOf<String>()
        if (words.isNotEmpty()) {
            // Exact name, then cards whose name holds every word, then names starting with the phrase
            order += "lower(c.name) = ? DESC"
            args += phrase
            order += words.joinToString(" AND ", prefix = "(", postfix = ") DESC") { "c.name LIKE ? ESCAPE '\\'" }
            args += words.map { "%${escapeLike(it)}%" }
            order += "c.name LIKE ? ESCAPE '\\' DESC"
            args += "${escapeLike(phrase)}%"
        }
        order += "s.releaseDate DESC"
        // Card number order: numeric numbers first (1, 2, 10), then others (GG01, TG05)
        order += "(CAST(c.number AS INTEGER) = 0) ASC, CAST(c.number AS INTEGER), c.number"

        val sql = "SELECT c.id, c.name, c.number, c.rarity, c.artist, c.imageSmall, c.imageLarge, c.setId, " +
            "s.name AS setName, s.series AS setSeries, s.releaseDate, c.supertype, c.types " +
            "FROM catalog_cards c LEFT JOIN card_sets s ON s.id = c.setId " +
            "WHERE ${conditions.joinToString(" AND ")} " +
            "ORDER BY ${order.joinToString(", ")} " +
            "LIMIT ${limitFor(input)}"
        return Sql(sql, args)
    }

    fun toSqliteQuery(sql: Sql) = SimpleSQLiteQuery(sql.sql, sql.args.toTypedArray())

    private fun escapeLike(text: String) =
        text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
}

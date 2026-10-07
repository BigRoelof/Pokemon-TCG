package com.example.pokemontcg.data.database

import androidx.sqlite.db.SimpleSQLiteQuery

/**
 * Builds the catalog search. Every word must match the card name (anywhere), the start of a
 * word in the set name, the set code (e.g. `OBF`) or the card number; so "charizard 151",
 * "charizard obf" and "obf 125" all work. Optionally limited to one set.
 */
object CardSearchQuery {

    const val MAX_RESULTS = 100

    // Sets the full set list apart from a search: a whole set can be larger than MAX_RESULTS
    private const val MAX_SET_RESULTS = 1000

    data class Sql(val sql: String, val args: List<String>)

    /** Returns null when there's nothing to search for (no words and no set). */
    fun build(input: String, setId: String? = null): Sql? {
        val words = input.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty() && setId == null) return null
        val phrase = words.joinToString(" ")
        val args = mutableListOf<String>()
        val conditions = mutableListOf<String>()

        if (setId != null) {
            conditions += "c.setId = ?"
            args += setId
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
            "s.name AS setName, s.series AS setSeries, s.releaseDate " +
            "FROM catalog_cards c LEFT JOIN card_sets s ON s.id = c.setId " +
            "WHERE ${conditions.joinToString(" AND ")} " +
            "ORDER BY ${order.joinToString(", ")} " +
            "LIMIT ${if (words.isEmpty()) MAX_SET_RESULTS else MAX_RESULTS}"
        return Sql(sql, args)
    }

    fun toSqliteQuery(sql: Sql) = SimpleSQLiteQuery(sql.sql, sql.args.toTypedArray())

    private fun escapeLike(text: String) =
        text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
}

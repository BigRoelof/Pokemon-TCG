package com.example.pokemontcg.data.database

import androidx.sqlite.db.SimpleSQLiteQuery

/** Builds the catalog search: every word must appear in the card name, in any position. */
object CardSearchQuery {

    const val MAX_RESULTS = 100

    data class Sql(val sql: String, val args: List<String>)

    /** Returns null when there's nothing to search for. */
    fun build(input: String): Sql? {
        val words = input.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return null
        val phrase = words.joinToString(" ")
        val sql = buildString {
            append("SELECT c.id, c.name, c.number, c.rarity, c.artist, c.imageSmall, c.imageLarge, ")
            append("s.name AS setName, s.series AS setSeries, s.releaseDate ")
            append("FROM catalog_cards c LEFT JOIN card_sets s ON s.id = c.setId WHERE ")
            append(words.joinToString(" AND ") { "c.name LIKE ? ESCAPE '\\'" })
            // Exact name first, then names starting with the phrase, then newest sets first
            append(" ORDER BY lower(c.name) = ? DESC, c.name LIKE ? ESCAPE '\\' DESC, s.releaseDate DESC, c.id")
            append(" LIMIT $MAX_RESULTS")
        }
        val args = words.map { "%${escapeLike(it)}%" } + phrase + "${escapeLike(phrase)}%"
        return Sql(sql, args)
    }

    fun toSqliteQuery(sql: Sql) = SimpleSQLiteQuery(sql.sql, sql.args.toTypedArray())

    private fun escapeLike(text: String) =
        text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
}

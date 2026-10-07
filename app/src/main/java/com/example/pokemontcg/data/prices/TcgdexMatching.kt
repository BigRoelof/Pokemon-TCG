package com.example.pokemontcg.data.prices

/** Translates our card ids (from pokemon-tcg-data) to TCGdex card ids. Pure, so it's unit-tested. */
object TcgdexMatching {

    /** Sets TCGdex names or structures differently. */
    private val SET_ALIASES = mapOf(
        "fut20" to "fut2020",
        "me55c" to "30th-c"
    )

    /** Sub-sets that TCGdex keeps as separate sets: our set id + card number prefix -> TCGdex set. */
    private val SUBSET_ALIASES = mapOf(
        ("sm115" to "SV") to "sma"
    )

    fun normalizeName(name: String): String =
        name.lowercase().replace("&", "and").replace(Regex("[^a-z0-9]"), "")

    /** Finds the TCGdex set for one of our sets: alias, then same name, then same id. */
    fun findSetId(ourSetId: String, ourSetName: String, number: String, tcgdexSets: List<TcgdexSetJson>): String? {
        SUBSET_ALIASES.entries.firstOrNull { (key, _) ->
            key.first == ourSetId && number.startsWith(key.second, ignoreCase = true)
        }?.let { return it.value }
        SET_ALIASES[ourSetId]?.let { return it }
        val name = normalizeName(ourSetName)
        tcgdexSets.firstOrNull { normalizeName(it.name) == name }?.let { return it.id }
        return tcgdexSets.firstOrNull { it.id.equals(ourSetId, ignoreCase = true) }?.id
    }

    /** Finds a card by number; TCGdex zero-pads numbers in some sets ("001" vs "1"). */
    fun findCardId(number: String, cards: List<TcgdexCardBriefJson>): String? {
        val wanted = stripLeadingZeros(number)
        return cards.firstOrNull { stripLeadingZeros(it.localId).equals(wanted, ignoreCase = true) }?.id
    }

    /** Cardmarket's trend price, falling back to the 30-day and overall averages. Zero means unknown. */
    fun pickPrice(cardmarket: CardmarketPriceJson?): Double? =
        listOf(cardmarket?.trend, cardmarket?.average30, cardmarket?.avg).firstOrNull { it != null && it > 0 }

    private fun stripLeadingZeros(number: String) = number.trimStart('0').ifEmpty { "0" }
}

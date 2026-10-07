package com.example.pokemontcg.data.model

/**
 * Search filter for a card's type: one of the 11 Pokémon types (matched against the card's
 * `types`) or a non-Pokémon card category (matched against its `supertype`).
 */
enum class CardType(val label: String, val isSupertype: Boolean = false) {
    GRASS("Grass"),
    FIRE("Fire"),
    WATER("Water"),
    LIGHTNING("Lightning"),
    PSYCHIC("Psychic"),
    FIGHTING("Fighting"),
    DARKNESS("Darkness"),
    METAL("Metal"),
    FAIRY("Fairy"),
    DRAGON("Dragon"),
    COLORLESS("Colorless"),
    TRAINER("Trainer", isSupertype = true),
    ENERGY("Energy", isSupertype = true)
}

/** Everything that narrows a catalog search besides the typed words. */
data class SearchFilters(
    val setId: String? = null,
    val type: CardType? = null,
    val rarity: String? = null
) {
    val isEmpty: Boolean get() = setId == null && type == null && rarity == null
}

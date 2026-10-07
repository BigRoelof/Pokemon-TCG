package com.example.pokemontcg.data.model

/** How the chase list is ordered; stored by name in [com.example.pokemontcg.data.preferences.UserPreferences]. */
enum class ChaseSort(val label: String) {
    NEWEST("Newest first"),
    PRICE_HIGH("Price: high to low"),
    PRICE_LOW("Price: low to high"),
    NAME("Name"),
    SET("Set")
}

package com.example.pokemontcg.data.catalog

/** Tidies values from the dataset before they're stored. Pure, so it's unit-tested. */
object CardDataCleanup {

    /**
     * The dataset spells some rarities several ways ("Holo Rare V" / "Rare Holo V",
     * "Rare Holo ex" / "Rare Holo EX", "MEGA_ATTACK_RARE"); this gives each one spelling.
     */
    fun rarity(raw: String?): String? {
        val text = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (text.contains('_')) {
            return text.split('_').joinToString(" ") { word -> word.lowercase().replaceFirstChar { it.uppercase() } }
        }
        val reordered = if (text.startsWith("Holo Rare")) "Rare Holo" + text.removePrefix("Holo Rare") else text
        return if (reordered == "Rare Holo ex") "Rare Holo EX" else reordered
    }

    /** Stores types delimited on both sides (",Fire,") so a LIKE can match one type exactly. */
    fun types(raw: List<String>?): String? =
        raw?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }?.joinToString(",", prefix = ",", postfix = ",")
}

package com.example.pokemontcg.data.model

/**
 * Orders card numbers like a binder: plain numbers numerically (2 before 10), then the rest
 * (GG01, SV49, TG05) alphabetically. Matches the SQL ordering in `CardSearchQuery`.
 */
val CardNumberOrder: Comparator<String> = compareBy<String> { it.toIntOrNull() == null }
    .thenBy { it.toIntOrNull() ?: 0 }
    .thenBy { it.lowercase() }

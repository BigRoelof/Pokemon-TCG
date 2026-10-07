package com.example.pokemontcg.data.model

/** A binder page holds 3 × 3 cards, like a 9-pocket sleeve page. */
const val POCKETS_PER_PAGE = 9

/** The first [count] pockets from [from] onwards that aren't in [occupied], in order. */
fun freePockets(occupied: Set<Int>, from: Int, count: Int): List<Int> {
    val pockets = ArrayList<Int>(count)
    var pocket = from.coerceAtLeast(0)
    while (pockets.size < count) {
        if (pocket !in occupied) pockets += pocket
        pocket++
    }
    return pockets
}

/** Pages needed to show every pocket up to [lastPocket] (null: an empty binder), never fewer than one. */
fun binderPageCount(lastPocket: Int?): Int = if (lastPocket == null) 1 else lastPocket / POCKETS_PER_PAGE + 1

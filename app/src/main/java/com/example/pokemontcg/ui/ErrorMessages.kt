package com.example.pokemontcg.ui

import retrofit2.HttpException
import java.io.IOException

/** Maps network/API failures to a message that makes sense to the user. */
fun Throwable.toUserMessage(): String = when (this) {
    is IOException -> "Couldn't reach the Pokémon TCG service. Check your internet connection."
    is HttpException -> when {
        code() >= 500 -> "The Pokémon TCG service is having trouble right now. Please try again."
        code() == 404 -> "This card couldn't be found."
        code() == 429 -> "Too many requests. Please wait a moment and try again."
        else -> "The search couldn't be processed. Try different search terms."
    }
    else -> "Something went wrong. Please try again."
}

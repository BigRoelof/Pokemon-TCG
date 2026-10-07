package com.example.pokemontcg.ui

import retrofit2.HttpException
import java.io.IOException

/** Maps card database download failures to a message that makes sense to the user. */
fun Throwable.toUserMessage(): String = when (this) {
    is IOException -> "Couldn't download the card database. Check your internet connection."
    is HttpException -> when (code()) {
        403, 429 -> "The card database is busy. Try again in a few minutes."
        in 500..599 -> "The card database server is having trouble. Try again later."
        else -> "Couldn't download the card database (error ${code()})."
    }
    else -> "Couldn't update the card database."
}

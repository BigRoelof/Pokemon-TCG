package com.example.pokemontcg.data.catalog

import retrofit2.http.GET
import retrofit2.http.Path

/**
 * The open-source card data behind the (deprecated) Pokémon TCG API:
 * https://github.com/PokemonTCG/pokemon-tcg-data. One JSON file per set under `cards/en/`.
 */
interface CatalogApi {

    /** Every file in the repository with its content hash, used to download only changed files. */
    @GET("https://api.github.com/repos/PokemonTCG/pokemon-tcg-data/git/trees/master?recursive=1")
    suspend fun getFileTree(): FileTreeJson

    @GET(SETS_PATH)
    suspend fun getSets(): List<SetJson>

    @GET("{path}")
    suspend fun getCards(@Path("path", encoded = true) path: String): List<CardJson>

    companion object {
        const val BASE_URL = "https://raw.githubusercontent.com/PokemonTCG/pokemon-tcg-data/master/"
        const val SETS_PATH = "sets/en.json"
        const val CARDS_DIR = "cards/en/"
    }
}

// Gson ignores Kotlin nullability, so everything the data might omit is nullable.

data class FileTreeJson(val tree: List<FileJson>)

data class FileJson(val path: String, val sha: String)

data class SetJson(
    val id: String,
    val name: String,
    val series: String?,
    val total: Int?,
    val releaseDate: String?,
    val ptcgoCode: String?,
    val images: SetImagesJson?
)

data class SetImagesJson(val symbol: String?, val logo: String?)

data class CardJson(
    val id: String,
    val name: String,
    val number: String,
    val rarity: String?,
    val artist: String?,
    val images: CardImagesJson?
)

data class CardImagesJson(val small: String?, val large: String?)

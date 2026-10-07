package com.example.pokemontcg.data.prices

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * TCGdex (https://tcgdex.dev): free, no API key, embeds Cardmarket and TCGplayer prices per card.
 * Its ids differ from ours (`sv03-125` vs `sv3-125`); see [TcgdexMatching].
 */
interface PriceApi {

    @GET("sets")
    suspend fun getSets(): List<TcgdexSetJson>

    @GET("sets/{id}")
    suspend fun getSet(@Path("id") id: String): TcgdexSetDetailJson

    @GET("cards/{id}")
    suspend fun getCard(@Path("id") id: String): TcgdexCardJson

    companion object {
        const val BASE_URL = "https://api.tcgdex.net/v2/en/"
    }
}

// Gson ignores Kotlin nullability, so everything the API might omit is nullable.

data class TcgdexSetJson(val id: String, val name: String)

data class TcgdexSetDetailJson(val id: String, val cards: List<TcgdexCardBriefJson>?)

data class TcgdexCardBriefJson(val id: String, val localId: String)

data class TcgdexCardJson(val id: String, val pricing: TcgdexPricingJson?)

data class TcgdexPricingJson(val cardmarket: CardmarketPriceJson?)

data class CardmarketPriceJson(
    val updated: String?,
    val idProduct: Int?,
    val avg: Double?,
    val low: Double?,
    val trend: Double?,
    @SerializedName("avg30") val average30: Double?
)

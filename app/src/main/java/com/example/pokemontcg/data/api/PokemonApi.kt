package com.example.pokemontcg.data.api

import com.example.pokemontcg.data.api.model.CardDetailResponse
import com.example.pokemontcg.data.api.model.CardListResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PokemonApi {

    @GET("v2/cards")
    suspend fun searchCards(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): CardListResponse

    @GET("v2/cards/{id}")
    suspend fun getCardDetails(
        @Path("id") id: String
    ): CardDetailResponse

    companion object {
        const val BASE_URL = "https://api.pokemontcg.io/"
    }
}

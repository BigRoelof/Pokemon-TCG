package com.example.pokemontcg.data.api.model

import com.google.gson.annotations.SerializedName

data class CardListResponse(
    val data: List<CardDto>
)

data class CardDetailResponse(
    val data: CardDto
)

data class CardDto(
    val id: String,
    val name: String,
    val number: String,
    val images: CardImages?,
    val set: CardSet?
)

data class CardImages(
    val small: String?,
    val large: String?
)

data class CardSet(
    val name: String
)

package com.example.pokemontcg.data.repository

import com.example.pokemontcg.data.api.PokemonApi
import com.example.pokemontcg.data.api.model.CardDto
import com.example.pokemontcg.data.database.ChaseCardEntity
import com.example.pokemontcg.data.database.PokemonDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.IOException

class PokemonRepository(
    private val api: PokemonApi,
    private val dao: PokemonDao
) {

    val chaseCards: Flow<List<ChaseCardEntity>> = dao.getAllChaseCards()

    fun isCardInChaseList(cardId: String): Flow<Boolean> = dao.isCardInChaseList(cardId)

    suspend fun searchCards(query: String): Result<List<CardDto>> {
        return withContext(Dispatchers.IO) {
            try {
                // Ensure query is formatted for API, e.g. name:charizard*
                val searchQuery = if (query.contains(":")) query else "name:*$query*"
                val response = api.searchCards(searchQuery)
                Result.success(response.data)
            } catch (e: Exception) {
                // Catch network and serialization errors
                Result.failure(e)
            }
        }
    }

    suspend fun getCardDetails(cardId: String): Result<CardDto> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getCardDetails(cardId)
                Result.success(response.data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun addCardToChaseList(card: CardDto) {
        withContext(Dispatchers.IO) {
            val entity = ChaseCardEntity(
                id = card.id,
                name = card.name,
                setName = card.set?.name ?: "Unknown Set",
                number = card.number,
                imageUrl = card.images?.small ?: "",
                largeImageUrl = card.images?.large ?: "",
                obtained = false,
                dateAdded = System.currentTimeMillis()
            )
            dao.insertCard(entity)
        }
    }

    suspend fun removeCardFromChaseList(cardId: String) {
        withContext(Dispatchers.IO) {
            dao.deleteCardById(cardId)
        }
    }
}

package com.example.pokemontcg.data.repository

import com.example.pokemontcg.data.api.PokemonApi
import com.example.pokemontcg.data.api.model.CardDto
import com.example.pokemontcg.data.database.ChaseCardEntity
import com.example.pokemontcg.data.database.PokemonDao
import com.example.pokemontcg.data.api.model.CardImages
import com.example.pokemontcg.data.api.model.CardSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class PokemonRepository(
    private val api: PokemonApi,
    private val dao: PokemonDao
) {

    val chaseCards: Flow<List<ChaseCardEntity>> = dao.getAllChaseCards()

    /** Emits the saved card, or null while it isn't on the chase list. */
    fun observeSavedCard(cardId: String): Flow<ChaseCardEntity?> = dao.observeCardById(cardId)

    suspend fun searchCards(query: String): Result<List<CardDto>> {
        return withContext(Dispatchers.IO) {
            runCatchingWithRetry {
                api.searchCards(buildSearchQuery(query)).data
            }
        }
    }

    suspend fun getCardDetails(cardId: String): Result<CardDto> {
        return withContext(Dispatchers.IO) {
            runCatchingWithRetry {
                api.getCardDetails(cardId).data
            }
        }
    }

    /** Returns the locally saved copy of a card, so saved cards can be shown offline. */
    suspend fun getSavedCard(cardId: String): CardDto? {
        return withContext(Dispatchers.IO) {
            dao.getCardById(cardId)?.let { entity ->
                CardDto(
                    id = entity.id,
                    name = entity.name,
                    number = entity.number,
                    images = CardImages(small = entity.imageUrl, large = entity.largeImageUrl),
                    set = CardSet(name = entity.setName)
                )
            }
        }
    }

    /**
     * The API is flaky and regularly answers valid requests with a 5xx, so server errors
     * are retried a few times. Client errors and network failures are returned immediately.
     */
    private suspend fun <T> runCatchingWithRetry(block: suspend () -> T): Result<T> {
        repeat(MAX_ATTEMPTS - 1) { attempt ->
            try {
                return Result.success(block())
            } catch (e: HttpException) {
                if (e.code() < 500) return Result.failure(e)
                delay(RETRY_DELAY_MS * (attempt + 1))
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }
        return runCatching { block() }
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

    suspend fun setObtained(cardId: String, obtained: Boolean) {
        withContext(Dispatchers.IO) {
            dao.setObtained(cardId, obtained)
        }
    }

    suspend fun removeCardFromChaseList(cardId: String) {
        withContext(Dispatchers.IO) {
            dao.deleteCardById(cardId)
        }
    }

    companion object {
        private const val MAX_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 500L

        /**
         * Plain text becomes a quoted prefix match on the card name, e.g. `name:"charizard ex*"`.
         * Unquoted input containing spaces or punctuation is rejected by the API. Input that
         * already contains `:` is treated as a raw API query.
         */
        fun buildSearchQuery(query: String): String {
            val trimmed = query.trim()
            if (trimmed.contains(":")) return trimmed
            val escaped = trimmed.replace("\\", "\\\\").replace("\"", "\\\"")
            return "name:\"$escaped*\""
        }
    }
}

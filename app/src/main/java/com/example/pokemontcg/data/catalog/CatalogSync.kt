package com.example.pokemontcg.data.catalog

import com.example.pokemontcg.data.database.CardSetEntity
import com.example.pokemontcg.data.database.CatalogCardEntity
import com.example.pokemontcg.data.database.CatalogDao
import com.example.pokemontcg.data.database.CatalogFileEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import retrofit2.HttpException
import java.util.concurrent.atomic.AtomicInteger

sealed class SyncState {
    object Idle : SyncState()
    /** [done] of [total] changed set files are processed. */
    data class Running(val done: Int, val total: Int) : SyncState()
    object UpToDate : SyncState()
    data class Failed(val error: Throwable) : SyncState()
}

/**
 * Keeps the local card catalog in step with the open-source dataset. One GitHub API call lists
 * every file with its content hash; only set files that are new or changed get downloaded.
 */
class CatalogSync(
    private val api: CatalogApi,
    private val dao: CatalogDao,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow<SyncState>(SyncState.Idle)
    val state: StateFlow<SyncState> = _state.asStateFlow()

    private var job: Job? = null

    /** Starts a sync unless one is already running. */
    fun sync() {
        if (job?.isActive == true) return
        job = scope.launch {
            _state.value = SyncState.Running(done = 0, total = 0)
            _state.value = try {
                runSync()
            } catch (e: Exception) {
                SyncState.Failed(e)
            }
        }
    }

    private suspend fun runSync(): SyncState {
        val remoteFiles = withRetry { api.getFileTree() }.tree.associate { it.path to it.sha }
        val localFiles = dao.getFiles().associate { it.path to it.sha }
        fun isChanged(path: String) = remoteFiles[path] != localFiles[path]

        val sets = withRetry { api.getSets() }
        if (isChanged(CatalogApi.SETS_PATH)) {
            dao.upsertSets(sets.map { it.toEntity() })
            dao.upsertFile(CatalogFileEntity(CatalogApi.SETS_PATH, remoteFiles.getValue(CatalogApi.SETS_PATH)))
        }

        // Newest sets first: they're the ones people search for most
        val changedSets = sets
            .sortedByDescending { it.releaseDate }
            .map { it.id to "${CatalogApi.CARDS_DIR}${it.id}.json" }
            .filter { (_, path) -> path in remoteFiles && isChanged(path) }
        if (changedSets.isEmpty()) return SyncState.UpToDate

        val done = AtomicInteger(0)
        val failures = mutableListOf<Throwable>()
        val downloads = Semaphore(PARALLEL_DOWNLOADS)
        _state.value = SyncState.Running(done = 0, total = changedSets.size)
        changedSets.map { (setId, path) ->
            scope.async {
                downloads.withPermit {
                    try {
                        val cards = withRetry { api.getCards(path) }
                        dao.replaceSetCards(
                            setId = setId,
                            cards = cards.map { it.toEntity(setId) },
                            file = CatalogFileEntity(path, remoteFiles.getValue(path))
                        )
                    } catch (e: Exception) {
                        // Leave the hash unrecorded so the next sync retries this set
                        synchronized(failures) { failures += e }
                    }
                    val count = done.incrementAndGet()
                    _state.update { SyncState.Running(done = count, total = changedSets.size) }
                }
            }
        }.awaitAll()

        return failures.firstOrNull()?.let { SyncState.Failed(it) } ?: SyncState.UpToDate
    }

    /** Retries server errors (5xx) a few times; other failures are thrown straight away. */
    private suspend fun <T> withRetry(block: suspend () -> T): T {
        repeat(MAX_ATTEMPTS - 1) { attempt ->
            try {
                return block()
            } catch (e: HttpException) {
                if (e.code() < 500) throw e
                delay(RETRY_DELAY_MS * (attempt + 1))
            }
        }
        return block()
    }

    private companion object {
        const val PARALLEL_DOWNLOADS = 4
        const val MAX_ATTEMPTS = 3
        const val RETRY_DELAY_MS = 1000L
    }
}

private fun SetJson.toEntity() = CardSetEntity(
    id = id,
    name = name,
    series = series,
    releaseDate = releaseDate,
    total = total,
    logoUrl = images?.logo,
    symbolUrl = images?.symbol,
    ptcgoCode = ptcgoCode
)

private fun CardJson.toEntity(setId: String) = CatalogCardEntity(
    id = id,
    setId = setId,
    name = name,
    number = number,
    rarity = CardDataCleanup.rarity(rarity),
    artist = artist,
    imageSmall = images?.small,
    imageLarge = images?.large,
    supertype = supertype,
    types = CardDataCleanup.types(types)
)

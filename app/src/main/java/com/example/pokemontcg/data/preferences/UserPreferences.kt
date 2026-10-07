package com.example.pokemontcg.data.preferences

import android.content.Context
import androidx.core.content.edit
import com.example.pokemontcg.data.model.CardSort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Small settings that should survive restarts, kept in SharedPreferences. */
class UserPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)

    private val _chaseSort = MutableStateFlow(readSort(KEY_CHASE_SORT))
    val chaseSort: StateFlow<CardSort> = _chaseSort.asStateFlow()

    private val _collectionSort = MutableStateFlow(readSort(KEY_COLLECTION_SORT))
    val collectionSort: StateFlow<CardSort> = _collectionSort.asStateFlow()

    fun setChaseSort(sort: CardSort) {
        prefs.edit { putString(KEY_CHASE_SORT, sort.name) }
        _chaseSort.value = sort
    }

    fun setCollectionSort(sort: CardSort) {
        prefs.edit { putString(KEY_COLLECTION_SORT, sort.name) }
        _collectionSort.value = sort
    }

    private fun readSort(key: String): CardSort =
        prefs.getString(key, null)?.let { name -> CardSort.entries.firstOrNull { it.name == name } } ?: CardSort.NEWEST

    private companion object {
        const val KEY_CHASE_SORT = "chase_sort"
        const val KEY_COLLECTION_SORT = "collection_sort"
    }
}

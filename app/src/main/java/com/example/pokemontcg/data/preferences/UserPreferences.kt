package com.example.pokemontcg.data.preferences

import android.content.Context
import androidx.core.content.edit
import com.example.pokemontcg.data.model.ChaseSort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Small settings that should survive restarts, kept in SharedPreferences. */
class UserPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)

    private val _chaseSort = MutableStateFlow(
        prefs.getString(KEY_CHASE_SORT, null)
            ?.let { name -> ChaseSort.entries.firstOrNull { it.name == name } }
            ?: ChaseSort.NEWEST
    )
    val chaseSort: StateFlow<ChaseSort> = _chaseSort.asStateFlow()

    fun setChaseSort(sort: ChaseSort) {
        prefs.edit { putString(KEY_CHASE_SORT, sort.name) }
        _chaseSort.value = sort
    }

    private companion object {
        const val KEY_CHASE_SORT = "chase_sort"
    }
}

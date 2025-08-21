package com.zelretch.aniiiiiict.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.annict.type.SeasonName
import com.annict.type.StatusState
import com.zelretch.aniiiiiict.domain.filter.FilterState
import com.zelretch.aniiiiiict.domain.filter.SortOrder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.filterDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "filter_preferences"
)

@Singleton
class FilterPreferences @Inject constructor(@ApplicationContext private val context: Context) {
    private val dataStore = context.filterDataStore

    private object PreferencesKeys {
        val SELECTED_MEDIA = stringPreferencesKey("selected_media")
        val SELECTED_SEASON = stringPreferencesKey("selected_season")
        val SELECTED_YEAR = stringPreferencesKey("selected_year")
        val SELECTED_CHANNEL = stringPreferencesKey("selected_channel")
        val SELECTED_STATUS = stringPreferencesKey("selected_status")
        val SEARCH_QUERY = stringPreferencesKey("search_query")
        val SHOW_ONLY_AIRED = booleanPreferencesKey("show_only_aired")
        val SORT_ORDER = stringPreferencesKey("sort_order")
    }

    val filterState: Flow<FilterState> = dataStore.data.map { preferences ->
        FilterState(
            selectedMedia = preferences[PreferencesKeys.SELECTED_MEDIA]?.split(",")?.filter { it.isNotEmpty() }
                ?.toSet() ?: emptySet(),
            selectedSeason = preferences[PreferencesKeys.SELECTED_SEASON]?.split(",")?.filter { it.isNotEmpty() }
                ?.mapNotNull { runCatching { SeasonName.valueOf(it) }.getOrNull() }?.toSet() ?: emptySet(),
            selectedYear = preferences[PreferencesKeys.SELECTED_YEAR]?.split(",")?.filter { it.isNotEmpty() }
                ?.mapNotNull { it.toIntOrNull() }?.filter { it > 0 }?.toSet() ?: emptySet(),
            selectedChannel = preferences[PreferencesKeys.SELECTED_CHANNEL]?.split(",")?.filter {
                it.isNotEmpty()
            }?.toSet() ?: emptySet(),
            selectedStatus = preferences[PreferencesKeys.SELECTED_STATUS]?.split(",")?.filter { it.isNotEmpty() }
                ?.mapNotNull { runCatching { StatusState.valueOf(it) }.getOrNull() }?.toSet() ?: emptySet(),
            searchQuery = preferences[PreferencesKeys.SEARCH_QUERY] ?: "",
            showOnlyAired = preferences[PreferencesKeys.SHOW_ONLY_AIRED] != false,
            sortOrder = preferences[PreferencesKeys.SORT_ORDER]?.let {
                runCatching { SortOrder.valueOf(it) }.getOrNull()
            } ?: SortOrder.START_TIME_DESC
        )
    }

    suspend fun updateFilterState(filterState: FilterState) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_MEDIA] = filterState.selectedMedia.joinToString(",")
            preferences[PreferencesKeys.SELECTED_SEASON] = filterState.selectedSeason.joinToString(",") { it.name }
            preferences[PreferencesKeys.SELECTED_YEAR] = filterState.selectedYear.filter { it > 0 }.joinToString(",")
            preferences[PreferencesKeys.SELECTED_CHANNEL] = filterState.selectedChannel.joinToString(",")
            preferences[PreferencesKeys.SELECTED_STATUS] = filterState.selectedStatus.joinToString(",") { it.name }
            preferences[PreferencesKeys.SEARCH_QUERY] = filterState.searchQuery
            preferences[PreferencesKeys.SHOW_ONLY_AIRED] = filterState.showOnlyAired
            preferences[PreferencesKeys.SORT_ORDER] = filterState.sortOrder.name
        }
    }
}

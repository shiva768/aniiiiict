package com.zelretch.aniiiiict.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.annict.type.SeasonName
import com.annict.type.StatusState
import com.zelretch.aniiiiict.data.model.LibraryFetchParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private const val DEFAULT_SEASON_FROM_YEARS_AGO = 5

private val Context.libraryDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "library_preferences"
)

@Singleton
class LibraryPreferences @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val dataStore = context.libraryDataStore

    private object Keys {
        val SELECTED_STATES = stringPreferencesKey("selected_states")
        val SEASON_FROM_YEAR = intPreferencesKey("season_from_year")
        val SEASON_FROM_NAME = stringPreferencesKey("season_from_name")
    }

    val fetchPrefs: Flow<LibraryFetchParams> = dataStore.data.map { preferences ->
        val defaultYear = LocalDate.now().year - DEFAULT_SEASON_FROM_YEARS_AGO
        val states = preferences[Keys.SELECTED_STATES]
            ?.split(",")
            ?.filter { it.isNotEmpty() }
            ?.mapNotNull { runCatching { StatusState.valueOf(it) }.getOrNull() }
            ?: listOf(StatusState.WANNA_WATCH, StatusState.ON_HOLD)
        val year = preferences[Keys.SEASON_FROM_YEAR] ?: defaultYear
        val seasonName = preferences[Keys.SEASON_FROM_NAME]
            ?.let { runCatching { SeasonName.valueOf(it) }.getOrNull() }
            ?: SeasonName.SPRING
        LibraryFetchParams(
            selectedStates = states,
            seasonFromYear = year,
            seasonFromName = seasonName
        )
    }

    suspend fun updateFetchPrefs(params: LibraryFetchParams) {
        dataStore.edit { preferences ->
            preferences[Keys.SELECTED_STATES] = params.selectedStates.joinToString(",") { it.name }
            preferences[Keys.SEASON_FROM_YEAR] = params.seasonFromYear
            preferences[Keys.SEASON_FROM_NAME] = params.seasonFromName.name
        }
    }
}

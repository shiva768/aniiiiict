package com.zelretch.aniiiiiict.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.workImageDataStore: DataStore<Preferences> by preferencesDataStore(name = "work_image_preferences")

@Singleton
class WorkImagePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.workImageDataStore

    suspend fun saveWorkImage(workId: Long, imageUrl: String) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(workId.toString())] = imageUrl
        }
    }

    fun getWorkImage(workId: Long): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[stringPreferencesKey(workId.toString())]
        }
    }

}
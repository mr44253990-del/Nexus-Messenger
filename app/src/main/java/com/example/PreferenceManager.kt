package com.example

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object PreferenceManager {
    private val KEY_ONBOARDING = booleanPreferencesKey("onboarding_completed_key")
    private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme_key")

    fun isOnboardingCompleted(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_ONBOARDING] == true
        }
    }

    suspend fun setOnboardingCompleted(context: Context) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING] = true
        }
    }

    fun isDarkTheme(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_DARK_THEME] ?: true
        }
    }

    suspend fun setDarkTheme(context: Context, isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DARK_THEME] = isDark
        }
    }
}

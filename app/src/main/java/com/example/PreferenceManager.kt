package com.example

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.example.ui.theme.AppTheme

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object PreferenceManager {
    private val KEY_ONBOARDING = booleanPreferencesKey("onboarding_completed_key")
    private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme_key")
    private val KEY_THEME = stringPreferencesKey("app_theme_key")
    private val KEY_MUTED_USERS = stringPreferencesKey("muted_users_json")
    private val KEY_NOTIFICATION_SOUND = booleanPreferencesKey("notification_sound_key")
    private val KEY_NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled_key")

    fun isOnboardingCompleted(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences -> preferences[KEY_ONBOARDING] == true }
    }
    suspend fun setOnboardingCompleted(context: Context) {
        context.dataStore.edit { preferences -> preferences[KEY_ONBOARDING] = true }
    }

    fun isDarkTheme(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences -> preferences[KEY_DARK_THEME] ?: true }
    }
    suspend fun setDarkTheme(context: Context, isDark: Boolean) {
        context.dataStore.edit { preferences -> preferences[KEY_DARK_THEME] = isDark }
    }

    fun getTheme(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_THEME] ?: AppTheme.PINK_GLASS.key
        }
    }
    suspend fun setTheme(context: Context, themeKey: String) {
        context.dataStore.edit { preferences -> preferences[KEY_THEME] = themeKey }
    }

    fun getMutedUsers(context: Context): Flow<Set<String>> {
        return context.dataStore.data.map { preferences ->
            val json = preferences[KEY_MUTED_USERS] ?: "[]"
            try {
                org.json.JSONArray(json).let { arr ->
                    mutableSetOf<String>().apply {
                        for (i in 0 until arr.length()) { add(arr.getString(i)) }
                    }
                }
            } catch (e: Exception) { emptySet() }
        }
    }

    suspend fun toggleMuteUser(context: Context, uid: String) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[KEY_MUTED_USERS] ?: "[]"
            val currentSet = try {
                org.json.JSONArray(currentJson).let { arr ->
                    mutableSetOf<String>().apply {
                        for (i in 0 until arr.length()) { add(arr.getString(i)) }
                    }
                }
            } catch (e: Exception) { mutableSetOf() }

            if (currentSet.contains(uid)) {
                currentSet.remove(uid)
            } else {
                currentSet.add(uid)
            }
            preferences[KEY_MUTED_USERS] = org.json.JSONArray(currentSet.toList()).toString()
        }
    }

    fun isNotificationSoundEnabled(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences -> preferences[KEY_NOTIFICATION_SOUND] ?: true }
    }
    suspend fun setNotificationSound(context: Context, enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[KEY_NOTIFICATION_SOUND] = enabled }
    }

    fun isNotificationEnabled(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences -> preferences[KEY_NOTIFICATION_ENABLED] ?: true }
    }
    suspend fun setNotificationEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[KEY_NOTIFICATION_ENABLED] = enabled }
    }
}
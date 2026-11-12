package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "smart_calc_prefs")

class PreferencesRepository(private val context: Context) {
  private object Keys {
    val DARK_MODE = booleanPreferencesKey("dark_mode")
    val DEGREE_MODE = booleanPreferencesKey("degree_mode")
  }

  val darkModePreference: Flow<Boolean?> =
    context.dataStore.data.map { prefs -> prefs[Keys.DARK_MODE] }

  val isDegreeMode: Flow<Boolean> =
    context.dataStore.data.map { prefs -> prefs[Keys.DEGREE_MODE] ?: true }

  suspend fun setDarkMode(isDark: Boolean) {
    context.dataStore.edit { prefs -> prefs[Keys.DARK_MODE] = isDark }
  }

  suspend fun setDegreeMode(isDegree: Boolean) {
    context.dataStore.edit { prefs -> prefs[Keys.DEGREE_MODE] = isDegree }
  }
}

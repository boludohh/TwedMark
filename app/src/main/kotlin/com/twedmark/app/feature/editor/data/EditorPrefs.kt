package com.twedmark.app.feature.editor.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Gestiona las preferencias del editor usando DataStore.
 */
class EditorPrefs(private val dataStore: DataStore<Preferences>) {
    
    companion object {
        val KEY_WORD_WRAP = booleanPreferencesKey("editor_word_wrap")
        val KEY_LINE_NUMBERS = booleanPreferencesKey("editor_line_numbers")
    }
    
    val wordWrapFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_WORD_WRAP] ?: true
    }
    
    val lineNumbersFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_LINE_NUMBERS] ?: true
    }
    
    suspend fun setWordWrap(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_WORD_WRAP] = enabled
        }
    }
    
    suspend fun setLineNumbers(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_LINE_NUMBERS] = enabled
        }
    }
}
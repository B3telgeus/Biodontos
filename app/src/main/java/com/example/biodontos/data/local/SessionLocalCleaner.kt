package com.example.biodontos.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SessionLocalCleaner(
    private val database: BiodontosDatabase,
    private val context: Context
) {
    suspend fun clearSessionData(): Unit = withContext(Dispatchers.IO) {
        database.clearAllTables()
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}

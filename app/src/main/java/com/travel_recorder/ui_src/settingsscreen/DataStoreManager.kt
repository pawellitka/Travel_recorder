package com.travel_recorder.ui_src.settingsscreen

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.preferencesDataStore
import com.travel_recorder.service.TrackingService.Companion.DEFAULT_TRACKING_INTERVAL
import com.travel_recorder.service.TrackingService.Companion.TRACKING_INTERVAL_UNIT_CONVERSION
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

val Context.dataStore by preferencesDataStore(name = "settings")

class DataStoreManager(private val context: Context) {

    companion object {
        val MS_INTERVAL = longPreferencesKey("ms_interval")
    }

    suspend fun saveInterval(interval_min: Long) {
        context.dataStore.edit { preferences ->
            preferences[MS_INTERVAL] = interval_min * TRACKING_INTERVAL_UNIT_CONVERSION
        }
    }

    fun getIntervalWithCallback(callback: (Long) -> Unit) {
        val job = SupervisorJob()
        val scope = CoroutineScope(Dispatchers.IO + job)
        scope.launch {
            callback(getInterval())
        }
    }

    @Composable
    fun composableGetInterval() : Long {
        return (intervalFlow.collectAsState(initial = DEFAULT_TRACKING_INTERVAL).value ?: DEFAULT_TRACKING_INTERVAL) / TRACKING_INTERVAL_UNIT_CONVERSION
    }

    private val intervalFlow: Flow<Long?> = context.dataStore.data
        .map { preferences ->
            preferences[MS_INTERVAL]
        }

    private suspend fun getInterval(): Long {
        return context.dataStore.data
            .map { prefs -> prefs[MS_INTERVAL] ?: 0 }.first().toLong()
    }

}

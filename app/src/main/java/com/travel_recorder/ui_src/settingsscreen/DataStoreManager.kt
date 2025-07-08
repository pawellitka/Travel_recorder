package com.travel_recorder.ui_src.settingsscreen

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.preferencesDataStore
import com.travel_recorder.service.TrackingService.Companion.DEFAULT_TRACKING_INTERVAL
import com.travel_recorder.service.TrackingService.Companion.TRACKING_INTERVAL_UNIT_CONVERSION
import com.travel_recorder.web_server.KtorServer.DEFAULT_WEB_PORT
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

val Context.dataStore by preferencesDataStore(name = "settings")

class DataStoreManager(private val context: Context) {

    suspend fun saveInterval(intervalMin : Int) {
        context.dataStore.edit { preferences ->
            preferences[MS_INTERVAL] = intervalMin * TRACKING_INTERVAL_UNIT_CONVERSION
        }
    }

    suspend fun saveWebPort(port : Int) {
        context.dataStore.edit { preferences ->
            preferences[WEB_PORT] = port
        }
    }

    fun getIntervalWithCallback(callback : (Int) -> Unit) {
        val job = SupervisorJob()
        val scope = CoroutineScope(Dispatchers.IO + job)
        scope.launch {
            callback(getIntPreference(MS_INTERVAL))
        }
    }

    @Composable
    fun composableGetInterval() : Int {
        return (getFlow(MS_INTERVAL).collectAsState(initial = DEFAULT_TRACKING_INTERVAL).value ?: DEFAULT_TRACKING_INTERVAL) / TRACKING_INTERVAL_UNIT_CONVERSION
    }

    @Composable
    fun composableGetPort() : Int {
        return (getFlow(WEB_PORT).collectAsState(initial = DEFAULT_WEB_PORT).value ?: DEFAULT_WEB_PORT)
    }

    private fun getFlow(key : Preferences.Key<Int>) : Flow<Int?> {
        return context.dataStore.data
            .map { preferences ->
                preferences[key]
            }
    }

    private suspend fun getIntPreference(key : Preferences.Key<Int>) : Int {
        return context.dataStore.data
            .map { prefs -> prefs[key] ?: 0 }.first()
    }

    companion object {
        val MS_INTERVAL = intPreferencesKey("ms_interval")
        val WEB_PORT = intPreferencesKey("web_port")
    }
}

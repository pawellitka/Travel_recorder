package com.travel_recorder.ui_src.settingsscreen

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.travel_recorder.R
import com.travel_recorder.service.restartIfRunningService
import com.travel_recorder.ui.theme.TravelRecorderTheme
import com.travel_recorder.web_server.KtorServer.restartServer

suspend fun onSetInterval(intervalMin : Int, context : Context, dataStoreManager : DataStoreManager) {
    dataStoreManager.saveInterval(intervalMin)
    restartIfRunningService(context)
}

suspend fun onSetPort(port : Int, context : Context, dataStoreManager : DataStoreManager) {
    dataStoreManager.saveWebPort(port)
    restartServer(context, port)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen(context : Context, dataStoreManager : DataStoreManager, onBack: () -> Unit) {
    TravelRecorderTheme {
        Column(modifier = Modifier) {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
            Surface(modifier = Modifier.fillMaxHeight()) {
                Column(modifier = Modifier) {
                    PreferenceSelector(
                        title = stringResource(R.string.location_check_interval),
                        value = dataStoreManager.composableGetInterval(),
                        stringResource(R.string.minutes),
                        onUpdate = { onSetInterval(it, context, dataStoreManager) },
                        validValues = 1..360,
                    )
                    PreferenceSelector(
                        title = stringResource(R.string.web_interface_port),
                        value = dataStoreManager.composableGetPort(),
                        "",
                        onUpdate = { onSetPort(it, context, dataStoreManager) },
                        validValues = 0..65535,
                    )
                }
            }
        }
    }
}
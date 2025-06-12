package com.travel_recorder.ui_src.popups

import android.content.Context
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.travel_recorder.R
import com.travel_recorder.database.Database

@Composable
fun Removing(trackName : String, context : Context, dataBase : Database, closingCallback: () -> (Unit)) {
    AlertDialog(
        title = {
            Text(String.format(stringResource(R.string.delete_warning), trackName))
        },
        text = {},
        onDismissRequest = closingCallback,
        dismissButton = {
            TextButton(onClick = closingCallback) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                dataBase.deleteTravel(trackName)
                closingCallback()
            }) {
                Text(stringResource(R.string.ok))
            }
        },
    )
}
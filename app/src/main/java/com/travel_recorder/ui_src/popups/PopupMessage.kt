package com.travel_recorder.ui_src.popups

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.travel_recorder.R

@Composable
fun PopupMessage(messageText : String, closingCallback: () -> (Unit)) {
    AlertDialog(
        title = {
            Text(messageText)
        },
        onDismissRequest = closingCallback,
        confirmButton = {
            TextButton(onClick = closingCallback) {
                Text(stringResource(R.string.ok))
            }
        },
        properties = DialogProperties(
            dismissOnClickOutside = true,
            dismissOnBackPress = true,
        ),
    )
}
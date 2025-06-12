package com.travel_recorder.ui_src

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.travel_recorder.R
import com.travel_recorder.database.Database
import com.travel_recorder.viewmodel.GoogleMapViewModel

@Composable
fun Saving(context : Context, dataBase : Database, gmapViewModel : GoogleMapViewModel?, closingCallback: (Boolean) -> (Unit)) {
    var nameOfSave by remember { mutableStateOf("") }
    AlertDialog(
        title = {
            Text(stringResource(R.string.saving_title))
        },
        text = {
            Column {
                OutlinedTextField(
                    value = nameOfSave,
                    onValueChange = { nameOfSave = it },
                )
            }
        },
        onDismissRequest = {},
        dismissButton = {
            TextButton(onClick = { closingCallback(false) }) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                var nameAlreadyPresent = false
                dataBase.checkName(nameOfSave).run {
                    this.use {
                        if (this.moveToFirst()) {
                            do {
                                nameAlreadyPresent = true
                            } while (this.moveToNext())
                        }
                    }
                }
                if (!nameAlreadyPresent) {
                    dataBase.saveTravel(nameOfSave, gmapViewModel?.getTrack())
                    gmapViewModel?.setTrack(nameOfSave)
                }
                closingCallback(nameAlreadyPresent)
            }) {
                Text(stringResource(R.string.ok))
            }
        },
    )
}
package com.travel_recorder.ui_src.popups

import android.content.Context
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.travel_recorder.R
import com.travel_recorder.database.Database
import com.travel_recorder.viewmodel.GoogleMapViewModel

@Composable
fun Loading(context : Context,
            dataBase : Database,
            gmapViewModel : GoogleMapViewModel?,
            removalMenu : Boolean,
            closingCallback: () -> (Unit),
            removingCallback: () -> (Unit)) {
    AlertDialog(
        title = {
            Text(stringResource(R.string.loadChoice_title))
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                dataBase.loadNames().run {
                    this.use {
                        if (this.moveToFirst()) {
                            do {
                                val label = this.getString(
                                    this.getColumnIndexOrThrow(Database.NAME_COLUMN)
                                )
                                Text(
                                    text = this.getString(
                                        this.getColumnIndexOrThrow(
                                            Database.NAME_COLUMN
                                        )
                                    ),
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .clickable {
                                            if (removalMenu) {
                                                removing(label, context, dataBase, removingCallback)
                                            } else {
                                                closingCallback()
                                                dataBase.resetTravel()
                                                gmapViewModel?.setShownTrack(
                                                    label
                                                )
                                            }
                                        }
                                        .height(50.dp)
                                        .fillMaxWidth()
                                )
                            } while (this.moveToNext())
                        }
                    }
                }
            }
        },
        onDismissRequest = closingCallback,
        dismissButton = {
            TextButton(
                onClick = closingCallback
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {},
    )
}
package com.travel_recorder.ui_src

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.travel_recorder.R
import com.travel_recorder.database.Database

fun removing(trackName : String, context : Context, dataBase : Database, callback: () -> (Unit)) {
    var confirmed = false
    AlertDialog.Builder(context).also {
        it.setTitle(String.format(context.getResources().getString(R.string.delete_warning), trackName))
        it.setPositiveButton(R.string.ok) { _, _ ->
            confirmed = true
        }
        it.setNegativeButton(R.string.cancel) { dialog, _ ->
            confirmed = false
            dialog.cancel()
        }
        it.setOnDismissListener {
            if(confirmed) {
                dataBase.deleteTravel(trackName)
                callback()
            }
        }
        it.show()
    }
}
package com.travel_recorder.ui_src

import android.content.Context
import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.travel_recorder.R
import com.travel_recorder.database.Database
import com.travel_recorder.viewmodel.GoogleMapViewModel

fun saving(context : Context, dataBase : Database, gmapViewModel : GoogleMapViewModel?) {
    var confirmed = false
    AlertDialog.Builder(context).also {
        it.setTitle(R.string.loadChoice_title)
        val input = EditText(context).apply {
            this.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_CLASS_TEXT
            it.setView(this)
        }
        it.setPositiveButton(R.string.ok) { _, _ ->
            confirmed = true
        }
        it.setNegativeButton(R.string.cancel) { dialog, _ ->
            confirmed = false
            dialog.cancel()
        }
        it.setOnDismissListener {
            if(confirmed) {
                var nameAlreadyPresent = false
                dataBase.checkName(input.text.toString()).run {
                    this.use {
                        if (this.moveToFirst()) {
                            do {
                                nameAlreadyPresent = true
                            } while (this.moveToNext())
                        }
                    }
                }
                if(!nameAlreadyPresent)
                    dataBase.saveTravel(input.text.toString(), gmapViewModel?.track)
                else {
                    AlertDialog.Builder(context).create().apply {
                        this.setMessage(context.resources.getString(R.string.saving_not_finalized_warning))
                        this.setCancelable(true)
                        this.show()
                    }
                }
            }
        }
        it.show()
    }
}
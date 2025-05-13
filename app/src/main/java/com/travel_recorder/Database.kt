package com.travel_recorder

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.Instant
import java.time.temporal.ChronoField

class Database(context: Context, factory: SQLiteDatabase.CursorFactory?) :
    SQLiteOpenHelper(context, "travel_recorder", factory, 1)  {

    override fun onCreate(dataBase: SQLiteDatabase) {
        val createQuery = """
            CREATE TABLE $TRAVEL_TABLE_NAME (
                $ID_COLUMN INTEGER PRIMARY KEY AUTOINCREMENT,
                $TIME_COLUMN INTEGER,
                $LAT_COLUMN NUMERIC,
                $LON_COLUMN NUMERIC,
                $NAME_COLUMN TEXT
            )
        """.trimIndent()
        dataBase.execSQL(createQuery)
    }

    override fun onUpgrade(dataBase: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        dataBase.execSQL("DROP TABLE IF EXISTS $TRAVEL_TABLE_NAME")
        onCreate(dataBase)
    }

    fun saveLocation(latitude : Double, longitude : Double) {
        val values = ContentValues().apply {
            put(TIME_COLUMN, Instant.now().getLong(ChronoField.INSTANT_SECONDS))
            put(LAT_COLUMN, latitude)
            put(LON_COLUMN, longitude)
            put(NAME_COLUMN, null as String?)
        }
        writableDatabase.use { dataBase ->
            dataBase.insert(TRAVEL_TABLE_NAME, null, values)
        }
    }

    fun resetTravel() {
        val deleteQuery = """
            DELETE FROM $TRAVEL_TABLE_NAME WHERE $NAME_COLUMN IS NULL
        """.trimIndent()
        writableDatabase.use { dataBase ->
            dataBase.execSQL(deleteQuery)
        }
    }

    fun checkName(travelName : String): Cursor {
        return readableDatabase.rawQuery("SELECT $NAME_COLUMN FROM $TRAVEL_TABLE_NAME WHERE $NAME_COLUMN = ?", arrayOf(travelName))
    }

    fun saveTravel(targetName : String?, sourceName : String?) {
        val deleteQuery = """
            UPDATE $TRAVEL_TABLE_NAME SET $NAME_COLUMN = "$targetName" WHERE $NAME_COLUMN = "$sourceName" OR $NAME_COLUMN IS NULL
        """.trimIndent()
        writableDatabase.use { dataBase ->
            dataBase.execSQL(deleteQuery)
        }
    }

    fun loadNames(): Cursor{
        return readableDatabase.rawQuery("SELECT $NAME_COLUMN FROM $TRAVEL_TABLE_NAME WHERE $NAME_COLUMN IS NOT NULL GROUP BY $NAME_COLUMN", null)
    }

    fun loadTravel(travelName : String?): Cursor {
        travelName?.let {
            return readableDatabase.rawQuery("SELECT * FROM $TRAVEL_TABLE_NAME WHERE $NAME_COLUMN = ? OR $NAME_COLUMN IS NULL ORDER BY $TIME_COLUMN ASC", arrayOf(travelName))
        }
        return readableDatabase.rawQuery("SELECT * FROM $TRAVEL_TABLE_NAME WHERE $NAME_COLUMN IS NULL ORDER BY $TIME_COLUMN ASC", null)
    }

    companion object {
        private const val TRAVEL_TABLE_NAME = "travels"
        private const val ID_COLUMN = "travel_id"
        const val TIME_COLUMN = "time"
        const val LAT_COLUMN = "lat"
        const val LON_COLUMN = "lon"
        const val NAME_COLUMN = "name"
    }
}
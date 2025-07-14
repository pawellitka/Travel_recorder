package com.travel_recorder.web_server

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import com.google.android.gms.maps.model.LatLng
import com.travel_recorder.R
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*

import io.ktor.http.content.*
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.application.*
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*

import io.ktor.server.netty.*
import io.netty.handler.codec.DefaultHeaders
import java.io.File
import java.io.FileOutputStream
import com.travel_recorder.database.Database
import com.travel_recorder.ui_src.popups.Removing
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import org.json.JSONArray
import org.json.JSONObject

object KtorServer {
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
    private var db : Database? = null

    fun start(context: Context, dataBase : Database? = null, port: Int = DEFAULT_WEB_PORT) {
        if (server != null) return
        if(dataBase != null)
            db = dataBase
        server = embeddedServer(Netty, port = port, watchPaths = emptyList()) {
            routing {
                singlePageApplication {
                    useResources = true
                    defaultPage = "index.html"
                    angular("server_frontend/dist/travel_recorder_web/browser")
                }
                get("/general") {
                    val jsonArray = JSONArray()
                    db!!.loadNames().run {
                        this.use {
                            var trackNo = 0
                            if (this.moveToFirst()) {
                                do {
                                    val jsonObject = JSONObject()
                                    jsonObject.put("trackId", trackNo++)
                                    jsonObject.put("name", this.getString(this.getColumnIndexOrThrow(Database.NAME_COLUMN)))
                                    jsonArray.put(jsonObject)
                                } while (this.moveToNext())
                            }
                        }
                    }
                    call.response.status(HttpStatusCode.OK)
                    call.respond(jsonArray.toString())
                }
                get("/specific_track") {
                    val jsonArray = JSONArray()
                    db!!.loadTravel(call.parameters["track_name"]).run {
                        this.use {
                            var locationNo = 0
                            if (this.moveToFirst()) {
                                do {
                                    val jsonObject = JSONObject()
                                    jsonObject.put("locationId", locationNo++)
                                    jsonObject.put("latitude", this.getDouble(this.getColumnIndexOrThrow(Database.LAT_COLUMN)).toString())
                                    jsonObject.put("longitude", this.getDouble(this.getColumnIndexOrThrow(Database.LON_COLUMN)).toString())
                                    jsonObject.put("time", this.getDouble(this.getColumnIndexOrThrow(Database.TIME_COLUMN)).toString())
                                    jsonArray.put(jsonObject)
                                } while (this.moveToNext())
                            }
                        }
                    }
                    call.response.status(HttpStatusCode.OK)
                    call.respond(jsonArray.toString())
                }
            }
        }.start(wait = false)
    }

    fun restartServer(context: Context, port: Int) {
        stop()
        start(context, null, port)
    }

    fun stop() {
        server?.stop(GRACE_PERIOD_MS, TIMEOUT_MS)
        server = null
    }

    const val DEFAULT_WEB_PORT = 26666
    private const val GRACE_PERIOD_MS : Long = 1000
    private const val TIMEOUT_MS : Long = 2000
}
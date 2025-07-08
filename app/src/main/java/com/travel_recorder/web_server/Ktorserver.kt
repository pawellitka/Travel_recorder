package com.travel_recorder.web_server

import android.content.Context
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.http.content.files
import io.ktor.http.content.static
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.io.File
import java.io.FileOutputStream

object KtorServer {
    private var server: ApplicationEngine? = null

    fun start(context: Context, port: Int = DEFAULT_WEB_PORT) {
        if (server != null) return

        val file = File(context.cacheDir, "local_server_index.html")
        context.assets.open("server_frontend/index.html").use { input ->
            FileOutputStream(file).use { output -> input.copyTo(output) }
        }
        server = embeddedServer(Netty, port = port, watchPaths = emptyList()) {
            install(CallLogging)
            routing {
                static("/") {
                    files(file)
                }
            }
        }.start(wait = false)
    }

    fun restartServer(context: Context, port: Int) {
        stop()
        start(context, port)
    }

    fun stop() {
        server?.stop(GRACE_PERIOD_MS, TIMEOUT_MS)
        server = null
    }

    const val DEFAULT_WEB_PORT = 26666
    private const val GRACE_PERIOD_MS : Long = 1000
    private const val TIMEOUT_MS : Long = 2000
}
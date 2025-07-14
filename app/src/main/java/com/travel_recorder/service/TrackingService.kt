package com.travel_recorder.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.travel_recorder.R
import com.travel_recorder.database.Database
import com.travel_recorder.ui_src.settingsscreen.DataStoreManager


fun launchService(context : Context) {
    Intent(context, TrackingService::class.java).also {
        it.action = TrackingService.START_ACTION
        context.startService(it)
    }
}

fun endService(context : Context) {
    Intent(context, TrackingService::class.java).also {
        it.action = TrackingService.STOP_ACTION
        context.startService(it)
    }
}

fun restartIfRunningService(context : Context) {
    Intent(context, TrackingService::class.java).also {
        it.action = TrackingService.RESTART_IF_RUNNING_ACTION
        context.startService(it)
    }
}

class TrackingService() : Service() {
    private val dataBase = Database(this, null)
    private var isServiceRunning = false
    private var callback : LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            locationResult.locations.lastOrNull()?.let { location ->
                dataBase.saveLocation(location.latitude, location.longitude)
                recordedLocation.postValue(location)
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startingServiceCallback(intervalMs : Int) {
        LocationServices.getFusedLocationProviderClient(applicationContext).requestLocationUpdates(
            LocationRequest.Builder(intervalMs.toLong()).setMinUpdateDistanceMeters(0.0F).build(),
            callback,
            Looper.getMainLooper()
        )
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startService() {
        val channel = NotificationChannel(
            this.resources.getString(R.string.channel_name),
            this.resources.getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.description = this.resources.getString(R.string.service_description)

        val notificationManager = getSystemService<NotificationManager>(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

        NotificationCompat.Builder(this, this.resources.getString(R.string.channel_name))
            .setContentTitle(this.resources.getString(R.string.notification))
            .setContentText("")
            .setOngoing(true).also {
                startForeground(1, it.build())
            }
        DataStoreManager(applicationContext).getIntervalWithCallback(::startingServiceCallback)
    }

    private fun stopService() {
        isServiceRunning = false
        LocationServices.getFusedLocationProviderClient(applicationContext).removeLocationUpdates(callback)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            START_ACTION -> {
                if(!isServiceRunning) {
                    isServiceRunning = true
                    startService()
                }
            }
            STOP_ACTION -> stopService()
            RESTART_IF_RUNNING_ACTION -> {
                if(isServiceRunning) {
                    startService()
                    stopService()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    companion object {
        const val START_ACTION = "START_ACTION"
        const val STOP_ACTION = "STOP_ACTION"
        const val RESTART_IF_RUNNING_ACTION = "RESTART_IF_RUNNING_ACTION"
        const val TRACKING_INTERVAL_UNIT_CONVERSION : Int = 60 * 1000
        const val DEFAULT_TRACKING_INTERVAL : Int = 2 * TRACKING_INTERVAL_UNIT_CONVERSION
        val recordedLocation = MutableLiveData<Location>()
    }
}
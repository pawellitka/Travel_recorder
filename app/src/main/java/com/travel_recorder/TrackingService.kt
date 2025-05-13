package com.travel_recorder

import android.Manifest
import android.app.Service
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

class TrackingService() : Service() {
    private val dataBase = Database(this, null)
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
    private fun startService() {
        NotificationCompat.Builder(this, "tracking")
            .setContentTitle(this.resources.getString(R.string.notification))
            .setContentText("")
            .setOngoing(true).also {
                startForeground(1, it.build())
            }
        LocationServices.getFusedLocationProviderClient(applicationContext).requestLocationUpdates(
            LocationRequest.Builder(TRACKING_INTERVAL).setMinUpdateDistanceMeters(0.0F).build(),
            callback,
            Looper.getMainLooper()
        )
    }

    private fun stopService() {
        LocationServices.getFusedLocationProviderClient(applicationContext).removeLocationUpdates(callback)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            START_ACTION -> startService()
            STOP_ACTION -> stopService()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    companion object {
        const val START_ACTION = "START_ACTION"
        const val STOP_ACTION = "STOP_ACTION"
        const val TRACKING_INTERVAL : Long = 120000
        val recordedLocation = MutableLiveData<Location>()
    }
}
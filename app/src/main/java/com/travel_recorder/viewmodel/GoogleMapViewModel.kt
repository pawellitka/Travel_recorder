package com.travel_recorder.viewmodel

import android.Manifest
import android.app.Application
import android.icu.text.DateFormat.getDateTimeInstance
import android.location.Location
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.travel_recorder.database.Database
import com.travel_recorder.R
import com.travel_recorder.service.TrackingService
import java.time.Instant
import java.time.temporal.ChronoField
import java.util.Date

class GoogleMapViewModel(private val application: Application, private val lifecycleOwner: LifecycleOwner) : AndroidViewModel(application) {
    private var gmap: GoogleMap? = null
    var track: String? = null
    var isTracking by mutableStateOf(false)
    var startedTracking by mutableStateOf(false)
    private var polylineOptions = PolylineOptions()

    init {
        val liveData: LiveData<Location> = TrackingService.recordedLocation

        liveData.observe(lifecycleOwner) { location ->
            println("hhjttttttttttttttttt")
            addMarker(location.latitude, location.longitude, Instant.now().getLong(ChronoField.INSTANT_SECONDS))
            setShownTrack(track)
        }
    }

    fun setMap(map: GoogleMap) {
        gmap = map
        setShownTrack(null)
        gmap?.mapType = GoogleMap.MAP_TYPE_HYBRID
        gmap?.isTrafficEnabled = false
        gmap?.setIndoorEnabled(true)
        gmap?.isBuildingsEnabled = false
        gmap?.uiSettings?.isZoomControlsEnabled = true
    }

    fun setShownTrack(name: String?) {
        gmap ?: return
        track = name

        gmap?.clear()
        polylineOptions = PolylineOptions()
        polylineOptions.color(R.color.white)
        polylineOptions.width(20f)
        var lat = 0.0
        var lon = 0.0
        Database(application, null).loadTravel(track).run {
            this.use {
                if (this.moveToFirst()) {
                    do {
                        lat = this.getDouble(this.getColumnIndexOrThrow(Database.LAT_COLUMN))
                        lon = this.getDouble(this.getColumnIndexOrThrow(Database.LON_COLUMN))
                        addMarker(lat, lon, this.getLong(this.getColumnIndexOrThrow(Database.TIME_COLUMN)))
                    } while (this.moveToNext())
                    cameraSetting(lat, lon)
                }
            }
        }
        gmap!!.addPolyline(polylineOptions)
    }

    private fun addMarker(latitude : Double, longitude : Double, time : Long) {
        polylineOptions.add(LatLng(latitude, longitude))
        val markerOptions = MarkerOptions()
        markerOptions.position(LatLng(latitude, longitude))
        markerOptions.title(getDateTimeInstance().format(Date(time * 1000)))
        gmap?.addMarker(markerOptions)
    }

    private fun cameraSetting(latitude : Double, longitude : Double) {
        val cameraPosition =
            CameraPosition.Builder().target( LatLng(latitude, longitude))
                .zoom(18f).build()
        gmap?.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }
}
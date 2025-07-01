package com.travel_recorder.viewmodel

import android.app.Application
import android.content.Context
import android.os.SystemClock
import android.icu.text.DateFormat.getDateTimeInstance
import android.location.Location
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
import com.google.maps.android.SphericalUtil
import com.travel_recorder.database.Database
import com.travel_recorder.R
import com.travel_recorder.service.TrackingService
import java.util.Date

class GoogleMapViewModel(private val application: Application, private val lifecycleOwner: LifecycleOwner) : AndroidViewModel(application) {
    private var gmap: GoogleMap? = null
    private var track: String? = null
    var isTracking by mutableStateOf(false)
    var startedTracking by mutableStateOf(false)
    private var polylineOptions = PolylineOptions()
    private var previousClickTime: Long = CAMERA_FIXED_TIME_AFTER_CLICK

    init {
        val liveData: LiveData<Location> = TrackingService.recordedLocation

        liveData.observe(lifecycleOwner) {
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

    fun setTrack(newTrack: String) {
        track = newTrack
    }

    fun getTrack() : String? {
        return track
    }

    fun setShownTrack(name: String?) {
        gmap ?: return
        track = name

        gmap?.clear()
        gmap?.setOnMapClickListener {
            previousClickTime = SystemClock.elapsedRealtime()
        }
        gmap?.setOnCameraIdleListener {
            previousClickTime = SystemClock.elapsedRealtime()
        }
        gmap?.setOnCameraMoveStartedListener { reason ->
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE)
                previousClickTime = SystemClock.elapsedRealtime()
        }
        polylineOptions = PolylineOptions()
        polylineOptions.color(R.color.white)
        polylineOptions.width(20f)
        var lat = 0.0
        var lon = 0.0
        Database(application, null).loadTravel(track).run {
            this.use {
                if (this.moveToFirst()) {
                    val latLngList = mutableListOf<LatLng>()
                    do {
                        lat = this.getDouble(this.getColumnIndexOrThrow(Database.LAT_COLUMN))
                        lon = this.getDouble(this.getColumnIndexOrThrow(Database.LON_COLUMN))
                        latLngList.add(LatLng(lat, lon))
                        addMarker(lat, lon, this.getLong(this.getColumnIndexOrThrow(Database.TIME_COLUMN)), SphericalUtil.computeLength(latLngList), application)
                    } while (this.moveToNext())
                    if(SystemClock.elapsedRealtime() - previousClickTime > CAMERA_FIXED_TIME_AFTER_CLICK)
                        cameraSetting(lat, lon)
                }
            }
        }
        gmap!!.addPolyline(polylineOptions)
    }

    private fun addMarker(latitude : Double, longitude : Double, time : Long, distance : Double, context : Context) {
        polylineOptions.add(LatLng(latitude, longitude))
        val markerOptions = MarkerOptions()
        markerOptions.position(LatLng(latitude, longitude))
        markerOptions.title(context.getString(R.string.map_marker_template).format(getDateTimeInstance().format(Date(time * 1000)), distance / 1000))
        gmap?.addMarker(markerOptions)
    }

    private fun cameraSetting(latitude : Double, longitude : Double) {
        val cameraPosition =
            CameraPosition.Builder().target( LatLng(latitude, longitude))
                .zoom(18f).build()
        gmap?.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    companion object {
        private const val CAMERA_FIXED_TIME_AFTER_CLICK : Long = 20 * 1000
    }
}
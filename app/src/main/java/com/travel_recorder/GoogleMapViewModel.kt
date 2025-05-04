package com.travel_recorder

import android.Manifest
import android.app.Application
import android.icu.text.DateFormat.getDateTimeInstance
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.tasks.CancellationTokenSource
import java.time.Instant
import java.time.temporal.ChronoField
import java.util.Date

class GoogleMapViewModel(private val application: Application) : AndroidViewModel(application){
    private var gmap: GoogleMap? = null
    var track: String? = null
    var isTracking by mutableStateOf(false)
    var startedTracking by mutableStateOf(false)
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    public fun setMap(map: GoogleMap) {
        gmap = map
        setShownTrack(null)
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    public fun setShownTrack(name: String?) {
        gmap ?: return
        track = name

        gmap?.setMapType(GoogleMap.MAP_TYPE_HYBRID)
        gmap?.isTrafficEnabled = false
        gmap?.setIndoorEnabled(true)
        gmap?.isBuildingsEnabled = false
        gmap?.uiSettings?.isZoomControlsEnabled = true
        gmap?.clear()
        val polylineOptions = PolylineOptions()
        if(track != null) {
            Database(application, null).loadTravel(track!!).run {
                this.use {
                    if (this.moveToFirst()) {
                        var lat = this.getDouble(this.getColumnIndexOrThrow(Database.LAT_COLUMN))
                        var lon = this.getDouble(this.getColumnIndexOrThrow(Database.LON_COLUMN))
                        polylineOptions.add(LatLng(lat, lon))
                        cameraSetting(lat, lon)
                        if(this.moveToNext()) {
                            do {
                                lat = this.getDouble(this.getColumnIndexOrThrow(Database.LAT_COLUMN))
                                lon = this.getDouble(this.getColumnIndexOrThrow(Database.LON_COLUMN))
                                polylineOptions.add(LatLng(lat, lon))
                                addMarker(lat, lon, this.getLong(this.getColumnIndexOrThrow(Database.TIME_COLUMN)))
                            } while (this.moveToNext())
                        }
                    }
                }
            }
        } else {
            Database(application, null).loadNewLocations().run {
                this.use {
                    if(this.moveToNext()) {
                        do {
                            val lat = this.getDouble(this.getColumnIndexOrThrow(Database.LAT_COLUMN))
                            val lon = this.getDouble(this.getColumnIndexOrThrow(Database.LON_COLUMN))
                            polylineOptions.add(LatLng(lat, lon))
                            addMarker(lat, lon, this.getLong(this.getColumnIndexOrThrow(Database.TIME_COLUMN)))
                        } while (this.moveToNext())
                    }
                }
            }
            val cancelationTokenSource = CancellationTokenSource()
            LocationServices.getFusedLocationProviderClient(application)
                .getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancelationTokenSource.token
                )
                .addOnSuccessListener { location ->
                    cameraSetting(location.latitude, location.longitude)
                    addMarker(location.latitude, location.longitude, Instant.now().getLong(
                        ChronoField.INSTANT_SECONDS))
                    polylineOptions.add(LatLng(location.latitude, location.longitude))
                    cancelationTokenSource.cancel()
                }
        }
        polylineOptions.color(R.color.white)
        polylineOptions.width(20f)
        gmap!!.addPolyline(polylineOptions)
    }

    private fun cameraSetting(latitude : Double, longitude : Double) {
        val cameraPosition =
            CameraPosition.Builder().target( LatLng(latitude, longitude))
                .zoom(18f).build()
        gmap?.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    private fun addMarker(latitude : Double, longitude : Double, time : Long) {
        val markerOptions = MarkerOptions()
        markerOptions.position(LatLng(latitude, longitude))
        markerOptions.title(getDateTimeInstance().format(Date(time * 1000)))
        gmap?.addMarker(markerOptions)
    }
}
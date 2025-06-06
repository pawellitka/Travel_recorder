package com.travel_recorder.ui_src

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity.LOCATION_SERVICE
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.travel_recorder.database.Database
import com.travel_recorder.viewmodel.GoogleMapViewModel
import com.travel_recorder.R
import com.travel_recorder.service.endService
import com.travel_recorder.service.launchService
import com.travel_recorder.ui.theme.TravelRecorderTheme
import com.travel_recorder.ui_src.popups.Loading

fun permissionsCheck(context : Context): Boolean {
    val locationManager = context.getSystemService(LOCATION_SERVICE) as LocationManager
    return ((ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED)
            && (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)))
}

@RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun mainScreen(mapViewModel : GoogleMapViewModel?,
               application : Application,
               lifecycleOwner : LifecycleOwner,
               context : Context,
               permissionRequest : ActivityResultLauncher<String>,
               dataBase : Database,
               onNavigate: () -> Unit) : GoogleMapViewModel? {
    var gmapViewModel : GoogleMapViewModel? = mapViewModel
    var toggleRecomposing = remember { mutableStateOf(false) }
    if(mapViewModel == null) {
        gmapViewModel = viewModel<GoogleMapViewModel>(
            factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return GoogleMapViewModel(application, lifecycleOwner) as T
                }
            }
        )
    }
    if(permissionsCheck(context))
        gmapViewModel?.setShownTrack(gmapViewModel?.getTrack())
    var loadChoice by rememberSaveable { mutableStateOf(false) }
    var removalMenu by remember { mutableStateOf(false) }
    LaunchedEffect(toggleRecomposing.value) {}
    TravelRecorderTheme {
        CenterAlignedTopAppBar (
            title = { Text(stringResource(id = R.string.app_name)) },
            actions = {
                Box {
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.options)
                        )
                    }
                    val localLifecycleOwner = LocalLifecycleOwner.current
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                if (!(gmapViewModel!!.startedTracking)) {
                                    Text(stringResource(R.string.start_tracking))
                                } else if (gmapViewModel!!.isTracking) {
                                    Text(stringResource(R.string.stop_tracking))
                                } else {
                                    Text(stringResource(R.string.resume_tracking))
                                }
                            },
                            onClick = dropUnlessResumed(localLifecycleOwner) {
                                showMenu = false
                                if(gmapViewModel!!.isTracking) {
                                    endService(context)
                                    gmapViewModel!!.isTracking = false
                                } else {

                                    if (permissionsCheck(context)) {
                                        gmapViewModel!!.isTracking = true
                                        gmapViewModel!!.startedTracking = true

                                        val cancellationTokenSource = CancellationTokenSource()
                                        LocationServices.getFusedLocationProviderClient(application)
                                            .getCurrentLocation(
                                                Priority.PRIORITY_HIGH_ACCURACY,
                                                cancellationTokenSource.token
                                            )
                                            .addOnSuccessListener { location ->
                                                dataBase.saveLocation(location.latitude, location.longitude)
                                                gmapViewModel?.setShownTrack(gmapViewModel?.getTrack())
                                                cancellationTokenSource.cancel()
                                            }
                                        launchService(context)
                                    }
                                    permissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                }
                            },
                        )
                        if (gmapViewModel!!.startedTracking) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.save)) },
                                onClick = dropUnlessResumed(localLifecycleOwner) {
                                    saving(context, dataBase, gmapViewModel)
                                    showMenu = false
                                },
                            )
                        }
                        dataBase.loadNames().run {
                            this.use {
                                if (this.moveToFirst()) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.load)) },
                                        onClick = dropUnlessResumed(localLifecycleOwner) {
                                            loadChoice = true
                                            showMenu = false
                                            removalMenu = false
                                            gmapViewModel!!.isTracking = false
                                            gmapViewModel!!.startedTracking = true
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.delete)) },
                                        onClick = dropUnlessResumed(localLifecycleOwner) {
                                            loadChoice = true
                                            showMenu = false
                                            removalMenu = true
                                        },
                                    )
                                }
                            }
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.reset)) },
                            onClick = dropUnlessResumed(localLifecycleOwner) {
                                dataBase.resetTravel()
                                gmapViewModel?.setShownTrack(null)
                                showMenu = false
                                gmapViewModel!!.isTracking = false
                                gmapViewModel!!.startedTracking = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.settings)) },
                            onClick = dropUnlessResumed(localLifecycleOwner) {
                                onNavigate()
                            },
                        )
                    }
                }
            }
        )
        if(loadChoice)
            Loading(context, dataBase, gmapViewModel, removalMenu, { loadChoice = false }) {
                toggleRecomposing.value =
                    !toggleRecomposing.value
            }
    }
    return gmapViewModel
}
package com.travel_recorder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.travel_recorder.ui.theme.TravelRecorderTheme

class MainActivity : AppCompatActivity(), OnMapReadyCallback  {
    private val dataBase = Database(this, null)
    private var gmapViewModel : GoogleMapViewModel? = null
    private var permissionRequest: ActivityResultLauncher<String> = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if(!isGranted)
            AlertDialog.Builder(this).create().also {
                it.setMessage(applicationContext.resources.getString(R.string.missing_permission_warning))
                it.setCancelable(true)
                it.show()
            }
        else {
            Intent(applicationContext, TrackingService::class.java).also {
                it.action = TrackingService.STOP_ACTION
                startService(it)
            }
            launchService()
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setDecorFitsSystemWindows(window, true)
        setContentView(R.layout.activity_main)
        val myComposeContent: @Composable () -> Unit = {
            gmapViewModel ?: let {
                gmapViewModel = viewModel<GoogleMapViewModel>(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return GoogleMapViewModel(application) as T
                        }
                    }
                )
            }
            if(permissionsCheck())
                gmapViewModel?.setShownTrack(gmapViewModel?.track)
            var loadChoice by rememberSaveable { mutableStateOf(false) }
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
                            val lifecycleOwner = LocalLifecycleOwner.current
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        if (!(gmapViewModel!!.startedTracking)) {
                                            gmapViewModel?.track?.let {
                                                Text(stringResource(R.string.reset))
                                            } ?: run {
                                                Text(stringResource(R.string.start_tracking))
                                            }
                                        } else if (gmapViewModel!!.isTracking) {
                                            Text(stringResource(R.string.stop_tracking))
                                        } else {
                                            Text(stringResource(R.string.resume_tracking))
                                        }
                                    },
                                    onClick = dropUnlessResumed(lifecycleOwner) {
                                        showMenu = false
                                        if(gmapViewModel!!.isTracking) {
                                            Intent(applicationContext, TrackingService::class.java).also {
                                                it.action = TrackingService.STOP_ACTION
                                                startService(it)
                                            }
                                            gmapViewModel!!.isTracking = false
                                        } else {
                                            if (launchTracking()) {
                                                gmapViewModel!!.isTracking = true
                                                gmapViewModel!!.startedTracking = true
                                                launchService()
                                                gmapViewModel?.setShownTrack(null)
                                            }
                                        }
                                    },
                                )
                                if (gmapViewModel!!.startedTracking) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.save)) },
                                        onClick = dropUnlessResumed(lifecycleOwner) {
                                            saving()
                                            showMenu = false
                                        },
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.load)) },
                                    onClick = dropUnlessResumed(lifecycleOwner) {
                                        loadChoice = true
                                        showMenu = false
                                        gmapViewModel!!.isTracking = false
                                        gmapViewModel!!.startedTracking = false
                                    },
                                )
                                if(gmapViewModel!!.startedTracking) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.reset)) },
                                        onClick = dropUnlessResumed(lifecycleOwner) {
                                            dataBase.resetTravel()
                                            gmapViewModel?.setShownTrack(null)
                                            showMenu = false
                                            gmapViewModel!!.isTracking = false
                                            gmapViewModel!!.startedTracking = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                )
                if(loadChoice)
                {
                    AlertDialog(
                        title = {
                            Text(stringResource(R.string.loadChoice_title))
                        },
                        text = {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                dataBase.loadNames().run {
                                    this.use {
                                        if (this.moveToFirst()) {
                                            do {
                                                val label = this.getString(this.getColumnIndexOrThrow(Database.NAME_COLUMN))
                                                Text(
                                                    text = this.getString(
                                                        this.getColumnIndexOrThrow(
                                                            Database.NAME_COLUMN
                                                        )
                                                    ),
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier
                                                        .clickable {
                                                            loadChoice = false
                                                            gmapViewModel?.setShownTrack(label)
                                                        }
                                                        .height(50.dp)
                                                        .fillMaxWidth()
                                                )
                                            } while (this.moveToNext())
                                        }
                                    }
                                }
                            }
                        },
                        onDismissRequest = { loadChoice = false },
                        dismissButton = {
                            TextButton(
                                onClick = { loadChoice = false }
                            ) {
                                Text(stringResource(R.string.cancel))
                            }
                        },
                        confirmButton = {},
                    )
                }
            }
            val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment?
            mapFragment?.getMapAsync(this@MainActivity)
        }
        val composeView = findViewById<ComposeView>(R.id.compose_view)
        composeView.setContent {
            myComposeContent()
        }
    }

    private fun saving() {
        var confirmed = false
        AlertDialog.Builder(this).also {
            it.setTitle(R.string.loadChoice_title)
            val input = EditText(this).apply {
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
                        dataBase.saveTravel(input.text.toString())
                    else {
                        AlertDialog.Builder(this).create().apply {
                            this.setMessage(applicationContext.resources.getString(R.string.saving_not_finalized_warning))
                            this.setCancelable(true)
                            this.show()
                        }
                    }
                }
            }
            it.show()
        }
    }

    private fun permissionsCheck(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)
                && (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)))
    }

    private fun launchTracking(): Boolean {
        if (permissionsCheck()) {
            return true
        }
        permissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        return false
    }

    private fun launchService() {
        Intent(applicationContext, TrackingService::class.java).also {
            it.action = TrackingService.START_ACTION
            startService(it)
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    @Override
    override fun onMapReady(googleMap: GoogleMap) {
        gmapViewModel?.setMap(googleMap)
    }
}
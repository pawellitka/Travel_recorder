package com.travel_recorder

import android.Manifest
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.travel_recorder.database.Database
import com.travel_recorder.service.endService
import com.travel_recorder.service.launchService
import com.travel_recorder.ui_src.settingsscreen.DataStoreManager
import com.travel_recorder.ui_src.settingsscreen.SettingsScreen
import com.travel_recorder.ui_src.mainScreen
import com.travel_recorder.viewmodel.GoogleMapViewModel
import com.travel_recorder.web_server.KtorServer

class MainActivity : AppCompatActivity(), OnMapReadyCallback  {
    private val dataBase = Database(this, null)
    private var gmapViewModel : GoogleMapViewModel? = null
    private lateinit var dataStoreManager: DataStoreManager
    private var permissionRequest : ActivityResultLauncher<String> = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if(!isGranted)
            AlertDialog.Builder(this).create().also {
                it.setMessage(applicationContext.resources.getString(R.string.missing_permission_warning))
                it.setCancelable(true)
                it.show()
            }
        else {
            endService(applicationContext)
            launchService(this@MainActivity)
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dataStoreManager = DataStoreManager(applicationContext)
        setDecorFitsSystemWindows(window, true)
        setContentView(R.layout.activity_main)
        val composeContent: @Composable () -> Unit = {
            val navController = rememberNavController()

            NavHost(navController = navController, startDestination = "home") {
                composable("home") {
                    gmapViewModel = mainScreen(gmapViewModel, application, this@MainActivity, this@MainActivity, permissionRequest, dataBase) {
                        navController.navigate("details")
                    }
                    val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment?
                    mapFragment?.getMapAsync(this@MainActivity)
                }
                composable("details") {
                    SettingsScreen(this@MainActivity, dataStoreManager) {
                        navController.popBackStack()
                    }
                }
            }
        }

        val composeView = findViewById<ComposeView>(R.id.compose_view)
        composeView.setContent {
            composeContent()
        }

        KtorServer.start(this)
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    @Override
    override fun onMapReady(googleMap: GoogleMap) {
        gmapViewModel?.setMap(googleMap)
    }

    override fun onDestroy() {
        KtorServer.stop()
        super.onDestroy()
    }
}
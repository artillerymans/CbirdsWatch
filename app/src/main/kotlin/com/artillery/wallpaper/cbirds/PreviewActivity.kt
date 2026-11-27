package com.artillery.wallpaper.cbirds

import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PreviewActivity : ComponentActivity() {
    private val wallpaperManager by lazy { WallpaperManager.getInstance(this) }
    private val cbirdsComponent by lazy { ComponentName(this, CbirdsWallpaperService::class.java) }
    private val flockSettings by lazy { FlockSettings.open(this) }
    private var birdCount by mutableIntStateOf(FlockSettings.DEFAULT_BIRD_COUNT)
    private var flightSpeed by mutableIntStateOf(FlockSettings.DEFAULT_FLIGHT_SPEED)
    private var active by mutableStateOf(false)
    private var applyingWallpaper by mutableStateOf(false)
    private var wallpaperMessage by mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        birdCount = FlockSettings.birdCount(flockSettings)
        flightSpeed = FlockSettings.flightSpeed(flockSettings)
        setContent {
            WallpaperSettingsScreen(
                birdCount = birdCount,
                flightSpeed = flightSpeed,
                active = active,
                applyingWallpaper = applyingWallpaper,
                wallpaperMessage = wallpaperMessage,
                onBirdCountChange = ::updateBirdCount,
                onFlightSpeedChange = ::updateFlightSpeed,
                onWallpaperAction = ::handleWallpaperAction,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        active = isCbirdsWallpaperActive()
    }

    private fun updateBirdCount(value: Int) {
        if (value == birdCount) return
        birdCount = value
        flockSettings.edit().putInt(FlockSettings.BIRD_COUNT, value).apply()
    }

    private fun updateFlightSpeed(value: Int) {
        if (value == flightSpeed) return
        flightSpeed = value
        flockSettings.edit().putInt(FlockSettings.FLIGHT_SPEED, value).apply()
    }

    private fun isCbirdsWallpaperActive(): Boolean =
        wallpaperManager.wallpaperInfo?.component == cbirdsComponent

    private fun handleWallpaperAction() {
        if (applyingWallpaper) return
        active = isCbirdsWallpaperActive()
        wallpaperMessage = null
        if (active) {
            openWallpaperPicker()
            return
        }

        applyingWallpaper = true
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    ShizukuWallpaper.apply(applicationContext, wallpaperManager)
                }
                active = isCbirdsWallpaperActive()
                wallpaperMessage = result
            } finally {
                applyingWallpaper = false
            }
        }
    }

    private fun openWallpaperPicker() {
        if (!wallpaperManager.isSetWallpaperAllowed) {
            showMessage(R.string.wallpaper_change_blocked)
            return
        }

        try {
            val intent = Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), getString(R.string.change_wallpaper))
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            showMessage(R.string.wallpaper_picker_unavailable)
        } catch (_: SecurityException) {
            showMessage(R.string.wallpaper_picker_unavailable)
        }
    }

    private fun showMessage(message: Int) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
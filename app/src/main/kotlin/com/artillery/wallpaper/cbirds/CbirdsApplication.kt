package com.artillery.wallpaper.cbirds

import android.app.Application
import android.content.Context
import rikka.shizuku.ShizukuProvider

class CbirdsApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        // Providers initialize before Application.onCreate().
        ShizukuProvider.disableAutomaticSuiInitialization()
    }
}
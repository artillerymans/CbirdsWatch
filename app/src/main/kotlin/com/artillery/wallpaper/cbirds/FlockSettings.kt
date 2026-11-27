package com.artillery.wallpaper.cbirds

import android.content.Context
import android.content.SharedPreferences

internal object FlockSettings {
    const val BIRD_COUNT = "bird_count"
    const val MIN_BIRD_COUNT = 200
    const val MAX_BIRD_COUNT = 800
    const val DEFAULT_BIRD_COUNT = 420

    const val FLIGHT_SPEED = "flight_speed_percent"
    const val MIN_FLIGHT_SPEED = 25
    const val MAX_FLIGHT_SPEED = 100
    const val DEFAULT_FLIGHT_SPEED = 25

    fun open(context: Context): SharedPreferences =
        context.getSharedPreferences("flock_settings", Context.MODE_PRIVATE)

    fun birdCount(settings: SharedPreferences): Int =
        settings.getInt(BIRD_COUNT, DEFAULT_BIRD_COUNT).coerceIn(MIN_BIRD_COUNT, MAX_BIRD_COUNT)

    fun flightSpeed(settings: SharedPreferences): Int =
        settings.getInt(FLIGHT_SPEED, DEFAULT_FLIGHT_SPEED)
            .coerceIn(MIN_FLIGHT_SPEED, MAX_FLIGHT_SPEED)
}
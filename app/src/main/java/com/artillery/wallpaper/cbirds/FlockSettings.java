package com.artillery.wallpaper.cbirds;

import android.content.Context;
import android.content.SharedPreferences;

final class FlockSettings {
    static final String BIRD_COUNT = "bird_count";
    static final int MIN_BIRD_COUNT = 200;
    static final int MAX_BIRD_COUNT = 800;
    static final int DEFAULT_BIRD_COUNT = 420;

    static final String FLIGHT_SPEED = "flight_speed_percent";
    static final int MIN_FLIGHT_SPEED = 25;
    static final int MAX_FLIGHT_SPEED = 100;
    static final int DEFAULT_FLIGHT_SPEED = 50;

    private FlockSettings() {}

    static SharedPreferences open(Context context) {
        return context.getSharedPreferences("flock_settings", Context.MODE_PRIVATE);
    }
}
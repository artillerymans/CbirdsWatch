package com.artillery.wallpaper.cbirds;

import android.app.Activity;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public final class PreviewActivity extends Activity {
    private WallpaperManager wallpaperManager;
    private ComponentName cbirdsComponent;
    private TextView wallpaperStatus;
    private Button wallpaperAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wallpaper_settings);

        wallpaperManager = WallpaperManager.getInstance(this);
        cbirdsComponent = new ComponentName(this, CbirdsWallpaperService.class);
        wallpaperStatus = findViewById(R.id.wallpaper_status);
        wallpaperAction = findViewById(R.id.wallpaper_action);
        wallpaperAction.setOnClickListener(view -> openWallpaperPicker());

        SharedPreferences settings = FlockSettings.open(this);
        bindSlider(settings, R.id.bird_count_slider, R.id.bird_count_label,
                R.string.bird_count_label, FlockSettings.BIRD_COUNT,
                FlockSettings.MIN_BIRD_COUNT, FlockSettings.MAX_BIRD_COUNT,
                FlockSettings.DEFAULT_BIRD_COUNT);
        bindSlider(settings, R.id.flight_speed_slider, R.id.flight_speed_label,
                R.string.flight_speed_label, FlockSettings.FLIGHT_SPEED,
                FlockSettings.MIN_FLIGHT_SPEED, FlockSettings.MAX_FLIGHT_SPEED,
                FlockSettings.DEFAULT_FLIGHT_SPEED);
    }

    private void bindSlider(SharedPreferences settings, int sliderId, int labelId, int labelFormat,
            String key, int minimum, int maximum, int defaultValue) {
        SeekBar slider = findViewById(sliderId);
        TextView label = findViewById(labelId);
        slider.setMax(maximum);
        slider.setMin(minimum);
        slider.setProgress(settings.getInt(key, defaultValue));
        label.setText(getString(labelFormat, slider.getProgress()));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
                label.setText(getString(labelFormat, value));
                if (fromUser) settings.edit().putInt(key, value).apply();
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean active = isCbirdsWallpaperActive();
        wallpaperStatus.setText(active ? R.string.wallpaper_active : R.string.wallpaper_inactive);
        wallpaperAction.setText(active ? R.string.change_wallpaper : R.string.set_default_wallpaper);
    }

    private boolean isCbirdsWallpaperActive() {
        WallpaperInfo current = wallpaperManager.getWallpaperInfo();
        return current != null && cbirdsComponent.equals(current.getComponent());
    }

    private void openWallpaperPicker() {
        if (!wallpaperManager.isSetWallpaperAllowed()) {
            Toast.makeText(this, R.string.wallpaper_change_blocked, Toast.LENGTH_LONG).show();
            return;
        }

        boolean active = isCbirdsWallpaperActive();
        Intent intent;
        if (active) {
            intent = Intent.createChooser(new Intent(Intent.ACTION_SET_WALLPAPER),
                    getString(R.string.change_wallpaper));
        } else {
            intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, cbirdsComponent);
        }

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException unavailable) {
            Toast.makeText(this, R.string.wallpaper_picker_unavailable, Toast.LENGTH_LONG).show();
        }
    }
}
package com.artillery.wallpaper.cbirds

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
internal fun WallpaperSettingsScreen(
    birdCount: Int,
    flightSpeed: Int,
    active: Boolean,
    applyingWallpaper: Boolean,
    wallpaperMessage: Int?,
    onBirdCountChange: (Int) -> Unit,
    onFlightSpeedChange: (Int) -> Unit,
    onWallpaperAction: () -> Unit,
) {
    val background = colorResource(R.color.wallpaper_background)
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = colorResource(R.color.wallpaper_accent),
            onPrimary = colorResource(R.color.wallpaper_on_accent),
            background = background,
            surface = background,
        ),
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.wallpaper_settings_title),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Image(
                    painter = painterResource(R.drawable.cbirds_wallpaper_thumbnail),
                    contentDescription = null,
                    modifier = Modifier.widthIn(max = 184.dp).fillMaxWidth().aspectRatio(1f),
                )
                Spacer(Modifier.height(16.dp))
                SettingSlider(
                    label = stringResource(R.string.bird_count_label, birdCount),
                    value = birdCount,
                    range = FlockSettings.MIN_BIRD_COUNT..FlockSettings.MAX_BIRD_COUNT,
                    onValueChange = onBirdCountChange,
                )
                SettingSlider(
                    label = stringResource(R.string.flight_speed_label, flightSpeed),
                    value = flightSpeed,
                    range = FlockSettings.MIN_FLIGHT_SPEED..FlockSettings.MAX_FLIGHT_SPEED,
                    onValueChange = onFlightSpeedChange,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(if (active) R.string.wallpaper_active else R.string.wallpaper_inactive),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onWallpaperAction,
                    enabled = !applyingWallpaper,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    val actionLabel = when {
                        applyingWallpaper -> R.string.wallpaper_applying
                        active -> R.string.change_wallpaper
                        else -> R.string.set_default_wallpaper
                    }
                    Text(stringResource(actionLabel))
                }
                if (wallpaperMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(wallpaperMessage),
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSlider(label: String, value: Int, range: IntRange, onValueChange: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt().coerceIn(range)) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
        )
    }
}

@Preview(widthDp = 320, heightDp = 560, showBackground = true)
@Composable
private fun WallpaperSettingsPreview() {
    WallpaperSettingsScreen(
        birdCount = FlockSettings.DEFAULT_BIRD_COUNT,
        flightSpeed = FlockSettings.DEFAULT_FLIGHT_SPEED,
        active = false,
        applyingWallpaper = false,
        wallpaperMessage = null,
        onBirdCountChange = {},
        onFlightSpeedChange = {},
        onWallpaperAction = {},
    )
}
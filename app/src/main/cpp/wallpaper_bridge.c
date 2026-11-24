// Keep the upstream files unchanged. The bridge uses the same single-frame
// simulation and pixel composition as cbirds' headless recorder.
#define main cbirds_cli_main
#include "cbirds/boids.c"
#undef main

#include <android/bitmap.h>
#include <jni.h>

// The framework invokes our engines on the main thread. Preview and home
// surfaces share the upstream simulation, which uses file-scope state.
static struct {
    png_image_t canvas;
    png_image_t sprites[ROTATION_FRAMES * MAX_SPRITE_SETS];
    spatial_grid_t grid;
    bird_t *birds;
    bird_t *snapshot;
    jlong last_frame_ms;
} renderer;

static void release_renderer(void) {
    free_sprites(renderer.sprites);
    png_image_free(&renderer.canvas);
    spatial_grid_destroy(&renderer.grid);
    free(renderer.birds);
    free(renderer.snapshot);
    renderer.birds = NULL;
    renderer.snapshot = NULL;
    renderer.last_frame_ms = 0;
}

static int init_renderer(int width, int height, int bird_count) {
    config.birds = bird_count;
    config.palette = palette_named("ember");
    config.bird_size = 0;
    legend_enabled = 0;
    clock_mode = 1;
    the_sign.kind = SIGN_CLOCK;
    the_sign.seconds = 0;
    the_sign.twelve_hours = 0;
    the_sign.virtual_clock = 0;
    memset(&clock_state, 0, sizeof(clock_state));

    trig_lookup_init();
    apply_notches();
    set_frame_seconds(1.0 / 15.0);
    apply_screen_size(width / DEFAULT_CELL_WIDTH, height / DEFAULT_CELL_HEIGHT, width, height);
    if (spatial_grid_init(&renderer.grid, SPATIAL_CELL_SIZE) != SPATIAL_GRID_OK ||
        spatial_grid_prepare(&renderer.grid, width, height, config.birds) != SPATIAL_GRID_OK ||
        rasterise_sprites(renderer.sprites) != PNG_OK ||
        png_image_alloc(&renderer.canvas, width, height) != PNG_OK) {
        release_renderer();
        return 0;
    }

    renderer.birds = calloc((size_t)config.birds, sizeof(*renderer.birds));
    renderer.snapshot = malloc((size_t)config.birds * sizeof(*renderer.snapshot));
    if (!renderer.birds || !renderer.snapshot) {
        release_renderer();
        return 0;
    }
    seed_random((unsigned)time(NULL));
    initialize_birds(renderer.birds);
    begin_the_sign();
    return 1;
}

static int advance_renderer(jlong uptime_ms, int flight_speed_percent) {
    const jlong previous_ms = renderer.last_frame_ms;
    const jlong elapsed_ms = uptime_ms - previous_ms;
    if (previous_ms != 0 && elapsed_ms >= 0 && elapsed_ms < 30) return 1;

    // Bound the step after the surface was hidden. The digits still read the
    // device's wall clock through cbirds' live sign implementation.
    config.pace = DEFAULT_PACE * flight_speed_percent / 100.0;
    set_frame_seconds(previous_ms == 0 ? 1.0 / 15.0 : fmin(elapsed_ms / 1000.0, 0.1));
    clock_state.seconds += frame_seconds;
    clock_state.frame++;
    if (formation.writing && formation.until >= 0 && clock_state.seconds >= formation.until)
        formation_clear();
    sign_advance(renderer.birds);
    maybe_drift();
    memcpy(renderer.snapshot, renderer.birds, (size_t)config.birds * sizeof(*renderer.snapshot));
    if (spatial_grid_build(&renderer.grid, config.birds, read_bird_position, renderer.snapshot) !=
        SPATIAL_GRID_OK) return 0;
    fly(renderer.birds, renderer.snapshot, &renderer.grid);
    compose_onto(&renderer.canvas, renderer.sprites, renderer.birds, 1);
    renderer.last_frame_ms = uptime_ms;
    return 1;
}

JNIEXPORT jboolean JNICALL Java_com_artillery_wallpaper_cbirds_CbirdsWallpaperService_drawFrame(
        JNIEnv *env, jclass type, jobject bitmap, jlong uptime_ms,
        jint bird_count, jint flight_speed_percent) {
    (void)type;
    if (bird_count < 1 || bird_count > MAX_BIRDS || flight_speed_percent < 1 ||
        flight_speed_percent > 100) return JNI_FALSE;

    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, bitmap, &info) != ANDROID_BITMAP_RESULT_SUCCESS ||
        info.format != ANDROID_BITMAP_FORMAT_RGBA_8888 || info.width < 96 || info.height < 96 ||
        info.width > 1024 || info.height > 1024 || info.stride < info.width * 4) return JNI_FALSE;
    if (renderer.birds && (bird_count != config.birds ||
            (int)info.width != renderer.canvas.width || (int)info.height != renderer.canvas.height))
        release_renderer();
    if (!renderer.birds && !init_renderer(info.width, info.height, bird_count)) return JNI_FALSE;
    if (!advance_renderer(uptime_ms, flight_speed_percent)) return JNI_FALSE;

    void *pixels = NULL;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) != ANDROID_BITMAP_RESULT_SUCCESS)
        return JNI_FALSE;
    const size_t row_bytes = (size_t)info.width * 4;
    for (uint32_t y = 0; y < info.height; y++) {
        memcpy((uint8_t *)pixels + y * (size_t)info.stride,
               renderer.canvas.pixels + y * row_bytes, row_bytes);
    }
    return AndroidBitmap_unlockPixels(env, bitmap) == ANDROID_BITMAP_RESULT_SUCCESS;
}

JNIEXPORT void JNICALL Java_com_artillery_wallpaper_cbirds_CbirdsWallpaperService_releaseRenderer(
        JNIEnv *env, jclass type) {
    (void)env;
    (void)type;
    release_renderer();
}
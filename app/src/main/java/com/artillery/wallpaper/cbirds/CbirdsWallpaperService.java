package com.artillery.wallpaper.cbirds;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;

public final class CbirdsWallpaperService extends WallpaperService {
    private static final int RENDER_SIDE = 400;
    private static final long FRAME_INTERVAL_MS = 67;
    private static final String TAG = "CbirdsWallpaper";
    private SharedPreferences flockSettings;

    static {
        System.loadLibrary("cbirds");
    }

    private static native boolean drawFrame(Bitmap bitmap, long uptimeMillis,
            int birdCount, int flightSpeedPercent);
    private static native void releaseRenderer();

    @Override
    public void onCreate() {
        super.onCreate();
        flockSettings = FlockSettings.open(this);
    }

    @Override
    public Engine onCreateEngine() {
        return new ClockEngine();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseRenderer();
    }

    private final class ClockEngine extends Engine {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Runnable nextFrame = this::render;
        private final RectF destination = new RectF();
        private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        private Bitmap bitmap;
        private boolean visible;
        private boolean surfaceReady;

        @Override
        public void onVisibilityChanged(boolean visible) {
            this.visible = visible;
            schedule();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            surfaceReady = true;
            schedule();
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            stopDrawing();
            super.onSurfaceDestroyed(holder);
        }

        @Override
        public void onDestroy() {
            stopDrawing();
            super.onDestroy();
        }

        private void stopDrawing() {
            surfaceReady = false;
            handler.removeCallbacks(nextFrame);
            bitmap = null;
        }

        private void schedule() {
            handler.removeCallbacks(nextFrame);
            if (visible && surfaceReady) handler.post(nextFrame);
        }

        private void render() {
            if (!visible || !surfaceReady) return;
            SurfaceHolder holder = getSurfaceHolder();
            if (!holder.getSurface().isValid()) return;
            if (bitmap == null) {
                bitmap = Bitmap.createBitmap(RENDER_SIDE, RENDER_SIDE, Bitmap.Config.ARGB_8888);
            }
            long start = SystemClock.uptimeMillis();
            int birdCount = flockSettings.getInt(FlockSettings.BIRD_COUNT,
                    FlockSettings.DEFAULT_BIRD_COUNT);
            int flightSpeed = flockSettings.getInt(FlockSettings.FLIGHT_SPEED,
                    FlockSettings.DEFAULT_FLIGHT_SPEED);
            if (!drawFrame(bitmap, start, birdCount, flightSpeed)) {
                Log.e(TAG, "Unable to render the cbirds frame");
                return;
            }
            Canvas canvas = holder.lockCanvas();
            if (canvas != null) {
                try {
                    int width = canvas.getWidth();
                    int height = canvas.getHeight();
                    int side = Math.min(width, height);
                    float left = (width - side) / 2f;
                    float top = (height - side) / 2f;
                    // The square destination applies the same scale to every bird.
                    destination.set(left, top, left + side, top + side);
                    canvas.drawColor(0xff121218);
                    canvas.drawBitmap(bitmap, null, destination, paint);
                } finally {
                    holder.unlockCanvasAndPost(canvas);
                }
            }
            handler.postAtTime(nextFrame, Math.max(start + FRAME_INTERVAL_MS,
                    SystemClock.uptimeMillis() + 1));
        }
    }
}

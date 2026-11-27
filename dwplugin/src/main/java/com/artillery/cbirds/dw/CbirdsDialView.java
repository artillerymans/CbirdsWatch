package com.artillery.cbirds.dw;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import java.io.IOException;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

// Created by WatchFactory with an explicit dial ID, never inflated from XML.
@SuppressLint("ViewConstructor")
public final class CbirdsDialView extends View {
    private static final String TAG = "CbirdsDW";
    private static final int RENDER_SIDE = 400;
    private static final long FRAME_INTERVAL_MS = 67L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF destination = new RectF();
    private final Runnable nextFrame = this::render;
    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { updateDrawing(); }
    };
    private final ContentObserver pageObserver = new ContentObserver(handler) {
        @Override public void onChange(boolean selfChange) { updateDrawing(); }
    };
    private Bitmap frame;
    private boolean attached;
    private boolean acquired;
    private boolean receiverRegistered;
    private boolean observerRegistered;
    private boolean drawing;
    private boolean firstFrame;
    private String failure;

    public CbirdsDialView(Context context, int dialId) {
        super(context);
        // DW constructs this View on Dispatchers.IO; rendering starts on attach.
        setTag("CbirdsDialView");
        setBackgroundColor(0xff121218);
        Log.i(TAG, "created dial=" + dialId);
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        attached = true;
        try {
            NativeRenderer.acquire(getContext());
            acquired = true;
            frame = Bitmap.createBitmap(RENDER_SIDE, RENDER_SIDE, Bitmap.Config.ARGB_8888);
            IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            getContext().registerReceiver(screenReceiver, filter);
            receiverRegistered = true;
            getContext().getContentResolver().registerContentObserver(
                    Settings.System.getUriFor("dw_page_home"), false, pageObserver);
            observerRegistered = true;
            failure = null;
            firstFrame = false;
            Log.i(TAG, "attached");
            updateDrawing();
        } catch (IOException | RuntimeException | LinkageError e) {
            fail(e);
        }
    }

    @Override protected void onDetachedFromWindow() {
        attached = false;
        handler.removeCallbacksAndMessages(null);
        drawing = false;
        if (receiverRegistered) {
            getContext().unregisterReceiver(screenReceiver);
            receiverRegistered = false;
        }
        if (observerRegistered) {
            getContext().getContentResolver().unregisterContentObserver(pageObserver);
            observerRegistered = false;
        }
        if (acquired) {
            NativeRenderer.release();
            acquired = false;
        }
        // Do not recycle a Bitmap that may still be referenced by a display list.
        frame = null;
        Log.i(TAG, "detached; renderer released");
        super.onDetachedFromWindow();
    }

    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (handler != null) updateDrawing();
    }

    @Override public void onVisibilityAggregated(boolean isVisible) {
        super.onVisibilityAggregated(isVisible);
        updateDrawing();
    }

    private boolean shouldDraw() {
        if (!attached || !acquired || failure != null || !isShown()
                || getWindowVisibility() != VISIBLE) return false;
        PowerManager power = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
        return (power == null || power.isInteractive())
                && Settings.System.getInt(getContext().getContentResolver(), "dw_page_home", 1) != 0;
    }

    private void updateDrawing() {
        handler.removeCallbacks(nextFrame);
        boolean active = shouldDraw();
        if (drawing != active) Log.i(TAG, active ? "drawing resumed" : "drawing paused");
        drawing = active;
        if (active) handler.post(nextFrame);
    }

    private void render() {
        if (!shouldDraw()) {
            updateDrawing();
            return;
        }
        long start = SystemClock.uptimeMillis();
        try {
            if (!NativeRenderer.drawFrame(frame, start)) {
                throw new IllegalStateException("Native renderer rejected frame");
            }
            if (!firstFrame) {
                firstFrame = true;
                Log.i(TAG, "first-frame birds=700 speed=15 size=" + getWidth() + "x" + getHeight());
            }
            invalidate();
            handler.postAtTime(nextFrame,
                    Math.max(start + FRAME_INTERVAL_MS, SystemClock.uptimeMillis() + 1));
        } catch (RuntimeException | LinkageError e) {
            fail(e);
        }
    }

    private void fail(Throwable error) {
        failure = "Cbirds 加载失败";
        drawing = false;
        handler.removeCallbacks(nextFrame);
        Log.e(TAG, "Plugin render failure", error);
        try {
            File directory = getContext().getExternalFilesDir(null);
            if (directory != null) {
                try (PrintWriter output = new PrintWriter(new FileWriter(
                        new File(directory, "cbirds-dw-error.txt"), false))) {
                    error.printStackTrace(output);
                }
            }
        } catch (IOException | RuntimeException ignored) {
            // Some release firmwares disable logcat; retain a readable cause.
            failure += "\n" + error.getClass().getSimpleName();
        }
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(0xff121218);
        if (frame != null && failure == null) {
            float side = Math.min(getWidth(), getHeight());
            float left = (getWidth() - side) / 2f;
            float top = (getHeight() - side) / 2f;
            destination.set(left, top, left + side, top + side);
            canvas.drawBitmap(frame, null, destination, paint);
        } else if (failure != null) {
            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(18 * getResources().getDisplayMetrics().scaledDensity);
            canvas.drawText(failure, getWidth() / 2f, getHeight() / 2f, paint);
        }
    }
}

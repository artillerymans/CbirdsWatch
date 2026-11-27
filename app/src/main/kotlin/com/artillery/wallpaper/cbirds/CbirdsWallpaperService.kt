package com.artillery.wallpaper.cbirds

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder

class CbirdsWallpaperService : WallpaperService() {
    private val flockSettings by lazy { FlockSettings.open(this) }

    override fun onCreateEngine(): Engine = ClockEngine()

    override fun onDestroy() {
        super.onDestroy()
        releaseRenderer()
    }

    private inner class ClockEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private val nextFrame = Runnable(::render)
        private val destination = RectF()
        private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        private var bitmap: Bitmap? = null
        private var visible = false
        private var surfaceReady = false

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            schedule()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceReady = true
            schedule()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            stopDrawing()
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            stopDrawing()
            super.onDestroy()
        }

        private fun stopDrawing() {
            surfaceReady = false
            handler.removeCallbacks(nextFrame)
            bitmap = null
        }

        private fun schedule() {
            handler.removeCallbacks(nextFrame)
            if (visible && surfaceReady) handler.post(nextFrame)
        }

        private fun render() {
            if (!visible || !surfaceReady) return
            val holder = surfaceHolder
            if (!holder.surface.isValid) return

            val frame = bitmap ?: Bitmap.createBitmap(RENDER_SIDE, RENDER_SIDE, Bitmap.Config.ARGB_8888)
                .also { bitmap = it }
            val start = SystemClock.uptimeMillis()
            val birdCount = FlockSettings.birdCount(flockSettings)
            val flightSpeed = FlockSettings.flightSpeed(flockSettings)
            if (!drawFrame(frame, start, birdCount, flightSpeed)) {
                Log.e(TAG, "Unable to render the cbirds frame")
                return
            }

            holder.lockCanvas()?.let { canvas ->
                try {
                    val side = minOf(canvas.width, canvas.height)
                    val left = (canvas.width - side) / 2f
                    val top = (canvas.height - side) / 2f
                    destination.set(left, top, left + side, top + side)
                    canvas.drawColor(0xff121218.toInt())
                    canvas.drawBitmap(frame, null, destination, paint)
                } finally {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
            handler.postAtTime(nextFrame, maxOf(start + FRAME_INTERVAL_MS, SystemClock.uptimeMillis() + 1))
        }
    }

    private companion object {
        const val RENDER_SIDE = 400
        const val FRAME_INTERVAL_MS = 67L
        const val TAG = "CbirdsWallpaper"

        init {
            System.loadLibrary("cbirds")
        }

        @JvmStatic
        external fun drawFrame(bitmap: Bitmap, uptimeMillis: Long, birdCount: Int, flightSpeedPercent: Int): Boolean

        @JvmStatic
        external fun releaseRenderer()
    }
}
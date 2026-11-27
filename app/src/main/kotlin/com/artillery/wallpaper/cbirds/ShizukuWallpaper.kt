package com.artillery.wallpaper.cbirds

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.DeadObjectException
import android.os.IBinder
import android.os.Parcel
import android.os.Process
import android.os.RemoteException
import android.os.UserHandle
import android.util.Log
import androidx.annotation.StringRes
import androidx.annotation.WorkerThread
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

internal object ShizukuWallpaper {
    private const val DESCRIPTOR = "android.app.IWallpaperManager"
    private const val SET_COMPONENT_PERMISSION = "android.permission.SET_WALLPAPER_COMPONENT"
    // Android 9 IWallpaperManager.aidl: setWallpaperComponent is the third transaction.
    private const val SET_WALLPAPER_COMPONENT = IBinder.FIRST_CALL_TRANSACTION + 2
    private const val TAG = "ShizukuWallpaper"

    @WorkerThread
    @StringRes
    fun apply(context: Context, wallpaperManager: WallpaperManager): Int? {
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.P || Process.myUserHandle() != UserHandle.getUserHandleForUid(0)) {
            return R.string.wallpaper_direct_unsupported
        }

        return try {
            if (!wallpaperManager.isWallpaperSupported || !wallpaperManager.isSetWallpaperAllowed) {
                return R.string.wallpaper_change_blocked
            }
            if (!Shizuku.pingBinder() || Shizuku.isPreV11()) return R.string.wallpaper_shizuku_start
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                return R.string.wallpaper_shizuku_authorize
            }
            if (Shizuku.checkRemotePermission(SET_COMPONENT_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
                return R.string.wallpaper_shizuku_root
            }

            val service = SystemServiceHelper.getSystemService(Context.WALLPAPER_SERVICE)
                ?: return R.string.wallpaper_direct_failed
            if (service.interfaceDescriptor != DESCRIPTOR) return R.string.wallpaper_direct_failed

            val component = ComponentName(context, CbirdsWallpaperService::class.java)
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            try {
                data.writeInterfaceToken(DESCRIPTOR)
                data.writeInt(1) // Non-null ComponentName in the Android 9 AIDL parcel.
                component.writeToParcel(data, 0)
                if (!ShizukuBinderWrapper(service).transact(SET_WALLPAPER_COMPONENT, data, reply, 0)) {
                    return R.string.wallpaper_direct_failed
                }
                reply.readException()
            } finally {
                reply.recycle()
                data.recycle()
            }

            if (wallpaperManager.wallpaperInfo?.component == component) null else R.string.wallpaper_direct_failed
        } catch (error: DeadObjectException) {
            Log.w(TAG, "Binder disconnected while setting wallpaper", error)
            R.string.wallpaper_shizuku_start
        } catch (error: SecurityException) {
            Log.w(TAG, "System denied the wallpaper operation", error)
            R.string.wallpaper_shizuku_denied
        } catch (error: RemoteException) {
            Log.w(TAG, "Wallpaper Binder call failed", error)
            R.string.wallpaper_direct_failed
        } catch (error: RuntimeException) {
            Log.w(TAG, "Unable to apply the wallpaper", error)
            R.string.wallpaper_direct_failed
        }
    }
}
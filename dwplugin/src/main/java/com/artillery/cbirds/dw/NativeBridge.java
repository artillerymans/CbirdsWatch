package com.artillery.cbirds.dw;

import android.graphics.Bitmap;
import android.os.Process;
import android.util.Log;

/** Loaded through the installed package Context, not DW's disposable loader. */
public final class NativeBridge {
    private static int clients;

    static {
        System.loadLibrary("cbirds_dw");
        Log.i("CbirdsDW", "native-loaded installed-package process64=" + Process.is64Bit());
    }

    private NativeBridge() {}

    public static synchronized void acquire() { clients++; }

    public static synchronized boolean drawFrame(Bitmap bitmap, long uptimeMillis) {
        return clients > 0 && nativeDrawFrame(bitmap, uptimeMillis, 700, 15);
    }

    public static synchronized void release() {
        if (clients > 0 && --clients == 0) nativeReleaseRenderer();
    }

    private static native boolean nativeDrawFrame(Bitmap bitmap, long uptimeMillis,
                                                   int birdCount, int flightSpeedPercent);
    private static native void nativeReleaseRenderer();
}

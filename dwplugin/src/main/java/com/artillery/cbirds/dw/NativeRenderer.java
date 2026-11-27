package com.artillery.cbirds.dw;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final class NativeRenderer {
    private static Method acquireMethod;
    private static Method drawMethod;
    private static Method releaseMethod;

    private NativeRenderer() {}

    static synchronized void acquire(Context context) throws IOException {
        if (acquireMethod == null) {
            try {
                // Android caches the installed package's code/native loader.
                // DW's disposable loaders must not own the same native library.
                Context source = context.createPackageContext("com.artillery.cbirds.dw",
                        Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
                Class<?> bridge = source.getClassLoader()
                        .loadClass("com.artillery.cbirds.dw.NativeBridge");
                Method acquire = bridge.getMethod("acquire");
                drawMethod = bridge.getMethod("drawFrame", Bitmap.class, long.class);
                releaseMethod = bridge.getMethod("release");
                acquireMethod = acquire;
            } catch (PackageManager.NameNotFoundException | ReflectiveOperationException e) {
                throw new IOException("Install the Cbirds DW APK before selecting the plugin", e);
            }
        }
        invoke(acquireMethod);
    }

    static synchronized boolean drawFrame(Bitmap bitmap, long uptimeMillis) {
        return (Boolean) invoke(drawMethod, bitmap, uptimeMillis);
    }

    static synchronized void release() { invoke(releaseMethod); }

    private static Object invoke(Method method, Object... arguments) {
        try {
            return method.invoke(null, arguments);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof LinkageError) throw (LinkageError) cause;
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            throw new IllegalStateException("Native bridge call failed", cause);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Native bridge method unavailable", e);
        }
    }
}

package com.dwiot.watchface.view;

import android.content.Context;
import android.view.View;
import com.artillery.cbirds.dw.CbirdsDialView;

/** Entry point required by com.dw.launcher.util.DexLoaderUtils. */
public final class WatchFactory {
    public WatchFactory() {}

    public View createWatchDial(Context context, int dialId) {
        return new CbirdsDialView(context, dialId);
    }
}

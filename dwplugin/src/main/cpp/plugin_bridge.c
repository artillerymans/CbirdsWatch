// Reuse the wallpaper renderer without changing its JNI entry points for app/.
#define Java_com_artillery_wallpaper_cbirds_CbirdsWallpaperService_drawFrame \
    Java_com_artillery_cbirds_dw_NativeBridge_nativeDrawFrame
#define Java_com_artillery_wallpaper_cbirds_CbirdsWallpaperService_releaseRenderer \
    Java_com_artillery_cbirds_dw_NativeBridge_nativeReleaseRenderer
#include "wallpaper_bridge.c"

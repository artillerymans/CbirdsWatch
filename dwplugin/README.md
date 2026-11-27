# DW Launcher 鸟群时钟插件

表盘 ID 为 `900001`，中文名称“鸟群时钟”。同一个 APK 需要安装为原生库提供包，并复制到 DW 表盘目录。安装包不提供独立桌面图标，表盘仍从 DW 的表盘列表选择。保留固定入口 `com.dwiot.watchface.view.WatchFactory.createWatchDial(Context, int)` 的名称和公开无参构造。

使用 JDK 17、Android SDK 35、NDK 30.0.16248370、CMake 4.1.2：

```sh
./gradlew :dwplugin:assembleDebug
adb install -r dwplugin/build/outputs/apk/debug/dwplugin-debug.apk
adb shell mkdir -p /sdcard/DwDials
adb push dwplugin/build/outputs/apk/debug/dwplugin-debug.apk /sdcard/DwDials/900001.apk
adb shell am broadcast -a com.timuen.action.refresh_watchface -p com.dw.launcher
```

在桌面表盘列表中选择“鸟群时钟”。桌面重启时也会扫描该目录。同路径覆盖后宿主可能保留列表缓存，验证更新应切换到其他表盘再切回；必要时重新打开桌面。

渲染复用 `app/src/main/cpp/` 的现有 C 源码，插件 JNI 入口通过预处理重命名，不修改动态壁纸入口。插件使用普通 View 和平台 API，不依赖宿主 Kotlin/Compose 版本。默认 700 只鸟、15% 速度、400×400 离屏位图，按短边居中缩放，约 15 FPS。此版没有接入独立应用的设置页。

DW 的类加载器没有 nativeLibrarySearchPath，而且该设备的 system Launcher 无法执行从私有 code cache 解压的库（dlopen map Permission denied）。原生库放入 APK 标准 lib 目录，由 Android 安装器放置。插件通过 createPackageContext(INCLUDE_CODE | IGNORE_SECURITY) 获取已安装包的框架类加载器，反射调用 NativeBridge；由其 System.loadLibrary 加载安装器放置的库。这个稳定的类加载器避免每次切换表盘时不同 DW ClassLoader 重复加载同一个 native 库。NativeBridge 串行化 C 全局渲染状态的调用，最后一个 View 移除时释放渲染内存。

更新时必须同时更新已安装包和 `/sdcard/DwDials/900001.apk`。如果已加载旧版 native 桥接代码，需重新启动 Launcher 进程以载入新版，不要只覆盖外部 APK。不要卸载 `com.artillery.cbirds.dw`，否则表盘无法获取原生库。

View 在附着后启动，监听窗口可见性、屏幕状态及 DW 的 `dw_page_home` 设置，停止不可见页面的刷新。日志标签：`CbirdsDW`。检查 `native-loaded`、`first-frame` 和切换时的暂停/释放日志。

默认构建 `armeabi-v7a,arm64-v8a`；可用 `-PdwNativeAbis=arm64-v8a` 指定打包 ABI。此设置必须覆盖 Launcher 进程的 ABI。

package net.kdt.pojavlaunch.plugins;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LibraryPlugin {
    private static final String TAG = "LibraryPlugin";

    // Known plugins constants
    public static final String ID_ANGLE_PLUGIN = "git.mojo.angle";
    public static final String ID_FFMPEG_PLUGIN = "git.mojo.ffmpeg";
    public static final String ID_ZINK_PLUGIN = "git.mojo.zink";
    public static final String ID_FCL_RENDER_PLUGIN = "com.fcl.render";

    private final String appId;
    private final String libraryPath;
    private final String appName;

    public LibraryPlugin(String app, String libraryPath, String appName) {
        this.appId = app;
        this.libraryPath = libraryPath;
        this.appName = appName;
    }

    public static LibraryPlugin discoverPlugin(Context ctx, String appId) {
        if (ctx == null || appId == null) return null;
        try {
            PackageManager pm = ctx.getPackageManager();
            PackageInfo pluginPackage = pm.getPackageInfo(appId, PackageManager.GET_SHARED_LIBRARY_FILES);
            String libraryPath = pluginPackage.applicationInfo != null ? pluginPackage.applicationInfo.nativeLibraryDir : null;
            if (libraryPath != null && new File(libraryPath).exists()) {
                CharSequence label = pluginPackage.applicationInfo.loadLabel(pm);
                String name = label != null ? label.toString() : appId;
                return new LibraryPlugin(appId, libraryPath, name);
            }
        } catch (Exception e) {
            Log.d(TAG, "Plugin discover failed for " + appId + ": " + e.getMessage());
        }
        return null;
    }

    /** Discover all installed external FCLRendererPlugin APK packages */
    public static List<LibraryPlugin> discoverAllFclPlugins(Context ctx) {
        List<LibraryPlugin> list = new ArrayList<>();
        if (ctx == null) return list;

        String[] knownAppIds = {
            "com.fcl.plugin.mobileglues",
            "com.fcl.mobileglues",
            "com.mobileglues",
            "com.shirosaki.fclrendererplugin",
            "com.commonlauncher.nativeplugin",
            "com.fcl.renderplugin",
            "com.fcl.render",
            "com.zalith.renderplugin",
            "org.fcl.renderplugin",
            "com.fcl.fclrendererplugin",
            "git.mojo.fcl"
        };

        for (String appId : knownAppIds) {
            LibraryPlugin p = discoverPlugin(ctx, appId);
            if (p != null) {
                list.add(p);
            }
        }

        try {
            PackageManager pm = ctx.getPackageManager();
            List<PackageInfo> packages = pm.getInstalledPackages(PackageManager.GET_SHARED_LIBRARY_FILES);
            for (PackageInfo pkg : packages) {
                if (pkg.packageName == null) continue;
                String lower = pkg.packageName.toLowerCase();
                if (lower.contains("fcl") || lower.contains("mobileglues") || (lower.contains("render") && lower.contains("plugin"))) {
                    boolean alreadyAdded = false;
                    for (LibraryPlugin existing : list) {
                        if (existing.getId().equals(pkg.packageName)) {
                            alreadyAdded = true;
                            break;
                        }
                    }
                    if (!alreadyAdded) {
                        String nativeDir = pkg.applicationInfo != null ? pkg.applicationInfo.nativeLibraryDir : null;
                        if (nativeDir != null && new File(nativeDir).exists()) {
                            CharSequence label = pkg.applicationInfo.loadLabel(pm);
                            String name = label != null ? label.toString() : pkg.packageName;
                            list.add(new LibraryPlugin(pkg.packageName, nativeDir, name));
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error discovering FCL plugins", e);
        }

        // Local directory overrides (e.g., user placed .so files in plugins folder)
        File[] overrideDirs = {
            new File(net.kdt.pojavlaunch.Tools.DIR_GAME_HOME, "fcl_render"),
            new File(net.kdt.pojavlaunch.Tools.DIR_CACHE, "fcl_render"),
            new File(ctx.getExternalFilesDir(null), "plugins"),
            new File(ctx.getFilesDir(), "plugins"),
            new File("/sdcard/PojavLauncher/plugins"),
            new File("/sdcard/fcl/renders")
        };

        for (File dir : overrideDirs) {
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    boolean hasSo = false;
                    for (File f : files) {
                        if (f.getName().endsWith(".so")) {
                            hasSo = true;
                            break;
                        }
                    }
                    if (hasSo) {
                        String overrideId = "fcl_dir:" + dir.getAbsolutePath();
                        list.add(new LibraryPlugin(overrideId, dir.getAbsolutePath(), "FCL Plugin (Local Override: " + dir.getName() + ")"));
                    }
                }
            }
        }

        return list;
    }

    public static LibraryPlugin discoverFclPlugin(Context ctx) {
        List<LibraryPlugin> plugins = discoverAllFclPlugins(ctx);
        return plugins.isEmpty() ? null : plugins.get(0);
    }

    public String getId() {
        return appId;
    }

    public String getLibraryPath() {
        return libraryPath;
    }

    public String getAppName() {
        return appName != null ? appName : appId;
    }

    public String resolveAbsolutePath(String library) {
        return new File(libraryPath, library).getAbsolutePath();
    }

    public boolean checkLibraries(String... libs) {
        for (String lib : libs) {
            if (!(new File(libraryPath, lib).exists())) return false;
        }
        return true;
    }
}

package net.kdt.pojavlaunch.plugins;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class LibraryPlugin {
    private static final String TAG = "LibraryPlugin";

    // Known plugins constants
    public static final String ID_ANGLE_PLUGIN = "git.mojo.angle";
    public static final String ID_FFMPEG_PLUGIN = "git.mojo.ffmpeg";
    public static final String ID_ZINK_PLUGIN = "git.mojo.zink";
    public static final String ID_FCL_RENDER_PLUGIN = "com.fcl.render";

    private String appId;
    private String libraryPath;
    private LibraryPlugin(String app, String libraryPath){
        this.appId = app;
        this.libraryPath = libraryPath;
    }
    public static LibraryPlugin discoverPlugin(Context ctx, String appId){
        if (ctx == null || appId == null) return null;
        String libraryPath;
        try {
            PackageInfo pluginPackage = ctx.getPackageManager().getPackageInfo(appId, PackageManager.GET_SHARED_LIBRARY_FILES);
            libraryPath = pluginPackage.applicationInfo != null ? pluginPackage.applicationInfo.nativeLibraryDir : null;
            if (libraryPath != null && new File(libraryPath).exists()) {
                return new LibraryPlugin(appId, libraryPath);
            }
        } catch (Exception e){
            Log.d(TAG, "Plugin discover failed for " + appId + ": " + e.getMessage());
        }
        return null;
    }

    /** Discover FCL / Custom Renderer Plugin installed via APK or plugin package */
    public static LibraryPlugin discoverFclPlugin(Context ctx) {
        String[] knownFclAppIds = {
            "com.shirosaki.fclrendererplugin",
            "com.fcl.renderplugin",
            "com.fcl.render",
            "com.zalith.renderplugin",
            "org.fcl.renderplugin",
            "com.fcl.fclrendererplugin",
            "git.mojo.fcl"
        };

        if (ctx != null) {
            for (String appId : knownFclAppIds) {
                LibraryPlugin plugin = discoverPlugin(ctx, appId);
                if (plugin != null) {
                    return plugin;
                }
            }

            try {
                PackageManager pm = ctx.getPackageManager();
                for (PackageInfo pkg : pm.getInstalledPackages(PackageManager.GET_SHARED_LIBRARY_FILES)) {
                    if (pkg.packageName == null) continue;
                    String lowerPkg = pkg.packageName.toLowerCase();
                    if (lowerPkg.contains("fclrendererplugin") || lowerPkg.contains("fclrender") || 
                        (lowerPkg.contains("fcl") && lowerPkg.contains("render")) ||
                        (lowerPkg.contains("renderer") && lowerPkg.contains("plugin"))) {
                        String nativeDir = pkg.applicationInfo != null ? pkg.applicationInfo.nativeLibraryDir : null;
                        if (nativeDir != null && new File(nativeDir).exists()) {
                            Log.i(TAG, "Discovered FCL Renderer Plugin APK: " + pkg.packageName + " at " + nativeDir);
                            return new LibraryPlugin(pkg.packageName, nativeDir);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error scanning installed packages for FCL plugin: " + e.getMessage());
            }
        }

        return null;
    }

    public String getId(){
        return appId;
    }

    public String getLibraryPath(){
        return libraryPath;
    }
    public String resolveAbsolutePath(String library) {
        return new File(libraryPath, library).getAbsolutePath();
    }

    public boolean checkLibraries(String... libs){
        for(String lib : libs){
            if(!(new File(libraryPath, lib).exists())) return false;
        }
        return true;
    }
}

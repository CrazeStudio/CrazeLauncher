package net.kdt.pojavlaunch.plugins;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.utils.RendererCompatUtil;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Manager for external FCLRendererPlugin APKs and custom render plugins.
 * Supports:
 * - Direct APK selection & extraction ("Add APK")
 * - Installed Android FCLRendererPlugin app discovery
 * - Dynamic loading of ARM64 native .so libraries and env configs
 */
public class FclPluginManager {
    private static final String TAG = "FclPluginManager";
    public static final String RENDERER_PREFIX = "fcl_plugin:";

    public static class FclPlugin {
        public String id;
        public String name;
        public String libraryPath;
        public String mainSoFile;
        public Map<String, String> environmentVars = new HashMap<>();
        public boolean isInstalledApp;

        public FclPlugin(String id, String name, String libraryPath, String mainSoFile, boolean isInstalledApp) {
            this.id = id;
            this.name = name;
            this.libraryPath = libraryPath;
            this.mainSoFile = mainSoFile;
            this.isInstalledApp = isInstalledApp;
        }

        public String getPreferenceId() {
            return RENDERER_PREFIX + id;
        }

        public String getResolvedMainSoPath() {
            return new File(libraryPath, mainSoFile).getAbsolutePath();
        }
    }

    public static File getPluginsDirectory() {
        File dir = new File(Tools.DIR_GAME_HOME, "renderer_plugins");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * Import an FCLRendererPlugin from a user-selected APK file.
     */
    public static FclPlugin importPluginApk(Context context, Uri apkUri) throws Exception {
        Log.i(TAG, "Importing FCLRendererPlugin APK from URI: " + apkUri);
        
        // Copy URI to temporary file
        File tempApk = new File(context.getCacheDir(), "temp_fcl_plugin_" + System.currentTimeMillis() + ".apk");
        try (InputStream is = context.getContentResolver().openInputStream(apkUri);
             OutputStream os = new FileOutputStream(tempApk)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
        }

        // Inspect package archive
        PackageManager pm = context.getPackageManager();
        PackageInfo pkgInfo = pm.getPackageArchiveInfo(tempApk.getAbsolutePath(), PackageManager.GET_ACTIVITIES | PackageManager.GET_SHARED_LIBRARY_FILES);
        
        String pkgName = pkgInfo != null && pkgInfo.packageName != null ? pkgInfo.packageName : "plugin_" + System.currentTimeMillis();
        String label = "FCL Renderer Plugin (" + pkgName + ")";
        if (pkgInfo != null && pkgInfo.applicationInfo != null) {
            pkgInfo.applicationInfo.sourceDir = tempApk.getAbsolutePath();
            pkgInfo.applicationInfo.publicSourceDir = tempApk.getAbsolutePath();
            CharSequence charSequence = pkgInfo.applicationInfo.loadLabel(pm);
            if (charSequence != null && charSequence.length() > 0) {
                label = charSequence.toString();
            }
        }

        File targetDir = new File(getPluginsDirectory(), pkgName.replace(".", "_"));
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        // Extract native .so libraries and assets from APK ZIP
        String primaryAbi = getTargetAbi();
        String mainSo = extractLibrariesFromApk(tempApk, targetDir, primaryAbi);

        // Extract env.json or plugin.json if present
        Map<String, String> envVars = extractEnvJsonFromApk(tempApk, targetDir);

        // Save metadata json
        JSONObject meta = new JSONObject();
        meta.put("id", pkgName);
        meta.put("name", label);
        meta.put("mainSo", mainSo != null ? mainSo : "libgl4es_115.so");
        meta.put("importedAt", System.currentTimeMillis());
        
        File metaFile = new File(targetDir, "plugin_info.json");
        try (FileOutputStream fos = new FileOutputStream(metaFile)) {
            fos.write(meta.toString(2).getBytes(StandardCharsets.UTF_8));
        }

        // Clean temp apk
        tempApk.delete();

        RendererCompatUtil.releaseRenderersCache();

        FclPlugin plugin = new FclPlugin(pkgName, label, targetDir.getAbsolutePath(), mainSo != null ? mainSo : "libgl4es_115.so", false);
        plugin.environmentVars.putAll(envVars);
        Log.i(TAG, "Successfully imported FCLRendererPlugin: " + label + " (" + mainSo + ")");
        return plugin;
    }

    /**
     * Get all available FCL Renderer Plugins (installed apps + imported APKs).
     */
    public static List<FclPlugin> getAvailablePlugins(Context context) {
        List<FclPlugin> result = new ArrayList<>();
        Map<String, FclPlugin> map = new HashMap<>();

        // 1. Scan imported plugin directories
        File pluginsDir = getPluginsDirectory();
        File[] subDirs = pluginsDir.listFiles();
        if (subDirs != null) {
            for (File dir : subDirs) {
                if (!dir.isDirectory()) continue;
                FclPlugin imported = loadImportedPlugin(dir);
                if (imported != null) {
                    map.put(imported.id, imported);
                }
            }
        }

        // 2. Scan installed Android packages for FCLRendererPlugin
        if (context != null) {
            try {
                PackageManager pm = context.getPackageManager();
                List<PackageInfo> installed = pm.getInstalledPackages(PackageManager.GET_SHARED_LIBRARY_FILES);
                for (PackageInfo pkg : installed) {
                    if (pkg.packageName == null) continue;
                    String lowerPkg = pkg.packageName.toLowerCase();
                    if (lowerPkg.contains("fclrendererplugin") || lowerPkg.contains("fclrender") ||
                        (lowerPkg.contains("fcl") && lowerPkg.contains("render")) ||
                        (lowerPkg.contains("renderer") && lowerPkg.contains("plugin"))) {

                        String nativeDir = pkg.applicationInfo != null ? pkg.applicationInfo.nativeLibraryDir : null;
                        if (nativeDir != null && new File(nativeDir).exists()) {
                            CharSequence appLabel = pkg.applicationInfo.loadLabel(pm);
                            String label = appLabel != null ? appLabel.toString() : pkg.packageName;
                            String mainSo = findMainSoInDir(new File(nativeDir));

                            FclPlugin installedPlugin = new FclPlugin(pkg.packageName, label, nativeDir, mainSo, true);
                            map.put(pkg.packageName, installedPlugin);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error scanning installed packages: " + e.getMessage());
            }
        }

        result.addAll(map.values());
        return result;
    }

    public static FclPlugin getPluginById(Context context, String preferenceOrPluginId) {
        String cleanId = preferenceOrPluginId;
        if (cleanId.startsWith(RENDERER_PREFIX)) {
            cleanId = cleanId.substring(RENDERER_PREFIX.length());
        }
        for (FclPlugin plugin : getAvailablePlugins(context)) {
            if (plugin.id.equals(cleanId) || plugin.getPreferenceId().equals(preferenceOrPluginId)) {
                return plugin;
            }
        }
        return null;
    }

    private static FclPlugin loadImportedPlugin(File dir) {
        File metaFile = new File(dir, "plugin_info.json");
        String name = "FCL Plugin (" + dir.getName() + ")";
        String mainSo = findMainSoInDir(dir);
        String id = dir.getName();

        if (metaFile.exists()) {
            try (InputStream is = java.nio.file.Files.newInputStream(metaFile.toPath())) {
                byte[] data = new byte[(int) metaFile.length()];
                is.read(data);
                JSONObject json = new JSONObject(new String(data, StandardCharsets.UTF_8));
                if (json.has("name")) name = json.getString("name");
                if (json.has("id")) id = json.getString("id");
                if (json.has("mainSo")) mainSo = json.getString("mainSo");
            } catch (Exception e) {
                Log.e(TAG, "Failed to read metadata for " + dir.getName(), e);
            }
        }

        if (mainSo == null) {
            mainSo = "libgl4es_114.so";
        }

        return new FclPlugin(id, name, dir.getAbsolutePath(), mainSo, false);
    }

    private static String extractLibrariesFromApk(File apkFile, File targetDir, String abi) throws Exception {
        String mainSo = null;
        String[] preferredAbis = getSupportedAbis();

        try (ZipFile zip = new ZipFile(apkFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            
            // First pass: look for exact native lib entries under lib/<abi>/
            for (String tryAbi : preferredAbis) {
                String prefix = "lib/" + tryAbi + "/";
                Enumeration<? extends ZipEntry> e = zip.entries();
                while (e.hasMoreElements()) {
                    ZipEntry entry = e.nextElement();
                    String name = entry.getName();
                    if (name.startsWith(prefix) && name.endsWith(".so")) {
                        String fileName = name.substring(name.lastIndexOf('/') + 1);
                        File outFile = new File(targetDir, fileName);
                        try (InputStream is = zip.getInputStream(entry);
                             OutputStream os = new FileOutputStream(outFile)) {
                            byte[] buf = new byte[8192];
                            int r;
                            while ((r = is.read(buf)) != -1) {
                                os.write(buf, 0, r);
                            }
                        }
                        if (mainSo == null || isPreferredSoName(fileName)) {
                            mainSo = fileName;
                        }
                    }
                }
                if (mainSo != null) break; // Found native libs for preferred ABI
            }
        }
        return mainSo;
    }

    private static Map<String, String> extractEnvJsonFromApk(File apkFile, File targetDir) {
        Map<String, String> envMap = new HashMap<>();
        try (ZipFile zip = new ZipFile(apkFile)) {
            ZipEntry envEntry = zip.getEntry("assets/env.json");
            if (envEntry == null) envEntry = zip.getEntry("assets/plugin.json");
            if (envEntry != null) {
                try (InputStream is = zip.getInputStream(envEntry)) {
                    byte[] bytes = new byte[(int) envEntry.getSize()];
                    is.read(bytes);
                    String jsonStr = new String(bytes, StandardCharsets.UTF_8);
                    JSONObject json = new JSONObject(jsonStr);
                    if (json.has("env")) {
                        JSONObject envObj = json.getJSONObject("env");
                        for (java.util.Iterator<String> it = envObj.keys(); it.hasNext(); ) {
                            String k = it.next();
                            envMap.put(k, envObj.getString(k));
                        }
                    } else {
                        for (java.util.Iterator<String> it = json.keys(); it.hasNext(); ) {
                            String k = it.next();
                            if (json.get(k) instanceof String) {
                                envMap.put(k, json.getString(k));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "No env.json in APK: " + e.getMessage());
        }
        return envMap;
    }

    private static String findMainSoInDir(File dir) {
        String[] candidates = {
            "libfcl_render.so", "libkrypton.so", "libgl4es_115.so", "libgl4es_114.so", "libzink.so", "libvirgl.so", "libGL.so", "libGLESv2.so", "libEGL_angle.so"
        };
        for (String c : candidates) {
            if (new File(dir, c).exists()) return c;
        }
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().endsWith(".so")) return f.getName();
            }
        }
        return null;
    }

    private static boolean isPreferredSoName(String name) {
        return name.contains("gl4es") || name.contains("krypton") || name.contains("fcl") || name.contains("zink") || name.contains("virgl") || name.contains("GL");
    }

    private static String getTargetAbi() {
        if (Build.SUPPORTED_ABIS != null && Build.SUPPORTED_ABIS.length > 0) {
            return Build.SUPPORTED_ABIS[0];
        }
        return "arm64-v8a";
    }

    private static String[] getSupportedAbis() {
        if (Build.SUPPORTED_ABIS != null && Build.SUPPORTED_ABIS.length > 0) {
            return Build.SUPPORTED_ABIS;
        }
        return new String[]{"arm64-v8a", "armeabi-v7a", "x86_64", "x86"};
    }
}

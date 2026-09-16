package net.kdt.pojavlaunch.instances;

import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.tasks.MoJsonExtras;
import net.kdt.pojavlaunch.utils.JSONUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Instance extends DisplayInstance {
    public static final int ARGS_MODE_REPLACE = 0;
    public static final int ARGS_MODE_MERGE_DEFAULT_FIRST = 1;
    public static final int ARGS_MODE_MERGE_INSTANCE_FIRST = 2;
    public static final int ARGS_MODE_LAST = ARGS_MODE_MERGE_INSTANCE_FIRST;

    public static final String VERSION_LATEST_RELEASE = "latest_release";
    public static final String VERSION_LATEST_SNAPSHOT = "latest_snapshot";

    public InstanceInstaller installer;
    public String renderer;
    public String jvmArgs;
    public int argsMode;
    public String selectedRuntime;
    public String controlLayout;
    public boolean sharedData;

    protected Instance() {
    }

    @Override
    protected void sanitize() {
        super.sanitize();
        sanitizeArgs();
    }

    private void sanitizeArgs() {
        if(argsMode > ARGS_MODE_LAST) {
            argsMode = 0;
            jvmArgs = null;
        }
    }

    /**
     * Write the current contents of the instance to persistent storage.
     * @throws IOException in case of write errors
     */
    public void write() throws IOException {
        JSONUtils.writeToFile(Instances.metadataLocation(mInstanceRoot), this);
    }

    /**
     * Try to write the contents of the instance, ignore any exceptions
     */
    public void maybeWrite() {
        try {
            write();
        }catch (IOException e) {
            Log.e("Instance", "Failed to write",e);
        }
    }

    /**
     * Encode the Bitmap as the new profile icon with required encoding settings.
     * @param bitmap the target bitmap
     * @throws IOException in case of errors while storing the icon
     */
    public void encodeNewIcon(Bitmap bitmap) throws IOException {
        try(FileOutputStream fileOutputStream = new FileOutputStream(getInstanceIconLocation())) {
            bitmap.compress(
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.R ?
                            // On Android < 30, there was no distinction between "lossy" and "lossless",
                            // and the type is picked by the quality parameter. We set the quality to 60.
                            // so it should be lossy,
                            Bitmap.CompressFormat.WEBP:
                            // On Android >= 30, we can explicitly specify that we want lossy compression
                            // with the visual quality of 60.
                            Bitmap.CompressFormat.WEBP_LOSSY,
                    60,
                    fileOutputStream
            );
        }
    }

    public String getLaunchRenderer() {
        if(Tools.isValidString(renderer)) return renderer;
        return LauncherPreferences.PREF_RENDERER;
    }

    public String getLaunchArgs() {
        if(!Tools.isValidString(jvmArgs)) return LauncherPreferences.PREF_CUSTOM_JAVA_ARGS;
        switch (argsMode) {
            case ARGS_MODE_REPLACE:
                return jvmArgs;
            case ARGS_MODE_MERGE_DEFAULT_FIRST:
                return LauncherPreferences.PREF_CUSTOM_JAVA_ARGS + " " + jvmArgs;
            case ARGS_MODE_MERGE_INSTANCE_FIRST:
                return jvmArgs + " " + LauncherPreferences.PREF_CUSTOM_JAVA_ARGS;
            default:
                throw new RuntimeException("Unknown value for argsMode: "+argsMode);
        }
    }

    public String getLaunchControls() {
        if(!Tools.isValidString(controlLayout)) return LauncherPreferences.PREF_DEFAULTCTRL_PATH;
        return Tools.CTRLMAP_PATH + "/" + controlLayout;
    }

    public File getGameDirectory() {
        if(sharedData) return Instances.SHARED_DATA_DIRECTORY;
        return mInstanceRoot;
    }

    private static final Pattern MC_VERSION_PATTERN = Pattern.compile("(?:^|[^0-9])(1\\.[0-9]+(?:\\.[0-9]+)?)");

    public String getMinecraftVersion() {
        if (!Tools.isValidString(versionId)) return null;
        String normalized = MoJsonExtras.normalizeVersionId(versionId);
        if (!Tools.isValidString(normalized)) normalized = versionId;

        // Try exact match or extraction from normalized versionId
        String extracted = extractMcVersion(normalized);
        if (extracted != null) return extracted;

        // Try checking version JSON file if present
        try {
            net.kdt.pojavlaunch.JVersionList.Version verInfo = Tools.getVersionInfo(normalized, true);
            if (verInfo != null) {
                if (Tools.isValidString(verInfo.inheritsFrom)) {
                    extracted = extractMcVersion(verInfo.inheritsFrom);
                    if (extracted != null) return extracted;
                }
                if (Tools.isValidString(verInfo.id)) {
                    extracted = extractMcVersion(verInfo.id);
                    if (extracted != null) return extracted;
                }
            }
        } catch (Throwable ignored) {}

        return extractMcVersion(versionId);
    }

    private static String extractMcVersion(String str) {
        if (str == null || str.isEmpty()) return null;
        if (str.matches("1\\.[0-9]+(\\.[0-9]+)?")) {
            return str;
        }
        Matcher matcher = MC_VERSION_PATTERN.matcher(str);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public String getModLoaderType() {
        String full = (versionId != null ? versionId : "").toLowerCase();
        try {
            if (Tools.isValidString(versionId)) {
                String normalized = MoJsonExtras.normalizeVersionId(versionId);
                net.kdt.pojavlaunch.JVersionList.Version verInfo = Tools.getVersionInfo(normalized, true);
                if (verInfo != null && Tools.isValidString(verInfo.inheritsFrom)) {
                    full += " " + verInfo.inheritsFrom.toLowerCase();
                }
            }
        } catch (Throwable ignored) {}

        if (full.contains("neoforge")) return "neoforge";
        if (full.contains("fabric")) return "fabric";
        if (full.contains("quilt")) return "quilt";
        if (full.contains("forge")) return "forge";
        if (full.contains("optifine")) return "optifine";
        return null;
    }
}

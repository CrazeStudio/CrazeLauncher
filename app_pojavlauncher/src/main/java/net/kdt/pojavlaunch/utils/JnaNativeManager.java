package net.kdt.pojavlaunch.utils;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import net.kdt.pojavlaunch.Tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class JnaNativeManager {
    private static final String TAG = "JnaNativeManager";
    public static final String EXPECTED_JNA_NATIVE_704 = "7.0.4";
    public static final String JNA_NATIVE_616 = "6.1.6";

    /**
     * Inspects the classpath and version ID to determine the expected JNA native version.
     */
    public static String getExpectedJnaNativeVersion(List<String> classpath, String versionId) {
        String detectedVersion = null;
        if (classpath != null) {
            for (String entry : classpath) {
                if (entry == null) continue;
                File file = new File(entry);
                String name = file.getName();
                if (name.startsWith("jna-") && !name.startsWith("jna-platform-") && name.endsWith(".jar")) {
                    String jnaVer = extractJnaVersionFromJar(file);
                    if (jnaVer != null) {
                        detectedVersion = jnaVer;
                        Log.i(TAG, "Found JNA jar on classpath: " + name + " (JNA version: " + jnaVer + ")");
                        break;
                    }
                }
            }
        }

        if (detectedVersion != null) {
            String[] parts = detectedVersion.split("[.-]");
            try {
                int major = Integer.parseInt(parts[0]);
                int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
                if (major > 5 || (major == 5 && minor >= 17)) {
                    return EXPECTED_JNA_NATIVE_704;
                } else if (major == 5 && minor == 13) {
                    return JNA_NATIVE_616;
                }
            } catch (Exception ignored) {}
        }

        // Modern versions (Fabric, Minecraft 26.2, 1.20+) expect 7.0.4
        return EXPECTED_JNA_NATIVE_704;
    }

    private static String extractJnaVersionFromJar(File jarFile) {
        if (!jarFile.exists()) return null;
        try (JarFile jar = new JarFile(jarFile)) {
            Manifest manifest = jar.getManifest();
            if (manifest != null && manifest.getMainAttributes() != null) {
                String impVer = manifest.getMainAttributes().getValue("Implementation-Version");
                if (impVer != null && !impVer.trim().isEmpty()) {
                    return impVer.split(" ")[0].trim();
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to read manifest from " + jarFile.getName(), e);
        }
        // Fallback to jar name parsing, e.g. jna-5.18.0.jar -> 5.18.0
        String name = jarFile.getName();
        if (name.startsWith("jna-") && name.endsWith(".jar")) {
            return name.substring(4, name.length() - 4);
        }
        return null;
    }

    /**
     * Deduplicates and resolves JNA entries on the classpath to prevent classloader conflicts.
     */
    public static void sanitizeClasspath(List<String> classpath, String expectedNativeVersion) {
        if (classpath == null || classpath.size() <= 1) return;

        List<String> jnaEntries = new ArrayList<>();
        List<String> jnaPlatformEntries = new ArrayList<>();

        for (String entry : classpath) {
            if (entry == null) continue;
            File f = new File(entry);
            String name = f.getName();
            if (name.startsWith("jna-platform-") && name.endsWith(".jar")) {
                jnaPlatformEntries.add(entry);
            } else if (name.startsWith("jna-") && name.endsWith(".jar")) {
                jnaEntries.add(entry);
            }
        }

        if (jnaEntries.size() > 1) {
            Log.w(TAG, "Multiple JNA jars detected on classpath: " + jnaEntries);
            String preferredEntry = null;
            for (String entry : jnaEntries) {
                String ver = extractJnaVersionFromJar(new File(entry));
                if (EXPECTED_JNA_NATIVE_704.equals(expectedNativeVersion)) {
                    if (ver != null && (ver.startsWith("5.18") || ver.startsWith("5.17") || ver.startsWith("5.19"))) {
                        preferredEntry = entry;
                        break;
                    }
                } else if (JNA_NATIVE_616.equals(expectedNativeVersion)) {
                    if (ver != null && ver.startsWith("5.13")) {
                        preferredEntry = entry;
                        break;
                    }
                }
            }
            if (preferredEntry == null) preferredEntry = jnaEntries.get(jnaEntries.size() - 1);

            for (String entry : jnaEntries) {
                if (!entry.equals(preferredEntry)) {
                    Log.i(TAG, "Removing conflicting JNA jar from classpath: " + entry);
                    classpath.remove(entry);
                }
            }
            Log.i(TAG, "Retained consistent JNA jar on classpath: " + preferredEntry);
        }

        if (jnaPlatformEntries.size() > 1) {
            Log.w(TAG, "Multiple jna-platform jars detected on classpath: " + jnaPlatformEntries);
            String preferredPlatform = jnaPlatformEntries.get(jnaPlatformEntries.size() - 1);
            for (String entry : jnaPlatformEntries) {
                if (!entry.equals(preferredPlatform)) {
                    Log.i(TAG, "Removing conflicting jna-platform jar: " + entry);
                    classpath.remove(entry);
                }
            }
        }
    }

    /**
     * Reads native version string from libjnidispatch.so bytes.
     */
    public static String getNativeVersionFromSo(File soFile) {
        if (soFile == null || !soFile.exists()) return null;
        try (FileInputStream fis = new FileInputStream(soFile)) {
            byte[] buffer = new byte[(int) Math.min(soFile.length(), 256 * 1024)];
            int read = fis.read(buffer);
            if (read <= 0) return null;
            String text = new String(buffer, 0, read, StandardCharsets.ISO_8859_1);
            if (text.contains("7.0.4")) return "7.0.4";
            if (text.contains("6.1.6")) return "6.1.6";
            if (text.contains("7.0.3")) return "7.0.3";
            if (text.contains("7.0.2")) return "7.0.2";
            if (text.contains("7.0.0")) return "7.0.0";
            if (text.contains("5.13.0")) return "6.1.6";
        } catch (Exception e) {
            Log.w(TAG, "Error checking version in " + soFile.getAbsolutePath(), e);
        }
        return null;
    }

    /**
     * Validates that the ELF binary header matches the target Android ABI.
     */
    public static boolean validateElfArchitecture(File soFile, String abi) {
        if (soFile == null || !soFile.exists() || soFile.length() < 52) return false;
        try (FileInputStream fis = new FileInputStream(soFile)) {
            byte[] header = new byte[52];
            int read = fis.read(header);
            if (read < 52) return false;
            // ELF magic: 0x7F 'E' 'L' 'F'
            if (header[0] != 0x7F || header[1] != 'E' || header[2] != 'L' || header[3] != 'F') {
                return false;
            }
            int elfClass = header[4] & 0xFF; // 1 = 32-bit, 2 = 64-bit
            int machine = (header[18] & 0xFF) | ((header[19] & 0xFF) << 8); // Little-endian

            if ("arm64-v8a".equals(abi)) {
                return elfClass == 2 && machine == 183; // EM_AARCH64
            } else if ("armeabi-v7a".equals(abi)) {
                return elfClass == 1 && machine == 40; // EM_ARM
            } else if ("x86_64".equals(abi)) {
                return elfClass == 2 && machine == 62; // EM_X86_64
            } else if ("x86".equals(abi)) {
                return elfClass == 1 && machine == 3; // EM_386
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Failed to validate ELF header for " + soFile.getAbsolutePath(), e);
            return false;
        }
    }

    /**
     * Removes stale 6.1.6 libjnidispatch.so files from cache directories.
     */
    public static void cleanStaleJnaCache() {
        try {
            File nativesCacheDir = new File(Tools.DIR_CACHE, "natives");
            if (nativesCacheDir.exists() && nativesCacheDir.isDirectory()) {
                File[] versionDirs = nativesCacheDir.listFiles();
                if (versionDirs != null) {
                    for (File dir : versionDirs) {
                        if (dir.isDirectory()) {
                            File so = new File(dir, "libjnidispatch.so");
                            if (so.exists()) {
                                String ver = getNativeVersionFromSo(so);
                                if (JNA_NATIVE_616.equals(ver)) {
                                    Log.i(TAG, "Cleaning stale 6.1.6 libjnidispatch.so from " + so.getAbsolutePath());
                                    so.delete();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Stale cache cleanup encountered an exception", e);
        }
    }

    /**
     * Ensures that the versionSpecificNativesDir contains a matching, architecture-verified libjnidispatch.so.
     */
    public static File ensureJnaNative(File targetDir, String versionId, String expectedVersion, String primaryAbi, Context context) {
        FileUtils.ensureDirectorySilently(targetDir);
        File activeSo = new File(targetDir, "libjnidispatch.so");
        String cacheAction = "none";
        boolean elfValid = false;

        if (activeSo.exists()) {
            String existingVer = getNativeVersionFromSo(activeSo);
            elfValid = validateElfArchitecture(activeSo, primaryAbi);
            if (expectedVersion.equals(existingVer) && elfValid) {
                cacheAction = "reused from cache";
                Log.i(TAG, "Reusing valid cached libjnidispatch.so (version " + existingVer + ") at " + activeSo.getAbsolutePath());
            } else {
                Log.w(TAG, "Stale/incompatible libjnidispatch.so detected: version=" + existingVer + ", expected=" + expectedVersion + ", elfValid=" + elfValid + ". Removing stale file.");
                activeSo.delete();
            }
        }

        if (!activeSo.exists()) {
            cacheAction = "extracted/installed";
            // 1. Check bundled Tools.NATIVE_LIB_DIR/libjnidispatch.so
            File bundledSo = new File(Tools.NATIVE_LIB_DIR, "libjnidispatch.so");
            if (bundledSo.exists()) {
                String bundledVer = getNativeVersionFromSo(bundledSo);
                boolean bundledElfValid = validateElfArchitecture(bundledSo, primaryAbi);
                if (expectedVersion.equals(bundledVer) && bundledElfValid) {
                    try {
                        java.nio.file.Files.copy(bundledSo.toPath(), activeSo.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        Log.i(TAG, "Copied matching bundled JNA native (" + bundledVer + ") from " + bundledSo.getAbsolutePath() + " to " + activeSo.getAbsolutePath());
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to copy bundled JNA native", e);
                    }
                }
            }

            // 2. Check downloaded AAR files in libraries directory
            if (!activeSo.exists()) {
                extractFromDownloadedAar(activeSo, expectedVersion, primaryAbi);
            }

            // 3. Fallback: extract from APK assets/lib
            if (!activeSo.exists() && context != null) {
                extractFromApk(activeSo, primaryAbi, context);
            }
        }

        String finalVer = getNativeVersionFromSo(activeSo);
        elfValid = validateElfArchitecture(activeSo, primaryAbi);

        // Required debug logging (User Requirement 9)
        Log.i(TAG, "=== JNA Subsystem Diagnostics ===");
        Log.i(TAG, "JNA Java version: " + expectedVersion);
        Log.i(TAG, "JNA native version if detectable: " + finalVer);
        Log.i(TAG, "selected ABI: " + primaryAbi);
        Log.i(TAG, "native directory: " + targetDir.getAbsolutePath());
        Log.i(TAG, "absolute libjnidispatch.so path: " + activeSo.getAbsolutePath());
        Log.i(TAG, "whether the file exists: " + activeSo.exists());
        Log.i(TAG, "whether the native is being extracted or reused from cache: " + cacheAction);
        Log.i(TAG, "ELF architecture verified: " + elfValid);
        Log.i(TAG, "=================================");

        if (activeSo.exists() && elfValid) {
            try {
                System.load(activeSo.getAbsolutePath());
                Log.i(TAG, "libjnidispatch.so loaded successfully in host process (version " + finalVer + ")");
            } catch (Throwable t) {
                Log.i(TAG, "Host process libjnidispatch.so pre-verification note: " + t.getMessage());
            }
        }

        return activeSo;
    }

    private static void extractFromDownloadedAar(File destination, String expectedVersion, String abi) {
        try {
            File libsDir = new File(Tools.DIR_HOME_LIBRARY, "net/java/dev/jna/jna");
            if (!libsDir.exists() || !libsDir.isDirectory()) return;

            File[] versionDirs = libsDir.listFiles();
            if (versionDirs == null) return;

            for (File vDir : versionDirs) {
                File[] aars = vDir.listFiles((dir, name) -> name.endsWith(".aar"));
                if (aars == null) continue;
                for (File aar : aars) {
                    try (ZipFile zip = new ZipFile(aar)) {
                        ZipEntry entry = zip.getEntry("jni/" + abi + "/libjnidispatch.so");
                        if (entry != null) {
                            try (InputStream is = zip.getInputStream(entry);
                                 FileOutputStream fos = new FileOutputStream(destination)) {
                                byte[] buf = new byte[8192];
                                int len;
                                while ((len = is.read(buf)) > 0) {
                                    fos.write(buf, 0, len);
                                }
                            }
                            String extractedVer = getNativeVersionFromSo(destination);
                            if (expectedVersion.equals(extractedVer) && validateElfArchitecture(destination, abi)) {
                                Log.i(TAG, "Extracted matching JNA native (" + extractedVer + ") from AAR " + aar.getName());
                                return;
                            } else {
                                destination.delete();
                            }
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Error inspecting AAR " + aar.getName(), e);
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed searching for JNA AARs", e);
        }
    }

    private static void extractFromApk(File destination, String abi, Context context) {
        try {
            String apkPath = context.getPackageCodePath();
            try (ZipFile zip = new ZipFile(new File(apkPath))) {
                ZipEntry entry = zip.getEntry("lib/" + abi + "/libjnidispatch.so");
                if (entry != null) {
                    try (InputStream is = zip.getInputStream(entry);
                         FileOutputStream fos = new FileOutputStream(destination)) {
                        byte[] buf = new byte[8192];
                        int len;
                        while ((len = is.read(buf)) > 0) {
                            fos.write(buf, 0, len);
                        }
                    }
                    Log.i(TAG, "Extracted libjnidispatch.so from APK package");
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed extracting libjnidispatch.so from APK", e);
        }
    }
}

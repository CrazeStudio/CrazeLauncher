package net.kdt.pojavlaunch;

import net.kdt.pojavlaunch.utils.JnaNativeManager;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JnaNativeManagerTest {

    @Test
    public void testExpectedJnaNativeVersionDefaultsTo704() {
        String version = JnaNativeManager.getExpectedJnaNativeVersion(new ArrayList<>(), "fabric-loader-0.19.3-26.2");
        Assert.assertEquals("Expected JNA 7.0.4 for modern Fabric/MC 26.2", "7.0.4", version);
    }

    @Test
    public void testSanitizeClasspathRemovesDuplicates() {
        List<String> classpath = new ArrayList<>(Arrays.asList(
                "/path/to/jna-5.13.0.jar",
                "/path/to/jna-5.18.0.jar",
                "/path/to/some-other-lib.jar"
        ));
        JnaNativeManager.sanitizeClasspath(classpath, "7.0.4");
        Assert.assertTrue("Should keep jna-5.18.0.jar", classpath.contains("/path/to/jna-5.18.0.jar"));
        Assert.assertFalse("Should remove older jna-5.13.0.jar", classpath.contains("/path/to/jna-5.13.0.jar"));
        Assert.assertTrue("Should keep unrelated lib", classpath.contains("/path/to/some-other-lib.jar"));
    }

    @Test
    public void testBundledArm64JnaNativeVersionIs704() {
        File so = new File("src/main/jniLibs/arm64-v8a/libjnidispatch.so");
        if (!so.exists()) {
            // Check relative path from root
            so = new File("app_pojavlauncher/src/main/jniLibs/arm64-v8a/libjnidispatch.so");
        }
        Assert.assertTrue("arm64-v8a libjnidispatch.so exists", so.exists());
        String ver = JnaNativeManager.getNativeVersionFromSo(so);
        Assert.assertEquals("Bundled arm64 libjnidispatch.so version must be 7.0.4", "7.0.4", ver);
        boolean validElf = JnaNativeManager.validateElfArchitecture(so, "arm64-v8a");
        Assert.assertTrue("ELF header must match arm64-v8a", validElf);
    }

    @Test
    public void testAllBundledJnaNativesAre704AndValidArchitecture() {
        String[] abis = {"arm64-v8a", "armeabi-v7a", "x86", "x86_64"};
        for (String abi : abis) {
            File so = new File("src/main/jniLibs/" + abi + "/libjnidispatch.so");
            if (!so.exists()) {
                so = new File("app_pojavlauncher/src/main/jniLibs/" + abi + "/libjnidispatch.so");
            }
            Assert.assertTrue("libjnidispatch.so exists for " + abi, so.exists());
            String ver = JnaNativeManager.getNativeVersionFromSo(so);
            Assert.assertEquals("Version must be 7.0.4 for " + abi, "7.0.4", ver);
            boolean valid = JnaNativeManager.validateElfArchitecture(so, abi);
            Assert.assertTrue("Architecture must be valid for " + abi, valid);
        }
    }
}

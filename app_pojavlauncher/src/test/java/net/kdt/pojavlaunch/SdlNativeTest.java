package net.kdt.pojavlaunch;

import net.kdt.pojavlaunch.utils.JnaNativeManager;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class SdlNativeTest {

    private static File findSo(String abi, String name) {
        File so = new File("src/main/jniLibs/" + abi + "/" + name);
        if (!so.exists()) {
            so = new File("app_pojavlauncher/src/main/jniLibs/" + abi + "/" + name);
        }
        return so;
    }

    @Test
    public void testArm64SDL3ExistsAndValidElf() throws IOException {
        File so = findSo("arm64-v8a", "libSDL3.so");
        Assert.assertTrue("arm64-v8a libSDL3.so must exist", so.exists());
        Assert.assertTrue("libSDL3.so must be an ARM64-v8a ELF binary",
                JnaNativeManager.validateElfArchitecture(so, "arm64-v8a"));

        // Verify SDL symbols are present in binary
        byte[] data = new byte[(int) so.length()];
        try (FileInputStream fis = new FileInputStream(so)) {
            int read = 0;
            while (read < data.length) {
                int r = fis.read(data, read, data.length - read);
                if (r < 0) break;
                read += r;
            }
        }
        String binaryContent = new String(data, java.nio.charset.StandardCharsets.ISO_8859_1);
        Assert.assertTrue("Must contain SDL_Init symbol", binaryContent.contains("SDL_Init"));
        Assert.assertTrue("Must contain SDL_Quit symbol", binaryContent.contains("SDL_Quit"));
        Assert.assertTrue("Must contain Java_git_mojo_sdl JNI symbol", binaryContent.contains("Java_git_mojo_sdl"));
    }

    @Test
    public void testAllAbisIncludeSdl3AndMojoexec() {
        String[] abis = {"arm64-v8a", "armeabi-v7a", "x86", "x86_64"};
        for (String abi : abis) {
            File sdlSo = findSo(abi, "libSDL3.so");
            Assert.assertTrue("libSDL3.so must exist for " + abi, sdlSo.exists());
            Assert.assertTrue("libSDL3.so architecture must match " + abi,
                    JnaNativeManager.validateElfArchitecture(sdlSo, abi));

            File mojoexecSo = findSo(abi, "libmojoexec.so");
            Assert.assertTrue("libmojoexec.so must exist for " + abi, mojoexecSo.exists());
            Assert.assertTrue("libmojoexec.so architecture must match " + abi,
                    JnaNativeManager.validateElfArchitecture(mojoexecSo, abi));
        }
    }

    @Test
    public void testAllMojoLauncherNativesPresentForArm64() {
        String[] requiredLibs = {
                "libSDL3.so",
                "libmojoexec.so",
                "libglfw.so",
                "libpojavexec.so",
                "libpojavexec_awt.so",
                "libawt_headless.so",
                "libawt_xawt.so",
                "libdrm.so",
                "libEGL_mesa.so",
                "libgallium_dri.so",
                "libopenal.so",
                "libshaderc.so",
                "libspirv-cross-c-shared.so",
                "libvulkan_freedreno.so"
        };
        for (String lib : requiredLibs) {
            File so = findSo("arm64-v8a", lib);
            Assert.assertTrue("Required native library " + lib + " must exist for arm64-v8a", so.exists());
        }
    }
}

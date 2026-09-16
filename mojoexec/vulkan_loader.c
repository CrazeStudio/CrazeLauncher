//
// Created by maks on 10.04.2026.
//

#include <android/api-level.h>
#include <stdio.h>
#include <dlfcn.h>
#include <stdlib.h>
#include <jni.h>

#include <driver_helper/nsbypass.h>
#include <android/dlext.h>
#include <mojoexec.h>

static bool turnip_enabled = false;

#ifdef ENABLE_TURNIP_LOADER
bool load_turnip_vulkan() {
    static bool driver_loaded = false;
    if(driver_loaded) return true;

    const char* cache_dir = getenv("TMPDIR");
    if(!linker_ns_load(mojoexec_native_dir)) return NULL;
    void* linkerhook = linker_ns_dlopen("liblinkerhook.so", RTLD_LOCAL | RTLD_NOW);
    if(linkerhook == NULL) return NULL;
    void* turnip_driver_handle = linker_ns_dlopen("libvulkan_freedreno.so", RTLD_LOCAL | RTLD_NOW);
    if(turnip_driver_handle == NULL) {
        printf("MojoExec: Failed to load Turnip!\n%s\n", dlerror());
        goto fail_l;
    }

    void* dl_android = linker_ns_dlopen("libdl_android.so", RTLD_LOCAL | RTLD_LAZY);
    if(dl_android == NULL) goto fail_t;

    void* android_get_exported_namespace = dlsym(dl_android, "android_get_exported_namespace");
    void (*linkerhook_pass_handles)(void*, void*, void*) = dlsym(linkerhook, "app__pojav_linkerhook_pass_handles");

    if(linkerhook_pass_handles == NULL || android_get_exported_namespace == NULL) goto fail_d;
    linkerhook_pass_handles(turnip_driver_handle, android_dlopen_ext, android_get_exported_namespace);

    void* libvulkan = linker_ns_dlopen_unique(cache_dir, "libvulkan.so", "libmjlvlk.so", RTLD_LOCAL | RTLD_NOW);
    printf("MojoExec: Loaded mjlvlk, ptr=%p\n", libvulkan);
    if(libvulkan) {
        driver_loaded = true;
        return true;
    }
    fail_d: dlclose(dl_android);
    fail_t: dlclose(turnip_driver_handle);
    fail_l: dlclose(linkerhook);
    return false;
}
#endif

void* mojoexec_acq_vulkan_handle() {
    int flags = RTLD_LOCAL | RTLD_NOW;
#ifdef ENABLE_TURNIP_LOADER
    if(android_get_device_api_level() >= 28) { // the loader does not support below that
        if(turnip_enabled && load_turnip_vulkan())
            // Reference the vulkan driver separately to avoid weirdness from libraries calling dlclose
            return linker_ns_dlopen("libmjlvlk.so", flags);
    }
#endif
    void* vulkan_ptr = dlopen("libvulkan.so", flags);
    printf("MojoExec: loaded system vulkan, ptr=%p\n", vulkan_ptr);
    return vulkan_ptr;
}

JNIEXPORT void JNICALL
Java_git_artdeell_mojoexec_MojoExec_setUseTurnip(JNIEnv *env, jclass clazz, jboolean enable) {
    turnip_enabled = enable;
}

// Does nothing if Turnip is unsupported - Mesa will load system driver automatically
JNIEXPORT void JNICALL
Java_git_artdeell_mojoexec_MojoExec_preloadVulkan(JNIEnv *env, jclass clazz) {
#ifdef ENABLE_TURNIP_LOADER
    if(!turnip_enabled) return;
    if(!load_turnip_vulkan()) {
        printf("MojoExec: Failed to preload Turnip!\n");
    }
#endif
}
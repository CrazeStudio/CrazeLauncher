//
// Created by maks on 30.06.2026.
//

#include <mojoexec.h>
#include <jni.h>
#include <string.h>
#include <stdlib.h>
#include <driver_helper/nsbypass.h>
#include <dlfcn.h>

#include <unistd.h>
#include <stdio.h>

mojoexec_renderspec_t mojoexec_renderspec;

const char* mojoexec_native_dir = NULL;
char* native_egl_path = NULL;
bool egl_use_bypass = false;

static void save_jvm_string(JNIEnv* env, char** target, jstring str) {
    if(*target != NULL) free(*target);
    if(str == NULL) {
        *target = NULL;
        return;
    }

    const char* path = (*env)->GetStringUTFChars(env, str, NULL);
    *target = strdup(path);
    (*env)->ReleaseStringUTFChars(env, str, path);
}

void* mojoexec_acq_egl_handle() {
    int flags = RTLD_GLOBAL | RTLD_NOW;
    if(native_egl_path == NULL) return NULL;

    void* handle = NULL;
    char resolved_path[1024];
    const char* path_to_open = native_egl_path;

    if(native_egl_path[0] != '/' && mojoexec_native_dir != NULL) {
        snprintf(resolved_path, sizeof(resolved_path), "%s/%s", mojoexec_native_dir, native_egl_path);
        if(access(resolved_path, F_OK) == 0) {
            path_to_open = resolved_path;
        }
    }

    if(egl_use_bypass) {
        handle = linker_ns_dlopen(path_to_open, flags);
    }
    if(!handle) {
        handle = dlopen(path_to_open, flags);
    }
    if(!handle && path_to_open != native_egl_path) {
        handle = dlopen(native_egl_path, flags);
    }
    return handle;
}

JNIEXPORT jboolean JNICALL
Java_git_artdeell_mojoexec_MojoExec_prepareEgl(JNIEnv *env, jclass clazz, jstring egl_path,
                                               jboolean use_bypass, jboolean use_gles,
                                               jint gles_version) {
    save_jvm_string(env, &native_egl_path, egl_path);
    if(native_egl_path == NULL) return false;
    if(use_bypass && !linker_ns_load(mojoexec_native_dir)) {
        printf("MojoExec: linker_ns_load failed, falling back to standard loader\n");
        use_bypass = false;
    }
    egl_use_bypass = use_bypass;
    void* preload_handle = mojoexec_acq_egl_handle();
    if(!preload_handle) {
        printf("MojoExec: Failed to load EGL (%s): %s\n", native_egl_path, dlerror());
        return false;
    }
    printf("MojoExec: Successfully loaded EGL %s (in namespace bypass: %i)\n", native_egl_path, egl_use_bypass);
    mojoexec_renderspec.force_gles_context = use_gles;
    mojoexec_renderspec.override_major_version = gles_version;
    return true;
}

JNIEXPORT void JNICALL
Java_git_artdeell_mojoexec_MojoExec_setNativeLibraryDir(JNIEnv *env, jclass clazz, jstring dir) {
    save_jvm_string(env, (char **) &mojoexec_native_dir, dir);
    return;
}

JNIEXPORT void JNICALL
Java_git_artdeell_mojoexec_MojoExec_setDisplayParams(JNIEnv *env, jclass clazz, jint width,
                                                     jint height, jfloat hz) {
    // TODO: implement setDisplayParams()
    mojoexec_renderspec.disp_width = width;
    mojoexec_renderspec.disp_height = height;
    mojoexec_renderspec.disp_hz = hz;
}


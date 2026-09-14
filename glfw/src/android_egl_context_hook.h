//
// Created by maks on 15.01.2026.
//

#ifndef DNBOOTSTRAP_ANDROID_EGL_CONTEXT_HOOK_H
#define DNBOOTSTRAP_ANDROID_EGL_CONTEXT_HOOK_H

// On Android the window is managed by the OS, which means that the EGLSurface will become invalid
// as soon as the user puts the app into background. To cope with this, the Android backend must
// add a few hooks in order to manage the EGL surfaces on its own.

// Select a new EGLSurface
EGLSurface _glfwManageEglSurfaceAndroid(_GLFWwindow*);
// Check if the surface has been changed before attempting to swap buffers
GLFWbool _glfwSwapBuffersAttentionEglAndroid(_GLFWwindow*);

void* _glfwLoadEglAndroid(void);

#endif //DNBOOTSTRAP_ANDROID_EGL_CONTEXT_HOOK_H

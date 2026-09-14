//========================================================================
// GLFW 3.4 - www.glfw.org
//------------------------------------------------------------------------
// Copyright (c) 2016-2017 Camilla Löwy <elmindreda@glfw.org>
//
// This software is provided 'as-is', without any express or implied
// warranty. In no event will the authors be held liable for any damages
// arising from the use of this software.
//
// Permission is granted to anyone to use this software for any purpose,
// including commercial applications, and to alter it and redistribute it
// freely, subject to the following restrictions:
//
// 1. The origin of this software must not be misrepresented; you must not
//    claim that you wrote the original software. If you use this software
//    in a product, an acknowledgment in the product documentation would
//    be appreciated but is not required.
//
// 2. Altered source versions must be plainly marked as such, and must not
//    be misrepresented as being the original software.
//
// 3. This notice may not be removed or altered from any source
//    distribution.
//
//========================================================================

#include "internal.h"
#include <stdlib.h>

//////////////////////////////////////////////////////////////////////////
//////                       GLFW platform API                      //////
//////////////////////////////////////////////////////////////////////////

static const char* android_joystick_guid = "616e64726f6964000000000000000000";
static _GLFWjoystick *android_joystick = NULL;
static GLFWbool initialized = false;
static _Atomic GLFWbool joystick_connected = false;

extern void _glfwEnableGamepadAndroid(unsigned char* buttons, int buttonCount, float* axes, int axisCount);
extern void _glfwSendJoystickConnectEvent(void);

GLFWbool _glfwInitJoysticksAndroid(void)
{
    const int buttonCount = 14;
    const int axisCount = 6;
    if(!android_joystick) android_joystick = _glfwAllocJoystick("DNB-GLFW Joystick", android_joystick_guid, axisCount, buttonCount, 0);
    if(!android_joystick) return GLFW_FALSE;
    _glfwEnableGamepadAndroid(android_joystick->buttons, buttonCount, android_joystick->axes, axisCount);
    if(joystick_connected) _glfwInputJoystick(android_joystick, GLFW_CONNECTED);
    initialized = true;
    return GLFW_TRUE;
}

void _glfwUpdateJoystickConnectSate(void) {
    if(!initialized) return;
    bool connected = joystick_connected;

    if(initialized && android_joystick->connected != connected) {
        _glfwInputJoystick(android_joystick, connected ? GLFW_CONNECTED : GLFW_DISCONNECTED);
    }
}

JNIEXPORT void JNICALL
Java_git_artdeell_dnbootstrap_glfw_GLFW_nativeNotifyGamepadConnected(JNIEnv *env, jclass clazz) {
    bool wasConnected = joystick_connected;
    joystick_connected = true;
    if(!wasConnected) _glfwSendJoystickConnectEvent();
}

void _glfwTerminateJoysticksAndroid(void)
{
    initialized = false;
    if(android_joystick != NULL && android_joystick->connected) _glfwInputJoystick(android_joystick, GLFW_DISCONNECTED);
}

GLFWbool _glfwPollJoystickAndroid(_GLFWjoystick* js, int mode)
{
    if(js != android_joystick) return GLFW_FALSE;
    return initialized ? GLFW_TRUE : GLFW_FALSE;
}

const char* _glfwGetMappingNameAndroid(void)
{
    return "";
}

void _glfwUpdateGamepadGUIDAndroid(char* guid)
{
}


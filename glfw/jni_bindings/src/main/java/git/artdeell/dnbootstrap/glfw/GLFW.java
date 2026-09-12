package git.artdeell.dnbootstrap.glfw;

import java.nio.ByteBuffer;

public class GLFW {
    public static final int GLFW_HOVERED = 1;
    public static final int GLFW_VISIBLE = 2;
    public static ByteBuffer gamepadButtonBuffer;
    public static ByteBuffer gamepadAxisBuffer;

    public static void setGrabListener(Object listener) {}
    public static void setPositionCallback(Object callback) {}
    public static void setCursorCallback(Object callback) {}
    public static void nativeSurfaceCreated(Object surface) {}
    public static void nativeSurfaceUpdated() {}
    public static void nativeSurfaceDestroyed() {}
    public static void sendMousePosition0(double x, double y) {}
    public static void sendMouseEvent(int button, int action, int mods) {}
    public static boolean sendRawKeyEvent(int key, int state, int mods, char codepoint) { return false; }
    public static void sendScrollEvent(double x, double y) {}
    public static void sendBulkUnicodeEvent(String text, int mods) {}
    public static void nativeSetWindowAttribs(int attrib, boolean value) {}
    public static void setInitCallback(Runnable callback) {}
    public static void setClipboardImpl(Object clipboard) {}
    public static void setGamepadEnableHandler(Runnable handler) {}
    public static void nativeNotifyGamepadConnected() {}
}

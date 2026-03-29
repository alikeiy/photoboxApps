package com.serenegiant.utils;

import com.serenegiant.system.BuildCheck;

/**
 * Compatibility Shim for BuildCheck.
 */
public class BuildCheck {
    public static boolean isAPI23() { return com.serenegiant.system.BuildCheck.isMarshmallow(); }
    public static boolean isAPI31() { return com.serenegiant.system.BuildCheck.isS(); }
    public static boolean isAndroid10() { return com.serenegiant.system.BuildCheck.isAndroid10(); }
    public static int getAndroidVersion() { return android.os.Build.VERSION.SDK_INT; }
}

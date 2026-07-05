package com.serenegiant.utils;

import android.os.Build;

public class BuildCheck {
    public static boolean isAPI23() { return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M; }
    public static boolean isAPI31() { return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S; }
}

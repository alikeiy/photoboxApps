package com.serenegiant.utils;

import android.content.Context;
import androidx.annotation.NonNull;
import com.serenegiant.system.PermissionUtils;

/**
 * Compatibility Shim for PermissionCheck.
 */
public class PermissionCheck {
    public static boolean hasCamera(@NonNull final Context context) {
        return PermissionUtils.hasCamera(context);
    }
    public static boolean hasAudio(@NonNull final Context context) {
        return PermissionUtils.hasAudio(context);
    }
    public static boolean hasWriteExternalStorage(@NonNull final Context context) {
        return PermissionUtils.hasWriteExternalStorage(context);
    }
}

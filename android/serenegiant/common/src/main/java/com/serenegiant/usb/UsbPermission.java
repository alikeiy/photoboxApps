package com.serenegiant.usb;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.serenegiant.app.PendingIntentCompat;
import com.serenegiant.system.BuildCheck;

/**
 * Compatibility Shim for UsbPermission with FLAG_MUTABLE fix for Android 12+.
 */
public class UsbPermission {
    private static final String TAG = UsbPermission.class.getSimpleName();
    private static final boolean DEBUG = false;
    private static final String ACTION_USB_PERMISSION = "com.serenegiant.USB_PERMISSION";

    public interface Callback extends com.serenegiant.usb.UsbPermission.Callback {
        @Override default void onPermission(@NonNull UsbDevice device, boolean canAccess) {}
        @Override default void onCancel(@NonNull UsbDevice device) {}
    }

    @SuppressLint({"WrongConstant"})
    private static PendingIntent createIntent(@NonNull final Context context) {
        int flags = 0;
        if (BuildCheck.isAPI31()) {
            flags |= PendingIntentCompat.FLAG_MUTABLE;
        } else if (BuildCheck.isAPI23()) {
            flags |= 0; 
        }
        final Intent intent = new Intent(ACTION_USB_PERMISSION);
        intent.setPackage(context.getPackageName());
        return PendingIntent.getBroadcast(context, 0, intent, flags);
    }

    public static void requestPermission(@NonNull final Context context, @NonNull final UsbDevice device, @Nullable final Callback callback) {
        final UsbManager manager = (UsbManager)context.getSystemService(Context.USB_SERVICE);
        if (manager.hasPermission(device)) {
            if (callback != null) callback.onPermission(device, true);
            return;
        }
        final DevicePermissionReceiver receiver = new DevicePermissionReceiver(device, callback);
        ContextCompat.registerReceiver(context, receiver, new IntentFilter(ACTION_USB_PERMISSION), ContextCompat.RECEIVER_EXPORTED);
        try {
            final PendingIntent intent = createIntent(context);
            manager.requestPermission(device, intent);
        } catch (final Exception e) {
            Log.w(TAG, e);
            context.unregisterReceiver(receiver);
            if (callback != null) callback.onPermission(device, false);
        }
    }

    private static class DevicePermissionReceiver extends android.content.BroadcastReceiver {
        private final UsbDevice mDevice;
        private final Callback mCallback;
        DevicePermissionReceiver(UsbDevice device, Callback callback) {
            this.mDevice = device;
            this.mCallback = callback;
        }
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_USB_PERMISSION.equals(intent.getAction())) {
                synchronized (this) {
                    final UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (mDevice.equals(device)) {
                        if (mCallback != null) {
                            mCallback.onPermission(device, intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false));
                        }
                    }
                }
                context.unregisterReceiver(this);
            }
        }
    }
}

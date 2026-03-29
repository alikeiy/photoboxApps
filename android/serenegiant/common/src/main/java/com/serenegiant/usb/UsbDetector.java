package com.serenegiant.usb;

import android.hardware.usb.UsbDevice;
import androidx.annotation.NonNull;

/**
 * Compatibility Shim for UsbDetector.
 */
public interface UsbDetector extends com.serenegiant.usb.UsbDetector {
    @Override default void onAttach(@NonNull final UsbDevice device) {}
    @Override default void onDettach(@NonNull final UsbDevice device) {}
}

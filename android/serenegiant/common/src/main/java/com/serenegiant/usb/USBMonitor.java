package com.serenegiant.usb;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.List;

/**
 * Compatibility Shim for USBMonitor to bridge legacy react-native-uvc-camera
 * with the updated serenegiant common library.
 */
public class USBMonitor extends com.serenegiant.usb.USBMonitor {

    public interface OnDeviceConnectListener extends com.serenegiant.usb.USBMonitor.OnDeviceConnectListener {
        @Override default void onAttach(UsbDevice device) {}
        @Override default void onDettach(UsbDevice device) {}
        @Override default void onConnect(UsbDevice device, UsbControlBlock ctrlBlock, boolean createNew) {}
        @Override default void onDisconnect(UsbDevice device, UsbControlBlock ctrlBlock) {}
        @Override default void onCancel(UsbDevice device) {}
    }

    public USBMonitor(@NonNull Context context, @NonNull OnDeviceConnectListener listener) {
        super(context, listener);
    }

    public interface ConnectionCallback extends com.serenegiant.usb.USBMonitor.ConnectionCallback {
        @Override default void onConnect(@NonNull UsbDevice device, @NonNull UsbControlBlock ctrlBlock, boolean createNew) {}
        @Override default void onDisconnect(@NonNull UsbDevice device, @NonNull UsbControlBlock ctrlBlock) {}
    }

    public static class UsbControlBlock extends com.serenegiant.usb.USBMonitor.UsbControlBlock {
        public UsbControlBlock(com.serenegiant.usb.USBMonitor parent, UsbDevice device) {
            super(parent, device);
        }
        // Legacy shims
        public int getVenderId() { return getVendorId(); }
        public int getBusNum() { return 0; }
        public int getDevNum() { return 0; }
    }

    public List<UsbDevice> getDeviceList(DeviceFilter filter) {
        return getDeviceList();
    }
}

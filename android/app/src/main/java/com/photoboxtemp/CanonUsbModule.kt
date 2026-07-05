package com.photoboxtemp

import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.util.Log
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.annotations.ReactModule
import com.photoboxtemp.specs.NativeCanonUsbSpec

/**
 * TurboModule skeleton for Canon EOS M10 control over USB OTG (PTP).
 *
 * Phase 1 (current): stub methods with logging — fill in USB permission,
 * open/claim, and PTP shutter commands in subsequent iterations.
 */
@ReactModule(name = CanonUsbModule.NAME)
class CanonUsbModule(
    reactContext: ReactApplicationContext,
) : NativeCanonUsbSpec(reactContext) {

    companion object {
        const val NAME: String = NativeCanonUsbSpec.NAME
        private const val TAG = "CanonUsbModule"
        private const val CANON_VENDOR_ID = 0x04A9
    }

    private val usbManager: UsbManager?
        get() = reactApplicationContext.getSystemService(UsbManager::class.java)

    override fun initializeUsb(promise: Promise) {
        try {
            val hostSupported = reactApplicationContext.packageManager
                .hasSystemFeature(PackageManager.FEATURE_USB_HOST)
            val manager = usbManager

            Log.i(TAG, "initializeUsb() — FEATURE_USB_HOST=$hostSupported, usbManager=${manager != null}")

            if (!hostSupported) {
                Log.w(TAG, "initializeUsb() — tablet does not report USB Host support")
                promise.resolve(false)
                return
            }

            if (manager == null) {
                Log.e(TAG, "initializeUsb() — UsbManager service unavailable")
                promise.resolve(false)
                return
            }

            val devices = manager.deviceList.values
            Log.i(TAG, "initializeUsb() — ${devices.size} USB device(s) connected")

            devices.forEach { device ->
                Log.i(
                    TAG,
                    "  vid=0x${device.vendorId.toString(16)} " +
                        "pid=0x${device.productId.toString(16)} " +
                        "class=${device.deviceClass} " +
                        "canon=${device.vendorId == CANON_VENDOR_ID}",
                )
            }

            // Skeleton: success when host is supported and UsbManager is ready.
            promise.resolve(true)
        } catch (t: Throwable) {
            Log.e(TAG, "initializeUsb() failed", t)
            promise.reject("USB_INIT_ERROR", t.message, t)
        }
    }

    override fun requestCameraPermission(promise: Promise) {
        Log.i(TAG, "requestCameraPermission() — skeleton, not yet implemented")
        // TODO: find Canon device (VID 0x04A9), UsbManager.requestPermission(), await broadcast
        promise.resolve(false)
    }

    override fun triggerShutter(promise: Promise) {
        Log.i(TAG, "triggerShutter() — skeleton, not yet implemented")
        // TODO: open device, claim PTP interface, send PTP InitiateCapture / EOS RemoteRelease
        promise.resolve(false)
    }
}

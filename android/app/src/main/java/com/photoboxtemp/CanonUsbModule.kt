package com.photoboxtemp

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.LifecycleEventListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.annotations.ReactModule
import com.photoboxtemp.specs.NativeCanonUsbSpec

/**
 * TurboModule for Canon EOS M10 over USB OTG (PTP).
 *
 * Phase 1: USB host check, permission, open + claim PTP interface.
 * Phase 2 (next): PTP InitiateCapture / EOS RemoteRelease in [triggerShutter].
 */
@ReactModule(name = CanonUsbModule.NAME)
class CanonUsbModule(
    reactContext: ReactApplicationContext,
) : NativeCanonUsbSpec(reactContext) {

    companion object {
        const val NAME: String = NativeCanonUsbSpec.NAME
        private const val TAG = "CanonUsbModule"
        const val ACTION_USB_PERMISSION = "com.photoboxtemp.USB_PERMISSION"
    }

    private val usbManager: UsbManager
        get() = reactApplicationContext.getSystemService(UsbManager::class.java)

    private val session = CanonUsbConnectionManager(usbManager)
    private var permissionPromise: Promise? = null
    private var permissionTarget: PermissionTarget? = null
    private var permissionReceiverRegistered = false

    private enum class PermissionTarget {
        CANON,
        UVC,
    }

    private val permissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_USB_PERMISSION) return

            val device = readDevice(intent)
            val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
            val promise = permissionPromise
            permissionPromise = null
            val target = permissionTarget
            permissionTarget = null

            Log.i(TAG, "USB permission broadcast — granted=$granted device=${device != null}")

            if (promise == null) {
                Log.w(TAG, "No pending promise for USB permission result")
                return
            }

            if (!granted || device == null) {
                promise.resolve(false)
                return
            }

            when (target) {
                PermissionTarget.CANON -> {
                    val ok = session.openAndClaim(device)
                    promise.resolve(ok)
                }
                PermissionTarget.UVC -> {
                    Log.i(TAG, "UVC USB permission granted for ${device.deviceName}")
                    promise.resolve(usbManager.hasPermission(device))
                }
                null -> promise.resolve(usbManager.hasPermission(device))
            }
        }
    }

    init {
        registerPermissionReceiver()
        reactApplicationContext.addLifecycleEventListener(object : LifecycleEventListener {
            override fun onHostResume() {}
            override fun onHostPause() {}
            override fun onHostDestroy() {
                session.release()
                unregisterPermissionReceiver()
            }
        })
    }

    override fun initializeUsb(promise: Promise) {
        try {
            val hostSupported = reactApplicationContext.packageManager
                .hasSystemFeature(PackageManager.FEATURE_USB_HOST)

            Log.i(TAG, "initializeUsb() — FEATURE_USB_HOST=$hostSupported")

            if (!hostSupported) {
                promise.resolve(false)
                return
            }

            val devices = usbManager.deviceList.values
            Log.i(TAG, "initializeUsb() — ${devices.size} USB device(s)")

            devices.forEach { device ->
                Log.i(
                    TAG,
                    "  vid=0x${device.vendorId.toString(16)} " +
                        "pid=0x${device.productId.toString(16)} " +
                        "class=${device.deviceClass} " +
                        "name=${device.productName ?: "?"}",
                )
            }

            val canon = session.findCanonDevice()
            promise.resolve(canon != null || hostSupported)
        } catch (t: Throwable) {
            Log.e(TAG, "initializeUsb() failed", t)
            promise.reject("USB_INIT_ERROR", t.message, t)
        }
    }

    override fun requestCameraPermission(promise: Promise) {
        try {
            val device = session.findCanonDevice()
            if (device == null) {
                Log.w(TAG, "requestCameraPermission() — no Canon device (VID 0x04A9) found")
                promise.resolve(false)
                return
            }

            Log.i(
                TAG,
                "requestCameraPermission() — found ${device.productName ?: "Canon"} " +
                    "pid=0x${device.productId.toString(16)}",
            )

            if (usbManager.hasPermission(device)) {
                Log.i(TAG, "requestCameraPermission() — already granted, opening device")
                promise.resolve(session.openAndClaim(device))
                return
            }

            if (permissionPromise != null) {
                promise.reject("USB_PERMISSION_PENDING", "USB permission request already in progress")
                return
            }

            permissionPromise = promise

            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_MUTABLE
                } else {
                    0
                }

            val intent = Intent(ACTION_USB_PERMISSION).setPackage(
                reactApplicationContext.packageName,
            )
            val pendingIntent = PendingIntent.getBroadcast(
                reactApplicationContext,
                0,
                intent,
                flags,
            )

            Log.i(TAG, "requestCameraPermission() — showing system USB permission dialog")
            permissionTarget = PermissionTarget.CANON
            usbManager.requestPermission(device, pendingIntent)
        } catch (t: Throwable) {
            permissionPromise = null
            permissionTarget = null
            Log.e(TAG, "requestCameraPermission() failed", t)
            promise.reject("USB_PERMISSION_ERROR", t.message, t)
        }
    }

    override fun requestUvcPermission(promise: Promise) {
        try {
            val devices = UsbDeviceUtils.findUvcDevices(usbManager)
            Log.i(TAG, "requestUvcPermission() — found ${devices.size} UVC device(s)")
            devices.forEach { device ->
                Log.i(
                    TAG,
                    "  UVC vid=0x${device.vendorId.toString(16)} " +
                        "pid=0x${device.productId.toString(16)} " +
                        "class=${device.deviceClass}",
                )
            }

            val device = devices.firstOrNull()
            if (device == null) {
                Log.w(TAG, "requestUvcPermission() — no HDMI capture card / UVC device found")
                promise.resolve(false)
                return
            }

            if (usbManager.hasPermission(device)) {
                Log.i(TAG, "requestUvcPermission() — already granted")
                promise.resolve(true)
                return
            }

            if (permissionPromise != null) {
                promise.reject("USB_PERMISSION_PENDING", "USB permission request already in progress")
                return
            }

            permissionPromise = promise
            permissionTarget = PermissionTarget.UVC

            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_MUTABLE
                } else {
                    0
                }

            val intent = Intent(ACTION_USB_PERMISSION).setPackage(
                reactApplicationContext.packageName,
            )
            val pendingIntent = PendingIntent.getBroadcast(
                reactApplicationContext,
                1,
                intent,
                flags,
            )

            Log.i(TAG, "requestUvcPermission() — showing system USB permission dialog for capture card")
            usbManager.requestPermission(device, pendingIntent)
        } catch (t: Throwable) {
            permissionPromise = null
            permissionTarget = null
            Log.e(TAG, "requestUvcPermission() failed", t)
            promise.reject("UVC_PERMISSION_ERROR", t.message, t)
        }
    }

    override fun triggerShutter(promise: Promise) {
        try {
            if (!session.isSessionOpen) {
                Log.w(TAG, "triggerShutter() — no open session, call requestCameraPermission() first")
                promise.resolve(false)
                return
            }

            if (!session.verifySession()) {
                Log.e(TAG, "triggerShutter() — session invalid, releasing")
                session.release()
                promise.resolve(false)
                return
            }

            // Phase 2: send PTP 0x100E InitiateCapture / Canon EOS RemoteRelease here.
            Log.i(
                TAG,
                "triggerShutter() — USB session OK, PTP shutter command not yet implemented",
            )
            promise.resolve(false)
        } catch (t: Throwable) {
            Log.e(TAG, "triggerShutter() failed", t)
            promise.reject("SHUTTER_ERROR", t.message, t)
        }
    }

    private fun registerPermissionReceiver() {
        if (permissionReceiverRegistered) return
        ContextCompat.registerReceiver(
            reactApplicationContext,
            permissionReceiver,
            IntentFilter(ACTION_USB_PERMISSION),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        permissionReceiverRegistered = true
    }

    private fun unregisterPermissionReceiver() {
        if (!permissionReceiverRegistered) return
        try {
            reactApplicationContext.unregisterReceiver(permissionReceiver)
        } catch (t: Throwable) {
            Log.w(TAG, "unregisterPermissionReceiver failed", t)
        }
        permissionReceiverRegistered = false
    }

    @Suppress("DEPRECATION")
    private fun readDevice(intent: Intent): UsbDevice? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }
}

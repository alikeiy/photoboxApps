package com.photoboxtemp

import android.content.Intent
import android.content.pm.ActivityInfo
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import com.facebook.react.ReactActivity
import com.facebook.react.ReactActivityDelegate
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.fabricEnabled
import com.facebook.react.defaults.DefaultReactActivityDelegate
import com.photoboxtemp.booth.PendingImport

class MainActivity : ReactActivity() {

    override fun getMainComponentName(): String = "photoboxTemp"

    override fun createReactActivityDelegate(): ReactActivityDelegate =
        DefaultReactActivityDelegate(this, mainComponentName, fabricEnabled)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Landscape for the booth UI. On a tablet this does not lock Canon Camera
        // Connect; that app follows the system rotation lock.
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        PendingImport.capture(intent)
        logUsbIntent(intent, "onCreate")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        PendingImport.capture(intent)
        logUsbIntent(intent, "onNewIntent")
    }

    private fun logUsbIntent(intent: Intent?, source: String) {
        if (intent?.action != UsbManager.ACTION_USB_DEVICE_ATTACHED) return
        Log.i(
            TAG,
            "$source: USB device attached — call CanonUsb.initializeUsb() from JS",
        )
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}

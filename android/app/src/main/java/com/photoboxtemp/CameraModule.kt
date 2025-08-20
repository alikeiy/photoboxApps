package com.photoboxtemp

import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import android.Manifest
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.modules.core.PermissionListener

class CameraModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    private var sharedRequestCode = 12345

    override fun getName(): String {
        return "CameraModule"
    }

    private fun getCurrentActivitySafe(): Activity? {
        return reactApplicationContext.currentActivity ?: run {
            Log.w("CameraModule", "currentActivity is null")
            null
        }
    }

    private fun requestPermission(permission: String, promise: Promise) {
        val activity = getCurrentActivitySafe()
        if (activity is PermissionAwareActivity) {
            val currentRequestCode = sharedRequestCode++
            val listener = PermissionListener { requestCode: Int, permissions: Array<String>, grantResults: IntArray ->
                if (requestCode == currentRequestCode) {
                    val status = if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        "authorized"
                    } else {
                        "denied"
                    }
                    promise.resolve(status)
                    return@PermissionListener true
                }
                return@PermissionListener false
            }
            activity.requestPermissions(arrayOf(permission), currentRequestCode, listener)
        } else {
            promise.reject(
                "NO_ACTIVITY",
                "No PermissionAwareActivity was found! Make sure the app has launched before calling this function."
            )
        }
    }

    @ReactMethod
    fun requestCameraPermission(promise: Promise) {
        requestPermission(Manifest.permission.CAMERA, promise)
    }
}

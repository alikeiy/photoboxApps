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
        return reactApplicationContext.currentActivity
    }

    @ReactMethod
    fun requestCameraPermission(promise: Promise) {
        val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        val activity = getCurrentActivitySafe()
        if (activity is PermissionAwareActivity) {
            val currentRequestCode = sharedRequestCode++
            val listener = PermissionListener { requestCode: Int, perms: Array<String>, grantResults: IntArray ->
                if (requestCode == currentRequestCode) {
                    val allGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                    val status = if (allGranted) "authorized" else "denied"
                    promise.resolve(status)
                    return@PermissionListener true
                }
                return@PermissionListener false
            }
            activity.requestPermissions(permissions, currentRequestCode, listener)
        } else {
            promise.reject("NO_ACTIVITY", "No PermissionAwareActivity found")
        }
    }
}

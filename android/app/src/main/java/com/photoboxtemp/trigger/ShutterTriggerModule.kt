package com.photoboxtemp.trigger

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.annotations.ReactModule
import com.photoboxtemp.specs.NativeShutterTriggerSpec

/**
 * JS bridge for the Canon Camera Connect shutter pin.
 * Permissions are opened as system screens — overlay and accessibility cannot
 * be granted with a normal runtime request.
 */
@ReactModule(name = ShutterTriggerModule.NAME)
class ShutterTriggerModule(
    reactContext: ReactApplicationContext,
) : NativeShutterTriggerSpec(reactContext) {

    private val store = PinAnchorStore.get(reactContext)
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun canDrawOverlays(promise: Promise) {
        promise.resolve(Settings.canDrawOverlays(reactApplicationContext))
    }

    override fun openOverlaySettings(promise: Promise) {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${reactApplicationContext.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            reactApplicationContext.startActivity(intent)
            promise.resolve(true)
        } catch (t: Throwable) {
            promise.reject("OVERLAY_SETTINGS", t.message, t)
        }
    }

    override fun isAccessibilityEnabled(promise: Promise) {
        promise.resolve(ShutterAccessibilityService.isEnabled(reactApplicationContext))
    }

    override fun openAccessibilitySettings(promise: Promise) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            reactApplicationContext.startActivity(intent)
            promise.resolve(true)
        } catch (t: Throwable) {
            promise.reject("A11Y_SETTINGS", t.message, t)
        }
    }

    override fun isIgnoringBatteryOptimizations(promise: Promise) {
        val power = reactApplicationContext.getSystemService(PowerManager::class.java)
        promise.resolve(power.isIgnoringBatteryOptimizations(reactApplicationContext.packageName))
    }

    override fun requestIgnoreBatteryOptimizations(promise: Promise) {
        try {
            val power = reactApplicationContext.getSystemService(PowerManager::class.java)
            if (power.isIgnoringBatteryOptimizations(reactApplicationContext.packageName)) {
                promise.resolve(true)
                return
            }
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(Uri.parse("package:${reactApplicationContext.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            reactApplicationContext.startActivity(intent)
            promise.resolve(true)
        } catch (t: Throwable) {
            promise.reject("BATTERY_SETTINGS", t.message, t)
        }
    }

    override fun openOemAutostartSettings(promise: Promise) {
        val candidates = listOf(
            ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.StartupAppListActivity",
            ),
            ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.startupapp.StartupAppListActivity",
            ),
            ComponentName(
                "com.oplus.safecenter",
                "com.oplus.safecenter.startupapp.StartupAppListActivity",
            ),
            ComponentName(
                "com.coloros.oppoguardelf",
                "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity",
            ),
        )
        for (component in candidates) {
            val intent = Intent().setComponent(component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(reactApplicationContext.packageManager) == null) continue
            try {
                reactApplicationContext.startActivity(intent)
                promise.resolve(true)
                return
            } catch (_: Throwable) {
                continue
            }
        }
        promise.resolve(false)
    }

    override fun startTrigger(promise: Promise) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            promise.resolve(false)
            return
        }
        if (!Settings.canDrawOverlays(reactApplicationContext)) {
            promise.resolve(false)
            return
        }
        ShutterTriggerService.ensureRunning(reactApplicationContext)
        promise.resolve(true)
    }

    override fun stopTrigger(promise: Promise) {
        deliverToService(ShutterTriggerService.ACTION_STOP, null)
        promise.resolve(true)
    }

    override fun isTriggerRunning(promise: Promise) {
        promise.resolve(ShutterTriggerService.running)
    }

    override fun setPinLocked(locked: Boolean, promise: Promise) {
        store.setLocked(locked)
        if (ShutterTriggerService.running) {
            deliverToService(ShutterTriggerService.ACTION_SET_LOCKED, locked)
        }
        promise.resolve(true)
    }

    private fun deliverToService(action: String, locked: Boolean?) {
        val intent = Intent(reactApplicationContext, ShutterTriggerService::class.java)
            .setAction(action)
        if (locked != null) intent.putExtra(ShutterTriggerService.EXTRA_LOCKED, locked)
        try {
            reactApplicationContext.startService(intent)
        } catch (_: Throwable) {
            try {
                ContextCompat.startForegroundService(reactApplicationContext, intent)
            } catch (t: Throwable) {
                Log.e("ShutterTrigger", "Unable to deliver $action", t)
            }
        }
    }

    override fun isPinLocked(promise: Promise) {
        promise.resolve(store.read().locked)
    }

    override fun isAnchorValid(promise: Promise) {
        promise.resolve(store.isValid())
    }

    override fun isDisplayLandscape(promise: Promise) {
        promise.resolve(DisplaySnapshots.capture(reactApplicationContext).isLandscape)
    }

    override fun getAnchorX(promise: Promise) {
        val anchor = store.read()
        promise.resolve(if (anchor.hasPosition) anchor.centerX.toDouble() else -1.0)
    }

    override fun getAnchorY(promise: Promise) {
        val anchor = store.read()
        promise.resolve(if (anchor.hasPosition) anchor.centerY.toDouble() else -1.0)
    }

    override fun fireTestTap(promise: Promise) {
        val service = ShutterAccessibilityService.instance
        if (service == null) {
            promise.resolve(false)
            return
        }
        mainHandler.post {
            promise.resolve(service.performShutterTap())
        }
    }

    companion object {
        const val NAME: String = NativeShutterTriggerSpec.NAME
    }
}

package com.photoboxtemp.trigger

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ComponentName
import android.content.Context
import android.graphics.Path
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.photoboxtemp.booth.PhotoImportWatcher

/**
 * Receives volume and Bluetooth-remote keys while Canon Camera Connect is in front,
 * then taps the saved pin with [dispatchGesture].
 *
 * A normal service never sees volume keys — they go to the focused window.
 * [onKeyEvent] only runs when the user has enabled this service and the metadata
 * sets canRequestFilterKeyEvents. Keys are consumed only while the pin is locked
 * and the saved point still matches this display, so setup does not steal volume.
 */
class ShutterAccessibilityService : AccessibilityService() {

    private val store by lazy { PinAnchorStore.get(this) }
    private var lastTapAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        val info = serviceInfo
        if (info != null) {
            info.flags = info.flags or
                android.accessibilityservice.AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
            serviceInfo = info
        }
        Log.i(TAG, "Accessibility service connected")
        if (store.isArmed()) {
            ShutterTriggerService.ensureRunning(this)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!store.isArmed() || !store.read().locked) return false
        if (event.keyCode !in TRIGGER_KEYS) return false
        if (!store.isValid()) return false

        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            performShutterTap()
        }
        return true
    }

    fun performShutterTap(): Boolean {
        val anchor = store.read()
        val current = DisplaySnapshots.capture(this)
        if (!store.isValid(anchor, current)) {
            Log.w(TAG, "Tap skipped — pin does not match the current display")
            return false
        }

        val now = SystemClock.uptimeMillis()
        if (now - lastTapAt < TAP_DEBOUNCE_MS) return false
        lastTapAt = now

        val x = anchor.centerX.toFloat().coerceIn(1f, current.widthPx - 2f)
        val y = anchor.centerY.toFloat().coerceIn(1f, current.heightPx - 2f)
        val path = Path().apply {
            moveTo(x, y)
            // A zero-length path is rejected by GestureDescription on some builds.
            lineTo(x + 1f, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, TAP_DURATION_MS)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        val accepted = dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    Log.i(TAG, "Shutter tap completed at $x,$y")
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    Log.w(TAG, "Shutter tap cancelled at $x,$y")
                }
            },
            null,
        )
        Log.i(TAG, "dispatchGesture accepted=$accepted at $x,$y rotation=${current.rotation}")
        if (accepted) PhotoImportWatcher.noteShutter()
        return accepted
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        if (instance === this) instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "ShutterA11y"
        private const val TAP_DURATION_MS = 60L
        private const val TAP_DEBOUNCE_MS = 700L

        private val TRIGGER_KEYS = setOf(
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_CAMERA,
            KeyEvent.KEYCODE_FOCUS,
            KeyEvent.KEYCODE_HEADSETHOOK,
            KeyEvent.KEYCODE_MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_PAGE_DOWN,
        )

        @Volatile
        var instance: ShutterAccessibilityService? = null
            private set

        fun isEnabled(context: Context): Boolean {
            val expected = ComponentName(context, ShutterAccessibilityService::class.java)
            val setting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false
            val listed = setting.split(':').any { flat ->
                ComponentName.unflattenFromString(flat) == expected
            }
            val master = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED,
                0,
            ) == 1
            return listed && master
        }
    }
}

package com.photoboxtemp.trigger

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Display
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.photoboxtemp.MainActivity
import com.photoboxtemp.R
import com.photoboxtemp.booth.PhotoImportWatcher

/**
 * Keeps the process in the foreground while Canon Camera Connect is the visible app.
 *
 * Realme UI will otherwise freeze or kill a background overlay. The persistent
 * notification is the contract that makes [startForeground] legal, and
 * [START_STICKY] asks the system to restore the session after a low-memory kill.
 * Key listening itself lives in [ShutterAccessibilityService]; this service owns
 * the pin and the notification.
 */
class ShutterTriggerService : Service() {

    private val store by lazy { PinAnchorStore.get(this) }
    private var overlay: ShutterPinOverlay? = null
    private var foregroundStarted = false

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) {
            if (displayId != Display.DEFAULT_DISPLAY) return
            overlay?.onDisplayChanged()
            refreshNotification()
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        overlay = ShutterPinOverlay(this, store) { refreshNotification() }
        PhotoImportWatcher.attach(this)
        getSystemService(DisplayManager::class.java)
            .registerDisplayListener(displayListener, null)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureForeground()
        running = true

        val action = intent?.action ?: if (store.isArmed()) ACTION_START else ACTION_STOP
        when (action) {
            ACTION_STOP -> {
                store.setArmed(false)
                overlay?.hide()
                running = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SET_LOCKED -> {
                overlay?.setLocked(intent?.getBooleanExtra(EXTRA_LOCKED, true) ?: true)
            }
            else -> {
                store.setArmed(true)
                overlay?.show()
                if (store.read().locked) overlay?.setLocked(true)
            }
        }
        refreshNotification()
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        PhotoImportWatcher.detach()
        getSystemService(DisplayManager::class.java).unregisterDisplayListener(displayListener)
        overlay?.hide()
        overlay = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun ensureForeground() {
        val notification = buildNotification()
        if (!foregroundStarted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            foregroundStarted = true
        } else {
            getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
        }
    }

    private fun refreshNotification() {
        if (!foregroundStarted) return
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val anchor = store.read()
        val valid = store.isValid(anchor)
            val accessibilityOn = ShutterAccessibilityService.isEnabled(this)
        val text = when {
            !accessibilityOn -> getString(R.string.shutter_notif_need_accessibility)
            !DisplaySnapshots.capture(this).isLandscape -> getString(R.string.shutter_notif_need_landscape)
            !valid -> getString(R.string.shutter_notif_stale)
            anchor.locked -> getString(R.string.shutter_notif_armed)
            else -> getString(R.string.shutter_notif_place_pin)
        }

        val open = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            pendingFlags(),
        )
        val stop = pendingService(2, Intent(this, ShutterTriggerService::class.java).setAction(ACTION_STOP))
        val lockIntent = Intent(this, ShutterTriggerService::class.java)
            .setAction(ACTION_SET_LOCKED)
            .putExtra(EXTRA_LOCKED, !anchor.locked)
        val lock = pendingService(3, lockIntent)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_shutter)
            .setContentTitle(getString(R.string.shutter_notif_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(open)
            .addAction(0, getString(R.string.shutter_action_stop), stop)
            .addAction(
                0,
                getString(if (anchor.locked) R.string.shutter_action_unlock else R.string.shutter_action_lock),
                lock,
            )
            .build()
    }

    private fun pendingService(requestCode: Int, intent: Intent): PendingIntent =
        PendingIntent.getService(this, requestCode, intent, pendingFlags())

    private fun pendingFlags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.shutter_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.shutter_channel_desc)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "ShutterTriggerSvc"
        private const val CHANNEL_ID = "shutter_trigger"
        private const val NOTIFICATION_ID = 5107

        const val ACTION_START = "com.photoboxtemp.trigger.START"
        const val ACTION_STOP = "com.photoboxtemp.trigger.STOP"
        const val ACTION_SET_LOCKED = "com.photoboxtemp.trigger.SET_LOCKED"
        const val EXTRA_LOCKED = "locked"

        @Volatile
        var running: Boolean = false
            private set

        fun ensureRunning(context: Context) {
            if (!android.provider.Settings.canDrawOverlays(context)) {
                Log.w(TAG, "Overlay permission missing — pin not started")
                return
            }
            val intent = Intent(context, ShutterTriggerService::class.java).setAction(ACTION_START)
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (t: Throwable) {
                Log.e(TAG, "startForegroundService rejected", t)
            }
        }
    }
}

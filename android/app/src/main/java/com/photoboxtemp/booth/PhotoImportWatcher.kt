package com.photoboxtemp.booth

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.photoboxtemp.MainActivity
import com.photoboxtemp.R

/**
 * Watches MediaStore for the JPEG Canon Camera Connect writes after a shutter tap.
 *
 * The observer only accepts a photo inside the window opened by [noteShutter],
 * and only after the file size stops growing, so a half-written Wi-Fi download
 * is not composed.
 */
class PhotoImportWatcher(private val context: Context) {

    private val main = Handler(Looper.getMainLooper())
    private val thread = HandlerThread("photo-import").also { it.start() }
    private val io = Handler(thread.looper)
    private val resolver = context.contentResolver

    private var candidateId = -1L
    private var candidateSize = -1L
    private var stableHits = 0

    private val observer = object : ContentObserver(main) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            if (SystemClock.elapsedRealtime() > armedUntilElapsed) return
            schedule(STABILIZE_MS)
        }
    }

    private val scanRunnable = Runnable { scan() }

    fun start() {
        resolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            observer,
        )
        if (SystemClock.elapsedRealtime() < armedUntilElapsed) schedule(STABILIZE_MS)
    }

    fun stop() {
        resolver.unregisterContentObserver(observer)
        io.removeCallbacksAndMessages(null)
        thread.quitSafely()
    }

    fun schedule(delayMs: Long) {
        io.removeCallbacks(scanRunnable)
        io.postDelayed(scanRunnable, delayMs)
    }

    fun resetCandidate() {
        candidateId = -1L
        candidateSize = -1L
        stableHits = 0
    }

    private fun scan() {
        if (SystemClock.elapsedRealtime() > armedUntilElapsed) return
        val image = try {
            newestSince(context, armedSinceEpochSec)
        } catch (t: SecurityException) {
            Log.e(TAG, "MediaStore read denied. Grant access to all photos.", t)
            main.post { notifyNeedPermission() }
            return
        } catch (t: Throwable) {
            Log.e(TAG, "MediaStore query failed", t)
            return
        } ?: return

        if (image.id == deliveredId) return
        if (image.size < MIN_BYTES) {
            schedule(STABILIZE_MS)
            return
        }
        if (image.id == candidateId && image.size == candidateSize) {
            stableHits += 1
        } else {
            candidateId = image.id
            candidateSize = image.size
            stableHits = 1
        }
        if (stableHits < 2) {
            schedule(STABILIZE_MS)
            return
        }
        deliveredId = image.id
        armedUntilElapsed = 0L
        Log.i(TAG, "Imported ${image.name} id=${image.id} size=${image.size}")
        main.post { publish(context, image.uri.toString()) }
    }

    private fun notifyNeedPermission() {
        val open = PendingIntent.getActivity(
            context,
            9,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            pendingFlags(),
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_shutter)
            .setContentTitle(context.getString(R.string.booth_photo_title))
            .setContentText(context.getString(R.string.booth_need_photo_permission))
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(PERMISSION_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "PhotoImport"
        private const val CHANNEL_ID = "shutter_trigger"
        private const val READY_NOTIFICATION_ID = 5108
        private const val PERMISSION_NOTIFICATION_ID = 5109
        private const val WINDOW_MS = 90_000L
        private const val STABILIZE_MS = 1_200L
        private const val MIN_BYTES = 200_000L
        const val EVENT = "photoboxPhotoImported"

        @Volatile
        private var instance: PhotoImportWatcher? = null

        @Volatile
        var armedUntilElapsed: Long = 0L

        @Volatile
        var armedSinceEpochSec: Long = 0L

        @Volatile
        private var deliveredId: Long = -1L

        fun attach(context: Context) {
            if (instance != null) return
            instance = PhotoImportWatcher(context.applicationContext).also { it.start() }
        }

        fun detach() {
            instance?.stop()
            instance = null
        }

        fun noteShutter() {
            armedSinceEpochSec = System.currentTimeMillis() / 1000L - 2L
            armedUntilElapsed = SystemClock.elapsedRealtime() + WINDOW_MS
            deliveredId = -1L
            instance?.resetCandidate()
            instance?.schedule(STABILIZE_MS)
            Log.i(TAG, "Watching MediaStore for ${WINDOW_MS}ms")
        }

        fun latestUri(context: Context): String? {
            val since = System.currentTimeMillis() / 1000L - 30L * 60L
            return newestSince(context, since)?.uri?.toString()
        }

        private fun publish(context: Context, uri: String) {
            PendingImport.offer(uri)
            val open = Intent(context, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT,
                )
                putExtra(PendingImport.EXTRA_PHOTO_URI, uri)
            }
            try {
                context.startActivity(open)
            } catch (t: Throwable) {
                Log.w(TAG, "Opening the booth was blocked. Notification remains.", t)
            }
            val pending = PendingIntent.getActivity(
                context,
                8,
                Intent(open),
                pendingFlags(),
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_shutter)
                .setContentTitle(context.getString(R.string.booth_photo_title))
                .setContentText(context.getString(R.string.booth_photo_ready))
                .setAutoCancel(true)
                .setContentIntent(pending)
                .build()
            context.getSystemService(NotificationManager::class.java)
                .notify(READY_NOTIFICATION_ID, notification)
            emit(uri)
        }

        private fun emit(uri: String) {
            val react = PhotoBoothModule.reactContext ?: return
            try {
                react
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                    .emit(EVENT, uri)
            } catch (t: Throwable) {
                Log.w(TAG, "React listener not ready", t)
            }
        }

        private fun newestSince(context: Context, sinceEpochSec: Long): GalleryImage? {
            val rows = query(context, sinceEpochSec)
            if (rows.isEmpty()) return null
            val canon = rows.filter { it.looksLikeCanon }
            val pool = if (canon.isNotEmpty()) canon else rows
            return pool.maxBy { it.dateAdded * 1_000_000L + it.size }
        }

        private fun query(context: Context, sinceEpochSec: Long): List<GalleryImage> {
            val resolver = context.contentResolver
            val projection = mutableListOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DISPLAY_NAME,
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                projection += MediaStore.Images.Media.RELATIVE_PATH
            }
            val cursor = resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection.toTypedArray(),
                "${MediaStore.Images.Media.DATE_ADDED} >= ?",
                arrayOf(sinceEpochSec.toString()),
                "${MediaStore.Images.Media.DATE_ADDED} DESC",
            ) ?: return emptyList()

            val images = ArrayList<GalleryImage>(8)
            cursor.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val sizeCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val nameCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val pathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    it.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
                } else {
                    -1
                }
                var count = 0
                while (it.moveToNext() && count < 20) {
                    count += 1
                    val id = it.getLong(idCol)
                    images += GalleryImage(
                        id = id,
                        uri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id,
                        ),
                        dateAdded = it.getLong(dateCol),
                        size = if (it.isNull(sizeCol)) 0L else it.getLong(sizeCol),
                        name = it.getString(nameCol) ?: "",
                        path = if (pathCol >= 0) it.getString(pathCol) ?: "" else "",
                    )
                }
            }
            return images
        }

        private fun pendingFlags(): Int =
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    }
}

private data class GalleryImage(
    val id: Long,
    val uri: Uri,
    val dateAdded: Long,
    val size: Long,
    val name: String,
    val path: String,
) {
    val looksLikeCanon: Boolean
        get() = name.contains("canon", ignoreCase = true) ||
            path.contains("canon", ignoreCase = true)
}

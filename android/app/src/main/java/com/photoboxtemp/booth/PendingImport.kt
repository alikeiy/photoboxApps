package com.photoboxtemp.booth

import android.content.Intent

/** Latest photo the booth should open, held until JavaScript consumes it. */
object PendingImport {
    const val EXTRA_PHOTO_URI = "com.photoboxtemp.EXTRA_PHOTO_URI"

    @Volatile
    private var uri: String? = null

    fun offer(value: String) {
        uri = value
    }

    fun capture(intent: Intent?) {
        val value = intent?.getStringExtra(EXTRA_PHOTO_URI) ?: return
        if (value.isNotBlank()) offer(value)
    }

    fun consume(): String {
        synchronized(this) {
            val current = uri
            uri = null
            return current ?: ""
        }
    }
}

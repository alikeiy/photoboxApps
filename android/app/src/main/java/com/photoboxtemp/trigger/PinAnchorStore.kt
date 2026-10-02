package com.photoboxtemp.trigger

import android.content.Context
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import android.view.Surface
import android.view.WindowManager

/**
 * Full-display pixel snapshot. [dispatchGesture] and [android.view.View.getLocationOnScreen]
 * both use this space (top-left of the panel, including system bars).
 *
 * Do not use [android.util.DisplayMetrics.widthPixels] or current window metrics —
 * those exclude the status/navigation bars and drift away from the Canon shutter.
 *
 * Landscape is width > height, not [Surface.ROTATION_0]. Some tablets (including
 * pads whose natural orientation is landscape) report ROTATION_0 while horizontal.
 */
data class DisplaySnapshot(
    val widthPx: Int,
    val heightPx: Int,
    val densityDpi: Int,
    val rotation: Int,
) {
    val isLandscape: Boolean
        get() = widthPx > heightPx

    fun matches(other: DisplaySnapshot): Boolean =
        widthPx == other.widthPx &&
            heightPx == other.heightPx &&
            densityDpi == other.densityDpi &&
            rotation == other.rotation
}

object DisplaySnapshots {
    private fun realDisplaySize(wm: WindowManager, display: Display?): Point {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val bounds = wm.maximumWindowMetrics.bounds
                return Point(bounds.width(), bounds.height())
            } catch (_: Throwable) {
                // Some OEM service contexts reject maximumWindowMetrics.
            }
        }
        val point = Point()
        @Suppress("DEPRECATION")
        (display ?: wm.defaultDisplay).getRealSize(point)
        return point
    }

    fun capture(context: Context): DisplaySnapshot {
        val wm = context.getSystemService(WindowManager::class.java)
        val display = context.getSystemService(DisplayManager::class.java)
            .getDisplay(Display.DEFAULT_DISPLAY)

        val size = realDisplaySize(wm, display)
        val width = size.x
        val height = size.y

        val rotation = display?.rotation
            ?: @Suppress("DEPRECATION") wm.defaultDisplay.rotation

        return DisplaySnapshot(
            widthPx = width,
            heightPx = height,
            densityDpi = context.resources.displayMetrics.densityDpi,
            rotation = rotation,
        )
    }
}

/**
 * Last pin center in raw screen pixels, plus the display it was measured on.
 * In-memory copy is the source of truth so a volume press does not wait on disk.
 */
class PinAnchorStore private constructor(context: Context) {

    data class Anchor(
        val centerX: Int,
        val centerY: Int,
        val snapshot: DisplaySnapshot,
        val locked: Boolean,
        val hasPosition: Boolean,
    )

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val appContext = context.applicationContext

    @Volatile
    private var memory: Anchor = readFromDisk()

    fun read(): Anchor = memory

    fun isArmed(): Boolean = prefs.getBoolean(KEY_ARMED, false)

    fun setArmed(armed: Boolean) {
        prefs.edit().putBoolean(KEY_ARMED, armed).apply()
    }

    fun setLocked(locked: Boolean) {
        synchronized(this) {
            memory = memory.copy(locked = locked)
            prefs.edit().putBoolean(KEY_LOCKED, locked).apply()
        }
    }

    fun saveCenter(centerX: Int, centerY: Int) {
        val snapshot = DisplaySnapshots.capture(appContext)
        synchronized(this) {
            memory = Anchor(
                centerX = centerX,
                centerY = centerY,
                snapshot = snapshot,
                locked = memory.locked,
                hasPosition = true,
            )
            prefs.edit()
                .putBoolean(KEY_HAS_POSITION, true)
                .putInt(KEY_X, centerX)
                .putInt(KEY_Y, centerY)
                .putInt(KEY_WIDTH, snapshot.widthPx)
                .putInt(KEY_HEIGHT, snapshot.heightPx)
                .putInt(KEY_DPI, snapshot.densityDpi)
                .putInt(KEY_ROTATION, snapshot.rotation)
                .apply()
        }
    }

    /**
     * Valid only when the pin was placed on this exact display: same pixel size,
     * density, and rotation, in landscape, and the point is still on screen.
     * A rotation or display-size change invalidates the mapping instead of scaling
     * it onto the wrong shutter position.
     */
    fun isValid(
        anchor: Anchor = memory,
        current: DisplaySnapshot = DisplaySnapshots.capture(appContext),
    ): Boolean {
        if (!anchor.hasPosition) return false
        if (!current.isLandscape) return false
        if (!anchor.snapshot.matches(current)) return false
        return anchor.centerX in 1 until current.widthPx - 1 &&
            anchor.centerY in 1 until current.heightPx - 1
    }

    private fun readFromDisk(): Anchor {
        val snapshot = DisplaySnapshot(
            widthPx = prefs.getInt(KEY_WIDTH, 0),
            heightPx = prefs.getInt(KEY_HEIGHT, 0),
            densityDpi = prefs.getInt(KEY_DPI, 0),
            rotation = prefs.getInt(KEY_ROTATION, Surface.ROTATION_0),
        )
        return Anchor(
            centerX = prefs.getInt(KEY_X, 0),
            centerY = prefs.getInt(KEY_Y, 0),
            snapshot = snapshot,
            locked = prefs.getBoolean(KEY_LOCKED, false),
            hasPosition = prefs.getBoolean(KEY_HAS_POSITION, false),
        )
    }

    companion object {
        private const val PREFS = "shutter_pin"
        private const val KEY_ARMED = "armed"
        private const val KEY_LOCKED = "locked"
        private const val KEY_HAS_POSITION = "has_position"
        private const val KEY_X = "center_x"
        private const val KEY_Y = "center_y"
        private const val KEY_WIDTH = "width_px"
        private const val KEY_HEIGHT = "height_px"
        private const val KEY_DPI = "density_dpi"
        private const val KEY_ROTATION = "rotation"

        @Volatile
        private var instance: PinAnchorStore? = null

        fun get(context: Context): PinAnchorStore =
            instance ?: synchronized(this) {
                instance ?: PinAnchorStore(context.applicationContext).also { instance = it }
            }
    }
}

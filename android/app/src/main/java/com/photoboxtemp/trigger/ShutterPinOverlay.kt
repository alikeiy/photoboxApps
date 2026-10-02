package com.photoboxtemp.trigger

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import kotlin.math.abs

/**
 * Two overlay windows:
 *
 * 1. Crosshair — visual only. [WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE] so
 *    every pixel, including the transparent center, goes to Canon Camera Connect.
 * 2. Drag handle — small window beside the ring. This is the only touch target.
 *
 * Alpha alone does not pass touches through. [FLAG_NOT_FOCUSABLE] keeps key focus
 * on Canon. [FLAG_NOT_TOUCH_MODAL] passes touches that miss the handle.
 * [FLAG_LAYOUT_IN_SCREEN] puts x/y in the same full-screen space as [dispatchGesture].
 * The stored point is still taken from [View.getLocationOnScreen] so an OEM status-bar
 * inset cannot drift the tap off the shutter.
 */
class ShutterPinOverlay(
    private val context: Context,
    private val store: PinAnchorStore,
    private val onChanged: () -> Unit,
) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val crossSize = dp(96f)
    private val handleWidth = dp(76f)
    private val handleHeight = dp(40f)
    private val touchSlop = dp(6f)

    private var crossView: CrosshairView? = null
    private var handleView: View? = null
    private var crossParams: WindowManager.LayoutParams? = null
    private var handleParams: WindowManager.LayoutParams? = null

    private var dragStartRawX = 0f
    private var dragStartRawY = 0f
    private var dragStartX = 0
    private var dragStartY = 0
    private var dragging = false

    private val lockAfterHold = Runnable {
        if (!dragging) setLocked(true)
    }

    val isShown: Boolean
        get() = crossView != null

    fun show() {
        if (crossView != null) {
            refreshAppearance()
            return
        }

        val snapshot = DisplaySnapshots.capture(context)
        val saved = store.read()
        val centerX = if (saved.hasPosition) saved.centerX else snapshot.widthPx / 2
        val centerY = if (saved.hasPosition) saved.centerY else snapshot.heightPx / 2

        val cross = CrosshairView(context)
        val params = layoutParams(
            width = crossSize,
            height = crossSize,
            touchable = false,
        )
        val maxX = (snapshot.widthPx - crossSize).coerceAtLeast(0)
        val maxY = (snapshot.heightPx - crossSize).coerceAtLeast(0)
        params.x = (centerX - crossSize / 2).coerceIn(0, maxX)
        params.y = (centerY - crossSize / 2).coerceIn(0, maxY)
        val targetOnScreen =
            centerX in 1 until snapshot.widthPx && centerY in 1 until snapshot.heightPx

        try {
            windowManager.addView(cross, params)
        } catch (t: Throwable) {
            Log.e(TAG, "Unable to add shutter pin. Overlay permission missing?", t)
            return
        }

        crossView = cross
        crossParams = params
        cross.post {
            if (targetOnScreen) correctToward(centerX, centerY)
            if (!store.read().locked) ensureHandle()
            refreshAppearance()
            onChanged()
        }
    }

    fun hide() {
        mainHandler.removeCallbacks(lockAfterHold)
        removeHandle()
        crossView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (t: Throwable) {
                Log.w(TAG, "remove crosshair failed", t)
            }
        }
        crossView = null
        crossParams = null
    }

    fun setLocked(locked: Boolean) {
        store.setLocked(locked)
        if (locked) {
            mainHandler.removeCallbacks(lockAfterHold)
            removeHandle()
        } else if (crossView != null) {
            ensureHandle()
        }
        refreshAppearance()
        onChanged()
    }

    fun onDisplayChanged() {
        val view = crossView ?: return
        view.post {
            val params = crossParams ?: return@post
            val location = IntArray(2)
            view.getLocationOnScreen(location)
            params.x = location[0]
            params.y = location[1]
            positionHandle()
            refreshAppearance()
            onChanged()
        }
    }

    private fun refreshAppearance() {
        val anchor = store.read()
        val valid = store.isValid(anchor)
        crossView?.color = when {
            !valid -> COLOR_STALE
            anchor.locked -> COLOR_LOCKED
            else -> COLOR_UNLOCKED
        }
        crossView?.invalidate()
    }

    private fun ensureHandle() {
        if (handleView != null || store.read().locked) return
        val params = crossParams ?: return
        val handle = TextView(context).apply {
            text = "GESER"
            setTextColor(Color.WHITE)
            textSize = 12f
            gravity = Gravity.CENTER
            setBackgroundColor(0xE6111111.toInt())
            setOnTouchListener { _, event -> onHandleTouch(event) }
        }
        val handleLayout = layoutParams(
            width = handleWidth,
            height = handleHeight,
            touchable = true,
        )
        handleLayout.x = params.x + crossSize - handleWidth / 2
        handleLayout.y = params.y - handleHeight / 2
        try {
            windowManager.addView(handle, handleLayout)
            handleView = handle
            handleParams = handleLayout
        } catch (t: Throwable) {
            Log.e(TAG, "Unable to add drag handle", t)
        }
    }

    private fun removeHandle() {
        handleView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (t: Throwable) {
                Log.w(TAG, "remove handle failed", t)
            }
        }
        handleView = null
        handleParams = null
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun onHandleTouch(event: MotionEvent): Boolean {
        val params = crossParams ?: return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragging = false
                dragStartRawX = event.rawX
                dragStartRawY = event.rawY
                dragStartX = params.x
                dragStartY = params.y
                mainHandler.postDelayed(lockAfterHold, LOCK_HOLD_MS)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - dragStartRawX
                val dy = event.rawY - dragStartRawY
                if (!dragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                    dragging = true
                    mainHandler.removeCallbacks(lockAfterHold)
                }
                if (dragging) {
                    moveCrosshairTo(dragStartX + dx.toInt(), dragStartY + dy.toInt())
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mainHandler.removeCallbacks(lockAfterHold)
                if (dragging) persistMeasuredCenter()
                dragging = false
            }
        }
        return true
    }

    private fun moveCrosshairTo(x: Int, y: Int) {
        val params = crossParams ?: return
        val view = crossView ?: return
        val snapshot = DisplaySnapshots.capture(context)
        val maxX = (snapshot.widthPx - crossSize).coerceAtLeast(0)
        val maxY = (snapshot.heightPx - crossSize).coerceAtLeast(0)
        params.x = x.coerceIn(0, maxX)
        params.y = y.coerceIn(0, maxY)
        try {
            windowManager.updateViewLayout(view, params)
        } catch (t: Throwable) {
            Log.w(TAG, "update crosshair failed", t)
        }
        positionHandle()
    }

    private fun positionHandle() {
        val cross = crossParams ?: return
        val handle = handleView ?: return
        val params = handleParams ?: return
        params.x = cross.x + crossSize - handleWidth / 2
        params.y = cross.y - handleHeight / 2
        try {
            windowManager.updateViewLayout(handle, params)
        } catch (t: Throwable) {
            Log.w(TAG, "update handle failed", t)
        }
    }

    /**
     * WindowManager x/y can be shifted by a status-bar inset on some OEM builds.
     * Nudge until the view's real on-screen center matches the target, then the
     * value we store is the value [dispatchGesture] will use.
     */
    private fun correctToward(targetCenterX: Int, targetCenterY: Int) {
        val view = crossView ?: return
        val params = crossParams ?: return
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val actualX = location[0] + view.width / 2
        val actualY = location[1] + view.height / 2
        val dx = targetCenterX - actualX
        val dy = targetCenterY - actualY
        if (dx == 0 && dy == 0) return
        params.x += dx
        params.y += dy
        try {
            windowManager.updateViewLayout(view, params)
            positionHandle()
        } catch (t: Throwable) {
            Log.w(TAG, "pin inset correction failed", t)
        }
    }

    private fun persistMeasuredCenter() {
        val view = crossView ?: return
        view.post {
            val location = IntArray(2)
            view.getLocationOnScreen(location)
            val centerX = location[0] + view.width / 2
            val centerY = location[1] + view.height / 2
            store.saveCenter(centerX, centerY)
            refreshAppearance()
            onChanged()
        }
    }

    private fun layoutParams(width: Int, height: Int, touchable: Boolean): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        if (!touchable) {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        return WindowManager.LayoutParams(width, height, type, flags, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun dp(value: Float): Int =
        (value * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)

    private class CrosshairView(context: Context) : View(context) {
        var color: Int = COLOR_UNLOCKED
        private val stroke = dp(context, 3f)
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
        }

        override fun onDraw(canvas: Canvas) {
            val cx = width / 2f
            val cy = height / 2f
            val radius = minOf(cx, cy) - stroke
            paint.color = color
            paint.style = Paint.Style.STROKE
            canvas.drawCircle(cx, cy, radius, paint)
            canvas.drawLine(cx - radius, cy, cx + radius, cy, paint)
            canvas.drawLine(cx, cy - radius, cx, cy + radius, paint)
            paint.style = Paint.Style.FILL
            canvas.drawCircle(cx, cy, stroke, paint)
        }
    }

    companion object {
        private const val TAG = "ShutterPinOverlay"
        private const val LOCK_HOLD_MS = 550L
        private val COLOR_UNLOCKED = Color.parseColor("#FF3B30")
        private val COLOR_LOCKED = Color.parseColor("#34C759")
        private val COLOR_STALE = Color.parseColor("#FF9F0A")

        private fun dp(context: Context, value: Float): Float =
            value * context.resources.displayMetrics.density
    }
}

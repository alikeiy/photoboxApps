package com.photoboxtemp.booth

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.media.ExifInterface
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Draws the Canon photo and a frame into one JPEG.
 *
 * Built-in frames are drawn in code. A PNG dropped in `android/app/src/main/assets/frames/`
 * is scaled over the photo; the PNG center should be transparent so the picture shows through.
 */
object FrameComposer {
    private val builtIn = listOf(
        "classic" to "Klasik putih",
        "gold" to "Garis emas",
        "film" to "Film",
    )

    private val assetNames = mapOf(
        "frame_cute_pink" to "Cute & Pink",
    )

    /**
     * Transparent windows on frame_cute_pink.png (2816×1536), measured from alpha.
     * Same numbers as src/config/frames.js. Used when JS does not send a layout.
     */
    private val cutePink = FrameLayout(
        designWidth = 2816,
        designHeight = 1536,
        photoSlots = listOf(
            Slot(590, 306, 368, 492),
            Slot(1013, 310, 373, 482),
            Slot(1435, 306, 372, 488),
            Slot(1857, 314, 363, 485),
        ),
        qrSlot = Slot(1192, 886, 436, 425),
    )

    fun listFramesJson(context: Context): String {
        val array = JSONArray()
        val used = HashSet<String>()
        for ((id, name) in builtIn) {
            used += id
            array.put(JSONObject().put("id", id).put("name", name))
        }
        val files = context.assets.list("frames").orEmpty().sorted()
        for (file in files) {
            if (!file.endsWith(".png", ignoreCase = true)) continue
            val id = file.substringBeforeLast('.')
            if (id.isBlank() || !used.add(id)) continue
            array.put(JSONObject().put("id", id).put("name", assetNames[id] ?: id))
        }
        return array.toString()
    }

    fun compose(
        context: Context,
        photoUri: String,
        frameId: String,
        maxEdge: Int,
        layoutJson: String = "",
        qrMatrix: String = "",
    ): String {
        val edge = maxEdge.coerceIn(480, 3600)
        val bytes = readBytes(context, photoUri)
        val photo = decodeOriented(bytes, edge)
        try {
            val layout = resolveLayout(frameId, layoutJson)
            if (layout != null) {
                return composeSlotted(context, photo, frameId, edge, layout, qrMatrix)
            }
            val longEdge = max(photo.width, photo.height)
            val scale = if (longEdge > edge) edge.toFloat() / longEdge else 1f
            val width = max(1, (photo.width * scale).toInt())
            val height = max(1, (photo.height * scale).toInt())
            val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            var saved = false
            try {
                val canvas = Canvas(output)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(photo, null, Rect(0, 0, width, height), paint)
                drawFrame(context, canvas, frameId, width, height)
                val path = writeJpeg(context, output)
                saved = true
                return path
            } finally {
                if (!saved && !output.isRecycled) output.recycle()
            }
        } finally {
            if (!photo.isRecycled) photo.recycle()
        }
    }

    private fun resolveLayout(frameId: String, layoutJson: String): FrameLayout? {
        if (layoutJson.isNotBlank()) return parseLayout(layoutJson)
        if (frameId == "frame_cute_pink") return cutePink
        return null
    }

    private fun parseLayout(json: String): FrameLayout {
        val root = JSONObject(json)
        val photos = root.getJSONArray("photoSlots")
        val slots = ArrayList<Slot>(photos.length())
        for (index in 0 until photos.length()) {
            slots += slotFrom(photos.getJSONObject(index))
        }
        val qr = if (root.has("qrSlot") && !root.isNull("qrSlot")) {
            slotFrom(root.getJSONObject("qrSlot"))
        } else {
            null
        }
        return FrameLayout(
            designWidth = root.getInt("width"),
            designHeight = root.getInt("height"),
            photoSlots = slots,
            qrSlot = qr,
        )
    }

    private fun slotFrom(json: JSONObject): Slot =
        Slot(json.getInt("x"), json.getInt("y"), json.getInt("w"), json.getInt("h"))

    /**
     * Output keeps the frame sheet's aspect ratio. Each photo window is a
     * center-crop of the same capture. The PNG is drawn over the photos so its
     * border covers the slot edges, then the QR is painted into the bottom hole.
     */
    private fun composeSlotted(
        context: Context,
        photo: Bitmap,
        frameId: String,
        maxEdge: Int,
        layout: FrameLayout,
        qrMatrix: String,
    ): String {
        val designLong = max(layout.designWidth, layout.designHeight).toFloat()
        val fit = maxEdge / designLong
        val width = max(1, (layout.designWidth * fit).roundToInt())
        val height = max(1, (layout.designHeight * fit).roundToInt())
        val scaleX = width.toFloat() / layout.designWidth
        val scaleY = height.toFloat() / layout.designHeight
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        var saved = false
        try {
            val canvas = Canvas(output)
            canvas.drawColor(Color.WHITE)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            for (slot in layout.photoSlots) {
                drawCover(canvas, photo, slot.toRect(scaleX, scaleY), paint)
            }
            drawPng(context, canvas, frameId, width, height)
            val qrSlot = layout.qrSlot
            if (qrSlot != null && qrMatrix.isNotBlank()) {
                drawQr(canvas, qrMatrix, qrSlot.toRect(scaleX, scaleY))
            }
            val path = writeJpeg(context, output)
            saved = true
            return path
        } finally {
            if (!saved && !output.isRecycled) output.recycle()
        }
    }

    private fun drawCover(canvas: Canvas, bitmap: Bitmap, dst: Rect, paint: Paint) {
        if (dst.width() <= 0 || dst.height() <= 0) return
        val scale = max(
            dst.width().toFloat() / bitmap.width,
            dst.height().toFloat() / bitmap.height,
        )
        val srcW = dst.width() / scale
        val srcH = dst.height() / scale
        val srcX = (bitmap.width - srcW) / 2f
        val srcY = (bitmap.height - srcH) / 2f
        val src = Rect(
            srcX.roundToInt().coerceIn(0, bitmap.width - 1),
            srcY.roundToInt().coerceIn(0, bitmap.height - 1),
            (srcX + srcW).roundToInt().coerceIn(1, bitmap.width),
            (srcY + srcH).roundToInt().coerceIn(1, bitmap.height),
        )
        canvas.drawBitmap(bitmap, src, dst, paint)
    }

    private fun drawQr(canvas: Canvas, matrix: String, dst: Rect) {
        val split = matrix.indexOf(':')
        if (split <= 0) return
        val count = matrix.substring(0, split).toIntOrNull() ?: return
        val bits = matrix.substring(split + 1)
        if (count <= 0 || bits.length != count * count) return
        val quiet = 2
        val modules = count + quiet * 2
        val cell = min(dst.width(), dst.height()).toFloat() / modules
        val size = cell * modules
        val left = dst.left + (dst.width() - size) / 2f
        val top = dst.top + (dst.height() - size) / 2f
        val background = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val foreground = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
        canvas.drawRect(dst, background)
        for (row in 0 until count) {
            for (col in 0 until count) {
                if (bits[row * count + col] != '1') continue
                val x = left + (col + quiet) * cell
                val y = top + (row + quiet) * cell
                canvas.drawRect(x, y, x + cell, y + cell, foreground)
            }
        }
    }

    private fun drawFrame(context: Context, canvas: Canvas, frameId: String, width: Int, height: Int) {
        when (frameId) {
            "classic" -> drawClassic(canvas, width, height)
            "gold" -> drawGold(canvas, width, height)
            "film" -> drawFilm(canvas, width, height)
            else -> drawPng(context, canvas, frameId, width, height)
        }
    }

    private fun drawClassic(canvas: Canvas, width: Int, height: Int) {
        val stroke = min(width, height) * 0.04f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.WHITE
            strokeWidth = stroke
        }
        val half = stroke / 2f
        canvas.drawRect(half, half, width - half, height - half, paint)
        paint.color = Color.parseColor("#1A1A1A")
        paint.strokeWidth = max(2f, stroke * 0.18f)
        val inner = stroke * 1.35f
        canvas.drawRect(inner, inner, width - inner, height - inner, paint)
    }

    private fun drawGold(canvas: Canvas, width: Int, height: Int) {
        val outer = min(width, height) * 0.028f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#C6A15B")
            strokeWidth = outer
        }
        val half = outer / 2f
        canvas.drawRect(half, half, width - half, height - half, paint)
        paint.strokeWidth = max(2f, outer * 0.35f)
        val inner = outer * 2.2f
        canvas.drawRect(inner, inner, width - inner, height - inner, paint)
    }

    private fun drawFilm(canvas: Canvas, width: Int, height: Int) {
        val bar = min(width, height) * 0.075f
        val band = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
        canvas.drawRect(0f, 0f, width.toFloat(), bar, band)
        canvas.drawRect(0f, height - bar, width.toFloat(), height.toFloat(), band)
        val hole = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val radius = bar * 0.18f
        var x = radius * 2.4f
        while (x < width - radius) {
            canvas.drawCircle(x, bar / 2f, radius, hole)
            canvas.drawCircle(x, height - bar / 2f, radius, hole)
            x += radius * 4.2f
        }
    }

    private fun drawPng(context: Context, canvas: Canvas, frameId: String, width: Int, height: Int) {
        val overlay = context.assets.open("frames/$frameId.png").use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: throw IllegalArgumentException("Frame tidak ditemukan: $frameId")
        try {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(overlay, null, Rect(0, 0, width, height), paint)
        } finally {
            overlay.recycle()
        }
    }

    private fun readBytes(context: Context, photoUri: String): ByteArray {
        val uri = Uri.parse(photoUri)
        val stream = when (uri.scheme) {
            "content" -> context.contentResolver.openInputStream(uri)
            "file" -> uri.path?.let { File(it).inputStream() }
            else -> null
        } ?: throw IllegalArgumentException("Foto tidak bisa dibuka")
        return stream.use { it.readBytes() }
    }

    private fun decodeOriented(bytes: ByteArray, maxEdge: Int): Bitmap {
        val orientation = ExifInterface(ByteArrayInputStream(bytes)).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IllegalArgumentException("Berkas foto kosong")
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxEdge)
        }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            ?: throw IllegalArgumentException("Foto tidak bisa dibaca")
        return applyExif(decoded, orientation)
    }

    private fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        while (max(width, height) / sample > maxEdge && sample < 32) {
            sample *= 2
        }
        return sample
    }

    private fun applyExif(source: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.preScale(-1f, 1f)
            }
            else -> return source
        }
        val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (rotated != source) source.recycle()
        return rotated
    }

    private fun writeJpeg(context: Context, bitmap: Bitmap): String {
        val dir = File(context.cacheDir, "booth").apply { mkdirs() }
        val stale = dir.listFiles()?.sortedBy { it.lastModified() }.orEmpty()
        if (stale.size > 12) stale.dropLast(8).forEach { it.delete() }
        val file = File(dir, "booth-${System.currentTimeMillis()}.jpg")
        try {
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        } finally {
            bitmap.recycle()
        }
        return "file://${file.absolutePath}"
    }
}

private data class Slot(val x: Int, val y: Int, val w: Int, val h: Int) {
    fun toRect(scaleX: Float, scaleY: Float): Rect = Rect(
        (x * scaleX).roundToInt(),
        (y * scaleY).roundToInt(),
        ((x + w) * scaleX).roundToInt(),
        ((y + h) * scaleY).roundToInt(),
    )
}

private data class FrameLayout(
    val designWidth: Int,
    val designHeight: Int,
    val photoSlots: List<Slot>,
    val qrSlot: Slot?,
)

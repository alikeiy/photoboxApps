package com.photoboxtemp.booth

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.annotations.ReactModule
import com.photoboxtemp.specs.NativePhotoBoothSpec

@ReactModule(name = PhotoBoothModule.NAME)
class PhotoBoothModule(
    reactContext: ReactApplicationContext,
) : NativePhotoBoothSpec(reactContext) {

    init {
        Companion.reactContext = reactContext
    }

    override fun consumePendingImport(promise: Promise) {
        promise.resolve(PendingImport.consume())
    }

    override fun latestImage(promise: Promise) {
        try {
            promise.resolve(PhotoImportWatcher.latestUri(reactApplicationContext) ?: "")
        } catch (t: SecurityException) {
            promise.reject(
                "MEDIA_PERMISSION",
                "Izinkan akses semua foto agar foto Canon Camera Connect bisa dibaca.",
                t,
            )
        } catch (t: Throwable) {
            promise.reject("LATEST_IMAGE", t.message, t)
        }
    }

    override fun listFrames(promise: Promise) {
        try {
            promise.resolve(FrameComposer.listFramesJson(reactApplicationContext))
        } catch (t: Throwable) {
            promise.reject("LIST_FRAMES", t.message, t)
        }
    }

    override fun compose(
        photoUri: String,
        frameId: String,
        maxEdge: Double,
        layoutJson: String,
        qrMatrix: String,
        promise: Promise,
    ) {
        try {
            val path = FrameComposer.compose(
                reactApplicationContext,
                photoUri,
                frameId,
                maxEdge.toInt(),
                layoutJson,
                qrMatrix,
            )
            promise.resolve(path)
        } catch (t: SecurityException) {
            promise.reject(
                "MEDIA_PERMISSION",
                "Izinkan akses semua foto agar foto Canon bisa digabung dengan frame.",
                t,
            )
        } catch (t: Throwable) {
            promise.reject("COMPOSE", t.message, t)
        }
    }

    companion object {
        const val NAME: String = NativePhotoBoothSpec.NAME

        @Volatile
        var reactContext: ReactApplicationContext? = null
    }
}

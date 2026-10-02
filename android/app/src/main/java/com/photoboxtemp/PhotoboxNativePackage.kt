package com.photoboxtemp

import com.facebook.react.TurboReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.model.ReactModuleInfo
import com.facebook.react.module.model.ReactModuleInfoProvider

import com.photoboxtemp.booth.PhotoBoothModule
import com.photoboxtemp.trigger.ShutterTriggerModule

/**
 * Registers Photo Booth native modules.
 *
 * - [CanonUsbModule] — TurboModule for Canon EOS M10 / USB PTP control
 * - [ShutterTriggerModule] — overlay pin + hardware tap for Canon Camera Connect
 * - [PhotoBoothModule] — MediaStore import and frame composite
 */
class PhotoboxNativePackage : TurboReactPackage() {

    override fun getModule(name: String, reactContext: ReactApplicationContext): NativeModule? =
        when (name) {
            CanonUsbModule.NAME -> CanonUsbModule(reactContext)
            ShutterTriggerModule.NAME -> ShutterTriggerModule(reactContext)
            PhotoBoothModule.NAME -> PhotoBoothModule(reactContext)
            else -> null
        }

    override fun getReactModuleInfoProvider(): ReactModuleInfoProvider =
        ReactModuleInfoProvider {
            val isTurboModule = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED
            mapOf(
                CanonUsbModule.NAME to ReactModuleInfo(
                    CanonUsbModule.NAME,
                    CanonUsbModule.NAME,
                    false,
                    false,
                    false,
                    false,
                    isTurboModule,
                ),
                ShutterTriggerModule.NAME to ReactModuleInfo(
                    ShutterTriggerModule.NAME,
                    ShutterTriggerModule.NAME,
                    false,
                    false,
                    false,
                    false,
                    isTurboModule,
                ),
                PhotoBoothModule.NAME to ReactModuleInfo(
                    PhotoBoothModule.NAME,
                    PhotoBoothModule.NAME,
                    false,
                    false,
                    false,
                    false,
                    isTurboModule,
                ),
            )
        }
}

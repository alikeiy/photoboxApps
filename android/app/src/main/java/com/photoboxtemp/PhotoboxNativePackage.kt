package com.photoboxtemp

import com.facebook.react.TurboReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.model.ReactModuleInfo
import com.facebook.react.module.model.ReactModuleInfoProvider

/**
 * Registers Photo Booth native modules.
 *
 * - [CanonUsbModule] — TurboModule for Canon EOS M10 / USB PTP control
 */
class PhotoboxNativePackage : TurboReactPackage() {

    override fun getModule(name: String, reactContext: ReactApplicationContext): NativeModule? =
        when (name) {
            CanonUsbModule.NAME -> CanonUsbModule(reactContext)
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
            )
        }
}

package com.photoboxtemp

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager

object UsbDeviceUtils {

    fun isUvcDevice(device: UsbDevice): Boolean {
        if (device.vendorId == CanonUsbConnectionManager.CANON_VENDOR_ID) {
            return false
        }

        if (device.deviceClass == 239 && device.deviceSubclass == 2) {
            return true
        }
        if (device.deviceClass == UsbConstants.USB_CLASS_VIDEO) {
            return true
        }

        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            if (iface.interfaceClass == UsbConstants.USB_CLASS_VIDEO) {
                return true
            }
            if (iface.interfaceClass == 239 && iface.interfaceSubclass == 2) {
                return true
            }
        }
        return false
    }

    fun findUvcDevices(usbManager: UsbManager): List<UsbDevice> =
        usbManager.deviceList.values.filter { isUvcDevice(it) }

    fun findUvcDevice(usbManager: UsbManager): UsbDevice? =
        findUvcDevices(usbManager).firstOrNull()
}

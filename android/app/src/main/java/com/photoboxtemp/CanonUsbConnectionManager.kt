package com.photoboxtemp

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log

/**
 * Holds the active USB session to the Canon camera (open + claimed PTP interface).
 */
class CanonUsbConnectionManager(private val usbManager: UsbManager) {

    companion object {
        private const val TAG = "CanonUsbModule"
        const val CANON_VENDOR_ID = 0x04A9
        const val EOS_M10_PRODUCT_ID = 0x32A0
    }

    var canonDevice: UsbDevice? = null
        private set

    private var connection: UsbDeviceConnection? = null
    private var claimedInterface: UsbInterface? = null

    val isSessionOpen: Boolean
        get() = connection != null && claimedInterface != null

    fun findCanonDevice(): UsbDevice? =
        usbManager.deviceList.values.firstOrNull { it.vendorId == CANON_VENDOR_ID }

    fun hasPermission(device: UsbDevice): Boolean = usbManager.hasPermission(device)

    fun openAndClaim(device: UsbDevice): Boolean {
        release()

        Log.i(TAG, "openAndClaim() — ${readableName(device)} " +
            "vid=0x${device.vendorId.toString(16)} pid=0x${device.productId.toString(16)}")

        val conn = usbManager.openDevice(device)
        if (conn == null) {
            Log.e(TAG, "openAndClaim() — openDevice() returned null")
            return false
        }

        val ptpInterface = findPtpInterface(device)
        if (ptpInterface == null) {
            Log.e(TAG, "openAndClaim() — no PTP Still Image interface found")
            conn.close()
            return false
        }

        val claimed = conn.claimInterface(ptpInterface, true)
        if (!claimed) {
            Log.e(TAG, "openAndClaim() — claimInterface(${ptpInterface.id}) failed (MTP may hold it)")
            conn.close()
            return false
        }

        canonDevice = device
        connection = conn
        claimedInterface = ptpInterface

        Log.i(
            TAG,
            "openAndClaim() SUCCESS — fd=${conn.fileDescriptor} " +
                "interface=${ptpInterface.id} endpoints=${ptpInterface.endpointCount}",
        )
        logEndpoints(ptpInterface)
        return true
    }

    fun verifySession(): Boolean {
        val conn = connection ?: return false
        return conn.fileDescriptor >= 0
    }

    fun release() {
        val conn = connection
        val iface = claimedInterface
        val device = canonDevice

        if (conn != null && iface != null) {
            try {
                conn.releaseInterface(iface)
                Log.i(TAG, "release() — interface ${iface.id} released")
            } catch (t: Throwable) {
                Log.w(TAG, "release() — releaseInterface failed", t)
            }
        }

        conn?.close()
        if (conn != null) Log.i(TAG, "release() — connection closed")

        connection = null
        claimedInterface = null
        canonDevice = null
    }

    private fun findPtpInterface(device: UsbDevice): UsbInterface? {
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            if (iface.interfaceClass == UsbConstants.USB_CLASS_STILL_IMAGE) {
                Log.i(TAG, "findPtpInterface() — interface ${iface.id} class=STILL_IMAGE")
                return iface
            }
        }
        // Fallback: first interface (some cameras report per-interface class)
        return if (device.interfaceCount > 0) {
            Log.w(TAG, "findPtpInterface() — no STILL_IMAGE class, using interface 0")
            device.getInterface(0)
        } else {
            null
        }
    }

    private fun logEndpoints(usbInterface: UsbInterface) {
        for (i in 0 until usbInterface.endpointCount) {
            val ep = usbInterface.getEndpoint(i)
            Log.i(
                TAG,
                "  endpoint 0x${ep.address.toString(16)} " +
                    "type=${ep.type} dir=${ep.direction} maxPacket=${ep.maxPacketSize}",
            )
        }
    }

    private fun readableName(device: UsbDevice): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            device.productName?.takeIf { it.isNotBlank() }?.let { return it }
            device.manufacturerName?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return "Canon USB device"
    }
}

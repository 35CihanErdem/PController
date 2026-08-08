package com.cihan.pcontroller.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import com.cihan.pcontroller.util.PermissionHelper

data class BondedDevice(
    val address: String,
    val name: String
)

class BondedDeviceRepository(
    private val context: Context
) {
    @SuppressLint("MissingPermission")
    fun loadBondedDevices(): List<BondedDevice> {
        if (!PermissionHelper.hasBluetoothPermissions(context)) return emptyList()
        val adapter = PermissionHelper.bluetoothAdapter(context) ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()

        return try {
            adapter.bondedDevices
                .orEmpty()
                .map { device ->
                    BondedDevice(
                        address = device.address,
                        name = device.safeDisplayName()
                    )
                }
                .sortedBy { it.name.lowercase() }
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.safeDisplayName(): String {
        return try {
            name?.takeIf { it.isNotBlank() } ?: address
        } catch (_: SecurityException) {
            address
        }
    }
}

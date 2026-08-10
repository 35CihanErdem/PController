package com.cihan.pccontroller.util

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

object PermissionHelper {

    fun bluetoothAdapter(context: Context): BluetoothAdapter? {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return manager?.adapter
    }

    fun isBluetoothSupported(context: Context): Boolean = bluetoothAdapter(context) != null

    fun isBluetoothEnabled(context: Context): Boolean =
        bluetoothAdapter(context)?.isEnabled == true

    fun requiredRuntimePermissions(): Array<String> {
        val list = mutableListOf<String>()
        list += requiredBluetoothPermissions()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list += Manifest.permission.POST_NOTIFICATIONS
        }
        return list.toTypedArray()
    }

    /** Permissions required to list bonded devices / HID connect (not notifications). */
    fun requiredBluetoothPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        } else {
            emptyArray()
        }
    }

    fun missingPermissions(context: Context, permissions: Array<String> = requiredBluetoothPermissions()): List<String> {
        return permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasBluetoothPermissions(context: Context): Boolean =
        missingPermissions(context).isEmpty()

    fun createEnableBluetoothIntent(): Intent =
        Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

    fun openAppSettings(activity: Activity) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", activity.packageName, null)
        )
        activity.startActivity(intent)
    }

    fun openBluetoothSettings(context: Context) {
        try {
            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        } catch (_: Exception) {
            context.startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }
}

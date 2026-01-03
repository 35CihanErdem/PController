package com.example.bluetoothkeyboard

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat

class BluetoothHidService : Service() {
    
    companion object {
        private const val TAG = "BluetoothHidService"
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_CHANNEL_ID = "bluetooth_keyboard_channel"
        
        private var hidDevice: BluetoothHidDevice? = null
        private var connectedDevice: BluetoothDevice? = null
        private var isServiceConnected = false
        
        fun isConnected(): Boolean {
            return isServiceConnected && connectedDevice != null
        }
        
        fun sendKey(keyCode: KeyCode) {
            if (!isConnected()) {
                Log.e(TAG, "Not connected, cannot send key")
                return
            }
            
            hidDevice?.let { device ->
                connectedDevice?.let { targetDevice ->
                    try {
                        // Send key press - Report ID 1 (as defined in descriptor)
                        val report = createKeyboardReport(keyCode)
                        val result = device.sendReport(targetDevice, 1, report)
                        Log.d(TAG, "Sent key: $keyCode, result: $result")
                        
                        // Release key after 100ms to ensure it's registered
                        Handler(Looper.getMainLooper()).postDelayed({
                            val releaseReport = createKeyboardReport(KeyCode.NONE)
                            device.sendReport(targetDevice, 1, releaseReport)
                            Log.d(TAG, "Released key: $keyCode")
                        }, 100)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error sending key: $keyCode", e)
                    }
                }
            }
        }
        
        private fun createKeyboardReport(keyCode: KeyCode): ByteArray {
            // HID Keyboard Report Format (8 bytes):
            // Byte 0: Modifier keys (Ctrl, Shift, Alt, etc.) - bit flags
            // Byte 1: Reserved (always 0)
            // Bytes 2-7: Key codes (up to 6 keys can be pressed simultaneously)
            val report = ByteArray(8)
            report[0] = 0x00 // No modifiers
            report[1] = 0x00 // Reserved
            // Bytes 2-7 are already 0x00 (no keys pressed)
            
            when (keyCode) {
                KeyCode.SPACE -> {
                    report[2] = 0x2C.toByte() // Space key (HID Usage ID 0x2C)
                    Log.d(TAG, "Creating report for SPACE (0x2C)")
                }
                KeyCode.LEFT_ARROW -> {
                    // Left Arrow - HID Usage ID 0x50 from Keyboard Usage Page
                    report[2] = 0x50.toByte()
                    Log.d(TAG, "Creating report for LEFT_ARROW (0x50)")
                }
                KeyCode.RIGHT_ARROW -> {
                    // Right Arrow - HID Usage ID 0x4F
                    report[2] = 0x4F.toByte()
                    Log.d(TAG, "Creating report for RIGHT_ARROW (0x4F)")
                }
                KeyCode.UP_ARROW -> {
                    // Up Arrow - HID Usage ID 0x52
                    report[2] = 0x52.toByte()
                    Log.d(TAG, "Creating report for UP_ARROW (0x52)")
                }
                KeyCode.DOWN_ARROW -> {
                    // Down Arrow - HID Usage ID 0x51
                    report[2] = 0x51.toByte()
                    Log.d(TAG, "Creating report for DOWN_ARROW (0x51)")
                }
                KeyCode.F -> {
                    // F key - HID Usage ID 0x09
                    report[2] = 0x09.toByte()
                    Log.d(TAG, "Creating report for F (0x09)")
                }
                KeyCode.NONE -> {
                    // Empty report - all zeros to release all keys
                    Log.d(TAG, "Creating release report (all zeros)")
                }
            }
            
            // Log the report for debugging
            Log.d(TAG, "Report bytes: ${report.joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }}")
            
            return report
        }
    }
    
    private val hidProfileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = proxy as BluetoothHidDevice
                isServiceConnected = true
                Log.d(TAG, "HID Device profile connected")
                
                // Register HID device
                registerHidDevice()
            }
        }
        
        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                isServiceConnected = false
                hidDevice = null
                Log.d(TAG, "HID Device profile disconnected")
            }
        }
    }
    
    private val hidHostCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            Log.d(TAG, "App status changed: registered=$registered")
        }
        
        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            Log.d(TAG, "Connection state changed: ${device?.address} state=$state")
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedDevice = device
                    Log.d(TAG, "Connected to ${device?.name}")
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (connectedDevice == device) {
                        connectedDevice = null
                        Log.d(TAG, "Disconnected from ${device?.name}")
                    }
                }
            }
        }
        
        override fun onGetReport(
            device: BluetoothDevice?,
            type: Byte,
            id: Byte,
            bufferSize: Int
        ) {
            Log.d(TAG, "Get report requested")
        }
        
        override fun onSetReport(
            device: BluetoothDevice?,
            type: Byte,
            id: Byte,
            report: ByteArray?
        ) {
            Log.d(TAG, "Set report requested")
        }
        
        override fun onSetProtocol(device: BluetoothDevice?, protocol: Byte) {
            Log.d(TAG, "Set protocol requested")
        }
        
        override fun onInterruptData(device: BluetoothDevice?, reportId: Byte, data: ByteArray?) {
            Log.d(TAG, "Interrupt data received")
        }
        
        override fun onVirtualCableUnplug(device: BluetoothDevice?) {
            Log.d(TAG, "Virtual cable unplugged")
            connectedDevice = null
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        
        if (adapter != null) {
            adapter.getProfileProxy(this, hidProfileListener, BluetoothProfile.HID_DEVICE)
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "CONNECT" -> {
                val deviceAddress = intent.getStringExtra("device_address")
                deviceAddress?.let { address ->
                    connectToDevice(address)
                }
            }
            "DISCONNECT" -> {
                disconnect()
                stopSelf()
            }
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        disconnect()
        
        hidDevice?.let { device ->
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = bluetoothManager.adapter
            adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, device)
        }
        
        hidDevice = null
        isServiceConnected = false
        connectedDevice = null
    }
    
    private fun registerHidDevice() {
        hidDevice?.let { device ->
            // HID Descriptor for a keyboard with extended keys (including arrow keys)
            val descriptor = byteArrayOf(
                0x05.toByte(), 0x01.toByte(),        // Usage Page (Generic Desktop)
                0x09.toByte(), 0x06.toByte(),        // Usage (Keyboard)
                0xa1.toByte(), 0x01.toByte(),        // Collection (Application)
                0x85.toByte(), 0x01.toByte(),        // Report ID (1)
                0x75.toByte(), 0x01.toByte(),        // Report Size (1)
                0x95.toByte(), 0x08.toByte(),        // Report Count (8)
                0x05.toByte(), 0x07.toByte(),        // Usage Page (Key Codes)
                0x19.toByte(), 0xe0.toByte(),        // Usage Minimum (224) - Left Control
                0x29.toByte(), 0xe7.toByte(),        // Usage Maximum (231) - Right GUI
                0x15.toByte(), 0x00.toByte(),        // Logical Minimum (0)
                0x25.toByte(), 0x01.toByte(),        // Logical Maximum (1)
                0x81.toByte(), 0x02.toByte(),        // Input (Data, Variable, Absolute) - Modifiers
                0x95.toByte(), 0x01.toByte(),        // Report Count (1)
                0x75.toByte(), 0x08.toByte(),        // Report Size (8)
                0x81.toByte(), 0x03.toByte(),        // Input (Constant) - Reserved byte
                0x95.toByte(), 0x05.toByte(),        // Report Count (5)
                0x75.toByte(), 0x01.toByte(),        // Report Size (1)
                0x05.toByte(), 0x08.toByte(),        // Usage Page (LEDs)
                0x19.toByte(), 0x01.toByte(),        // Usage Minimum (1) - Num Lock
                0x29.toByte(), 0x05.toByte(),        // Usage Maximum (5) - Kana
                0x91.toByte(), 0x02.toByte(),        // Output (Data, Variable, Absolute) - LED states
                0x95.toByte(), 0x01.toByte(),        // Report Count (1)
                0x75.toByte(), 0x03.toByte(),        // Report Size (3)
                0x91.toByte(), 0x03.toByte(),        // Output (Constant) - LED padding
                0x95.toByte(), 0x06.toByte(),        // Report Count (6) - 6 keys
                0x75.toByte(), 0x08.toByte(),        // Report Size (8)
                0x15.toByte(), 0x00.toByte(),        // Logical Minimum (0)
                0x25.toByte(), 0xE7.toByte(),        // Logical Maximum (231) - Extended range for arrow keys
                0x05.toByte(), 0x07.toByte(),        // Usage Page (Key Codes)
                0x19.toByte(), 0x00.toByte(),        // Usage Minimum (0) - No key
                0x29.toByte(), 0xE7.toByte(),        // Usage Maximum (231) - Extended keys
                0x81.toByte(), 0x00.toByte(),        // Input (Data, Array) - Key codes
                0xc0.toByte()                        // End Collection
            )
            
            // Create SDP settings for HID device
            val sdpSettings = BluetoothHidDeviceAppSdpSettings(
                "Bluetooth Keyboard",
                "Remote keyboard control",
                "Bluetooth Keyboard",
                BluetoothHidDevice.SUBCLASS1_COMBO,
                descriptor
            )
            
            val result = device.registerApp(
                sdpSettings,
                null, // In QOS - use default
                null, // Out QOS - use default
                mainExecutor,
                hidHostCallback
            )
            
            Log.d(TAG, "HID device registration result: $result")
        }
    }
    
    private fun connectToDevice(deviceAddress: String) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        
        if (adapter == null) {
            Log.e(TAG, "Bluetooth adapter not available")
            return
        }
        
        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            adapter.getRemoteDevice(deviceAddress)
        } else {
            @Suppress("DEPRECATION")
            adapter.getRemoteDevice(deviceAddress)
        }
        
        if (isServiceConnected && hidDevice != null) {
            val connected = hidDevice!!.connect(device)
            Log.d(TAG, "Connection attempt result: $connected")
        } else {
            Log.e(TAG, "HID service not ready")
        }
    }
    
    private fun disconnect() {
        connectedDevice?.let { device ->
            hidDevice?.disconnect(device)
            connectedDevice = null
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Bluetooth Keyboard Service",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Bluetooth HID Keyboard service notification"
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}

enum class KeyCode {
    SPACE,
    LEFT_ARROW,
    RIGHT_ARROW,
    UP_ARROW,
    DOWN_ARROW,
    F,
    NONE
}


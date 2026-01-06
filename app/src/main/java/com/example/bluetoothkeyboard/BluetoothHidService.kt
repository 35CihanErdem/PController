package com.example.bluetoothkeyboard

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class BluetoothHidService : Service() {
    
    companion object {
        private const val TAG = "BluetoothHidService"
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_CHANNEL_ID = "bluetooth_keyboard_channel"
        
        private var hidDevice: BluetoothHidDevice? = null
        private var connectedDevice: BluetoothDevice? = null
        private var isServiceConnected = false
        private var serviceInstance: BluetoothHidService? = null
        
        fun setServiceInstance(instance: BluetoothHidService?) {
            serviceInstance = instance
        }
        
        private fun hasBluetoothConnectPermission(): Boolean {
            return serviceInstance?.let { service ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ContextCompat.checkSelfPermission(
                        service,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }
            } ?: false
        }
        
        fun isConnected(): Boolean {
            return isServiceConnected && connectedDevice != null
        }
        
        fun sendKey(keyCode: KeyCode) {
            if (!isConnected()) {
                Log.e(TAG, "Not connected, cannot send key")
                return
            }
            
            if (!hasBluetoothConnectPermission()) {
                Log.e(TAG, "BLUETOOTH_CONNECT permission not granted")
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
                            try {
                                val releaseReport = createKeyboardReport(KeyCode.NONE)
                                device.sendReport(targetDevice, 1, releaseReport)
                                Log.d(TAG, "Released key: $keyCode")
                            } catch (e: SecurityException) {
                                Log.e(TAG, "SecurityException releasing key: $keyCode", e)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error releasing key: $keyCode", e)
                            }
                        }, 100)
                    } catch (e: SecurityException) {
                        Log.e(TAG, "SecurityException sending key: $keyCode", e)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error sending key: $keyCode", e)
                    }
                }
            }
        }
        
        fun sendBrightnessUp() {
            if (!isConnected()) {
                Log.e(TAG, "Not connected, cannot send brightness up")
                return
            }
            
            if (!hasBluetoothConnectPermission()) {
                Log.e(TAG, "BLUETOOTH_CONNECT permission not granted")
                return
            }
            
            hidDevice?.let { device ->
                connectedDevice?.let { targetDevice ->
                    try {
                        // Method 1: Consumer Control - Brightness Increment (0x6F)
                        val report = createConsumerControlReport(0x6F)
                        val result = device.sendReport(targetDevice, 2, report)
                        Log.d(TAG, "Sent brightness up (Consumer Control 0x6F), result: $result")
                        
                        // Release after 150ms
                        Handler(Looper.getMainLooper()).postDelayed({
                            try {
                                val releaseReport = createConsumerControlReport(0x00)
                                device.sendReport(targetDevice, 2, releaseReport)
                                Log.d(TAG, "Released brightness up")
                            } catch (e: SecurityException) {
                                Log.e(TAG, "SecurityException releasing brightness up", e)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error releasing brightness up", e)
                            }
                        }, 150)
                        
                        // Method 2: F tuşları (test için - hangisi çalışıyorsa onu kullan)
                        // F tuşları HID kodları:
                        // F1: 0x3A, F2: 0x3B, F3: 0x3C, F4: 0x3D, F5: 0x3E, F6: 0x3F
                        // F7: 0x40, F8: 0x41, F9: 0x42, F10: 0x43, F11: 0x44, F12: 0x45
                        // F7 (0x40) - Bazı PC'lerde parlaklık artırır
                        Handler(Looper.getMainLooper()).postDelayed({
                            try {
                                sendFKey(targetDevice, device, 0x40) // F7
                                Log.d(TAG, "Sent F7 key (0x40) for brightness up")
                            } catch (e: SecurityException) {
                                Log.e(TAG, "SecurityException sending F7 key", e)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error sending F7 key", e)
                            }
                        }, 200) // Consumer Control'dan sonra 200ms bekle
                    } catch (e: SecurityException) {
                        Log.e(TAG, "SecurityException sending brightness up", e)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error sending brightness up", e)
                    }
                }
            }
        }
        
        fun sendBrightnessDown() {
            if (!isConnected()) {
                Log.e(TAG, "Not connected, cannot send brightness down")
                return
            }
            
            if (!hasBluetoothConnectPermission()) {
                Log.e(TAG, "BLUETOOTH_CONNECT permission not granted")
                return
            }
            
            hidDevice?.let { device ->
                connectedDevice?.let { targetDevice ->
                    try {
                        // Method 1: Consumer Control - Brightness Decrement (0x70)
                        val report = createConsumerControlReport(0x70)
                        val result = device.sendReport(targetDevice, 2, report)
                        Log.d(TAG, "Sent brightness down (Consumer Control 0x70), result: $result")
                        
                        // Release after 150ms
                        Handler(Looper.getMainLooper()).postDelayed({
                            try {
                                val releaseReport = createConsumerControlReport(0x00)
                                device.sendReport(targetDevice, 2, releaseReport)
                                Log.d(TAG, "Released brightness down")
                            } catch (e: SecurityException) {
                                Log.e(TAG, "SecurityException releasing brightness down", e)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error releasing brightness down", e)
                            }
                        }, 150)
                        
                        // Method 2: F tuşları (test için - hangisi çalışıyorsa onu kullan)
                        // F6 (0x3F) - Bazı PC'lerde parlaklık azaltır
                        Handler(Looper.getMainLooper()).postDelayed({
                            try {
                                sendFKey(targetDevice, device, 0x3F) // F6
                                Log.d(TAG, "Sent F6 key (0x3F) for brightness down")
                            } catch (e: SecurityException) {
                                Log.e(TAG, "SecurityException sending F6 key", e)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error sending F6 key", e)
                            }
                        }, 200) // Consumer Control'dan sonra 200ms bekle
                    } catch (e: SecurityException) {
                        Log.e(TAG, "SecurityException sending brightness down", e)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error sending brightness down", e)
                    }
                }
            }
        }
        
        // F tuşu gönderme fonksiyonu (test için)
        private fun sendFKey(targetDevice: BluetoothDevice, device: BluetoothHidDevice, fKeyCode: Int) {
            if (!hasBluetoothConnectPermission()) {
                Log.e(TAG, "BLUETOOTH_CONNECT permission not granted for F key")
                return
            }
            
            try {
                val report = ByteArray(8)
                report[0] = 0x00 // No modifiers
                report[1] = 0x00 // Reserved
                report[2] = fKeyCode.toByte() // F key code
                
                device.sendReport(targetDevice, 1, report)
                
                // Release after 100ms
                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        val releaseReport = ByteArray(8)
                        device.sendReport(targetDevice, 1, releaseReport)
                    } catch (e: SecurityException) {
                        Log.e(TAG, "SecurityException releasing F key", e)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error releasing F key", e)
                    }
                }, 100)
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException sending F key", e)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending F key", e)
            }
        }
        
        private fun createConsumerControlReport(usageCode: Int): ByteArray {
            // Consumer Control Report Format (1 byte for Windows compatibility):
            // Windows typically expects 1-byte Consumer Control reports
            val report = ByteArray(1)
            report[0] = (usageCode and 0xFF).toByte()
            Log.d(TAG, "Creating consumer control report: 0x${usageCode.toString(16).uppercase()}")
            return report
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
                KeyCode.LEFT_ARROW -> {
                    // Left Arrow - HID Usage ID 0x50
                    report[2] = 0x50.toByte()
                    Log.d(TAG, "Creating report for LEFT_ARROW (0x50)")
                }
                KeyCode.RIGHT_ARROW -> {
                    // Right Arrow - HID Usage ID 0x4F
                    report[2] = 0x4F.toByte()
                    Log.d(TAG, "Creating report for RIGHT_ARROW (0x4F)")
                }
                KeyCode.SPACE -> {
                    // Space key - HID Usage ID 0x2C
                    report[2] = 0x2C.toByte()
                    Log.d(TAG, "Creating report for SPACE (0x2C)")
                }
                KeyCode.F -> {
                    // F key - HID Usage ID 0x09 (Fullscreen toggle)
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
            val deviceAddress = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ContextCompat.checkSelfPermission(
                            this@BluetoothHidService,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        device?.address ?: "Unknown"
                    } else {
                        "Unknown (no permission)"
                    }
                } else {
                    device?.address ?: "Unknown"
                }
            } catch (e: SecurityException) {
                "Unknown (security exception)"
            }
            Log.d(TAG, "Connection state changed: $deviceAddress state=$state")
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedDevice = device
                    val deviceName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (ContextCompat.checkSelfPermission(
                                this@BluetoothHidService,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            device?.name ?: device?.address ?: "Bilinmeyen"
                        } else {
                            device?.address ?: "Bilinmeyen"
                        }
                    } else {
                        device?.name ?: device?.address ?: "Bilinmeyen"
                    }
                    Log.d(TAG, "Connected to $deviceName")
                    // Update notification when connected
                    val notificationManager = this@BluetoothHidService.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(NOTIFICATION_ID, this@BluetoothHidService.createNotification())
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (connectedDevice == device) {
                        val deviceName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (ContextCompat.checkSelfPermission(
                                    this@BluetoothHidService,
                                    Manifest.permission.BLUETOOTH_CONNECT
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                device?.name ?: device?.address ?: "Bilinmeyen"
                            } else {
                                device?.address ?: "Bilinmeyen"
                            }
                        } else {
                            device?.name ?: device?.address ?: "Bilinmeyen"
                        }
                        Log.d(TAG, "Disconnected from $deviceName")
                        connectedDevice = null
                        // Update notification when disconnected
                        val notificationManager = this@BluetoothHidService.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.notify(NOTIFICATION_ID, this@BluetoothHidService.createNotification())
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
        // Set service instance for permission checks
        setServiceInstance(this)
        
        createNotificationChannel()
        // Delete old channel and recreate if needed (for testing)
        val notificationManager = getSystemService(NotificationManager::class.java)
        try {
            notificationManager.deleteNotificationChannel(NOTIFICATION_CHANNEL_ID)
        } catch (e: Exception) {
            // Ignore
        }
        createNotificationChannel()
        
        startForeground(NOTIFICATION_ID, createNotification())
        
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        
        adapter?.getProfileProxy(this, hidProfileListener, BluetoothProfile.HID_DEVICE)
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
            "ACTION_LEFT" -> {
                sendKey(KeyCode.LEFT_ARROW)
            }
            "ACTION_RIGHT" -> {
                sendKey(KeyCode.RIGHT_ARROW)
            }
            "ACTION_UP" -> {
                sendKey(KeyCode.UP_ARROW)
            }
            "ACTION_DOWN" -> {
                sendKey(KeyCode.DOWN_ARROW)
            }
        }
        // Update notification after action
        if (intent?.action?.startsWith("ACTION_") == true) {
            startForeground(NOTIFICATION_ID, createNotification())
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
        setServiceInstance(null) // Clear service instance
    }
    
    private fun registerHidDevice() {
        hidDevice?.let { device ->
            // HID Descriptor for a keyboard with extended keys + Consumer Control (brightness)
            val descriptor = byteArrayOf(
                // Keyboard Collection (Report ID 1)
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
                0xc0.toByte(),                       // End Collection
                
                // Consumer Control Collection (Report ID 2) - For brightness control
                0x05.toByte(), 0x0C.toByte(),        // Usage Page (Consumer)
                0x09.toByte(), 0x01.toByte(),        // Usage (Consumer Control)
                0xa1.toByte(), 0x01.toByte(),        // Collection (Application)
                0x85.toByte(), 0x02.toByte(),        // Report ID (2)
                0x75.toByte(), 0x08.toByte(),        // Report Size (8 bits) - Changed to 8 bits for Windows
                0x95.toByte(), 0x01.toByte(),        // Report Count (1)
                0x15.toByte(), 0x00.toByte(),        // Logical Minimum (0)
                0x25.toByte(), 0xFF.toByte(),        // Logical Maximum (255) - Changed to 255 for 8-bit
                0x19.toByte(), 0x00.toByte(),        // Usage Minimum (0)
                0x29.toByte(), 0xFF.toByte(),        // Usage Maximum (255) - Changed to 255 for 8-bit
                0x81.toByte(), 0x00.toByte(),        // Input (Data, Array) - Consumer Control codes
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
            
            // Check permission for Android 12+ (registerApp requires BLUETOOTH_CONNECT)
            val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
            
            if (!hasPermission) {
                Log.e(TAG, "BLUETOOTH_CONNECT permission not granted for registerApp")
                return
            }
            
            val result = try {
                device.registerApp(
                    sdpSettings,
                    null, // In QOS - use default
                    null, // Out QOS - use default
                    mainExecutor,
                    hidHostCallback
                )
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException registering HID device", e)
                return
            }
            
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
        
        // Check permission for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e(TAG, "BLUETOOTH_CONNECT permission not granted")
                return
            }
        }
        
        val device = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                adapter.getRemoteDevice(deviceAddress)
            } else {
                adapter.getRemoteDevice(deviceAddress)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException getting remote device", e)
            return
        }
        
        if (isServiceConnected && hidDevice != null) {
            try {
                val connected = hidDevice!!.connect(device)
                Log.d(TAG, "Connection attempt result: $connected")
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException connecting to device", e)
            }
        } else {
            Log.e(TAG, "HID service not ready")
        }
    }
    
    private fun disconnect() {
        connectedDevice?.let { device ->
            // Check permission for Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.e(TAG, "BLUETOOTH_CONNECT permission not granted for disconnect")
                    connectedDevice = null
                    return
                }
            }
            
            try {
                hidDevice?.disconnect(device)
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException disconnecting from device", e)
            }
            connectedDevice = null
        }
    }
    
    private fun createNotificationChannel() {
        // minSdk is 28, so NotificationChannel is always available
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Bluetooth Keyboard Service",
            NotificationManager.IMPORTANCE_HIGH  // HIGH importance for lock screen
        )
        channel.description = "Bluetooth HID Keyboard service notification"
        channel.setShowBadge(false)
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        channel.enableLights(true)
        channel.enableVibration(false)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Create action buttons for media controls
        val leftIntent = Intent(this, BluetoothHidService::class.java).apply {
            action = "ACTION_LEFT"
        }
        val leftPendingIntent = PendingIntent.getService(
            this, 0, leftIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val rightIntent = Intent(this, BluetoothHidService::class.java).apply {
            action = "ACTION_RIGHT"
        }
        val rightPendingIntent = PendingIntent.getService(
            this, 1, rightIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val upIntent = Intent(this, BluetoothHidService::class.java).apply {
            action = "ACTION_UP"
        }
        val upPendingIntent = PendingIntent.getService(
            this, 2, upIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val downIntent = Intent(this, BluetoothHidService::class.java).apply {
            action = "ACTION_DOWN"
        }
        val downPendingIntent = PendingIntent.getService(
            this, 3, downIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val deviceName = if (connectedDevice != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    try {
                        connectedDevice?.name ?: "Bağlı"
                    } catch (e: SecurityException) {
                        "Bağlı"
                    }
                } else {
                    "Bağlı"
                }
            } else {
                try {
                    connectedDevice?.name ?: "Bağlı"
                } catch (e: SecurityException) {
                    "Bağlı"
                }
            }
        } else {
            "Bağlantı yok"
        }
        
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Bluetooth Klavye")
            .setContentText(deviceName)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)  // Media transport category
            // MediaStyle removed due to import issues - using standard notification style
            .addAction(android.R.drawable.ic_media_previous, "Sol", leftPendingIntent)
            .addAction(android.R.drawable.ic_menu_sort_by_size, "Yukarı", upPendingIntent)
            .addAction(android.R.drawable.ic_menu_sort_by_size, "Aşağı", downPendingIntent)
            .addAction(android.R.drawable.ic_media_next, "Sağ", rightPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)  // Show on lock screen
            .setPriority(NotificationCompat.PRIORITY_HIGH)  // HIGH priority for lock screen
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .build()
    }
}

enum class KeyCode {
    UP_ARROW,
    DOWN_ARROW,
    LEFT_ARROW,
    RIGHT_ARROW,
    SPACE,
    F,
    NONE
}


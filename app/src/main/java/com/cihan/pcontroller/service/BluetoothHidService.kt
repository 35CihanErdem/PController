package com.cihan.pcontroller.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.cihan.pcontroller.R
import com.cihan.pcontroller.bluetooth.ConnectionState
import com.cihan.pcontroller.bluetooth.HidDeviceManager
import com.cihan.pcontroller.bluetooth.KeyCode
import com.cihan.pcontroller.ui.MainActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Foreground service hosting [HidDeviceManager].
 * UI binds via [LocalBinder] and collects [connectionState] — no static companion state.
 */
class BluetoothHidService : LifecycleService() {

    companion object {
        private const val TAG = "BluetoothHidService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "pcontroller_hid_channel"

        const val ACTION_CONNECT = "com.cihan.pcontroller.CONNECT"
        const val ACTION_DISCONNECT = "com.cihan.pcontroller.DISCONNECT"
        const val ACTION_LEFT = "com.cihan.pcontroller.ACTION_LEFT"
        const val ACTION_RIGHT = "com.cihan.pcontroller.ACTION_RIGHT"
        const val ACTION_UP = "com.cihan.pcontroller.ACTION_UP"
        const val ACTION_DOWN = "com.cihan.pcontroller.ACTION_DOWN"
        const val EXTRA_DEVICE_ADDRESS = "device_address"
    }

    private lateinit var hidManager: HidDeviceManager
    private var stateCollectJob: Job? = null
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothHidService = this@BluetoothHidService
    }

    val connectionState: StateFlow<ConnectionState>
        get() = hidManager.connectionState

    fun connect(deviceAddress: String) = hidManager.connect(deviceAddress)

    fun disconnect() = hidManager.disconnect()

    fun sendKey(keyCode: KeyCode) = hidManager.sendKey(keyCode)

    fun sendVolumeUp() = hidManager.sendVolumeUp()

    fun sendVolumeDown() = hidManager.sendVolumeDown()

    fun sendPlayPause() = hidManager.sendPlayPause()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        hidManager = HidDeviceManager(applicationContext)
        startAsForeground()
        hidManager.start()
        stateCollectJob = lifecycleScope.launch {
            hidManager.connectionState.collect { state ->
                updateNotification(state)
            }
        }
        Log.d(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_CONNECT -> {
                intent.getStringExtra(EXTRA_DEVICE_ADDRESS)?.let { address ->
                    hidManager.connect(address)
                }
            }
            ACTION_DISCONNECT -> {
                hidManager.disconnect()
                stopSelf()
            }
            ACTION_LEFT -> hidManager.sendKey(KeyCode.LEFT_ARROW)
            ACTION_RIGHT -> hidManager.sendKey(KeyCode.RIGHT_ARROW)
            ACTION_UP -> hidManager.sendKey(KeyCode.UP_ARROW)
            ACTION_DOWN -> hidManager.sendKey(KeyCode.DOWN_ARROW)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onDestroy() {
        stateCollectJob?.cancel()
        hidManager.release()
        Log.d(TAG, "Service destroyed")
        super.onDestroy()
    }

    private fun startAsForeground() {
        val notification = buildNotification(ConnectionState.Starting)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(state: ConnectionState) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(state))
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "PController",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Bluetooth HID uzak kumanda"
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            enableVibration(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(state: ConnectionState): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val text = when (state) {
            is ConnectionState.Connected -> state.deviceName ?: state.deviceAddress
            is ConnectionState.Connecting -> "Bağlanıyor…"
            is ConnectionState.Starting, is ConnectionState.Registered -> "Hazır"
            is ConnectionState.Failed -> state.reason
            is ConnectionState.Idle -> "Bağlantı yok"
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (state is ConnectionState.Connected) {
            builder
                .addAction(android.R.drawable.ic_media_previous, "Sol", actionPending(ACTION_LEFT, 1))
                .addAction(android.R.drawable.ic_menu_sort_by_size, "Yukarı", actionPending(ACTION_UP, 2))
                .addAction(android.R.drawable.ic_menu_sort_by_size, "Aşağı", actionPending(ACTION_DOWN, 3))
                .addAction(android.R.drawable.ic_media_next, "Sağ", actionPending(ACTION_RIGHT, 4))
        }

        return builder.build()
    }

    private fun actionPending(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, BluetoothHidService::class.java).apply { this.action = action }
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}

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
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.media.app.NotificationCompat.MediaStyle
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
 * Only started when user connects — not on every app open.
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
        const val ACTION_PLAY_PAUSE = "com.cihan.pcontroller.ACTION_PLAY_PAUSE"
        const val ACTION_VOL_UP = "com.cihan.pcontroller.ACTION_VOL_UP"
        const val ACTION_VOL_DOWN = "com.cihan.pcontroller.ACTION_VOL_DOWN"
        const val EXTRA_DEVICE_ADDRESS = "device_address"
    }

    private var hidManager: HidDeviceManager? = null
    private var stateCollectJob: Job? = null
    private val binder = LocalBinder()
    private var foregroundStarted = false
    private var mediaSession: MediaSessionCompat? = null

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothHidService = this@BluetoothHidService
    }

    val connectionState: StateFlow<ConnectionState>
        get() = requireManager().connectionState

    fun connect(deviceAddress: String) = requireManager().connect(deviceAddress)

    fun disconnect() = hidManager?.disconnect()

    fun sendKey(keyCode: KeyCode) = hidManager?.sendKey(keyCode)

    fun sendVolumeUp() = hidManager?.sendVolumeUp()

    fun sendVolumeDown() = hidManager?.sendVolumeDown()

    fun sendPlayPause() = hidManager?.sendPlayPause()

    override fun onCreate() {
        super.onCreate()
        try {
            createNotificationChannel()
            mediaSession = MediaSessionCompat(this, "PController").apply {
                setCallback(object : MediaSessionCompat.Callback() {
                    override fun onPlay() {
                        hidManager?.sendPlayPause()
                    }

                    override fun onPause() {
                        hidManager?.sendPlayPause()
                    }

                    override fun onSkipToPrevious() {
                        hidManager?.sendKey(KeyCode.LEFT_ARROW)
                    }

                    override fun onSkipToNext() {
                        hidManager?.sendKey(KeyCode.RIGHT_ARROW)
                    }
                })
                isActive = true
            }
            if (!startAsForegroundSafe()) {
                Log.e(TAG, "Could not enter foreground — stopping service")
                stopSelf()
                return
            }
            val manager = HidDeviceManager(applicationContext)
            hidManager = manager
            manager.start()
            stateCollectJob = lifecycleScope.launch {
                manager.connectionState.collect { state ->
                    if (foregroundStarted) {
                        updateNotification(state)
                    }
                }
            }
            Log.d(TAG, "Service created")
        } catch (e: Exception) {
            Log.e(TAG, "onCreate failed", e)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (!foregroundStarted || hidManager == null) {
            if (!startAsForegroundSafe()) {
                stopSelf()
                return START_NOT_STICKY
            }
            if (hidManager == null) {
                hidManager = HidDeviceManager(applicationContext).also { it.start() }
            }
        }
        when (intent?.action) {
            ACTION_CONNECT -> {
                intent.getStringExtra(EXTRA_DEVICE_ADDRESS)?.let { address ->
                    hidManager?.connect(address)
                }
            }
            ACTION_DISCONNECT -> {
                hidManager?.disconnect()
                stopSelf()
            }
            ACTION_LEFT -> hidManager?.sendKey(KeyCode.LEFT_ARROW)
            ACTION_RIGHT -> hidManager?.sendKey(KeyCode.RIGHT_ARROW)
            ACTION_UP -> hidManager?.sendKey(KeyCode.UP_ARROW)
            ACTION_DOWN -> hidManager?.sendKey(KeyCode.DOWN_ARROW)
            ACTION_PLAY_PAUSE -> hidManager?.sendPlayPause()
            ACTION_VOL_UP -> hidManager?.sendVolumeUp()
            ACTION_VOL_DOWN -> hidManager?.sendVolumeDown()
        }
        // Avoid crash-restart loops if something goes wrong
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onDestroy() {
        stateCollectJob?.cancel()
        hidManager?.release()
        hidManager = null
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        Log.d(TAG, "Service destroyed")
        super.onDestroy()
    }

    private fun requireManager(): HidDeviceManager {
        return hidManager ?: HidDeviceManager(applicationContext).also {
            hidManager = it
            it.start()
        }
    }

    private fun startAsForegroundSafe(): Boolean {
        val notification = buildNotification(ConnectionState.Starting)
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            foregroundStarted = true
            true
        } catch (e: Exception) {
            Log.e(TAG, "startForeground(connectedDevice) failed, fallback", e)
            try {
                startForeground(NOTIFICATION_ID, notification)
                foregroundStarted = true
                true
            } catch (e2: Exception) {
                Log.e(TAG, "startForeground fallback failed", e2)
                foregroundStarted = false
                false
            }
        }
    }

    private fun updateNotification(state: ConnectionState) {
        try {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, buildNotification(state))
        } catch (e: Exception) {
            Log.e(TAG, "updateNotification", e)
        }
    }

    private fun createNotificationChannel() {
        // Eski kanalı sil — MediaStyle için DEFAULT importance
        val nm = getSystemService(NotificationManager::class.java)
        try {
            nm.deleteNotificationChannel(CHANNEL_ID)
        } catch (_: Exception) {
        }
        val channel = NotificationChannel(
            CHANNEL_ID,
            "PController Kumanda",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Spotify tarzı medya kumanda bildirimi"
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            enableVibration(false)
            setSound(null, null)
        }
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(state: ConnectionState): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(MainActivity.EXTRA_FROM_NOTIFICATION, true)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = getString(R.string.app_name)
        val text = when (state) {
            is ConnectionState.Connected -> state.deviceName ?: state.deviceAddress
            is ConnectionState.Connecting -> "Bağlanıyor…"
            is ConnectionState.Starting, is ConnectionState.Registered -> "Hazır"
            is ConnectionState.Failed -> state.reason
            is ConnectionState.Idle -> "Bağlantı yok"
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_stat_pcontroller)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (state is ConnectionState.Connected) {
            // Sıra: -5 | V- | Play | V+ | +5
            // Compact (Spotify çubuğu): V- · Play · V+
            builder
                .addAction(
                    android.R.drawable.ic_media_rew,
                    "-5",
                    actionPending(ACTION_LEFT, 1)
                )
                .addAction(
                    R.drawable.ic_notif_vol_down,
                    "V-",
                    actionPending(ACTION_VOL_DOWN, 2)
                )
                .addAction(
                    android.R.drawable.ic_media_play,
                    "Play",
                    actionPending(ACTION_PLAY_PAUSE, 3)
                )
                .addAction(
                    R.drawable.ic_notif_vol_up,
                    "V+",
                    actionPending(ACTION_VOL_UP, 4)
                )
                .addAction(
                    android.R.drawable.ic_media_ff,
                    "+5",
                    actionPending(ACTION_RIGHT, 5)
                )

            val style = MediaStyle()
                .setShowActionsInCompactView(1, 2, 3) // V- · Play · V+
                .setShowCancelButton(false)
            mediaSession?.sessionToken?.let { style.setMediaSession(it) }
            builder.setStyle(style)
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

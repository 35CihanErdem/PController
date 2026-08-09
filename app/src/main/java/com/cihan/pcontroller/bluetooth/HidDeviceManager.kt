package com.cihan.pcontroller.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.cihan.pcontroller.domain.HidCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Owns BluetoothHidDevice lifecycle: profile proxy → registerApp → connect → reports.
 */
class HidDeviceManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "HidDeviceManager"
        private const val KEY_RELEASE_MS = 120L
        private const val KEY_MODIFIER_STEP_MS = 30L
        private const val CONSUMER_RELEASE_MS = 120L
        private const val MOUSE_CLICK_MS = 50L
        private const val CONNECT_RETRY_DELAY_MS = 800L
        private const val MAX_CONNECT_ATTEMPTS = 3
        /** Ignore stale DISCONNECTED right after connect() — common OEM quirk. */
        private const val DISCONNECT_GRACE_MS = 1500L
        private const val CONNECT_TIMEOUT_MS = 12_000L
        /** registerApp / profile proxy sonsuza kadar "HID hazırlanıyor"da kalmasın */
        private const val STARTING_TIMEOUT_MS = 10_000L
    }

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var hidDevice: BluetoothHidDevice? = null
    private var connectedDevice: BluetoothDevice? = null
    private var pendingConnectAddress: String? = null
    private var targetAddress: String? = null
    private var isAppRegistered = false
    private var isReleased = false
    private var profileProxyRequested = false
    private var connectAttempts = 0
    private var connectStartedAtMs = 0L
    private var connectTimeoutRunnable: Runnable? = null
    private var startingTimeoutRunnable: Runnable? = null
    private var retryRunnable: Runnable? = null
    private var disconnectConfirmRunnable: Runnable? = null
    private var keyReleaseRunnable: Runnable? = null
    private var consumerReleaseRunnable: Runnable? = null
    private var mouseClickReleaseRunnable: Runnable? = null
    private var mouseButtonsHeld: Int = 0
    private var connectGeneration: Int = 0
    private var usedComboSubclass = false

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile != BluetoothProfile.HID_DEVICE || isReleased) return
            hidDevice = proxy as BluetoothHidDevice
            Log.d(TAG, "HID profile proxy ready")
            registerHidApp()
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            Log.d(TAG, "HID profile proxy lost")
            isAppRegistered = false
            profileProxyRequested = false
            hidDevice = null
            // Bağlıyken anında Failed gösterme — debounce
            if (_connectionState.value is ConnectionState.Connected) {
                scheduleDisconnectConfirm(connectedDevice?.let { safeAddress(it) })
                connectedDevice = null
                return
            }
            connectedDevice = null
            if (!isReleased) {
                _connectionState.value = ConnectionState.Failed("HID profili koptu")
            }
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            Log.d(TAG, "App status: registered=$registered plugged=${pluggedDevice?.address}")
            if (isReleased) return
            isAppRegistered = registered
            if (registered) {
                cancelStartingTimeout()
                val current = _connectionState.value
                if (current is ConnectionState.Connected) {
                    Log.d(TAG, "Already connected — ignore re-register")
                    return
                }
                // Connecting iken ikinci connectInternal ÇAĞIRMA
                if (current is ConnectionState.Connecting) {
                    Log.d(TAG, "Already connecting — keep waiting, no second connect()")
                    return
                }
                val toConnect = pendingConnectAddress ?: pluggedDevice?.address
                pendingConnectAddress = null
                if (toConnect != null) {
                    connectInternal(toConnect, isRetry = false)
                } else {
                    _connectionState.value = ConnectionState.Registered
                }
            } else {
                if (_connectionState.value is ConnectionState.Connected) {
                    Log.w(TAG, "unregister while connected — ignore")
                    return
                }
                connectedDevice = null
                if (_connectionState.value !is ConnectionState.Idle) {
                    _connectionState.value = ConnectionState.Failed("HID uygulama kaydı düştü")
                }
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            if (isReleased || device == null) return
            val addr = safeAddress(device)
            Log.d(TAG, "Connection state=$state device=$addr target=$targetAddress")
            when (state) {
                BluetoothProfile.STATE_CONNECTING -> {
                    if (targetAddress == null || targetAddress.equals(addr, ignoreCase = true)) {
                        targetAddress = addr
                        if (_connectionState.value !is ConnectionState.Connected) {
                            _connectionState.value = ConnectionState.Connecting
                        }
                    }
                }
                BluetoothProfile.STATE_CONNECTED -> {
                    cancelTimeouts()
                    cancelDisconnectConfirm()
                    connectAttempts = 0
                    connectedDevice = device
                    targetAddress = addr
                    _connectionState.value = ConnectionState.Connected(
                        deviceAddress = addr,
                        deviceName = safeName(device)
                    )
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    handleDisconnected(device, addr)
                }
            }
        }

        override fun onVirtualCableUnplug(device: BluetoothDevice?) {
            Log.d(TAG, "Virtual cable unplug")
            if (isReleased) return
            scheduleDisconnectConfirm(device?.let { safeAddress(it) })
        }
    }

    fun start() {
        if (isReleased) return

        // Proxy already held — just ensure app is registered / queued connect runs
        if (hidDevice != null) {
            if (!isAppRegistered) {
                _connectionState.value = ConnectionState.Starting
                scheduleStartingTimeout()
                registerHidApp()
            } else if (pendingConnectAddress != null) {
                val address = pendingConnectAddress
                pendingConnectAddress = null
                if (address != null) connectInternal(address, isRetry = false)
            }
            return
        }

        if (profileProxyRequested &&
            _connectionState.value is ConnectionState.Starting
        ) {
            return
        }

        _connectionState.value = ConnectionState.Starting
        scheduleStartingTimeout()
        val adapter = bluetoothAdapter()
        if (adapter == null) {
            cancelStartingTimeout()
            _connectionState.value = ConnectionState.Failed("Bluetooth desteklenmiyor")
            return
        }
        if (!adapter.isEnabled) {
            cancelStartingTimeout()
            _connectionState.value = ConnectionState.Failed("Bluetooth kapalı")
            return
        }
        if (!hasConnectPermission()) {
            cancelStartingTimeout()
            _connectionState.value = ConnectionState.Failed("Bluetooth izni yok")
            return
        }
        profileProxyRequested = true
        val ok = adapter.getProfileProxy(context, profileListener, BluetoothProfile.HID_DEVICE)
        Log.d(TAG, "getProfileProxy(HID_DEVICE)=$ok")
        if (!ok) {
            profileProxyRequested = false
            cancelStartingTimeout()
            _connectionState.value = ConnectionState.Failed(
                "HID Device profili yok (telefon desteklemiyor olabilir)"
            )
        }
    }

    fun connect(deviceAddress: String) {
        if (isReleased) return
        if (!hasConnectPermission()) {
            _connectionState.value = ConnectionState.Failed("Bluetooth izni yok")
            return
        }

        val current = _connectionState.value
        if (current is ConnectionState.Connected &&
            current.deviceAddress.equals(deviceAddress, ignoreCase = true)
        ) {
            Log.d(TAG, "Already connected to $deviceAddress — skip")
            return
        }
        if (current is ConnectionState.Connecting &&
            targetAddress.equals(deviceAddress, ignoreCase = true)
        ) {
            Log.d(TAG, "Already connecting to $deviceAddress — skip")
            return
        }

        cancelTimeouts()
        cancelDisconnectConfirm()
        connectAttempts = 0
        connectGeneration++
        pendingConnectAddress = deviceAddress
        targetAddress = deviceAddress
        ensureConnectable()

        if (isAppRegistered && hidDevice != null) {
            pendingConnectAddress = null
            connectInternal(deviceAddress, isRetry = false)
        } else {
            start()
        }
    }

    fun disconnect() {
        cancelTimeouts()
        cancelDisconnectConfirm()
        mouseClickReleaseRunnable?.let { mainHandler.removeCallbacks(it) }
        mouseClickReleaseRunnable = null
        mouseButtonsHeld = 0
        connectGeneration++
        pendingConnectAddress = null
        targetAddress = null
        connectAttempts = 0
        val device = connectedDevice
        val hid = hidDevice
        if (device != null && hid != null && hasConnectPermission()) {
            try {
                hid.disconnect(device)
            } catch (e: SecurityException) {
                Log.e(TAG, "disconnect SecurityException", e)
            }
        }
        connectedDevice = null
        _connectionState.value =
            if (isAppRegistered) ConnectionState.Registered else ConnectionState.Idle
    }

    fun sendKey(keyCode: KeyCode, modifiers: Int = 0) {
        if (keyCode == KeyCode.NONE) return
        sendKeyboardPulse(keyCode, modifiers)
    }

    fun sendVolumeUp() = sendConsumerPulse(ConsumerAction.VOLUME_UP)

    fun sendVolumeDown() = sendConsumerPulse(ConsumerAction.VOLUME_DOWN)

    fun sendPlayPause() = sendConsumerPulse(ConsumerAction.PLAY_PAUSE)

    fun sendConsumer(action: ConsumerAction) {
        if (action == ConsumerAction.NONE) return
        sendConsumerPulse(action)
    }

    fun sendCommand(command: HidCommand) {
        when (command) {
            is HidCommand.Key -> sendKey(command.key, command.modifiers)
            is HidCommand.Consumer -> sendConsumer(command.action)
        }
    }

    fun sendMouseMove(dx: Int, dy: Int): Boolean {
        if (dx == 0 && dy == 0) return true
        return sendMouseRaw(buttons = mouseButtonsHeld, dx = dx, dy = dy, wheel = 0)
    }

    fun sendMouseScroll(wheel: Int): Boolean {
        if (wheel == 0) return true
        var remain = wheel
        var ok = true
        while (remain != 0) {
            val step = remain.coerceIn(-127, 127)
            if (!sendMouseRaw(buttons = mouseButtonsHeld, dx = 0, dy = 0, wheel = step)) {
                ok = false
            }
            remain -= step
        }
        return ok
    }

    fun sendMouseButtonDown(buttonMask: Int): Boolean {
        mouseButtonsHeld = mouseButtonsHeld or (buttonMask and 0x07)
        return sendMouseRaw(buttons = mouseButtonsHeld, dx = 0, dy = 0, wheel = 0)
    }

    fun sendMouseButtonUp(buttonMask: Int): Boolean {
        mouseButtonsHeld = mouseButtonsHeld and (buttonMask and 0x07).inv()
        return sendMouseRaw(buttons = mouseButtonsHeld, dx = 0, dy = 0, wheel = 0)
    }

    /** Sol/sağ tık pulse. */
    fun sendMouseClick(buttonMask: Int): Boolean {
        val mask = buttonMask and 0x07
        if (mask == 0) return false
        mouseClickReleaseRunnable?.let { mainHandler.removeCallbacks(it) }
        val downOk = sendMouseButtonDown(mask)
        val release = Runnable {
            sendMouseButtonUp(mask)
            mouseClickReleaseRunnable = null
        }
        mouseClickReleaseRunnable = release
        mainHandler.postDelayed(release, MOUSE_CLICK_MS)
        return downOk
    }

    /** Bağlantı + mouse report smoke test. */
    fun probeMouse(): Boolean {
        if (connectedDevice == null || hidDevice == null) return false
        return sendMouseRaw(0, 1, 0, 0) && sendMouseRaw(0, -1, 0, 0)
    }

    private fun sendMouseRaw(buttons: Int, dx: Int, dy: Int, wheel: Int): Boolean {
        val hid = hidDevice
        val target = connectedDevice
        if (hid == null || target == null || !hasConnectPermission()) {
            Log.w(TAG, "sendMouse skipped: hid=${hid != null} target=${target != null}")
            return false
        }
        return try {
            val ok = hid.sendReport(
                target,
                HidReports.REPORT_ID_MOUSE,
                HidReports.mouseReport(buttons, dx, dy, wheel)
            )
            if (!ok) {
                Log.e(TAG, "sendMouse sendReport=false — Windows eski HID cache / yeniden eşleştir")
            }
            ok
        } catch (e: Exception) {
            Log.e(TAG, "sendMouse", e)
            false
        }
    }

    fun release() {
        isReleased = true
        cancelTimeouts()
        cancelDisconnectConfirm()
        pendingConnectAddress = null
        targetAddress = null
        try {
            connectedDevice?.let { device ->
                if (hasConnectPermission()) {
                    hidDevice?.disconnect(device)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "release disconnect", e)
        }
        try {
            if (hasConnectPermission()) {
                hidDevice?.unregisterApp()
            }
        } catch (e: Exception) {
            Log.e(TAG, "unregisterApp", e)
        }
        hidDevice?.let { device ->
            bluetoothAdapter()?.closeProfileProxy(BluetoothProfile.HID_DEVICE, device)
        }
        hidDevice = null
        connectedDevice = null
        isAppRegistered = false
        profileProxyRequested = false
        _connectionState.value = ConnectionState.Idle
    }

    private fun registerHidApp(preferCombo: Boolean = true) {
        val device = hidDevice ?: return
        if (!hasConnectPermission()) {
            cancelStartingTimeout()
            _connectionState.value = ConnectionState.Failed("Bluetooth izni yok")
            return
        }
        // Windows mouse için COMBO şart. KEYBOARD subclass Windows'ta mouse report'u yutuyor.
        val subclass = if (preferCombo) {
            usedComboSubclass = true
            BluetoothHidDevice.SUBCLASS1_COMBO
        } else {
            usedComboSubclass = false
            BluetoothHidDevice.SUBCLASS1_KEYBOARD
        }
        val sdp = BluetoothHidDeviceAppSdpSettings(
            "PController",
            "Keyboard, media and mouse remote",
            "PController",
            subclass,
            HidReports.descriptor()
        )
        // Interrupt kanalı — mouse için BEST_EFFORT QoS
        val outQos = try {
            android.bluetooth.BluetoothHidDeviceAppQosSettings(
                android.bluetooth.BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
                800,
                9,
                0,
                11250,
                android.bluetooth.BluetoothHidDeviceAppQosSettings.MAX
            )
        } catch (_: Exception) {
            null
        }
        scheduleStartingTimeout()
        val registered = try {
            device.registerApp(
                sdp,
                null,
                outQos,
                context.mainExecutor,
                hidCallback
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "registerApp SecurityException", e)
            cancelStartingTimeout()
            _connectionState.value = ConnectionState.Failed("HID kayıt izni yok")
            return
        }
        Log.d(TAG, "registerApp requested=$registered subclassCombo=$preferCombo")
        if (!registered) {
            try {
                device.unregisterApp()
            } catch (_: Exception) {
            }
            mainHandler.postDelayed({
                if (isReleased || hidDevice == null) return@postDelayed
                if (preferCombo) {
                    Log.d(TAG, "COMBO register false — KEYBOARD fallback (mouse zayıf olabilir)")
                    registerHidApp(preferCombo = false)
                    return@postDelayed
                }
                cancelStartingTimeout()
                _connectionState.value = ConnectionState.Failed(
                    "HID kaydı başarısız (başka uygulama kullanıyor olabilir)"
                )
            }, 400)
        }
    }

    private fun scheduleStartingTimeout() {
        cancelStartingTimeout()
        val runnable = Runnable {
            if (isReleased) return@Runnable
            if (_connectionState.value !is ConnectionState.Starting) return@Runnable
            Log.e(TAG, "Starting timeout — HID hazırlanıyor takıldı")
            if (usedComboSubclass && hidDevice != null) {
                Log.d(TAG, "COMBO timeout — KEYBOARD fallback")
                try {
                    hidDevice?.unregisterApp()
                } catch (_: Exception) {
                }
                isAppRegistered = false
                _connectionState.value = ConnectionState.Starting
                registerHidApp(preferCombo = false)
                return@Runnable
            }
            pendingConnectAddress = null
            _connectionState.value = ConnectionState.Failed(
                "HID hazırlanamadı. Tekrar Dene; olmazsa uygulamayı kapatıp aç."
            )
        }
        startingTimeoutRunnable = runnable
        mainHandler.postDelayed(runnable, STARTING_TIMEOUT_MS)
    }

    private fun cancelStartingTimeout() {
        startingTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        startingTimeoutRunnable = null
    }

    @SuppressLint("MissingPermission")
    private fun connectInternal(deviceAddress: String, isRetry: Boolean) {
        val hid = hidDevice
        if (hid == null || !isAppRegistered) {
            pendingConnectAddress = deviceAddress
            start()
            return
        }

        val current = _connectionState.value
        if (current is ConnectionState.Connected &&
            current.deviceAddress.equals(deviceAddress, ignoreCase = true)
        ) {
            Log.d(TAG, "connectInternal skip — already connected")
            return
        }

        if (!hasConnectPermission()) {
            _connectionState.value = ConnectionState.Failed("Bluetooth izni yok")
            return
        }
        val adapter = bluetoothAdapter() ?: run {
            _connectionState.value = ConnectionState.Failed("Bluetooth desteklenmiyor")
            return
        }
        val remote = try {
            adapter.getRemoteDevice(deviceAddress)
        } catch (e: IllegalArgumentException) {
            _connectionState.value = ConnectionState.Failed("Geçersiz cihaz adresi")
            return
        } catch (e: SecurityException) {
            _connectionState.value = ConnectionState.Failed("Bluetooth izni yok")
            return
        }

        if (remote.bondState != BluetoothDevice.BOND_BONDED) {
            _connectionState.value = ConnectionState.Failed(
                "Önce sistem Bluetooth ayarlarından PC ile eşleştir"
            )
            return
        }

        targetAddress = deviceAddress
        if (!isRetry) {
            connectAttempts = 1
        }
        connectStartedAtMs = System.currentTimeMillis()
        ensureConnectable()
        if (_connectionState.value !is ConnectionState.Connected) {
            _connectionState.value = ConnectionState.Connecting
        }
        cancelStartingTimeout()
        scheduleConnectTimeout(deviceAddress)

        val ok = try {
            hid.connect(remote)
        } catch (e: SecurityException) {
            Log.e(TAG, "connect SecurityException", e)
            _connectionState.value = ConnectionState.Failed("Bağlantı izni yok")
            return
        }
        Log.d(TAG, "connect($deviceAddress) attempt=$connectAttempts => $ok")
        // false çoğu OEM'de "zaten bağlanıyor" demek — hemen fail etme, timeout beklesin
        if (!ok) {
            Log.w(TAG, "connect()=false — callback/timeout bekleniyor (çift bağlama yok)")
        }
    }

    private fun handleDisconnected(device: BluetoothDevice, addr: String) {
        val current = _connectionState.value
        val isTarget = targetAddress != null &&
            targetAddress.equals(addr, ignoreCase = true)

        when (current) {
            is ConnectionState.Connecting -> {
                if (!isTarget) {
                    Log.d(TAG, "Ignoring DISCONNECTED for non-target $addr")
                    return
                }
                val elapsed = System.currentTimeMillis() - connectStartedAtMs
                if (elapsed < DISCONNECT_GRACE_MS) {
                    Log.d(TAG, "Ignoring early DISCONNECTED (${elapsed}ms grace)")
                    return
                }
                scheduleRetryOrFail(addr, "PC kabul etmedi / HID host kapalı")
            }
            is ConnectionState.Connected -> {
                if (connectedDevice?.address.equals(addr, ignoreCase = true) == true || isTarget) {
                    // Bağlandıktan hemen sonra gelen sahte DISCONNECTED'ı yut
                    scheduleDisconnectConfirm(addr)
                }
            }
            else -> {
                if (connectedDevice?.address.equals(addr, ignoreCase = true) == true) {
                    connectedDevice = null
                }
            }
        }
    }

    private fun scheduleDisconnectConfirm(addr: String?) {
        cancelDisconnectConfirm()
        val generation = connectGeneration
        val runnable = Runnable {
            if (isReleased) return@Runnable
            if (generation != connectGeneration) return@Runnable
            if (_connectionState.value !is ConnectionState.Connected) return@Runnable
            // Hâlâ Connected görünüyor ama disconnect confirm geldi — gerçekten düş
            Log.d(TAG, "Confirming disconnect for $addr")
            connectedDevice = null
            targetAddress = null
            cancelTimeouts()
            _connectionState.value =
                if (isAppRegistered) ConnectionState.Registered else ConnectionState.Idle
        }
        disconnectConfirmRunnable = runnable
        mainHandler.postDelayed(runnable, 1800L)
    }

    private fun cancelDisconnectConfirm() {
        disconnectConfirmRunnable?.let { mainHandler.removeCallbacks(it) }
        disconnectConfirmRunnable = null
    }

    private fun scheduleRetryOrFail(address: String, reason: String) {
        // Bağlıyken retry/fail yapma
        if (_connectionState.value is ConnectionState.Connected) {
            Log.d(TAG, "Skip retry/fail — already connected")
            return
        }
        if (connectAttempts < MAX_CONNECT_ATTEMPTS) {
            connectAttempts++
            Log.d(TAG, "Retry connect in ${CONNECT_RETRY_DELAY_MS}ms (attempt $connectAttempts)")
            _connectionState.value = ConnectionState.Connecting
            retryRunnable?.let { mainHandler.removeCallbacks(it) }
            val generation = connectGeneration
            val runnable = Runnable {
                if (isReleased) return@Runnable
                if (generation != connectGeneration) return@Runnable
                if (_connectionState.value is ConnectionState.Connected) return@Runnable
                connectInternal(address, isRetry = true)
            }
            retryRunnable = runnable
            mainHandler.postDelayed(runnable, CONNECT_RETRY_DELAY_MS)
        } else {
            cancelTimeouts()
            targetAddress = null
            _connectionState.value = ConnectionState.Failed(
                "$reason. Windows’ta cihazı kaldırıp yeniden eşleştir, sonra Tekrar Dene."
            )
        }
    }

    private fun scheduleConnectTimeout(address: String) {
        connectTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        val generation = connectGeneration
        val runnable = Runnable {
            if (isReleased) return@Runnable
            if (generation != connectGeneration) return@Runnable
            if (_connectionState.value is ConnectionState.Connecting &&
                targetAddress.equals(address, ignoreCase = true)
            ) {
                Log.d(TAG, "Connect timeout")
                scheduleRetryOrFail(address, "Zaman aşımı")
            }
        }
        connectTimeoutRunnable = runnable
        mainHandler.postDelayed(runnable, CONNECT_TIMEOUT_MS)
    }

    private fun cancelTimeouts() {
        connectTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        connectTimeoutRunnable = null
        retryRunnable?.let { mainHandler.removeCallbacks(it) }
        retryRunnable = null
        cancelStartingTimeout()
    }

    @SuppressLint("MissingPermission")
    private fun ensureConnectable() {
        try {
            val adapter = bluetoothAdapter() ?: return
            if (!hasConnectPermission()) return
            // setScanMode removed from public SDK on newer platforms; call via reflection if present
            val method = BluetoothAdapter::class.java.methods.firstOrNull {
                it.name == "setScanMode" && it.parameterTypes.size == 2
            } ?: return
            method.invoke(
                adapter,
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE,
                60
            )
        } catch (e: Exception) {
            Log.w(TAG, "ensureConnectable failed", e)
        }
    }

    private fun sendKeyboardPulse(keyCode: KeyCode, modifiers: Int = 0) {
        val hid = hidDevice
        val target = connectedDevice
        if (hid == null || target == null || !hasConnectPermission()) return

        // Önceki release'i iptal et — hızlı basışta tuş yarışını keser
        keyReleaseRunnable?.let { mainHandler.removeCallbacks(it) }
        keyReleaseRunnable = null

        try {
            // Modifier varsa önce Shift basılı (Windows/YouTube daha güvenilir)
            if (modifiers != 0) {
                hid.sendReport(
                    target,
                    HidReports.REPORT_ID_KEYBOARD,
                    HidReports.keyboardReport(KeyCode.NONE, modifiers)
                )
                mainHandler.postDelayed({
                    if (connectedDevice != target || !hasConnectPermission()) return@postDelayed
                    try {
                        hid.sendReport(
                            target,
                            HidReports.REPORT_ID_KEYBOARD,
                            HidReports.keyboardReport(keyCode, modifiers)
                        )
                        scheduleKeyRelease(hid, target)
                    } catch (e: Exception) {
                        Log.e(TAG, "sendKey mod+key", e)
                    }
                }, KEY_MODIFIER_STEP_MS)
            } else {
                hid.sendReport(
                    target,
                    HidReports.REPORT_ID_KEYBOARD,
                    HidReports.keyboardReport(keyCode, 0)
                )
                scheduleKeyRelease(hid, target)
            }
        } catch (e: Exception) {
            Log.e(TAG, "sendKey", e)
        }
    }

    private fun scheduleKeyRelease(hid: BluetoothHidDevice, target: BluetoothDevice) {
        val release = Runnable {
            try {
                if (connectedDevice == target && hasConnectPermission()) {
                    hid.sendReport(
                        target,
                        HidReports.REPORT_ID_KEYBOARD,
                        HidReports.keyboardReport(KeyCode.NONE)
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "key release", e)
            } finally {
                keyReleaseRunnable = null
            }
        }
        keyReleaseRunnable = release
        mainHandler.postDelayed(release, KEY_RELEASE_MS)
    }

    private fun sendConsumerPulse(action: ConsumerAction) {
        val hid = hidDevice
        val target = connectedDevice
        if (hid == null || target == null || !hasConnectPermission()) return
        consumerReleaseRunnable?.let { mainHandler.removeCallbacks(it) }
        consumerReleaseRunnable = null
        try {
            hid.sendReport(
                target,
                HidReports.REPORT_ID_CONSUMER,
                HidReports.consumerReport(action)
            )
            val release = Runnable {
                try {
                    if (connectedDevice == target && hasConnectPermission()) {
                        hid.sendReport(
                            target,
                            HidReports.REPORT_ID_CONSUMER,
                            HidReports.consumerReport(ConsumerAction.NONE)
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "consumer release", e)
                } finally {
                    consumerReleaseRunnable = null
                }
            }
            consumerReleaseRunnable = release
            mainHandler.postDelayed(release, CONSUMER_RELEASE_MS)
        } catch (e: Exception) {
            Log.e(TAG, "sendConsumer", e)
        }
    }

    private fun bluetoothAdapter() =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun safeAddress(device: BluetoothDevice): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasConnectPermission()) {
                "unknown"
            } else {
                device.address
            }
        } catch (e: SecurityException) {
            "unknown"
        }
    }

    private fun safeName(device: BluetoothDevice): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasConnectPermission()) {
                null
            } else {
                device.name
            }
        } catch (e: SecurityException) {
            null
        }
    }
}

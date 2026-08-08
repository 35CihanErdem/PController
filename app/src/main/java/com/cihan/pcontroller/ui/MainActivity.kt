package com.cihan.pcontroller.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cihan.pcontroller.R
import com.cihan.pcontroller.bluetooth.BondedDevice
import com.cihan.pcontroller.bluetooth.ConnectionState
import com.cihan.pcontroller.bluetooth.KeyCode
import com.cihan.pcontroller.service.BluetoothHidService
import com.cihan.pcontroller.util.PermissionHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FROM_NOTIFICATION = "from_notification"
    }

    private val viewModel: RemoteViewModel by viewModels()

    private lateinit var statusDot: View
    private lateinit var statusText: TextView
    private lateinit var setupContainer: LinearLayout
    private lateinit var setupMessage: TextView
    private lateinit var enableBluetoothButton: Button
    private lateinit var requestPermissionButton: Button
    private lateinit var openAppSettingsButton: Button

    private lateinit var deviceSelectionContainer: LinearLayout
    private lateinit var refreshDevicesButton: Button
    private lateinit var openBluetoothSettingsButton: Button
    private lateinit var emptyDevicesText: TextView
    private lateinit var deviceListRecyclerView: RecyclerView

    private lateinit var connectionContainer: LinearLayout
    private lateinit var connectionProgressBar: ProgressBar
    private lateinit var retryButton: Button
    private lateinit var repairHintButton: Button
    private lateinit var cancelConnectButton: Button

    private lateinit var inputContainer: ScrollView
    private lateinit var upButton: Button
    private lateinit var downButton: Button
    private lateinit var leftButton: Button
    private lateinit var rightButton: Button
    private lateinit var spaceButton: Button
    private lateinit var escButton: Button
    private lateinit var fullscreenButton: Button
    private lateinit var playPauseButton: Button
    private lateinit var volumeUpButton: Button
    private lateinit var volumeDownButton: Button
    private lateinit var disconnectButton: Button

    private var hidService: BluetoothHidService? = null
    private var bound = false
    private var stateJob: Job? = null
    private var connectRequestedAddress: String? = null
    /** True between user tap-connect and Connected/Failed/cancel. */
    private var awaitingConnection = false
    /** Servis state'i gelene kadar cihaz listesine düşme (bildirimden dönüş). */
    private var syncingServiceState = false

    private val deviceAdapter = BondedDeviceAdapter { device ->
        viewModel.selectDevice(device)
        connectTo(device.address)
    }

    private val enableBtLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { viewModel.refreshSetup() }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.any { !it.value }) {
            Toast.makeText(this, R.string.bluetooth_permission_required, Toast.LENGTH_LONG).show()
        }
        viewModel.refreshSetup()
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothHidService.LocalBinder
            hidService = binder.getService()
            bound = true
            val liveState = hidService!!.connectionState.value
            viewModel.onConnectionState(liveState)
            syncingServiceState = false
            observeServiceState()
            val pending = connectRequestedAddress
            if (pending != null) {
                connectRequestedAddress = null
                val already =
                    liveState is ConnectionState.Connected ||
                        liveState is ConnectionState.Connecting
                if (!already) {
                    hidService?.connect(pending)
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bound = false
            hidService = null
            stateJob?.cancel()
            stateJob = null
            syncingServiceState = false
        }

        override fun onNullBinding(name: ComponentName?) {
            syncingServiceState = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)
            bindViews()
            setupListeners()
            deviceListRecyclerView.layoutManager = LinearLayoutManager(this)
            deviceListRecyclerView.adapter = deviceAdapter
            handleOpenIntent(intent)

            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.uiState.collectLatest { render(it) }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Açılış hatası: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOpenIntent(intent)
        tryBindExistingService()
    }

    override fun onStart() {
        super.onStart()
        viewModel.refreshSetup()
        // Bildirimden / geri dönüşte çalışan servise bağlan → Connected ekranı gelsin
        tryBindExistingService()
        val conn = viewModel.uiState.value.connection
        if (conn is ConnectionState.Connected || awaitingConnection) {
            ensureServiceBound()
        }
    }

    private fun handleOpenIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_FROM_NOTIFICATION, false) == true) {
            syncingServiceState = true
        }
    }

    override fun onStop() {
        super.onStop()
        stateJob?.cancel()
        stateJob = null
        if (bound) {
            try {
                unbindService(serviceConnection)
            } catch (_: Exception) {
            }
            bound = false
            hidService = null
        }
    }

    private fun bindViews() {
        statusDot = findViewById(R.id.statusDot)
        statusText = findViewById(R.id.statusText)
        setupContainer = findViewById(R.id.setupContainer)
        setupMessage = findViewById(R.id.setupMessage)
        enableBluetoothButton = findViewById(R.id.enableBluetoothButton)
        requestPermissionButton = findViewById(R.id.requestPermissionButton)
        openAppSettingsButton = findViewById(R.id.openAppSettingsButton)

        deviceSelectionContainer = findViewById(R.id.deviceSelectionContainer)
        refreshDevicesButton = findViewById(R.id.refreshDevicesButton)
        openBluetoothSettingsButton = findViewById(R.id.openBluetoothSettingsButton)
        emptyDevicesText = findViewById(R.id.emptyDevicesText)
        deviceListRecyclerView = findViewById(R.id.deviceListRecyclerView)

        connectionContainer = findViewById(R.id.connectionContainer)
        connectionProgressBar = findViewById(R.id.connectionProgressBar)
        retryButton = findViewById(R.id.retryButton)
        repairHintButton = findViewById(R.id.repairHintButton)
        cancelConnectButton = findViewById(R.id.cancelConnectButton)

        inputContainer = findViewById(R.id.inputContainer)
        upButton = findViewById(R.id.upButton)
        downButton = findViewById(R.id.downButton)
        leftButton = findViewById(R.id.leftButton)
        rightButton = findViewById(R.id.rightButton)
        spaceButton = findViewById(R.id.spaceButton)
        escButton = findViewById(R.id.escButton)
        fullscreenButton = findViewById(R.id.fullscreenButton)
        playPauseButton = findViewById(R.id.playPauseButton)
        volumeUpButton = findViewById(R.id.volumeUpButton)
        volumeDownButton = findViewById(R.id.volumeDownButton)
        disconnectButton = findViewById(R.id.disconnectButton)
    }

    private fun setupListeners() {
        enableBluetoothButton.setOnClickListener {
            enableBtLauncher.launch(PermissionHelper.createEnableBluetoothIntent())
        }
        requestPermissionButton.setOnClickListener {
            permissionLauncher.launch(PermissionHelper.requiredRuntimePermissions())
        }
        openAppSettingsButton.setOnClickListener { PermissionHelper.openAppSettings(this) }
        refreshDevicesButton.setOnClickListener { viewModel.refreshSetup() }
        openBluetoothSettingsButton.setOnClickListener {
            PermissionHelper.openBluetoothSettings(this)
        }
        retryButton.setOnClickListener {
            viewModel.uiState.value.selectedAddress?.let { connectTo(it) }
        }
        repairHintButton.setOnClickListener { showRepairDialog() }
        cancelConnectButton.setOnClickListener {
            awaitingConnection = false
            hidService?.disconnect()
            viewModel.clearSelection()
            viewModel.refreshSetup()
        }
        disconnectButton.setOnClickListener {
            awaitingConnection = false
            hidService?.disconnect()
            viewModel.clearSelection()
            viewModel.refreshSetup()
        }

        upButton.setOnClickListener { hidService?.sendKey(KeyCode.UP_ARROW) }
        downButton.setOnClickListener { hidService?.sendKey(KeyCode.DOWN_ARROW) }
        leftButton.setOnClickListener { hidService?.sendKey(KeyCode.LEFT_ARROW) }
        rightButton.setOnClickListener { hidService?.sendKey(KeyCode.RIGHT_ARROW) }
        spaceButton.setOnClickListener { hidService?.sendKey(KeyCode.SPACE) }
        escButton.setOnClickListener { hidService?.sendKey(KeyCode.ESC) }
        fullscreenButton.setOnClickListener { hidService?.sendKey(KeyCode.F) }
        playPauseButton.setOnClickListener { hidService?.sendPlayPause() }
        volumeUpButton.setOnClickListener { hidService?.sendVolumeUp() }
        volumeDownButton.setOnClickListener { hidService?.sendVolumeDown() }
    }

    private fun ensureServiceBound() {
        if (!PermissionHelper.hasBluetoothPermissions(this)) return
        if (!PermissionHelper.isBluetoothEnabled(this)) return
        if (bound) return
        try {
            val intent = Intent(this, BluetoothHidService::class.java)
            startForegroundService(intent)
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            Toast.makeText(this, "Servis başlatılamadı: ${e.message}", Toast.LENGTH_LONG).show()
            awaitingConnection = false
            syncingServiceState = false
        }
    }

    /** Servis zaten çalışıyorsa (bildirim) AUTO_CREATE olmadan bağlan — Connected state gelsin. */
    private fun tryBindExistingService() {
        if (bound) return
        if (!PermissionHelper.hasBluetoothPermissions(this)) {
            syncingServiceState = false
            return
        }
        if (!PermissionHelper.isBluetoothEnabled(this)) {
            syncingServiceState = false
            return
        }
        try {
            val intent = Intent(this, BluetoothHidService::class.java)
            val started = bindService(intent, serviceConnection, 0)
            if (!started) {
                syncingServiceState = false
            } else {
                // onServiceConnected async; kısa süre cihaz listesine düşme
                syncingServiceState = true
                window.decorView.postDelayed({
                    if (syncingServiceState && !bound) {
                        syncingServiceState = false
                        render(viewModel.uiState.value)
                    }
                }, 800)
            }
        } catch (_: Exception) {
            syncingServiceState = false
        }
    }

    private fun observeServiceState() {
        stateJob?.cancel()
        val service = hidService ?: return
        stateJob = lifecycleScope.launch {
            service.connectionState.collectLatest { state ->
                if (state is ConnectionState.Connected || state is ConnectionState.Failed) {
                    awaitingConnection = false
                }
                viewModel.onConnectionState(state)
            }
        }
    }

    /** Tek connect yolu: binder hazır olunca bir kez connect. Intent ile ikinci connect yok. */
    private fun connectTo(address: String) {
        awaitingConnection = true
        connectRequestedAddress = address
        try {
            if (bound && hidService != null) {
                val addr = connectRequestedAddress
                connectRequestedAddress = null
                if (addr != null) hidService?.connect(addr)
            } else {
                ensureServiceBound()
            }
        } catch (e: Exception) {
            awaitingConnection = false
            connectRequestedAddress = null
            Toast.makeText(this, "Bağlantı başlatılamadı: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun render(state: RemoteUiState) {
        when (state.setup) {
            SetupPhase.Checking -> {
                hideAllPanels()
                setStatus(R.string.status_ready, connected = false)
            }
            SetupPhase.NeedBluetoothEnable -> {
                hideAllPanels()
                setupContainer.visibility = View.VISIBLE
                setupMessage.setText(R.string.bt_off_message)
                setStatus("Bluetooth kapalı", connected = false)
                enableBluetoothButton.visibility = View.VISIBLE
                requestPermissionButton.visibility = View.GONE
                openAppSettingsButton.visibility = View.GONE
            }
            SetupPhase.NeedPermission -> {
                hideAllPanels()
                setupContainer.visibility = View.VISIBLE
                setupMessage.setText(R.string.bt_permission_message)
                setStatus(R.string.bluetooth_permission_required, connected = false)
                enableBluetoothButton.visibility = View.GONE
                requestPermissionButton.visibility = View.VISIBLE
                openAppSettingsButton.visibility = View.VISIBLE
            }
            SetupPhase.Ready -> renderReady(state)
        }
    }

    private fun renderReady(state: RemoteUiState) {
        when (val connection = state.connection) {
            is ConnectionState.Connected -> {
                awaitingConnection = false
                hideAllPanels()
                inputContainer.visibility = View.VISIBLE
                setStatus(
                    getString(
                        R.string.status_connected,
                        connection.deviceName ?: connection.deviceAddress
                    ),
                    connected = true
                )
            }
            is ConnectionState.Connecting, is ConnectionState.Starting -> {
                hideAllPanels()
                connectionContainer.visibility = View.VISIBLE
                connectionProgressBar.visibility = View.VISIBLE
                retryButton.visibility = View.GONE
                repairHintButton.visibility = View.GONE
                setStatus(
                    getString(
                        if (connection is ConnectionState.Connecting) R.string.status_connecting
                        else R.string.status_preparing
                    ),
                    connected = false
                )
            }
            is ConnectionState.Failed -> {
                awaitingConnection = false
                hideAllPanels()
                connectionContainer.visibility = View.VISIBLE
                connectionProgressBar.visibility = View.GONE
                retryButton.visibility = View.VISIBLE
                repairHintButton.visibility =
                    if (state.showRepairHint) View.VISIBLE else View.GONE
                setStatus(getString(R.string.status_error, connection.reason), connected = false)
            }
            is ConnectionState.Registered, is ConnectionState.Idle -> {
                if (awaitingConnection && state.selectedAddress != null) {
                    hideAllPanels()
                    connectionContainer.visibility = View.VISIBLE
                    connectionProgressBar.visibility = View.VISIBLE
                    retryButton.visibility = View.GONE
                    repairHintButton.visibility = View.GONE
                    setStatus(R.string.status_preparing, connected = false)
                    return
                }
                // Bildirimden geldik / servis sync — cihaz listesine atlama
                if (syncingServiceState) {
                    hideAllPanels()
                    connectionContainer.visibility = View.VISIBLE
                    connectionProgressBar.visibility = View.VISIBLE
                    retryButton.visibility = View.GONE
                    repairHintButton.visibility = View.GONE
                    setStatus(R.string.status_preparing, connected = false)
                    return
                }
                hideAllPanels()
                deviceSelectionContainer.visibility = View.VISIBLE
                setStatus(R.string.status_ready, connected = false)
                deviceAdapter.submit(state.devices)
                emptyDevicesText.visibility =
                    if (state.devices.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun setStatus(textRes: Int, connected: Boolean) {
        statusText.setText(textRes)
        statusDot.setBackgroundResource(
            if (connected) R.drawable.dot_connected else R.drawable.dot_idle
        )
    }

    private fun setStatus(text: String, connected: Boolean) {
        statusText.text = text
        statusDot.setBackgroundResource(
            if (connected) R.drawable.dot_connected else R.drawable.dot_idle
        )
    }

    private fun hideAllPanels() {
        setupContainer.visibility = View.GONE
        deviceSelectionContainer.visibility = View.GONE
        connectionContainer.visibility = View.GONE
        inputContainer.visibility = View.GONE
    }

    private fun showRepairDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.repair_dialog_title)
            .setMessage(R.string.repair_dialog_message)
            .setPositiveButton(R.string.repair_open_settings) { _, _ ->
                PermissionHelper.openBluetoothSettings(this)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}

private class BondedDeviceAdapter(
    private val onClick: (BondedDevice) -> Unit
) : RecyclerView.Adapter<BondedDeviceAdapter.Holder>() {

    private val items = mutableListOf<BondedDevice>()

    fun submit(devices: List<BondedDevice>) {
        items.clear()
        items.addAll(devices)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.name.text = item.name
        holder.address.text = item.address
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.deviceName)
        val address: TextView = view.findViewById(R.id.deviceAddress)
    }
}

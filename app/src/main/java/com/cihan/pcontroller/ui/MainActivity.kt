package com.cihan.pcontroller.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
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

/**
 * Sprint 2: bonded-only discovery, BT enable/permission gate, ViewModel UI state.
 * Platform profiles → Sprint 3.
 */
class MainActivity : AppCompatActivity() {

    private val viewModel: RemoteViewModel by viewModels()

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

    private val deviceAdapter = BondedDeviceAdapter { device ->
        viewModel.selectDevice(device)
        connectTo(device.address)
    }

    private val enableBtLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshSetup()
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val denied = result.filterValues { granted -> !granted }.keys
        if (denied.isNotEmpty()) {
            Toast.makeText(this, R.string.bluetooth_permission_required, Toast.LENGTH_LONG).show()
        }
        viewModel.refreshSetup()
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothHidService.LocalBinder
            hidService = binder.getService()
            bound = true
            observeServiceState()
            connectRequestedAddress?.let { address ->
                connectRequestedAddress = null
                hidService?.connect(address)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bound = false
            hidService = null
            stateJob?.cancel()
            stateJob = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()
        setupListeners()
        deviceListRecyclerView.layoutManager = LinearLayoutManager(this)
        deviceListRecyclerView.adapter = deviceAdapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { render(it) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.refreshSetup()
        ensureServiceBound()
    }

    override fun onStop() {
        super.onStop()
        stateJob?.cancel()
        stateJob = null
        if (bound) {
            unbindService(serviceConnection)
            bound = false
            hidService = null
        }
    }

    private fun bindViews() {
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
        openAppSettingsButton.setOnClickListener {
            PermissionHelper.openAppSettings(this)
        }
        refreshDevicesButton.setOnClickListener {
            viewModel.refreshSetup()
        }
        openBluetoothSettingsButton.setOnClickListener {
            PermissionHelper.openBluetoothSettings(this)
        }
        retryButton.setOnClickListener {
            viewModel.uiState.value.selectedAddress?.let { connectTo(it) }
        }
        repairHintButton.setOnClickListener { showRepairDialog() }
        cancelConnectButton.setOnClickListener {
            hidService?.disconnect()
            viewModel.clearSelection()
            viewModel.refreshSetup()
        }
        disconnectButton.setOnClickListener {
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
        val intent = Intent(this, BluetoothHidService::class.java)
        startForegroundService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun observeServiceState() {
        stateJob?.cancel()
        val service = hidService ?: return
        stateJob = lifecycleScope.launch {
            service.connectionState.collectLatest { state ->
                viewModel.onConnectionState(state)
            }
        }
    }

    private fun connectTo(address: String) {
        ensureServiceBound()
        val service = hidService
        if (service != null && bound) {
            service.connect(address)
        } else {
            connectRequestedAddress = address
            val intent = Intent(this, BluetoothHidService::class.java).apply {
                action = BluetoothHidService.ACTION_CONNECT
                putExtra(BluetoothHidService.EXTRA_DEVICE_ADDRESS, address)
            }
            startForegroundService(intent)
            bindService(Intent(this, BluetoothHidService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun render(state: RemoteUiState) {
        when (state.setup) {
            SetupPhase.Checking -> {
                hideAllPanels()
                statusText.setText(R.string.status_ready)
            }
            SetupPhase.NeedBluetoothEnable -> {
                hideAllPanels()
                setupContainer.visibility = View.VISIBLE
                setupMessage.setText(R.string.bt_off_message)
                statusText.text = "Bluetooth kapalı"
                enableBluetoothButton.visibility = View.VISIBLE
                requestPermissionButton.visibility = View.GONE
                openAppSettingsButton.visibility = View.GONE
            }
            SetupPhase.NeedPermission -> {
                hideAllPanels()
                setupContainer.visibility = View.VISIBLE
                setupMessage.setText(R.string.bt_permission_message)
                statusText.setText(R.string.bluetooth_permission_required)
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
                hideAllPanels()
                inputContainer.visibility = View.VISIBLE
                statusText.text = getString(
                    R.string.status_connected,
                    connection.deviceName ?: connection.deviceAddress
                )
            }
            is ConnectionState.Connecting, is ConnectionState.Starting -> {
                hideAllPanels()
                connectionContainer.visibility = View.VISIBLE
                connectionProgressBar.visibility = View.VISIBLE
                retryButton.visibility = View.GONE
                repairHintButton.visibility = View.GONE
                statusText.setText(
                    if (connection is ConnectionState.Connecting) R.string.status_connecting
                    else R.string.status_preparing
                )
            }
            is ConnectionState.Failed -> {
                hideAllPanels()
                connectionContainer.visibility = View.VISIBLE
                connectionProgressBar.visibility = View.GONE
                retryButton.visibility = View.VISIBLE
                repairHintButton.visibility =
                    if (state.showRepairHint) View.VISIBLE else View.GONE
                statusText.text = getString(R.string.status_error, connection.reason)
            }
            is ConnectionState.Registered, is ConnectionState.Idle -> {
                // Mid-connect Registered: keep connecting chrome if we have a selection
                if (state.selectedAddress != null &&
                    connectionContainer.visibility == View.VISIBLE &&
                    inputContainer.visibility != View.VISIBLE
                ) {
                    connectionProgressBar.visibility = View.VISIBLE
                    retryButton.visibility = View.GONE
                    statusText.setText(R.string.status_preparing)
                    return
                }
                hideAllPanels()
                deviceSelectionContainer.visibility = View.VISIBLE
                statusText.setText(R.string.status_ready)
                deviceAdapter.submit(state.devices)
                emptyDevicesText.visibility =
                    if (state.devices.isEmpty()) View.VISIBLE else View.GONE
            }
        }
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

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): Holder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.title.text = item.name
        holder.subtitle.text = item.address
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(android.R.id.text1)
        val subtitle: TextView = view.findViewById(android.R.id.text2)
    }
}

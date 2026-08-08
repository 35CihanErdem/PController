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
import com.cihan.pcontroller.domain.PlatformProfile
import com.cihan.pcontroller.domain.RemoteAction
import com.cihan.pcontroller.domain.RemoteButtonSpec
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

    private lateinit var platformSelectionContainer: LinearLayout
    private lateinit var platformListRecyclerView: RecyclerView
    private lateinit var disconnectFromPlatformButton: Button

    private lateinit var inputContainer: ScrollView
    private lateinit var changePlatformButton: Button
    private lateinit var topPrimaryButton: Button
    private lateinit var mediaRow: LinearLayout
    private lateinit var mediaPrevButton: Button
    private lateinit var mediaPlayButton: Button
    private lateinit var mediaNextButton: Button
    private lateinit var dpadPanel: LinearLayout
    private lateinit var upButton: Button
    private lateinit var downButton: Button
    private lateinit var leftButton: Button
    private lateinit var rightButton: Button
    private lateinit var centerButton: Button
    private lateinit var rowA: LinearLayout
    private lateinit var rowALeftButton: Button
    private lateinit var rowARightButton: Button
    private lateinit var rowB: LinearLayout
    private lateinit var rowBLeftButton: Button
    private lateinit var rowBRightButton: Button
    private lateinit var extraButton: Button
    private lateinit var disconnectButton: Button

    private var hidService: BluetoothHidService? = null
    private var bound = false
    private var stateJob: Job? = null
    private var connectRequestedAddress: String? = null
    private var awaitingConnection = false
    private var syncingServiceState = false

    private val deviceAdapter = BondedDeviceAdapter { device ->
        viewModel.selectDevice(device)
        connectTo(device.address)
    }

    private val platformAdapter = PlatformAdapter { profile ->
        viewModel.selectPlatform(profile.id)
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
            platformListRecyclerView.layoutManager = LinearLayoutManager(this)
            platformListRecyclerView.adapter = platformAdapter
            platformAdapter.submit(viewModel.uiState.value.platforms)
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

        platformSelectionContainer = findViewById(R.id.platformSelectionContainer)
        platformListRecyclerView = findViewById(R.id.platformListRecyclerView)
        disconnectFromPlatformButton = findViewById(R.id.disconnectFromPlatformButton)

        inputContainer = findViewById(R.id.inputContainer)
        changePlatformButton = findViewById(R.id.changePlatformButton)
        topPrimaryButton = findViewById(R.id.topPrimaryButton)
        mediaRow = findViewById(R.id.mediaRow)
        mediaPrevButton = findViewById(R.id.mediaPrevButton)
        mediaPlayButton = findViewById(R.id.mediaPlayButton)
        mediaNextButton = findViewById(R.id.mediaNextButton)
        dpadPanel = findViewById(R.id.dpadPanel)
        upButton = findViewById(R.id.upButton)
        downButton = findViewById(R.id.downButton)
        leftButton = findViewById(R.id.leftButton)
        rightButton = findViewById(R.id.rightButton)
        centerButton = findViewById(R.id.centerButton)
        rowA = findViewById(R.id.rowA)
        rowALeftButton = findViewById(R.id.rowALeftButton)
        rowARightButton = findViewById(R.id.rowARightButton)
        rowB = findViewById(R.id.rowB)
        rowBLeftButton = findViewById(R.id.rowBLeftButton)
        rowBRightButton = findViewById(R.id.rowBRightButton)
        extraButton = findViewById(R.id.extraButton)
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
        cancelConnectButton.setOnClickListener { disconnectAndReset() }
        disconnectButton.setOnClickListener { disconnectAndReset() }
        disconnectFromPlatformButton.setOnClickListener { disconnectAndReset() }
        changePlatformButton.setOnClickListener { viewModel.clearPlatformSelection() }
    }

    private fun disconnectAndReset() {
        awaitingConnection = false
        hidService?.disconnect()
        viewModel.clearSelection()
        viewModel.refreshSetup()
    }

    private fun sendAction(action: RemoteAction) {
        hidService?.sendAction(action)
    }

    private fun bindSpec(button: Button, spec: RemoteButtonSpec?) {
        if (spec == null || !spec.visible) {
            button.visibility = View.GONE
            button.setOnClickListener(null)
            return
        }
        button.visibility = View.VISIBLE
        button.setText(spec.labelRes)
        button.setOnClickListener { sendAction(spec.action) }
    }

    private fun applyRemoteLayout(profile: PlatformProfile) {
        val layout = profile.layout
        changePlatformButton.text =
            getString(R.string.platform_change) + " · " + getString(profile.titleRes)

        bindSpec(topPrimaryButton, layout.topPrimary)

        val media = layout.mediaRow
        if (media == null) {
            mediaRow.visibility = View.GONE
        } else {
            mediaRow.visibility = View.VISIBLE
            bindSpec(mediaPrevButton, media.first)
            bindSpec(mediaPlayButton, media.second)
            bindSpec(mediaNextButton, media.third)
        }

        val hasDpad = layout.dpadUp != null || layout.dpadLeft != null ||
            layout.dpadCenter != null || layout.dpadRight != null || layout.dpadDown != null
        dpadPanel.visibility = if (hasDpad) View.VISIBLE else View.GONE
        bindSpec(upButton, layout.dpadUp)
        bindSpec(leftButton, layout.dpadLeft)
        bindSpec(centerButton, layout.dpadCenter)
        bindSpec(rightButton, layout.dpadRight)
        bindSpec(downButton, layout.dpadDown)

        val a = layout.rowA
        if (a == null) {
            rowA.visibility = View.GONE
        } else {
            rowA.visibility = View.VISIBLE
            bindSpec(rowALeftButton, a.first)
            bindSpec(rowARightButton, a.second)
        }

        val b = layout.rowB
        if (b == null) {
            rowB.visibility = View.GONE
        } else {
            rowB.visibility = View.VISIBLE
            bindSpec(rowBLeftButton, b.first)
            bindSpec(rowBRightButton, b.second)
        }

        bindSpec(extraButton, layout.extra)
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
                val deviceLabel = connection.deviceName ?: connection.deviceAddress
                if (state.activePlatformId == null) {
                    platformSelectionContainer.visibility = View.VISIBLE
                    setStatus(
                        getString(R.string.status_connected, deviceLabel),
                        connected = true
                    )
                } else {
                    val profile = state.activeProfile ?: return
                    inputContainer.visibility = View.VISIBLE
                    applyRemoteLayout(profile)
                    setStatus(
                        getString(R.string.status_connected, deviceLabel) +
                            " · " + getString(profile.titleRes),
                        connected = true
                    )
                }
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
        platformSelectionContainer.visibility = View.GONE
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

private class PlatformAdapter(
    private val onClick: (PlatformProfile) -> Unit
) : RecyclerView.Adapter<PlatformAdapter.Holder>() {

    private val items = mutableListOf<PlatformProfile>()

    fun submit(platforms: List<PlatformProfile>) {
        items.clear()
        items.addAll(platforms)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_platform, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.title.setText(item.titleRes)
        holder.subtitle.setText(item.subtitleRes)
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.platformTitle)
        val subtitle: TextView = view.findViewById(R.id.platformSubtitle)
    }
}

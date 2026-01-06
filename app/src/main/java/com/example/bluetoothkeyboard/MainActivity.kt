package com.example.bluetoothkeyboard

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    // Views
    private lateinit var statusText: TextView
    private lateinit var connectButton: Button
    private lateinit var disconnectButton: Button
    private lateinit var retryButton: Button
    private lateinit var scanButton: Button
    
    // D-Pad buttons
    private lateinit var upButton: Button
    private lateinit var downButton: Button
    private lateinit var leftButton: Button
    private lateinit var rightButton: Button
    private lateinit var enterButton: Button
    private lateinit var fullscreenButton: Button
    private lateinit var brightnessUpButton: Button
    private lateinit var brightnessDownButton: Button
    
    // Screen containers
    private lateinit var deviceSelectionContainer: LinearLayout
    private lateinit var connectionContainer: LinearLayout
    private lateinit var inputContainer: LinearLayout
    private lateinit var deviceListRecyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var connectionProgressBar: ProgressBar
    
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var selectedDevice: BluetoothDevice? = null
    private var isConnected = false
    private var isConnecting = false
    private var isScanning = false
    
    private val bluetoothPermissionRequestCode = 100
    private val locationPermissionRequestCode = 101
    private val scanResults = mutableListOf<BluetoothDevice>()
    private val deviceAdapter = DeviceAdapter { device ->
        selectedDevice = device
        connect() // Direkt bağlanmaya başla
    }
    
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
            }
            
            val device = result.device
            // Add device if not already in list (check by address)
            val exists = scanResults.any { it.address == device.address }
            if (!exists && device.name != null) {
                scanResults.add(device)
                deviceAdapter.updateDevices(scanResults)
            }
        }
        
        override fun onScanFailed(errorCode: Int) {
            stopScanning()
            // Don't show error if we have paired devices
            if (scanResults.isEmpty()) {
                Toast.makeText(
                    this@MainActivity,
                    "Tarama hatası: $errorCode",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_main)
            
            initializeViews()
            checkPermissions()
            initializeBluetooth()
            setupButtonListeners()
            showDeviceSelectionScreen()
            loadPairedDevices()
        } catch (e: Exception) {
            Log.e("MainActivity", "onCreate error", e)
            Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
    
    private fun initializeViews() {
        statusText = findViewById(R.id.statusText)
        connectButton = findViewById(R.id.connectButton)
        disconnectButton = findViewById(R.id.disconnectButton)
        retryButton = findViewById(R.id.retryButton)
        scanButton = findViewById(R.id.scanButton)
        
        upButton = findViewById(R.id.upButton)
        downButton = findViewById(R.id.downButton)
        leftButton = findViewById(R.id.leftButton)
        rightButton = findViewById(R.id.rightButton)
        enterButton = findViewById(R.id.enterButton)
        fullscreenButton = findViewById(R.id.fullscreenButton)
        brightnessUpButton = findViewById(R.id.brightnessUpButton)
        brightnessDownButton = findViewById(R.id.brightnessDownButton)
        
        deviceSelectionContainer = findViewById(R.id.deviceSelectionContainer)
        connectionContainer = findViewById(R.id.connectionContainer)
        inputContainer = findViewById(R.id.inputContainer)
        deviceListRecyclerView = findViewById(R.id.deviceListRecyclerView)
        progressBar = findViewById(R.id.progressBar)
        connectionProgressBar = findViewById(R.id.connectionProgressBar)
        
        deviceListRecyclerView.layoutManager = LinearLayoutManager(this)
        deviceListRecyclerView.adapter = deviceAdapter
    }
    
    private fun checkPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        
        // Bluetooth permissions for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }
        } else {
            // For Android 11 and below
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                bluetoothPermissionRequestCode
            )
        }
    }
    
    private fun initializeBluetooth() {
        try {
            val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager
            if (bluetoothManager == null) {
                statusText.text = "Bluetooth servisi mevcut değil"
                scanButton.isEnabled = false
                return
            }
            
            bluetoothAdapter = bluetoothManager.adapter
            
            if (bluetoothAdapter == null) {
                statusText.text = "Bluetooth desteklenmiyor"
                scanButton.isEnabled = false
                return
            }
            
            if (!bluetoothAdapter!!.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivity(enableBtIntent)
            }
            
            bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        } catch (e: Exception) {
            Log.e("MainActivity", "initializeBluetooth error", e)
            statusText.text = "Bluetooth hatası: ${e.message}"
            scanButton.isEnabled = false
        }
    }
    
    private fun setupButtonListeners() {
        scanButton.setOnClickListener {
            startScanning()
        }
        
        connectButton.setOnClickListener {
            connect()
        }
        
        retryButton.setOnClickListener {
            connect()
        }
        
        disconnectButton.setOnClickListener {
            disconnect()
        }
        
        upButton.setOnClickListener {
            if (isConnected) {
                BluetoothHidService.sendKey(KeyCode.UP_ARROW)
            }
        }
        
        downButton.setOnClickListener {
            if (isConnected) {
                BluetoothHidService.sendKey(KeyCode.DOWN_ARROW)
            }
        }
        
        leftButton.setOnClickListener {
            if (isConnected) {
                BluetoothHidService.sendKey(KeyCode.LEFT_ARROW)
            }
        }
        
        rightButton.setOnClickListener {
            if (isConnected) {
                BluetoothHidService.sendKey(KeyCode.RIGHT_ARROW)
            }
        }
        
        enterButton.setOnClickListener {
            if (isConnected) {
                BluetoothHidService.sendKey(KeyCode.SPACE)
            }
        }
        
        fullscreenButton.setOnClickListener {
            if (isConnected) {
                BluetoothHidService.sendKey(KeyCode.F)
            }
        }
        
        brightnessUpButton.setOnClickListener {
            if (isConnected) {
                // Önce Consumer Control kodunu dene
                BluetoothHidService.sendBrightnessUp()
                // Eğer çalışmazsa Windows kısayolunu dene (Win + I sonra parlaklık)
                // Not: Bu her PC'de çalışmayabilir
            } else {
                Toast.makeText(this, "Bluetooth bağlantısı gerekli", Toast.LENGTH_SHORT).show()
            }
        }
        
        brightnessDownButton.setOnClickListener {
            if (isConnected) {
                // Önce Consumer Control kodunu dene
                BluetoothHidService.sendBrightnessDown()
                // Eğer çalışmazsa Windows kısayolunu dene
                // Not: Bu her PC'de çalışmayabilir
            } else {
                Toast.makeText(this, "Bluetooth bağlantısı gerekli", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showDeviceSelectionScreen() {
        deviceSelectionContainer.visibility = View.VISIBLE
        connectionContainer.visibility = View.GONE
        inputContainer.visibility = View.GONE
        statusText.text = "Cihaz seçin"
    }
    
    private fun showConnectionScreen() {
        deviceSelectionContainer.visibility = View.GONE
        connectionContainer.visibility = View.VISIBLE
        inputContainer.visibility = View.GONE
        
        selectedDevice?.let { device ->
            val deviceName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    device.name ?: device.address
                } else {
                    device.address
                }
            } else {
                device.name ?: device.address
            }
            
            statusText.text = "Seçilen: $deviceName"
            connectButton.visibility = View.VISIBLE
            retryButton.visibility = View.GONE
            connectionProgressBar.visibility = View.GONE
        }
    }
    
    private fun showInputScreen() {
        deviceSelectionContainer.visibility = View.GONE
        connectionContainer.visibility = View.GONE
        inputContainer.visibility = View.VISIBLE
        
        selectedDevice?.let { device ->
            val deviceName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    device.name ?: device.address
                } else {
                    device.address
                }
            } else {
                device.name ?: device.address
            }
            
            statusText.text = "Bağlı: $deviceName"
        }
        
        updateInputButtons()
    }
    
    private fun startScanning() {
        if (isScanning) {
            stopScanning()
            return
        }
        
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Toast.makeText(this, "Bluetooth açık değil", Toast.LENGTH_SHORT).show()
            return
        }
        
        // First, show paired devices
        loadPairedDevices()
        
        // Then start BLE scan for additional devices
        if (bluetoothLeScanner != null) {
            scanResults.clear()
            isScanning = true
            scanButton.text = "Durdur"
            progressBar.visibility = View.VISIBLE
            
            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            
            val scanFilters = listOf<ScanFilter>()
            
            try {
                bluetoothLeScanner?.startScan(scanFilters, scanSettings, scanCallback)
                
                // Stop scanning after 10 seconds
                Handler(Looper.getMainLooper()).postDelayed({
                    stopScanning()
                }, 10000)
            } catch (e: SecurityException) {
                Toast.makeText(this, "Tarama izni gerekli", Toast.LENGTH_SHORT).show()
                stopScanning()
            }
        } else {
            // If BLE scanner not available, just show paired devices
            progressBar.visibility = View.GONE
        }
    }
    
    private fun loadPairedDevices() {
        try {
            if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
                return
            }
            
            val pairedDevices = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    bluetoothAdapter!!.bondedDevices
                } else {
                    emptySet()
                }
            } else {
                bluetoothAdapter!!.bondedDevices
            }
            
            scanResults.clear()
            scanResults.addAll(pairedDevices)
            deviceAdapter.updateDevices(scanResults)
            
            if (pairedDevices.isEmpty()) {
                // Don't show toast on startup, just silently load
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "loadPairedDevices error", e)
        }
    }
    
    private fun stopScanning() {
        if (isScanning) {
            try {
                bluetoothLeScanner?.stopScan(scanCallback)
            } catch (e: SecurityException) {
                // Ignore
            }
            isScanning = false
            scanButton.text = "Tara"
            progressBar.visibility = View.GONE
        }
    }
    
    private fun connect() {
        if (selectedDevice == null) {
            Toast.makeText(this, "Cihaz seçilmedi", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (isConnecting) return
        
        isConnecting = true
        deviceSelectionContainer.visibility = View.GONE
        connectionContainer.visibility = View.VISIBLE
        inputContainer.visibility = View.GONE
        connectButton.visibility = View.GONE
        retryButton.visibility = View.GONE
        connectionProgressBar.visibility = View.VISIBLE
        
        selectedDevice?.let { device ->
            val deviceName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    device.name ?: device.address
                } else {
                    device.address
                }
            } else {
                device.name ?: device.address
            }
            
            statusText.text = "Bağlanıyor: $deviceName"
        }
        
        lifecycleScope.launch {
            try {
                val intent = Intent(this@MainActivity, BluetoothHidService::class.java)
                intent.putExtra("device_address", selectedDevice!!.address)
                intent.action = "CONNECT"
                
                // minSdk is 28, so startForegroundService is always available
                startForegroundService(intent)
                
                // Wait for connection with timeout
                var attempts = 0
                while (attempts < 30 && !BluetoothHidService.isConnected()) {
                    delay(500)
                    attempts++
                }
                
                if (BluetoothHidService.isConnected()) {
                    isConnected = true
                    isConnecting = false
                    showInputScreen()
                } else {
                    // Connection failed
                    isConnecting = false
                    connectionProgressBar.visibility = View.GONE
                    connectButton.visibility = View.GONE
                    retryButton.visibility = View.VISIBLE
                    statusText.text = "Bağlantı başarısız. Tekrar dene?"
                }
            } catch (e: Exception) {
                isConnecting = false
                connectionProgressBar.visibility = View.GONE
                connectButton.visibility = View.GONE
                retryButton.visibility = View.VISIBLE
                statusText.text = "Hata: ${e.message}"
            }
        }
    }
    
    private fun disconnect() {
        val intent = Intent(this, BluetoothHidService::class.java)
        intent.action = "DISCONNECT"
        stopService(intent)
        
        isConnected = false
        selectedDevice = null
        updateInputButtons()
        showDeviceSelectionScreen()
    }
    
    private fun updateInputButtons() {
        val enabled = isConnected
        upButton.isEnabled = enabled
        downButton.isEnabled = enabled
        leftButton.isEnabled = enabled
        rightButton.isEnabled = enabled
        enterButton.isEnabled = enabled
        fullscreenButton.isEnabled = enabled
        brightnessUpButton.isEnabled = enabled
        brightnessDownButton.isEnabled = enabled
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == bluetoothPermissionRequestCode) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (!allGranted) {
                Toast.makeText(
                    this,
                    "Bluetooth izinleri gerekli",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Check connection status
        if (BluetoothHidService.isConnected() && selectedDevice != null) {
            isConnected = true
            showInputScreen()
        }
    }
    
    override fun onPause() {
        super.onPause()
        stopScanning()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopScanning()
    }
    
}

// Simple RecyclerView adapter for device list
class DeviceAdapter(
    private val onDeviceClick: (BluetoothDevice) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {
    
    private val devices = mutableListOf<BluetoothDevice>()
    
    fun updateDevices(newDevices: List<BluetoothDevice>) {
        devices.clear()
        devices.addAll(newDevices)
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): DeviceViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return DeviceViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        val deviceName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    holder.itemView.context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                device.name ?: device.address
            } else {
                device.address
            }
        } else {
            device.name ?: device.address
        }
        
        (holder.itemView as TextView).text = deviceName
        holder.itemView.setOnClickListener {
            onDeviceClick(device)
        }
    }
    
    override fun getItemCount() = devices.size
    
    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}

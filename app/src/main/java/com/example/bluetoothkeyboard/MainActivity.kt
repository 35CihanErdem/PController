package com.example.bluetoothkeyboard

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var statusText: TextView
    private lateinit var connectButton: Button
    private lateinit var playPauseButton: Button
    private lateinit var forwardButton: Button
    private lateinit var backwardButton: Button
    private lateinit var volumeUpButton: Button
    private lateinit var volumeDownButton: Button
    private lateinit var fullscreenButton: Button
    
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var selectedDevice: BluetoothDevice? = null
    private var isConnected = false
    
    private val bluetoothPermissionRequestCode = 100
    private val notificationPermissionRequestCode = 101
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeViews()
        checkPermissions()
        initializeBluetooth()
        setupButtonListeners()
    }
    
    private fun initializeViews() {
        statusText = findViewById(R.id.statusText)
        connectButton = findViewById(R.id.connectButton)
        playPauseButton = findViewById(R.id.playPauseButton)
        forwardButton = findViewById(R.id.forwardButton)
        backwardButton = findViewById(R.id.backwardButton)
        volumeUpButton = findViewById(R.id.volumeUpButton)
        volumeDownButton = findViewById(R.id.volumeDownButton)
        fullscreenButton = findViewById(R.id.fullscreenButton)
    }
    
    private fun checkPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        
        // Bluetooth permissions for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            }
        }
        
        // Notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
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
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        
        if (bluetoothAdapter == null) {
            statusText.text = getString(R.string.status_error, "Bluetooth desteklenmiyor")
            connectButton.isEnabled = false
            return
        }
        
        if (!bluetoothAdapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivity(enableBtIntent)
        }
    }
    
    private fun setupButtonListeners() {
        connectButton.setOnClickListener {
            if (isConnected) {
                disconnect()
            } else {
                showDeviceSelectionDialog()
            }
        }
        
        playPauseButton.setOnClickListener {
            BluetoothHidService.sendKey(KeyCode.SPACE)
        }
        
        forwardButton.setOnClickListener {
            BluetoothHidService.sendKey(KeyCode.RIGHT_ARROW)
        }
        
        backwardButton.setOnClickListener {
            BluetoothHidService.sendKey(KeyCode.LEFT_ARROW)
        }
        
        volumeUpButton.setOnClickListener {
            BluetoothHidService.sendKey(KeyCode.UP_ARROW)
        }
        
        volumeDownButton.setOnClickListener {
            BluetoothHidService.sendKey(KeyCode.DOWN_ARROW)
        }
        
        fullscreenButton.setOnClickListener {
            BluetoothHidService.sendKey(KeyCode.F)
        }
    }
    
    private fun showDeviceSelectionDialog() {
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Toast.makeText(this, "Bluetooth açık değil", Toast.LENGTH_SHORT).show()
            return
        }
        
        val pairedDevices = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) 
                == PackageManager.PERMISSION_GRANTED) {
                bluetoothAdapter!!.bondedDevices
            } else {
                emptySet()
            }
        } else {
            @Suppress("DEPRECATION")
            bluetoothAdapter!!.bondedDevices
        }
        
        if (pairedDevices.isEmpty()) {
            Toast.makeText(this, "Eşleştirilmiş cihaz bulunamadı", Toast.LENGTH_SHORT).show()
            return
        }
        
        val deviceNames = pairedDevices.map { 
            it.name ?: it.address 
        }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("Cihaz Seçin")
            .setItems(deviceNames) { _, which ->
                selectedDevice = pairedDevices.elementAt(which)
                connect()
            }
            .setNegativeButton("İptal", null)
            .show()
    }
    
    private fun connect() {
        if (selectedDevice == null) return
        
        lifecycleScope.launch {
            try {
                updateStatus(getString(R.string.status_connecting))
                connectButton.isEnabled = false
                
                val intent = Intent(this@MainActivity, BluetoothHidService::class.java)
                intent.putExtra("device_address", selectedDevice!!.address)
                intent.action = "CONNECT"
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
                
                // Wait a bit for connection
                kotlinx.coroutines.delay(2000)
                
                if (BluetoothHidService.isConnected()) {
                    isConnected = true
                    updateUI(true)
                    updateStatus(getString(R.string.status_connected, selectedDevice!!.name ?: selectedDevice!!.address))
                } else {
                    updateStatus(getString(R.string.status_error, "Bağlantı başarısız"))
                    connectButton.isEnabled = true
                }
            } catch (e: Exception) {
                updateStatus(getString(R.string.status_error, e.message))
                connectButton.isEnabled = true
            }
        }
    }
    
    private fun disconnect() {
        val intent = Intent(this, BluetoothHidService::class.java)
        intent.action = "DISCONNECT"
        stopService(intent)
        
        isConnected = false
        updateUI(false)
        updateStatus(getString(R.string.status_ready))
    }
    
    private fun updateUI(connected: Boolean) {
        playPauseButton.isEnabled = connected
        forwardButton.isEnabled = connected
        backwardButton.isEnabled = connected
        volumeUpButton.isEnabled = connected
        volumeDownButton.isEnabled = connected
        fullscreenButton.isEnabled = connected
        
        connectButton.text = if (connected) {
            getString(R.string.disconnect)
        } else {
            getString(R.string.connect)
        }
    }
    
    private fun updateStatus(message: String) {
        runOnUiThread {
            statusText.text = message
        }
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
                    getString(R.string.bluetooth_permission_required),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Check connection status
        if (BluetoothHidService.isConnected()) {
            isConnected = true
            updateUI(true)
            selectedDevice?.let {
                updateStatus(getString(R.string.status_connected, it.name ?: it.address))
            }
        }
    }
}


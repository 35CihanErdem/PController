package com.cihan.pcontroller.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cihan.pcontroller.bluetooth.BondedDevice
import com.cihan.pcontroller.bluetooth.BondedDeviceRepository
import com.cihan.pcontroller.bluetooth.ConnectionState
import com.cihan.pcontroller.util.PermissionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SetupPhase {
    Checking,
    NeedBluetoothEnable,
    NeedPermission,
    Ready
}

data class RemoteUiState(
    val setup: SetupPhase = SetupPhase.Checking,
    val devices: List<BondedDevice> = emptyList(),
    val connection: ConnectionState = ConnectionState.Idle,
    val selectedAddress: String? = null,
    val selectedName: String? = null,
    val showRepairHint: Boolean = false
)

class RemoteViewModel(application: Application) : AndroidViewModel(application) {

    private val bondedRepo = BondedDeviceRepository(application)

    private val _uiState = MutableStateFlow(RemoteUiState())
    val uiState: StateFlow<RemoteUiState> = _uiState.asStateFlow()

    fun refreshSetup() {
        viewModelScope.launch {
            val app = getApplication<Application>()
            when {
                !PermissionHelper.isBluetoothSupported(app) -> {
                    _uiState.update {
                        it.copy(
                            setup = SetupPhase.NeedPermission,
                            devices = emptyList()
                        )
                    }
                }
                !PermissionHelper.isBluetoothEnabled(app) -> {
                    _uiState.update {
                        it.copy(
                            setup = SetupPhase.NeedBluetoothEnable,
                            devices = emptyList()
                        )
                    }
                }
                !PermissionHelper.hasBluetoothPermissions(app) -> {
                    _uiState.update {
                        it.copy(
                            setup = SetupPhase.NeedPermission,
                            devices = emptyList()
                        )
                    }
                }
                else -> {
                    val devices = bondedRepo.loadBondedDevices()
                    _uiState.update {
                        it.copy(
                            setup = SetupPhase.Ready,
                            devices = devices,
                            showRepairHint = false
                        )
                    }
                }
            }
        }
    }

    fun onConnectionState(state: ConnectionState) {
        _uiState.update { current ->
            val hint = if (state is ConnectionState.Failed) {
                val reason = state.reason
                reason.contains("eşleştir", ignoreCase = true) ||
                    reason.contains("kabul etmedi", ignoreCase = true) ||
                    reason.contains("HID", ignoreCase = true) ||
                    reason.contains("Zaman aşımı", ignoreCase = true)
            } else {
                false
            }
            current.copy(
                connection = state,
                showRepairHint = hint,
                selectedAddress = when (state) {
                    is ConnectionState.Connected -> state.deviceAddress
                    is ConnectionState.Failed -> current.selectedAddress
                    is ConnectionState.Connecting, is ConnectionState.Starting ->
                        current.selectedAddress
                    is ConnectionState.Idle -> null
                    is ConnectionState.Registered -> current.selectedAddress
                },
                selectedName = when (state) {
                    is ConnectionState.Connected -> state.deviceName ?: state.deviceAddress
                    is ConnectionState.Failed -> current.selectedName
                    is ConnectionState.Connecting, is ConnectionState.Starting ->
                        current.selectedName
                    is ConnectionState.Idle -> null
                    is ConnectionState.Registered -> current.selectedName
                }
            )
        }
    }

    fun selectDevice(device: BondedDevice) {
        _uiState.update {
            it.copy(
                selectedAddress = device.address,
                selectedName = device.name,
                showRepairHint = false
            )
        }
    }

    fun clearSelection() {
        _uiState.update {
            it.copy(selectedAddress = null, selectedName = null, showRepairHint = false)
        }
    }
}

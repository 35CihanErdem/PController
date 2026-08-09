package com.cihan.pcontroller.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cihan.pcontroller.bluetooth.BondedDevice
import com.cihan.pcontroller.bluetooth.BondedDeviceRepository
import com.cihan.pcontroller.bluetooth.ConnectionState
import com.cihan.pcontroller.domain.PlatformCatalog
import com.cihan.pcontroller.domain.PlatformId
import com.cihan.pcontroller.domain.PlatformProfile
import com.cihan.pcontroller.domain.PlatformStore
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

/** Remote (platform tuşları) veya Mouse (trackpad) yüzeyi. */
enum class ControlSurface {
    Remote,
    Mouse
}

data class RemoteUiState(
    val setup: SetupPhase = SetupPhase.Checking,
    val devices: List<BondedDevice> = emptyList(),
    val connection: ConnectionState = ConnectionState.Idle,
    val selectedAddress: String? = null,
    val selectedName: String? = null,
    val showRepairHint: Boolean = false,
    /** null = platform seçim ekranı (bağlıyken) */
    val activePlatformId: PlatformId? = null,
    val controlSurface: ControlSurface = ControlSurface.Remote,
    val platforms: List<PlatformProfile> = PlatformCatalog.all
) {
    val activeProfile: PlatformProfile?
        get() = activePlatformId?.let { PlatformCatalog.byId(it) }
}

class RemoteViewModel(application: Application) : AndroidViewModel(application) {

    private val bondedRepo = BondedDeviceRepository(application)
    private val platformStore = PlatformStore(application)

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
                !PermissionHelper.hasBluetoothPermissions(app) -> {
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
            // Bağlantı düşünce platform seçimini sıfırla → tekrar seçtir
            val platform = when (state) {
                is ConnectionState.Connected -> current.activePlatformId
                is ConnectionState.Idle -> null
                else -> current.activePlatformId
            }
            val surface = when (state) {
                is ConnectionState.Idle -> ControlSurface.Remote
                else -> current.controlSurface
            }
            current.copy(
                connection = state,
                showRepairHint = hint,
                activePlatformId = platform,
                controlSurface = surface,
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
                showRepairHint = false,
                activePlatformId = null,
                controlSurface = ControlSurface.Remote
            )
        }
    }

    fun selectPlatform(id: PlatformId) {
        platformStore.lastPlatformId = id
        _uiState.update {
            it.copy(activePlatformId = id, controlSurface = ControlSurface.Remote)
        }
    }

    /** Platform seçmeden doğrudan trackpad. */
    fun selectMouseMode() {
        _uiState.update {
            it.copy(
                controlSurface = ControlSurface.Mouse,
                // Mouse için platform şart değil; dönüşte seçim ekranı için null tut
                activePlatformId = it.activePlatformId
            )
        }
    }

    fun showRemoteSurface() {
        _uiState.update { it.copy(controlSurface = ControlSurface.Remote) }
    }

    fun clearPlatformSelection() {
        _uiState.update {
            it.copy(activePlatformId = null, controlSurface = ControlSurface.Remote)
        }
    }

    fun clearSelection() {
        _uiState.update {
            it.copy(
                selectedAddress = null,
                selectedName = null,
                showRepairHint = false,
                activePlatformId = null,
                controlSurface = ControlSurface.Remote
            )
        }
    }
}

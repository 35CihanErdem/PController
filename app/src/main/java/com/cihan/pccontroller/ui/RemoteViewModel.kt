package com.cihan.pccontroller.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cihan.pccontroller.bluetooth.BondedDevice
import com.cihan.pccontroller.bluetooth.BondedDeviceRepository
import com.cihan.pccontroller.bluetooth.ConnectionState
import com.cihan.pccontroller.domain.PlatformCatalog
import com.cihan.pccontroller.domain.PlatformId
import com.cihan.pccontroller.domain.PlatformProfile
import com.cihan.pccontroller.domain.PlatformStore
import com.cihan.pccontroller.domain.ProStore
import com.cihan.pccontroller.util.PermissionHelper
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

/** Remote (platform tuşları), Mouse (trackpad) veya Keyboard (QWERTY). */
enum class ControlSurface {
    Remote,
    Mouse,
    Keyboard
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
    val platforms: List<PlatformProfile> = PlatformCatalog.all,
    /** Touchpad + Klavye için Pro */
    val isPro: Boolean = false
) {
    val activeProfile: PlatformProfile?
        get() = activePlatformId?.let { PlatformCatalog.byId(it) }
}

class RemoteViewModel(application: Application) : AndroidViewModel(application) {

    private val bondedRepo = BondedDeviceRepository(application)
    private val platformStore = PlatformStore(application)
    private val proStore = ProStore(application)

    private val _uiState = MutableStateFlow(RemoteUiState(isPro = proStore.isPro))
    val uiState: StateFlow<RemoteUiState> = _uiState.asStateFlow()

    fun refreshPro() {
        _uiState.update { it.copy(isPro = proStore.isPro) }
    }

    fun unlockProForDebug() {
        proStore.unlockForDebug()
        refreshPro()
    }

    fun lockProForDebug() {
        proStore.lockForDebug()
        refreshPro()
        // Pro kapandıysa mouse/keyboard yüzeyinden çık
        _uiState.update {
            if (it.controlSurface == ControlSurface.Mouse ||
                it.controlSurface == ControlSurface.Keyboard
            ) {
                it.copy(controlSurface = ControlSurface.Remote, activePlatformId = null)
            } else {
                it
            }
        }
    }

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

    /** Platform seçmeden doğrudan trackpad. Pro gerekir. */
    fun selectMouseMode(): Boolean {
        if (!proStore.isPro) return false
        _uiState.update {
            it.copy(
                controlSurface = ControlSurface.Mouse,
                activePlatformId = it.activePlatformId,
                isPro = true
            )
        }
        return true
    }

    /** Platform seçmeden doğrudan HID klavye. Pro gerekir. */
    fun selectKeyboardMode(): Boolean {
        if (!proStore.isPro) return false
        _uiState.update {
            it.copy(
                controlSurface = ControlSurface.Keyboard,
                activePlatformId = it.activePlatformId,
                isPro = true
            )
        }
        return true
    }

    fun showRemoteSurface() {
        _uiState.update { it.copy(controlSurface = ControlSurface.Remote) }
    }

    /** Mod seçim ekranına dön (Kumanda / Mouse / Klavye). */
    fun backToModePick() {
        _uiState.update {
            it.copy(activePlatformId = null, controlSurface = ControlSurface.Remote)
        }
    }

    fun clearPlatformSelection() = backToModePick()

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

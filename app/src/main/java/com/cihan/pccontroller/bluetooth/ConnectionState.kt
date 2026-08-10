package com.cihan.pccontroller.bluetooth

/**
 * Single source of truth for HID connection lifecycle.
 * Emitted by [HidDeviceManager] via StateFlow.
 */
sealed class ConnectionState {
    /** Service / manager not started or fully torn down. */
    data object Idle : ConnectionState()

    /** Acquiring HID profile proxy and/or registering the HID app. */
    data object Starting : ConnectionState()

    /** HID app registered; ready to accept connect(address). */
    data object Registered : ConnectionState()

    /** connect() issued; waiting for host. */
    data object Connecting : ConnectionState()

    data class Connected(
        val deviceAddress: String,
        val deviceName: String?
    ) : ConnectionState()

    data class Failed(val reason: String) : ConnectionState()

    val isConnected: Boolean
        get() = this is Connected
}

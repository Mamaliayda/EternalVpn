package com.eternalvpn.app.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Phase 2: owns the Xray-core lifecycle via libv2ray. Reports state, logs, and traffic stats.
 */
object XrayController {
    private val _state = MutableStateFlow(VpnState.DISCONNECTED)
    val state: StateFlow<VpnState> = _state.asStateFlow()

    fun start(configJson: String) {
        // TODO(Phase 2): libv2ray core start
        _state.value = VpnState.CONNECTING
    }

    fun stop() {
        // TODO(Phase 2): libv2ray core stop
        _state.value = VpnState.DISCONNECTED
    }

    fun isRunning(): Boolean = _state.value == VpnState.CONNECTED
}

enum class VpnState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

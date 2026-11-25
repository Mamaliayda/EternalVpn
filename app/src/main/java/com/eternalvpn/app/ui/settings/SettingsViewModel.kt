package com.eternalvpn.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eternalvpn.app.core.VpnState
import com.eternalvpn.app.core.XrayController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL

data class TrafficStats(val uplink: Long = 0L, val downlink: Long = 0L)

class SettingsViewModel : ViewModel() {
    val vpnState: StateFlow<VpnState> = XrayController.state
    val logs: StateFlow<List<String>> = XrayController.logs

    private val _stats = MutableStateFlow(TrafficStats())
    val stats: StateFlow<TrafficStats> = _stats.asStateFlow()

    private val _exitIp = MutableStateFlow<String>("Not checked")
    val exitIp: StateFlow<String> = _exitIp.asStateFlow()

    private val _checkingIp = MutableStateFlow(false)
    val checkingIp: StateFlow<Boolean> = _checkingIp.asStateFlow()

    fun refreshStats() {
        val raw = XrayController.queryStats()
        var up = 0L
        var down = 0L
        raw.split(';').filter { it.isNotBlank() }.forEach { entry ->
            val parts = entry.split(',')
            if (parts.size == 3) {
                val dir = parts[1]
                val value = parts[2].toLongOrNull() ?: 0L
                if (dir == "uplink") up += value else if (dir == "downlink") down += value
            }
        }
        _stats.value = TrafficStats(up, down)
    }

    fun checkExitIp() {
        viewModelScope.launch(Dispatchers.IO) {
            _checkingIp.value = true
            _exitIp.value = "Checking..."
            try {
                val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", 10808))
                val conn = URL("https://api.ipify.org?format=text").openConnection(proxy)
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                val ip = conn.getInputStream().bufferedReader().use { it.readText().trim() }
                _exitIp.value = ip.ifBlank { "Empty response" }
            } catch (t: Throwable) {
                _exitIp.value = "Error: ${t.message ?: t.javaClass.simpleName}"
            } finally {
                _checkingIp.value = false
            }
        }
    }
}

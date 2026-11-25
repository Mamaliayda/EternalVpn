package com.eternalvpn.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eternalvpn.app.R
import com.eternalvpn.app.core.VpnState
import java.util.Locale

@Composable
fun SettingsScreen(vm: SettingsViewModel = viewModel()) {
    val vpnState by vm.vpnState.collectAsState()
    val logs by vm.logs.collectAsState()
    val stats by vm.stats.collectAsState()
    val exitIp by vm.exitIp.collectAsState()
    val checkingIp by vm.checkingIp.collectAsState()

    LaunchedEffect(vpnState) { vm.refreshStats() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Debug", style = MaterialTheme.typography.headlineMedium)
        }
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Core state", style = MaterialTheme.typography.titleMedium)
                    Text("State: ${vpnState.name}", color = stateColor(vpnState))
                    Text("Uplink: ${formatBytes(stats.uplink)}")
                    Text("Downlink: ${formatBytes(stats.downlink)}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { vm.refreshStats() }) { Text("Refresh stats") }
                        Button(onClick = { vm.checkExitIp() }, enabled = !checkingIp) {
                            Text(if (checkingIp) "Checking..." else "Check exit IP")
                        }
                    }
                    Text("Exit IP (via SOCKS 127.0.0.1:10808): $exitIp")
                }
            }
        }
        item {
            Text("Last ${logs.size} log lines", style = MaterialTheme.typography.titleMedium)
        }
        if (logs.isEmpty()) {
            item { Text("No logs yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(logs.takeLast(200)) { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                )
            }
        }
    }
}

@Composable
private fun stateColor(state: VpnState) = when (state) {
    VpnState.CONNECTED -> MaterialTheme.colorScheme.primary
    VpnState.CONNECTING -> MaterialTheme.colorScheme.tertiary
    VpnState.ERROR -> MaterialTheme.colorScheme.error
    VpnState.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale.US, "%.2f MB", mb)
    val gb = mb / 1024.0
    return String.format(Locale.US, "%.2f GB", gb)
}

package com.eternalvpn.app.ui.home

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eternalvpn.app.R
import com.eternalvpn.app.core.TunService
import com.eternalvpn.app.core.VpnState

@Composable
fun HomeScreen(vm: HomeViewModel = viewModel()) {
    val context = LocalContext.current
    val vpnState by vm.vpnState.collectAsState()
    val lastError by vm.lastError.collectAsState()
    val selectedNode by vm.selectedNode.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val vpnPrepareLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            context.startForegroundServiceCompat(Intent(context, TunService::class.java))
        } else {
            // user denied VPN permission
        }
    }

    LaunchedEffect(lastError) {
        val err = lastError
        if (!err.isNullOrBlank() && vpnState == VpnState.ERROR) {
            snackbarHostState.showSnackbar(err)
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(12.dp))

            val statusText = when (vpnState) {
                VpnState.DISCONNECTED -> stringResource(R.string.disconnected)
                VpnState.CONNECTING -> stringResource(R.string.connecting)
                VpnState.CONNECTED -> stringResource(R.string.connected)
                VpnState.ERROR -> "Error: ${lastError ?: "unknown"}"
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyLarge,
                color = when (vpnState) {
                    VpnState.ERROR -> MaterialTheme.colorScheme.error
                    VpnState.CONNECTED -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))
            Text(
                text = selectedNode?.let { "${it.name} (${it.protocol.name.lowercase()})" } ?: "No node selected",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    when (vpnState) {
                        VpnState.CONNECTED, VpnState.CONNECTING -> {
                            context.startService(Intent(context, TunService::class.java).setAction(TunService.ACTION_STOP))
                        }
                        VpnState.DISCONNECTED, VpnState.ERROR -> {
                            val prepare = VpnService.prepare(context)
                            if (prepare != null) {
                                vpnPrepareLauncher.launch(prepare)
                            } else {
                                context.startForegroundServiceCompat(Intent(context, TunService::class.java))
                            }
                        }
                    }
                },
                modifier = Modifier.size(160.dp),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Text(
                    text = when (vpnState) {
                        VpnState.CONNECTED, VpnState.CONNECTING -> stringResource(R.string.disconnect)
                        VpnState.DISCONNECTED, VpnState.ERROR -> stringResource(R.string.connect)
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

private fun android.content.Context.startForegroundServiceCompat(intent: Intent) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        startForegroundService(intent)
    } else {
        startService(intent)
    }
}

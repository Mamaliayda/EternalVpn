package com.eternalvpn.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eternalvpn.app.R

@Composable
fun HomeScreen() {
    var connected by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (connected) stringResource(R.string.connected) else stringResource(R.string.disconnected),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { connected = !connected },
            modifier = Modifier.size(160.dp),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Text(
                text = if (connected) stringResource(R.string.disconnect) else stringResource(R.string.connect),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Phase 0 skeleton — core wiring lands in Phase 2",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

package com.eternalvpn.app.ui.nodes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eternalvpn.app.R
import com.eternalvpn.app.data.model.Node

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodesScreen(vm: NodesViewModel = viewModel()) {
    val nodes by vm.nodes.collectAsState()
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showQr by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    if (showQr) {
        QrScanDialog(
            onDismiss = { showQr = false },
            onScanned = { value ->
                showQr = false
                vm.importSingleLink(value)
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_nodes)) },
                actions = {
                    IconButton(onClick = {
                        val text = clipboard.getText()?.text?.trim().orEmpty()
                        if (text.isBlank()) {
                            // feedback via VM message channel isn't directly accessible here; import handles empty
                            vm.importFromText("")
                        } else {
                            vm.importFromText(text)
                        }
                    }) {
                        Icon(Icons.Filled.ContentPaste, contentDescription = stringResource(R.string.import_clipboard))
                    }
                    IconButton(onClick = { showQr = true }) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = stringResource(R.string.import_qr))
                    }
                }
            )
        },
    ) { padding ->
        if (nodes.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(stringResource(R.string.no_nodes), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Use the clipboard or QR buttons above to import vless/vmess/trojan/ss links.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(nodes, key = { it.id }) { node ->
                    NodeRow(node = node, onDelete = { vm.deleteNode(node.id) })
                }
            }
        }
    }
}

@Composable
private fun NodeRow(node: Node, onDelete: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(node.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${node.protocol.name.lowercase()} · ${node.address}:${node.port} · ${node.network.name.lowercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                node.latencyMs?.let {
                    Text("${it} ms", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
            }
        }
    }
}

package com.eternalvpn.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eternalvpn.app.EternalVpnApp
import com.eternalvpn.app.core.VpnState
import com.eternalvpn.app.core.XrayController
import com.eternalvpn.app.data.model.Node
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = (app as EternalVpnApp).settingsStore
    private val nodeDao = app.database.nodeDao()

    val vpnState: StateFlow<VpnState> = XrayController.state
    val lastError: StateFlow<String?> = XrayController.lastError

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedNode: StateFlow<Node?> = settings.selectedNodeId
        .flatMapLatest { id -> if (id == null) flowOf(null) else flow { emit(nodeDao.getById(id)) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}

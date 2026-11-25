package com.eternalvpn.app.ui.nodes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eternalvpn.app.EternalVpnApp
import com.eternalvpn.app.data.NodeRepository
import com.eternalvpn.app.data.model.Node
import com.eternalvpn.app.data.parser.LinkParser
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NodesViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = NodeRepository((app as EternalVpnApp).database.nodeDao())

    val nodes: StateFlow<List<Node>> = repo.observeNodes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()

    fun importFromText(text: String) {
        viewModelScope.launch {
            val parsed = LinkParser.parseLines(text)
            if (parsed.isEmpty()) {
                _messages.tryEmit("No valid links found")
                return@launch
            }
            repo.upsertAll(parsed)
            _messages.tryEmit("Imported ${parsed.size} node(s)")
        }
    }

    fun importSingleLink(link: String) {
        viewModelScope.launch {
            val node = LinkParser.parse(link.trim())
            if (node == null) {
                _messages.tryEmit("Invalid link")
                return@launch
            }
            repo.upsert(node)
            _messages.tryEmit("Imported ${node.name}")
        }
    }

    fun deleteNode(id: Long) {
        viewModelScope.launch { repo.delete(id) }
    }
}

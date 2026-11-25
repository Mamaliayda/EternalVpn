package com.eternalvpn.app.data

import com.eternalvpn.app.data.db.NodeDao
import com.eternalvpn.app.data.model.Node
import kotlinx.coroutines.flow.Flow

class NodeRepository(private val nodeDao: NodeDao) {
    fun observeNodes(): Flow<List<Node>> = nodeDao.observeAll()

    suspend fun getNode(id: Long): Node? = nodeDao.getById(id)

    suspend fun upsert(node: Node): Long = nodeDao.upsert(node)

    suspend fun upsertAll(nodes: List<Node>) = nodeDao.upsertAll(nodes)

    suspend fun delete(id: Long) = nodeDao.deleteById(id)

    suspend fun updateLatency(id: Long, latencyMs: Long?, testedAt: Long) =
        nodeDao.updateLatency(id, latencyMs, testedAt)
}

package com.eternalvpn.app.data.db

import androidx.room.*
import com.eternalvpn.app.data.model.Node
import kotlinx.coroutines.flow.Flow

@Dao
interface NodeDao {
    @Query("SELECT * FROM nodes ORDER BY isFavorite DESC, latencyMs IS NULL, latencyMs ASC, updatedAt DESC")
    fun observeAll(): Flow<List<Node>>

    @Query("SELECT * FROM nodes WHERE id = :id")
    suspend fun getById(id: Long): Node?

    @Query("SELECT * FROM nodes WHERE subscriptionId = :subId")
    suspend fun getBySubscription(subId: Long): List<Node>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(node: Node): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(nodes: List<Node>): List<Long>

    @Update
    suspend fun update(node: Node)

    @Query("DELETE FROM nodes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM nodes WHERE subscriptionId = :subId")
    suspend fun deleteBySubscription(subId: Long)

    @Query("UPDATE nodes SET latencyMs = :latencyMs, lastTestAt = :testedAt WHERE id = :id")
    suspend fun updateLatency(id: Long, latencyMs: Long?, testedAt: Long)
}

package com.eternalvpn.app.data.db

import androidx.room.*
import com.eternalvpn.app.data.model.ScanResult
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanResultDao {
    @Query("SELECT * FROM scan_results ORDER BY stage2Success DESC, stage2LatencyMs IS NULL, stage2LatencyMs ASC, stage1LatencyMs IS NULL, stage1LatencyMs ASC")
    fun observeAll(): Flow<List<ScanResult>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(results: List<ScanResult>)

    @Query("DELETE FROM scan_results")
    suspend fun clear()
}

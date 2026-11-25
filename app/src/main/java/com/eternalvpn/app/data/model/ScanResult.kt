package com.eternalvpn.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_results")
data class ScanResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ip: String,
    val port: Int = 443,
    val stage1LatencyMs: Long? = null,
    val stage2LatencyMs: Long? = null,
    val stage2Success: Boolean = false,
    val sni: String? = null,
    val scannedAt: Long = System.currentTimeMillis(),
)

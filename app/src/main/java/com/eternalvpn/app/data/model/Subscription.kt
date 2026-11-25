package com.eternalvpn.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String,
    val lastUpdatedAt: Long? = null,
    val autoUpdate: Boolean = false,
    val updateIntervalHours: Int = 24,
    val lastError: String? = null,
    val enabled: Boolean = true,
)

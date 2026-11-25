package com.eternalvpn.app.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    private object Keys {
        val SELECTED_NODE_ID = longPreferencesKey("selected_node_id")
        val IRAN_ROUTING_ENABLED = booleanPreferencesKey("iran_routing_enabled")
        val PER_APP_MODE = stringPreferencesKey("per_app_mode") // DISALLOW or ALLOW
        val PER_APP_PACKAGES = stringSetPreferencesKey("per_app_packages")
        val DNS_REMOTE = stringPreferencesKey("dns_remote")
        val DNS_DIRECT = stringPreferencesKey("dns_direct")
        val AUTO_UPDATE_SUBS = booleanPreferencesKey("auto_update_subs")
        val SCAN_CONCURRENCY = intPreferencesKey("scan_concurrency")
        val SCAN_TIMEOUT_MS = intPreferencesKey("scan_timeout_ms")
        val SCAN_SNI = stringPreferencesKey("scan_sni")
        val SCAN_PORT = intPreferencesKey("scan_port")
    }

    val selectedNodeId: Flow<Long?> = context.dataStore.data.map { it[Keys.SELECTED_NODE_ID] }
    val iranRoutingEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.IRAN_ROUTING_ENABLED] ?: true }
    val perAppMode: Flow<String> = context.dataStore.data.map { it[Keys.PER_APP_MODE] ?: "DISALLOW" }
    val perAppPackages: Flow<Set<String>> = context.dataStore.data.map { it[Keys.PER_APP_PACKAGES] ?: emptySet() }
    val dnsRemote: Flow<String> = context.dataStore.data.map { it[Keys.DNS_REMOTE] ?: "https://1.1.1.1/dns-query" }
    val dnsDirect: Flow<String> = context.dataStore.data.map { it[Keys.DNS_DIRECT] ?: "local" }
    val scanConcurrency: Flow<Int> = context.dataStore.data.map { it[Keys.SCAN_CONCURRENCY] ?: 200 }
    val scanTimeoutMs: Flow<Int> = context.dataStore.data.map { it[Keys.SCAN_TIMEOUT_MS] ?: 3000 }
    val scanSni: Flow<String> = context.dataStore.data.map { it[Keys.SCAN_SNI] ?: "www.cloudflare.com" }
    val scanPort: Flow<Int> = context.dataStore.data.map { it[Keys.SCAN_PORT] ?: 443 }

    suspend fun setSelectedNodeId(id: Long?) = context.dataStore.edit {
        if (id == null) it.remove(Keys.SELECTED_NODE_ID) else it[Keys.SELECTED_NODE_ID] = id
    }

    suspend fun setIranRoutingEnabled(enabled: Boolean) = context.dataStore.edit {
        it[Keys.IRAN_ROUTING_ENABLED] = enabled
    }

    suspend fun setPerAppMode(mode: String) = context.dataStore.edit { it[Keys.PER_APP_MODE] = mode }
    suspend fun setPerAppPackages(pkgs: Set<String>) = context.dataStore.edit { it[Keys.PER_APP_PACKAGES] = pkgs }

    suspend fun setDns(remote: String, direct: String) = context.dataStore.edit {
        it[Keys.DNS_REMOTE] = remote
        it[Keys.DNS_DIRECT] = direct
    }

    suspend fun setScanConfig(concurrency: Int, timeoutMs: Int, sni: String, port: Int) = context.dataStore.edit {
        it[Keys.SCAN_CONCURRENCY] = concurrency
        it[Keys.SCAN_TIMEOUT_MS] = timeoutMs
        it[Keys.SCAN_SNI] = sni
        it[Keys.SCAN_PORT] = port
    }
}

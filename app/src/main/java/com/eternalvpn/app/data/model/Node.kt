package com.eternalvpn.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single VPN node/proxy configuration.
 * [rawLink] preserves the original URI for round-tripping; [configJson] is the parsed
 * normalized form used by the config builder.
 */
@Entity(tableName = "nodes")
data class Node(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val protocol: Protocol,
    val address: String,
    val port: Int,
    val uuid: String? = null,            // vmess/vless
    val password: String? = null,        // trojan/ss
    val encryption: String? = null,      // ss method or vmess/vless encryption
    val network: Transport = Transport.TCP,
    val security: Security = Security.NONE,
    val sni: String? = null,
    val host: String? = null,
    val path: String? = null,
    val serviceName: String? = null,     // gRPC
    val xhttpMode: String? = null,       // XHTTP
    val alpn: String? = null,
    val fingerprint: String? = null,
    val publicKey: String? = null,       // REALITY
    val shortId: String? = null,         // REALITY
    val spiderX: String? = null,         // REALITY
    val flow: String? = null,            // VLESS flow
    val allowInsecure: Boolean = false,
    val rawLink: String = "",
    val subscriptionId: Long? = null,
    val latencyMs: Long? = null,
    val lastTestAt: Long? = null,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

enum class Protocol { VLESS, VMESS, TROJAN, SHADOWSOCKS }

enum class Transport { TCP, WS, GRPC, XHTTP, HTTPUPGRADE }

enum class Security { NONE, TLS, REALITY }

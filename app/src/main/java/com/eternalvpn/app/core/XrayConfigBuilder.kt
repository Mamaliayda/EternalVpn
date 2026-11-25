package com.eternalvpn.app.core

import com.eternalvpn.app.data.model.Node
import com.eternalvpn.app.data.model.Protocol
import com.eternalvpn.app.data.model.Security
import com.eternalvpn.app.data.model.Transport
import kotlinx.serialization.json.*

/**
 * Builds Xray-core JSON config for AndroidLibXrayLite.
 *
 * TunFd mode: include a "tun" inbound; libv2ray sets xray.tun.fd env when StartLoop receives tunFd.
 * A local SOCKS inbound is also included for debug/testing.
 */
object XrayConfigBuilder {

    data class RoutingOptions(
        val iranDirect: Boolean = true,
        val blockAds: Boolean = false,
    )

    data class DnsOptions(
        val remoteDns: String = "https://1.1.1.1/dns-query",
        val directDns: String = "223.5.5.5",
        val bootstrapDns: String = "1.1.1.1",
    )

    fun build(
        node: Node,
        socksPort: Int = 10808,
        tunFdMode: Boolean = true,
        mtu: Int = 1500,
        routing: RoutingOptions = RoutingOptions(),
        dns: DnsOptions = DnsOptions(),
        logLevel: String = "warning",
    ): String {
        val config = buildJsonObject {
            put("log", buildJsonObject { put("loglevel", logLevel) })
            put("stats", buildJsonObject {})
            put("policy", policy())
            put("inbounds", buildJsonArray {
                add(socksInbound(socksPort))
                if (tunFdMode) add(tunInbound(mtu))
            })
            put("outbounds", buildJsonArray {
                add(proxyOutbound(node))
                add(directOutbound())
                add(blockOutbound())
                add(dnsOutbound())
            })
            put("routing", routing(routing))
            put("dns", dns(dns, routing))
        }
        return config.toString()
    }

    private fun policy(): JsonObject = buildJsonObject {
        put("levels", buildJsonObject {
            put("8", buildJsonObject {
                put("handshake", 4)
                put("connIdle", 300)
                put("uplinkOnly", 1)
                put("downlinkOnly", 1)
            })
        })
        put("system", buildJsonObject {
            put("statsOutboundUplink", true)
            put("statsOutboundDownlink", true)
        })
    }

    private fun socksInbound(port: Int): JsonObject = buildJsonObject {
        put("tag", "socks")
        put("listen", "127.0.0.1")
        put("port", port)
        put("protocol", "socks")
        put("settings", buildJsonObject {
            put("auth", "noauth")
            put("udp", true)
            put("userLevel", 8)
        })
        put("sniffing", sniffing())
    }

    private fun tunInbound(mtu: Int): JsonObject = buildJsonObject {
        put("tag", "tun")
        put("protocol", "tun")
        put("settings", buildJsonObject {
            put("name", "xray0")
            put("MTU", mtu)
            put("userLevel", 8)
        })
        put("sniffing", sniffing())
    }

    private fun sniffing(): JsonObject = buildJsonObject {
        put("enabled", true)
        put("destOverride", buildJsonArray {
            add("http"); add("tls"); add("quic"); add("fakedns")
        })
        put("routeOnly", false)
    }

    private fun proxyOutbound(node: Node): JsonObject = buildJsonObject {
        put("tag", "proxy")
        put("protocol", protocolName(node.protocol))
        put("settings", outboundSettings(node))
        put("streamSettings", streamSettings(node))
        put("mux", buildJsonObject { put("enabled", false); put("concurrency", -1) })
    }

    private fun protocolName(p: Protocol): String = when (p) {
        Protocol.VLESS -> "vless"
        Protocol.VMESS -> "vmess"
        Protocol.TROJAN -> "trojan"
        Protocol.SHADOWSOCKS -> "shadowsocks"
    }

    private fun outboundSettings(node: Node): JsonObject = when (node.protocol) {
        Protocol.VLESS -> buildJsonObject {
            put("vnext", buildJsonArray {
                add(buildJsonObject {
                    put("address", node.address)
                    put("port", node.port)
                    put("users", buildJsonArray {
                        add(buildJsonObject {
                            put("id", node.uuid.orEmpty())
                            put("encryption", node.encryption?.ifBlank { "none" } ?: "none")
                            node.flow?.takeIf { it.isNotBlank() }?.let { put("flow", it) }
                            put("level", 8)
                        })
                    })
                })
            })
        }
        Protocol.VMESS -> buildJsonObject {
            put("vnext", buildJsonArray {
                add(buildJsonObject {
                    put("address", node.address)
                    put("port", node.port)
                    put("users", buildJsonArray {
                        add(buildJsonObject {
                            put("id", node.uuid.orEmpty())
                            put("alterId", 0)
                            put("security", node.encryption?.ifBlank { "auto" } ?: "auto")
                            put("level", 8)
                        })
                    })
                })
            })
        }
        Protocol.TROJAN -> buildJsonObject {
            put("servers", buildJsonArray {
                add(buildJsonObject {
                    put("address", node.address)
                    put("port", node.port)
                    put("password", node.password.orEmpty())
                    put("level", 8)
                })
            })
        }
        Protocol.SHADOWSOCKS -> buildJsonObject {
            put("servers", buildJsonArray {
                add(buildJsonObject {
                    put("address", node.address)
                    put("port", node.port)
                    put("method", node.encryption?.ifBlank { "aes-256-gcm" } ?: "aes-256-gcm")
                    put("password", node.password.orEmpty())
                    put("level", 8)
                })
            })
        }
    }

    private fun streamSettings(node: Node): JsonObject = buildJsonObject {
        put("network", transportName(node.network))
        put("security", securityName(node.security))
        when (node.network) {
            Transport.TCP -> { /* no extra */ }
            Transport.WS -> put("wsSettings", wsSettings(node))
            Transport.GRPC -> put("grpcSettings", grpcSettings(node))
            Transport.XHTTP -> put("xhttpSettings", xhttpSettings(node))
            Transport.HTTPUPGRADE -> put("httpupgradeSettings", httpUpgradeSettings(node))
        }
        when (node.security) {
            Security.TLS -> put("tlsSettings", tlsSettings(node))
            Security.REALITY -> put("realitySettings", realitySettings(node))
            Security.NONE -> {}
        }
        put("sockopt", buildJsonObject {
            put("domainStrategy", "UseIP")
        })
    }

    private fun transportName(t: Transport): String = when (t) {
        Transport.TCP -> "tcp"
        Transport.WS -> "ws"
        Transport.GRPC -> "grpc"
        Transport.XHTTP -> "xhttp"
        Transport.HTTPUPGRADE -> "httpupgrade"
    }

    private fun securityName(s: Security): String = when (s) {
        Security.NONE -> "none"
        Security.TLS -> "tls"
        Security.REALITY -> "reality"
    }

    private fun wsSettings(node: Node): JsonObject = buildJsonObject {
        put("path", node.path?.ifBlank { "/" } ?: "/")
        val host = node.host ?: node.sni
        if (!host.isNullOrBlank()) {
            put("headers", buildJsonObject { put("Host", host) })
        }
    }

    private fun grpcSettings(node: Node): JsonObject = buildJsonObject {
        put("serviceName", node.serviceName ?: node.path?.trim('/') ?: "")
        put("multiMode", false)
    }

    private fun xhttpSettings(node: Node): JsonObject = buildJsonObject {
        put("path", node.path?.ifBlank { "/" } ?: "/")
        node.host?.takeIf { it.isNotBlank() }?.let { put("host", it) }
        put("mode", node.xhttpMode?.ifBlank { "auto" } ?: "auto")
    }

    private fun httpUpgradeSettings(node: Node): JsonObject = buildJsonObject {
        put("path", node.path?.ifBlank { "/" } ?: "/")
        node.host?.takeIf { it.isNotBlank() }?.let { put("host", it) }
    }

    private fun tlsSettings(node: Node): JsonObject = buildJsonObject {
        node.sni?.takeIf { it.isNotBlank() }?.let { put("serverName", it) }
        put("allowInsecure", node.allowInsecure)
        val alpnList = node.alpn?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
        if (alpnList.isNotEmpty()) {
            put("alpn", buildJsonArray { alpnList.forEach { add(it) } })
        }
        node.fingerprint?.takeIf { it.isNotBlank() }?.let { put("fingerprint", it) }
    }

    private fun realitySettings(node: Node): JsonObject = buildJsonObject {
        node.sni?.takeIf { it.isNotBlank() }?.let { put("serverName", it) }
        node.fingerprint?.takeIf { it.isNotBlank() }?.let { put("fingerprint", it) }
        node.publicKey?.takeIf { it.isNotBlank() }?.let { put("publicKey", it) }
        node.shortId?.takeIf { it.isNotBlank() }?.let { put("shortId", it) }
        node.spiderX?.takeIf { it.isNotBlank() }?.let { put("spiderX", it) }
    }

    private fun directOutbound(): JsonObject = buildJsonObject {
        put("tag", "direct")
        put("protocol", "freedom")
        put("settings", buildJsonObject {})
    }

    private fun blockOutbound(): JsonObject = buildJsonObject {
        put("tag", "block")
        put("protocol", "blackhole")
        put("settings", buildJsonObject {
            put("response", buildJsonObject { put("type", "http") })
        })
    }

    private fun dnsOutbound(): JsonObject = buildJsonObject {
        put("tag", "dns-out")
        put("protocol", "dns")
        put("settings", buildJsonObject {})
    }

    private fun routing(opts: RoutingOptions): JsonObject = buildJsonObject {
        put("domainStrategy", "AsIs")
        put("rules", buildJsonArray {
            // DNS hijack
            add(buildJsonObject {
                put("type", "field")
                put("inboundTag", buildJsonArray { add("tun"); add("socks") })
                put("port", "53")
                put("outboundTag", "dns-out")
            })
            // loopback safety
            add(buildJsonObject {
                put("type", "field")
                put("ip", buildJsonArray { add("127.0.0.1/32"); add("::1/128") })
                put("outboundTag", "direct")
            })
            if (opts.iranDirect) {
                add(buildJsonObject {
                    put("type", "field")
                    put("domain", buildJsonArray { add("geosite:category-ir"); add("geosite:private") })
                    put("outboundTag", "direct")
                })
                add(buildJsonObject {
                    put("type", "field")
                    put("ip", buildJsonArray { add("geoip:ir"); add("geoip:private") })
                    put("outboundTag", "direct")
                })
            }
            add(buildJsonObject {
                put("type", "field")
                put("network", "tcp,udp")
                put("outboundTag", "proxy")
            })
        })
    }

    private fun dns(opts: DnsOptions, routing: RoutingOptions): JsonObject = buildJsonObject {
        put("hosts", buildJsonObject {
            put("domain:googleapis.cn", "googleapis.com")
        })
        put("servers", buildJsonArray {
            if (routing.iranDirect) {
                add(buildJsonObject {
                    put("address", opts.directDns)
                    put("domains", buildJsonArray { add("geosite:category-ir"); add("geosite:private") })
                    put("expectIPs", buildJsonArray { add("geoip:ir") })
                })
            }
            add(opts.remoteDns)
            add(opts.bootstrapDns)
        })
    }
}

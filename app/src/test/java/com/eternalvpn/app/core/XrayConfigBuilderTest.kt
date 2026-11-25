package com.eternalvpn.app.core

import com.eternalvpn.app.data.model.Node
import com.eternalvpn.app.data.model.Protocol
import com.eternalvpn.app.data.model.Security
import com.eternalvpn.app.data.model.Transport
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class XrayConfigBuilderTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun build(node: Node): JsonObject =
        json.parseToJsonElement(XrayConfigBuilder.build(node)).jsonObject

    private fun sampleNode(
        protocol: Protocol,
        transport: Transport = Transport.TCP,
        security: Security = Security.TLS,
    ): Node = Node(
        name = "test-${protocol.name}",
        protocol = protocol,
        address = "example.com",
        port = 443,
        uuid = "11111111-1111-1111-1111-111111111111",
        password = "secret-pass",
        encryption = when (protocol) {
            Protocol.SHADOWSOCKS -> "aes-256-gcm"
            Protocol.VMESS -> "auto"
            else -> "none"
        },
        network = transport,
        security = security,
        sni = "example.com",
        host = "example.com",
        path = "/ws",
        serviceName = "grpcsvc",
        xhttpMode = "auto",
        alpn = "h2,http/1.1",
        fingerprint = "chrome",
        publicKey = "PUBKEY123",
        shortId = "abcd",
        spiderX = "/",
        flow = if (protocol == Protocol.VLESS && security == Security.REALITY) "xtls-rprx-vision" else null,
    )

    @Test
    fun `build produces required top-level sections`() {
        val root = build(sampleNode(Protocol.VLESS))
        assertTrue(root.containsKey("log"))
        assertTrue(root.containsKey("inbounds"))
        assertTrue(root.containsKey("outbounds"))
        assertTrue(root.containsKey("routing"))
        assertTrue(root.containsKey("dns"))
        assertTrue(root.containsKey("policy"))
    }

    @Test
    fun `tunFd mode includes tun and socks inbounds`() {
        val root = build(sampleNode(Protocol.VLESS))
        val inbounds = root["inbounds"]!!.jsonArray
        val tags = inbounds.map { it.jsonObject["tag"]!!.jsonPrimitive.content }
        assertTrue(tags.contains("socks"))
        assertTrue(tags.contains("tun"))
        val tun = inbounds.first { it.jsonObject["tag"]!!.jsonPrimitive.content == "tun" }.jsonObject
        assertEquals("tun", tun["protocol"]!!.jsonPrimitive.content)
    }

    @Test
    fun `vless tcp tls outbound`() {
        val root = build(sampleNode(Protocol.VLESS, Transport.TCP, Security.TLS))
        val proxy = proxy(root)
        assertEquals("vless", proxy["protocol"]!!.jsonPrimitive.content)
        val user = proxy["settings"]!!.jsonObject["vnext"]!!.jsonArray[0].jsonObject["users"]!!.jsonArray[0].jsonObject
        assertEquals("11111111-1111-1111-1111-111111111111", user["id"]!!.jsonPrimitive.content)
        assertEquals("none", user["encryption"]!!.jsonPrimitive.content)
        val stream = proxy["streamSettings"]!!.jsonObject
        assertEquals("tcp", stream["network"]!!.jsonPrimitive.content)
        assertEquals("tls", stream["security"]!!.jsonPrimitive.content)
        assertEquals("example.com", stream["tlsSettings"]!!.jsonObject["serverName"]!!.jsonPrimitive.content)
    }

    @Test
    fun `vless ws tls outbound`() {
        val root = build(sampleNode(Protocol.VLESS, Transport.WS, Security.TLS))
        val stream = proxy(root)["streamSettings"]!!.jsonObject
        assertEquals("ws", stream["network"]!!.jsonPrimitive.content)
        assertEquals("/ws", stream["wsSettings"]!!.jsonObject["path"]!!.jsonPrimitive.content)
    }

    @Test
    fun `vless grpc reality outbound`() {
        val node = sampleNode(Protocol.VLESS, Transport.GRPC, Security.REALITY)
        val root = build(node)
        val stream = proxy(root)["streamSettings"]!!.jsonObject
        assertEquals("grpc", stream["network"]!!.jsonPrimitive.content)
        assertEquals("grpcsvc", stream["grpcSettings"]!!.jsonObject["serviceName"]!!.jsonPrimitive.content)
        assertEquals("reality", stream["security"]!!.jsonPrimitive.content)
        val reality = stream["realitySettings"]!!.jsonObject
        assertEquals("PUBKEY123", reality["publicKey"]!!.jsonPrimitive.content)
        assertEquals("abcd", reality["shortId"]!!.jsonPrimitive.content)
        val user = proxy(root)["settings"]!!.jsonObject["vnext"]!!.jsonArray[0].jsonObject["users"]!!.jsonArray[0].jsonObject
        assertEquals("xtls-rprx-vision", user["flow"]!!.jsonPrimitive.content)
    }

    @Test
    fun `vless xhttp tls outbound`() {
        val root = build(sampleNode(Protocol.VLESS, Transport.XHTTP, Security.TLS))
        val stream = proxy(root)["streamSettings"]!!.jsonObject
        assertEquals("xhttp", stream["network"]!!.jsonPrimitive.content)
        assertEquals("/ws", stream["xhttpSettings"]!!.jsonObject["path"]!!.jsonPrimitive.content)
        assertEquals("auto", stream["xhttpSettings"]!!.jsonObject["mode"]!!.jsonPrimitive.content)
    }

    @Test
    fun `vmess ws tls outbound`() {
        val root = build(sampleNode(Protocol.VMESS, Transport.WS, Security.TLS))
        val proxy = proxy(root)
        assertEquals("vmess", proxy["protocol"]!!.jsonPrimitive.content)
        val user = proxy["settings"]!!.jsonObject["vnext"]!!.jsonArray[0].jsonObject["users"]!!.jsonArray[0].jsonObject
        assertEquals("auto", user["security"]!!.jsonPrimitive.content)
        assertEquals(0, user["alterId"]!!.jsonPrimitive.int)
    }

    @Test
    fun `trojan tcp tls outbound`() {
        val root = build(sampleNode(Protocol.TROJAN, Transport.TCP, Security.TLS))
        val proxy = proxy(root)
        assertEquals("trojan", proxy["protocol"]!!.jsonPrimitive.content)
        val server = proxy["settings"]!!.jsonObject["servers"]!!.jsonArray[0].jsonObject
        assertEquals("secret-pass", server["password"]!!.jsonPrimitive.content)
        assertEquals("example.com", server["address"]!!.jsonPrimitive.content)
    }

    @Test
    fun `shadowsocks outbound`() {
        val root = build(sampleNode(Protocol.SHADOWSOCKS, Transport.TCP, Security.NONE))
        val proxy = proxy(root)
        assertEquals("shadowsocks", proxy["protocol"]!!.jsonPrimitive.content)
        val server = proxy["settings"]!!.jsonObject["servers"]!!.jsonArray[0].jsonObject
        assertEquals("aes-256-gcm", server["method"]!!.jsonPrimitive.content)
        assertEquals("secret-pass", server["password"]!!.jsonPrimitive.content)
    }

    @Test
    fun `routing includes iran direct rules when enabled`() {
        val root = build(sampleNode(Protocol.VLESS))
        val rules = root["routing"]!!.jsonObject["rules"]!!.jsonArray
        val hasIr = rules.any { rule ->
            rule.jsonObject["domain"]?.jsonArray?.any { it.jsonPrimitive.content == "geosite:category-ir" } == true
        }
        assertTrue(hasIr)
    }

    @Test
    fun `dns has remote and direct servers`() {
        val root = build(sampleNode(Protocol.VLESS))
        val servers = root["dns"]!!.jsonObject["servers"]!!.jsonArray
        assertTrue(servers.size >= 2)
    }

    private fun proxy(root: JsonObject): JsonObject =
        root["outbounds"]!!.jsonArray.first { it.jsonObject["tag"]!!.jsonPrimitive.content == "proxy" }.jsonObject
}

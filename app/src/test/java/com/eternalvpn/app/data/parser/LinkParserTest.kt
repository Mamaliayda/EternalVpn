package com.eternalvpn.app.data.parser

import com.eternalvpn.app.data.model.Protocol
import com.eternalvpn.app.data.model.Security
import com.eternalvpn.app.data.model.Transport
import org.junit.Assert.*
import org.junit.Test
import java.util.Base64

class LinkParserTest {

    @Test
    fun `parse vless tcp tls`() {
        val link = "vless://11111111-1111-1111-1111-111111111111@example.com:443?type=tcp&security=tls&sni=example.com&fp=chrome#MyVless"
        val node = LinkParser.parse(link)
        assertNotNull(node)
        node!!
        assertEquals(Protocol.VLESS, node.protocol)
        assertEquals("example.com", node.address)
        assertEquals(443, node.port)
        assertEquals("11111111-1111-1111-1111-111111111111", node.uuid)
        assertEquals(Transport.TCP, node.network)
        assertEquals(Security.TLS, node.security)
        assertEquals("example.com", node.sni)
        assertEquals("chrome", node.fingerprint)
        assertEquals("MyVless", node.name)
    }

    @Test
    fun `parse vless reality grpc`() {
        val link = "vless://uuid-here@1.2.3.4:443?type=grpc&security=reality&sni=www.yahoo.com&fp=chrome&pbk=PUBKEY&sid=ABCD&spx=%2F&serviceName=grpcsvc#RealityNode"
        val node = LinkParser.parse(link)
        assertNotNull(node)
        node!!
        assertEquals(Protocol.VLESS, node.protocol)
        assertEquals(Transport.GRPC, node.network)
        assertEquals(Security.REALITY, node.security)
        assertEquals("PUBKEY", node.publicKey)
        assertEquals("ABCD", node.shortId)
        assertEquals("grpcsvc", node.serviceName)
        assertEquals("/", node.spiderX)
    }

    @Test
    fun `parse vmess ws tls`() {
        val json = """
            {"v":"2","ps":"vmess-ws","add":"example.org","port":"443","id":"22222222-2222-2222-2222-222222222222","aid":"0","scy":"auto","net":"ws","type":"none","host":"example.org","path":"/ws","tls":"tls","sni":"example.org"}
        """.trimIndent()
        val b64 = Base64.getEncoder().encodeToString(json.toByteArray())
        val node = LinkParser.parse("vmess://$b64")
        assertNotNull(node)
        node!!
        assertEquals(Protocol.VMESS, node.protocol)
        assertEquals("vmess-ws", node.name)
        assertEquals("example.org", node.address)
        assertEquals(443, node.port)
        assertEquals("22222222-2222-2222-2222-222222222222", node.uuid)
        assertEquals(Transport.WS, node.network)
        assertEquals(Security.TLS, node.security)
        assertEquals("/ws", node.path)
    }

    @Test
    fun `parse trojan tls`() {
        val link = "trojan://mypassword@trojan.example.com:443?security=tls&sni=trojan.example.com&type=tcp#TrojanNode"
        val node = LinkParser.parse(link)
        assertNotNull(node)
        node!!
        assertEquals(Protocol.TROJAN, node.protocol)
        assertEquals("trojan.example.com", node.address)
        assertEquals(443, node.port)
        assertEquals("mypassword", node.password)
        assertEquals(Security.TLS, node.security)
        assertEquals("TrojanNode", node.name)
    }

    @Test
    fun `parse shadowsocks sip002`() {
        val userInfo = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("aes-256-gcm:pass123".toByteArray())
        val link = "ss://$userInfo@ss.example.com:8388#SSNode"
        val node = LinkParser.parse(link)
        assertNotNull(node)
        node!!
        assertEquals(Protocol.SHADOWSOCKS, node.protocol)
        assertEquals("ss.example.com", node.address)
        assertEquals(8388, node.port)
        assertEquals("aes-256-gcm", node.encryption)
        assertEquals("pass123", node.password)
        assertEquals("SSNode", node.name)
    }

    @Test
    fun `parse shadowsocks legacy full base64`() {
        val full = Base64.getEncoder().encodeToString("chacha20-ietf-poly1305:secret@1.2.3.4:9999".toByteArray())
        val node = LinkParser.parse("ss://$full#Legacy")
        assertNotNull(node)
        node!!
        assertEquals("1.2.3.4", node.address)
        assertEquals(9999, node.port)
        assertEquals("chacha20-ietf-poly1305", node.encryption)
        assertEquals("secret", node.password)
    }

    @Test
    fun `parse subscription base64`() {
        val links = """
            vless://11111111-1111-1111-1111-111111111111@a.com:443?security=tls&type=tcp#A
            trojan://pw@b.com:443?security=tls&type=tcp#B
        """.trimIndent()
        val body = Base64.getEncoder().encodeToString(links.toByteArray())
        val nodes = LinkParser.parseSubscription(body)
        assertEquals(2, nodes.size)
        assertEquals(Protocol.VLESS, nodes[0].protocol)
        assertEquals(Protocol.TROJAN, nodes[1].protocol)
    }

    @Test
    fun `parse invalid returns null`() {
        assertNull(LinkParser.parse("https://example.com"))
        assertNull(LinkParser.parse("garbage"))
    }
}

package com.eternalvpn.app.data.parser

import com.eternalvpn.app.data.model.Node
import com.eternalvpn.app.data.model.Protocol
import com.eternalvpn.app.data.model.Security
import com.eternalvpn.app.data.model.Transport
import kotlinx.serialization.json.*
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object LinkParser {

    fun parse(link: String): Node? {
        val trimmed = link.trim()
        return when {
            trimmed.startsWith("vless://", ignoreCase = true) -> parseVless(trimmed)
            trimmed.startsWith("vmess://", ignoreCase = true) -> parseVmess(trimmed)
            trimmed.startsWith("trojan://", ignoreCase = true) -> parseTrojan(trimmed)
            trimmed.startsWith("ss://", ignoreCase = true) -> parseShadowsocks(trimmed)
            else -> null
        }
    }

    fun parseLines(text: String): List<Node> =
        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapNotNull { parse(it) }
            .toList()

    /** Decode a subscription body: base64 (URL-safe or standard) of newline-separated links, or plain text. */
    fun parseSubscription(body: String): List<Node> {
        val trimmed = body.trim()
        val decoded = tryDecodeBase64(trimmed) ?: trimmed
        return parseLines(decoded)
    }

    private fun parseVless(link: String): Node? = runCatching {
        val uri = URI(link)
        val params = queryParams(uri.rawQuery)
        Node(
            name = fragment(uri) ?: "VLESS ${uri.host}",
            protocol = Protocol.VLESS,
            address = uri.host ?: return null,
            port = uri.port.takeIf { it > 0 } ?: 443,
            uuid = uri.userInfo?.substringBefore(':'),
            network = parseTransport(params["type"]),
            security = parseSecurity(params["security"]),
            sni = params["sni"] ?: params["host"],
            host = params["host"],
            path = params["path"],
            serviceName = params["serviceName"],
            xhttpMode = params["mode"],
            alpn = params["alpn"],
            fingerprint = params["fp"],
            publicKey = params["pbk"],
            shortId = params["sid"],
            spiderX = params["spx"],
            flow = params["flow"],
            allowInsecure = params["allowInsecure"] == "1",
            rawLink = link,
        )
    }.getOrNull()

    private fun parseVmess(link: String): Node? = runCatching {
        val payload = link.removePrefix("vmess://").removePrefix("VMESS://").trim()
        val jsonStr = decodeBase64(payload) ?: return null
        val obj = Json.parseToJsonElement(jsonStr).jsonObject
        fun str(key: String) = obj[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
        fun int(key: String) = obj[key]?.jsonPrimitive?.intOrNull
            ?: obj[key]?.jsonPrimitive?.contentOrNull?.toIntOrNull()

        val net = parseTransport(str("net"))
        val tls = str("tls")?.lowercase()
        val security = when (tls) {
            "tls" -> Security.TLS
            "reality" -> Security.REALITY
            else -> Security.NONE
        }
        Node(
            name = str("ps") ?: "VMess ${str("add")}",
            protocol = Protocol.VMESS,
            address = str("add") ?: return null,
            port = int("port") ?: 443,
            uuid = str("id"),
            encryption = str("scy") ?: "auto",
            network = net,
            security = security,
            sni = str("sni") ?: str("host"),
            host = str("host"),
            path = str("path"),
            serviceName = str("path").takeIf { net == Transport.GRPC },
            alpn = str("alpn"),
            fingerprint = str("fp"),
            publicKey = str("pbk"),
            shortId = str("sid"),
            spiderX = str("spx"),
            allowInsecure = str("allowInsecure") == "1" || str("allowInsecure") == "true",
            rawLink = link,
        )
    }.getOrNull()

    private fun parseTrojan(link: String): Node? = runCatching {
        val uri = URI(link)
        val params = queryParams(uri.rawQuery)
        Node(
            name = fragment(uri) ?: "Trojan ${uri.host}",
            protocol = Protocol.TROJAN,
            address = uri.host ?: return null,
            port = uri.port.takeIf { it > 0 } ?: 443,
            password = uri.userInfo?.let { urlDecode(it) },
            network = parseTransport(params["type"]),
            security = parseSecurity(params["security"]).let { if (it == Security.NONE) Security.TLS else it },
            sni = params["sni"] ?: params["host"],
            host = params["host"],
            path = params["path"],
            serviceName = params["serviceName"],
            alpn = params["alpn"],
            fingerprint = params["fp"],
            publicKey = params["pbk"],
            shortId = params["sid"],
            spiderX = params["spx"],
            allowInsecure = params["allowInsecure"] == "1",
            rawLink = link,
        )
    }.getOrNull()

    private fun parseShadowsocks(link: String): Node? = runCatching {
        // ss://BASE64(method:password)@host:port#name  OR  ss://BASE64(method:password@host:port)#name
        val raw = link.removePrefix("ss://").removePrefix("SS://")
        val fragmentName = raw.substringAfter('#', "").let { urlDecode(it) }.ifBlank { null }
        val withoutFragment = raw.substringBefore('#')
        val (main, query) = withoutFragment.split('?', limit = 2).let { it[0] to it.getOrNull(1) }
        val params = queryParams(query)

        val decoded: String
        val host: String
        val port: Int
        if (main.contains('@')) {
            val beforeAt = main.substringBeforeLast('@')
            val afterAt = main.substringAfterLast('@')
            decoded = decodeBase64(beforeAt) ?: urlDecode(beforeAt)
            host = afterAt.substringBefore(':').substringBefore('/')
            port = afterAt.substringAfter(':').substringBefore('/').substringBefore('?').toIntOrNull() ?: 8388
        } else {
            val full = decodeBase64(main) ?: return null
            // method:password@host:port
            val atIdx = full.lastIndexOf('@')
            if (atIdx < 0) return null
            decoded = full.substring(0, atIdx)
            val hostPort = full.substring(atIdx + 1)
            host = hostPort.substringBefore(':')
            port = hostPort.substringAfter(':').substringBefore('/').toIntOrNull() ?: 8388
        }

        val method = decoded.substringBefore(':')
        val password = decoded.substringAfter(':')
        Node(
            name = fragmentName ?: "SS $host",
            protocol = Protocol.SHADOWSOCKS,
            address = host,
            port = port,
            password = password,
            encryption = method,
            network = Transport.TCP,
            security = Security.NONE,
            rawLink = link,
        )
    }.getOrNull()

    private fun parseTransport(type: String?): Transport = when (type?.lowercase()) {
        "ws", "websocket" -> Transport.WS
        "grpc" -> Transport.GRPC
        "xhttp", "splithttp" -> Transport.XHTTP
        "httpupgrade" -> Transport.HTTPUPGRADE
        else -> Transport.TCP
    }

    private fun parseSecurity(sec: String?): Security = when (sec?.lowercase()) {
        "tls" -> Security.TLS
        "reality" -> Security.REALITY
        else -> Security.NONE
    }

    private fun queryParams(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split('&').mapNotNull { pair ->
            val idx = pair.indexOf('=')
            if (idx <= 0) null
            else urlDecode(pair.substring(0, idx)) to urlDecode(pair.substring(idx + 1))
        }.toMap()
    }

    private fun fragment(uri: URI): String? = uri.rawFragment?.let { urlDecode(it) }?.takeIf { it.isNotBlank() }

    private fun urlDecode(s: String): String = runCatching {
        URLDecoder.decode(s, StandardCharsets.UTF_8.name())
    }.getOrElse { s }

    private fun decodeBase64(s: String): String? = runCatching {
        val normalized = s.trim().replace('-', '+').replace('_', '/')
        val padded = normalized + "=".repeat((4 - normalized.length % 4) % 4)
        String(java.util.Base64.getDecoder().decode(padded), StandardCharsets.UTF_8)
    }.getOrNull()

    private fun tryDecodeBase64(s: String): String? {
        if (s.contains("://")) return null // already plain links
        return decodeBase64(s)?.takeIf { decoded -> decoded.contains("://") }
    }
}

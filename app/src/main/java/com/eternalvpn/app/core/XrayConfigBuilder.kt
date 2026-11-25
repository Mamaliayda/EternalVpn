package com.eternalvpn.app.core

import com.eternalvpn.app.data.model.Node

/**
 * Phase 2: builds an Xray JSON config from a selected [Node] + routing/DNS settings.
 * Returns a JSON string suitable for AndroidLibXrayLite.
 */
object XrayConfigBuilder {
    fun build(node: Node, iranRoutingEnabled: Boolean, dnsRemote: String, dnsDirect: String): String {
        // TODO(Phase 2): implement full Xray JSON generation.
        // Must include: local SOCKS inbound, node outbound, routing rules (geoip:ir, geosite:category-ir -> direct),
        // DNS (remote DoH for proxied, direct for IR), and sniffing.
        throw NotImplementedError("XrayConfigBuilder.build is implemented in Phase 2")
    }
}

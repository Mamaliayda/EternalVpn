package com.eternalvpn.app.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Phase 9: optionally restore last VPN state on boot (only if user enabled it).
 * Stub for now - no auto-connect without explicit user opt-in.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        // Intentionally no-op until Phase 9 adds an explicit "reconnect on boot" setting.
    }
}

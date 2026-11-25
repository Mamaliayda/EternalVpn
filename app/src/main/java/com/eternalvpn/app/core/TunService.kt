package com.eternalvpn.app.core

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.eternalvpn.app.EternalVpnApp
import com.eternalvpn.app.MainActivity
import com.eternalvpn.app.R

/**
 * Phase 2: owns the TUN interface and wires hev-socks5-tunnel to the local Xray SOCKS inbound.
 * This is a compile-safe skeleton; full VPN bring-up is implemented in Phase 2.
 */
class TunService : VpnService() {

    private var tunInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopVpn()
                return START_NOT_STICKY
            }
        }
        startForeground(NOTIFICATION_ID, buildNotification(getString(R.string.connecting)))
        // Phase 2: build Xray config, start core, establish TUN + tun2socks.
        return START_STICKY
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }

    private fun stopVpn() {
        runCatching { tunInterface?.close() }
        tunInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(content: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentMutability(),
        )
        val stopIntent = PendingIntent.getService(
            this, 1, Intent(this, TunService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentMutability(),
        )
        return NotificationCompat.Builder(this, EternalVpnApp.VPN_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_vpn_key)
            .setContentTitle(getString(R.string.vpn_notification_title))
            .setContentText(content)
            .setContentIntent(openIntent)
            .addAction(0, getString(R.string.action_stop), stopIntent)
            .setOngoing(true)
            .build()
    }

    private fun pendingIntentMutability(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

    companion object {
        const val ACTION_STOP = "com.eternalvpn.app.action.STOP"
        const val NOTIFICATION_ID = 1
    }
}

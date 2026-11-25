package com.eternalvpn.app.core

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.eternalvpn.app.EternalVpnApp
import com.eternalvpn.app.MainActivity
import com.eternalvpn.app.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class TunService : VpnService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var tunInterface: ParcelFileDescriptor? = null
    @Volatile private var starting = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopVpn("User requested stop")
                return START_NOT_STICKY
            }
        }
        if (XrayController.isRunning() || starting) {
            XrayController.appendLog("Start ignored: already running/starting")
            return START_STICKY
        }
        startForeground(NOTIFICATION_ID, buildNotification(getString(R.string.connecting)))
        starting = true
        scope.launch { startVpn() }
        return START_STICKY
    }

    private suspend fun startVpn() {
        val app = application as EternalVpnApp
        try {
            XrayController.ensureInitialized(this)
            val settings = app.settingsStore
            val selectedId = settings.selectedNodeId.first()
                ?: throw IllegalStateException("No node selected")
            val node = app.database.nodeDao().getById(selectedId)
                ?: throw IllegalStateException("Selected node not found")
            val iranRouting = settings.iranRoutingEnabled.first()
            val remoteDns = settings.dnsRemote.first()
            val directDns = settings.dnsDirect.first()

            val config = XrayConfigBuilder.build(
                node = node,
                socksPort = 10808,
                tunFdMode = true,
                mtu = 1500,
                routing = XrayConfigBuilder.RoutingOptions(iranDirect = iranRouting),
                dns = XrayConfigBuilder.DnsOptions(remoteDns = remoteDns, directDns = directDns),
                logLevel = "warning",
            )
            XrayController.appendLog("Config built for ${node.name} (${node.protocol})")

            val builder = Builder()
                .setSession(getString(R.string.app_name))
                .setMtu(1500)
                .addAddress("10.10.12.1", 24)
                .addDnsServer("10.10.12.2")
                .addRoute("0.0.0.0", 0)
                .setBlocking(true)

            // Try IPv6 route; ignore if unsupported on device/network.
            runCatching {
                builder.addAddress("fdfe:dcba:9876::1", 126)
                builder.addRoute("::", 0)
            }.onFailure { XrayController.appendLog("IPv6 TUN setup skipped: ${it.message}") }

            // Exclude own app to avoid routing loops.
            runCatching { builder.addDisallowedApplication(packageName) }
                .onFailure { XrayController.appendLog("addDisallowedApplication failed: ${it.message}") }

            val pfd = builder.establish()
                ?: throw IllegalStateException("VpnService.Builder.establish() returned null")
            tunInterface = pfd

            updateNotification(getString(R.string.connecting))
            val result = XrayController.start(config, pfd.fd)
            result.onSuccess {
                updateNotification(getString(R.string.vpn_notification_connected, node.name))
            }.onFailure { t ->
                throw t
            }
        } catch (t: Throwable) {
            Log.e(TAG, "startVpn failed", t)
            XrayController.appendLog("startVpn failed: ${t.message}")
            XrayController.stop()
            closeTun()
            updateNotification(getString(R.string.disconnected))
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } finally {
            starting = false
        }
    }

    private fun stopVpn(reason: String) {
        XrayController.appendLog("Stopping VPN: $reason")
        scope.launch {
            runCatching { XrayController.stop() }
            closeTun()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun closeTun() {
        runCatching { tunInterface?.close() }
        tunInterface = null
    }

    override fun onDestroy() {
        runCatching { XrayController.stop() }
        closeTun()
        scope.cancel()
        super.onDestroy()
    }

    override fun onRevoke() {
        stopVpn("VPN revoked")
        super.onRevoke()
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
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification(content: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(content))
    }

    private fun pendingIntentMutability(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

    companion object {
        const val ACTION_STOP = "com.eternalvpn.app.action.STOP"
        const val NOTIFICATION_ID = 1
        private const val TAG = "TunService"
    }
}

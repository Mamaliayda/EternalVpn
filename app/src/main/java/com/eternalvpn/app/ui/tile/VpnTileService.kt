package com.eternalvpn.app.ui.tile

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.eternalvpn.app.core.TunService

/**
 * Phase 9: Quick Settings tile toggling the VPN. Stubbed for Phase 0 skeleton.
 */
@RequiresApi(Build.VERSION_CODES.N)
class VpnTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        // Phase 9: toggle real VPN state. For now, just request start (skeleton).
        val intent = Intent(this, TunService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            updateTile()
        }
    }
}

package com.example.melectro.service

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.example.melectro.Daedalus
import com.example.melectro.R


@RequiresApi(api = Build.VERSION_CODES.N)
class DaedalusTileService : TileService() {
    override fun onClick() {
        val tile = qsTile
        tile.label = getString(R.string.quick_toggle)
        tile.contentDescription = getString(R.string.app_name)
        tile.state = if (Daedalus.switchService()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }

    override fun onStartListening() {
        updateTile()
    }

    private fun updateTile() {
        val activate: Boolean = com.example.melectro.service.DaedalusVpnService.isActivated
        val tile = qsTile
        tile.label = getString(R.string.quick_toggle)
        tile.contentDescription = getString(R.string.app_name)
        tile.state = if (activate) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
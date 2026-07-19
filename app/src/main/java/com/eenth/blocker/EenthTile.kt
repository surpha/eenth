package com.eenth.blocker

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class EenthTile : TileService() {

    override fun onStartListening() {
        updateTileState()
    }

    override fun onClick() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(intent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
        val isBricked = prefs.getBoolean(MainActivity.KEY_IS_BRICKED, false)
        tile.state = if (isBricked) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Block"
        tile.subtitle = if (isBricked) "Blocking" else "Off"
        tile.updateTile()
    }
}

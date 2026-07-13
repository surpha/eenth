package com.eenth.blocker

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.view.accessibility.AccessibilityManager

class EenthTile : TileService() {

    override fun onStartListening() {
        updateTileState()
    }

    override fun onClick() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
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
        val enabled = isEenthAccessibilityEnabled()
        tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Eenth"
        tile.subtitle = if (enabled) "On" else "Off"
        tile.updateTile()
    }

    private fun isEenthAccessibilityEnabled(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val services = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return services.any { it.resolveInfo.serviceInfo.packageName == packageName }
    }
}

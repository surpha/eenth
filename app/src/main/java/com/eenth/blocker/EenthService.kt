package com.eenth.blocker

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class EenthService : AccessibilityService() {

    companion object {
        const val ACTION_STATE_CHANGED = "com.eenth.blocker.ACTION_STATE_CHANGED"
    }

    private lateinit var prefs: SharedPreferences

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_STATE_CHANGED) {
                val isBricked = prefs.getBoolean(MainActivity.KEY_IS_BRICKED, false)
                Log.d("EenthService", "State changed. Bricked: $isBricked")
                if (!isBricked) {
                    // Send user home and dismiss the blocker activity
                    val closeIntent = Intent(BlockerActivity.ACTION_CLOSE_BLOCKER)
                    sendBroadcast(closeIntent)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
        val filter = IntentFilter(ACTION_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stateReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(stateReceiver, filter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(stateReceiver)
    }

    // System UI packages to ignore
    private val systemUiPackages = setOf(
        "com.android.systemui",
        "com.samsung.android.app.cocktailbarservice",
        "com.samsung.android.edge",
        "com.android.launcher",
        "com.google.android.apps.nexuslauncher",
        "com.sec.android.app.launcher"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            // Ignore our own app and system UI
            if (packageName == this.packageName) return
            if (packageName == "com.eenth.blocker") return
            if (systemUiPackages.contains(packageName)) return

            val isBricked = prefs.getBoolean(MainActivity.KEY_IS_BRICKED, false)
            if (!isBricked) return

            val blockedApps = prefs.getStringSet(MainActivity.KEY_BLOCKED_APPS, emptySet()) ?: emptySet()

            if (blockedApps.contains(packageName)) {
                Log.d("EenthService", "BLOCKING: $packageName")
                // Launch the blocker as a full-screen Activity
                val intent = Intent(this, BlockerActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
            }
        }
    }

    override fun onInterrupt() { }
}
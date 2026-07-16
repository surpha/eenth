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
                Log.d("BlockService", "State changed. Blocked: $isBricked")
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

    // System UI & essential packages to always ignore
    private val systemUiPackages = setOf(
        "com.android.systemui",
        "com.samsung.android.app.cocktailbarservice",
        "com.samsung.android.edge",
        "com.android.launcher",
        "com.google.android.apps.nexuslauncher",
        "com.sec.android.app.launcher",
        "com.android.settings",
        "android",
        "com.samsung.android.app.resolver",
        "com.samsung.android.service.tagservice",
        "com.android.nfc"
    )

    // Payment & banking apps — never blocked to avoid accessibility warnings
    private val paymentApps = setOf(
        // Google Pay
        "com.google.android.apps.nbu.paisa.user",
        "com.google.android.apps.walletnfcrel",
        // Indian UPI
        "net.one97.paytm",
        "com.phonepe.app",
        "in.org.npci.upiapp",
        "in.amazon.mShop.android.shopping",
        "com.mobikwik_new",
        "com.freecharge.android",
        // Samsung Pay
        "com.samsung.android.spay",
        "com.samsung.android.samsungpay.gear",
        // Banking
        "com.android.vending",  // Play Store (has payments)
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            // Ignore our own app and system UI
            if (packageName == this.packageName) return
            if (packageName == "com.eenth.blocker") return
            if (systemUiPackages.contains(packageName)) return

            // Never block payment/banking apps
            if (paymentApps.contains(packageName)) return

            val isBricked = prefs.getBoolean(MainActivity.KEY_IS_BRICKED, false)
            if (!isBricked) return

            val brickEverything = prefs.getBoolean(MainActivity.KEY_BRICK_EVERYTHING, false)
            val shouldBlock = if (brickEverything) {
                true // Block everything except allowlisted packages (already filtered above)
            } else {
                val blockedApps = prefs.getStringSet(MainActivity.KEY_BLOCKED_APPS, emptySet()) ?: emptySet()
                val groupBlocked = GroupManager.getGroupBlockedPackages(prefs)
                blockedApps.contains(packageName) || groupBlocked.contains(packageName)
            }

            if (shouldBlock) {
                Log.d("BlockService", "BLOCKING: $packageName")
                // Launch the blocker as a full-screen Activity
                val intent = Intent(this, BlockerActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
            }
        }
    }

    override fun onInterrupt() { }
}
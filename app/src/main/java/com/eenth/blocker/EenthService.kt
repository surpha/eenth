package com.eenth.blocker

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.util.Log
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.Build

class EenthService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var blockerView: View? = null
    private var isBlocking = false
    private var isUnlocked = false

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.eenth.blocker.ACTION_UNLOCK") {
                Log.d("EenthSensor", "Unlock Command Received! Unlocking session.")
                
                isUnlocked = true // <--- Permanent unlock
                removeBlocker()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        // Turn on the "Ear" when service starts
        val filter = IntentFilter("com.eenth.blocker.ACTION_UNLOCK")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(unlockReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(unlockReceiver, filter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Turn off the "Ear" when service dies
        unregisterReceiver(unlockReceiver)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Initialize the Window Manager (The system service that handles screens)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            if (packageName == this.packageName) return 

            // --- NEW LOGIC ---
            // If the user unlocked the app, we simply stop checking.
            if (isUnlocked) {
                Log.d("EenthSensor", "Allowed: $packageName (Session Unlocked)")
                return
            }

            Log.d("EenthSensor", "DETECTED APP: $packageName")

            if (packageName == "com.android.chrome" || packageName == "com.google.android.youtube") {
                showBlocker()
            } else {
                removeBlocker()
            }
        }
    }

    private fun showBlocker() {
       if (isBlocking) return

        try {
            // 1. Inflate the view if it doesn't exist
            if (blockerView == null) {
                val inflater = LayoutInflater.from(this)
                blockerView = inflater.inflate(R.layout.window_blocker, null)
            }

            // 2. ALWAYS attach the listener (Fixes the stale button bug)
            val btnUnlock = blockerView?.findViewById<View>(R.id.btnUnlock)
            btnUnlock?.setOnClickListener {
                Log.d("EenthSensor", "Unlock Button Clicked!") // Check logs for this!
                
                try {
                    val intent = Intent(this, NfcUnlockActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("EenthSensor", "Failed to launch activity: ${e.message}")
                }
            }

            // 3. Configure window
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                // FLAG_NOT_TOUCH_MODAL allows touches to pass to the window
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.CENTER

            windowManager?.addView(blockerView, params)
            isBlocking = true
            Log.d("EenthSensor", "BLOCKER SHOWN!")

        } catch (e: Exception) {
            Log.e("EenthSensor", "Error showing blocker: ${e.message}")
        }
    }

    private fun removeBlocker() {
        if (!isBlocking || blockerView == null) return

        try {
            windowManager?.removeView(blockerView)
            isBlocking = false
            Log.d("EenthSensor", "BLOCKER REMOVED!")
        } catch (e: Exception) {
            Log.e("EenthSensor", "Error removing blocker: ${e.message}")
        }
    }

    override fun onInterrupt() { }
}
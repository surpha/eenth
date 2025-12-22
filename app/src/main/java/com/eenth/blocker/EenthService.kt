package com.eenth.blocker

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.util.Log

class EenthService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var blockerView: View? = null
    private var isBlocking = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Initialize the Window Manager (The system service that handles screens)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            
            // --- THE FIX IS HERE ---
            // If the event comes from Eenth itself, IGNORE IT.
            // This prevents us from unblocking ourselves.
            if (packageName == this.packageName) {
                return 
            }

            Log.d("EenthSensor", "DETECTED APP: $packageName")

            // Block Logic
            if (packageName == "com.android.chrome" || packageName == "com.google.android.youtube") {
                showBlocker()
            } else {
                // Only remove if we switched to a SAFE app (like Home screen or Settings)
                removeBlocker()
            }
        }
    }

    private fun showBlocker() {
        if (isBlocking) return // Don't add it if it's already there

        try {
            // 1. Create the view from your XML file
            if (blockerView == null) {
                val inflater = LayoutInflater.from(this)
                blockerView = inflater.inflate(R.layout.window_blocker, null)
            }

            // 2. Configure the window settings (Full screen, On Top of everything)
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, // This places it above apps
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.CENTER

            // 3. Add to screen
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
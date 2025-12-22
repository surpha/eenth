package com.eenth.blocker

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.util.Log

class EenthService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // This triggers whenever the screen content changes
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()
            // This is the "Spy" log we are looking for
            Log.d("EenthSensor", "DETECTED APP: $packageName")
        }
    }

    override fun onInterrupt() {
        // Required method, but we don't need logic here yet
    }
}
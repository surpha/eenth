package com.eenth.blocker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class BlockMonitorService : Service() {

    companion object {
        const val TAG = "BlockMonitor"
        const val CHANNEL_ID = "block_monitor_channel"
        const val NOTIFICATION_ID = 1001
        private const val POLL_INTERVAL_MS = 500L

        fun start(context: Context) {
            val intent = Intent(context, BlockMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BlockMonitorService::class.java))
        }
    }

    private lateinit var prefs: SharedPreferences
    private lateinit var windowManager: WindowManager
    private lateinit var handler: Handler
    private var overlayView: View? = null
    private var isOverlayShowing = false
    private var lastBlockedPackage: String? = null

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

    private val paymentApps = setOf(
        "com.google.android.apps.nbu.paisa.user",
        "com.google.android.apps.walletnfcrel",
        "net.one97.paytm",
        "com.phonepe.app",
        "in.org.npci.upiapp",
        "in.amazon.mShop.android.shopping",
        "com.mobikwik_new",
        "com.freecharge.android",
        "com.samsung.android.spay",
        "com.samsung.android.samsungpay.gear",
        "com.android.vending"
    )

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == MainActivity.ACTION_STATE_CHANGED) {
                val isBricked = prefs.getBoolean(MainActivity.KEY_IS_BRICKED, false)
                Log.d(TAG, "State changed. Blocked: $isBricked")
                if (!isBricked) {
                    hideOverlay()
                }
            }
        }
    }

    private val pollRunnable = object : Runnable {
        override fun run() {
            checkForegroundApp()
            handler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        handler = Handler(Looper.getMainLooper())

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        val filter = IntentFilter(MainActivity.ACTION_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stateReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(stateReceiver, filter)
        }

        handler.post(pollRunnable)
        Log.d(TAG, "BlockMonitorService started")
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(pollRunnable)
        hideOverlay()
        unregisterReceiver(stateReceiver)
        Log.d(TAG, "BlockMonitorService stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun checkForegroundApp() {
        val isBricked = prefs.getBoolean(MainActivity.KEY_IS_BRICKED, false)
        if (!isBricked) {
            if (isOverlayShowing) hideOverlay()
            return
        }

        val foregroundPkg = getForegroundPackage() ?: return

        // Don't block our own app or system packages
        if (foregroundPkg == packageName) {
            if (isOverlayShowing) hideOverlay()
            return
        }
        if (foregroundPkg == "com.eenth.blocker") {
            if (isOverlayShowing) hideOverlay()
            return
        }
        if (systemUiPackages.contains(foregroundPkg)) {
            if (isOverlayShowing) hideOverlay()
            return
        }
        if (paymentApps.contains(foregroundPkg)) {
            if (isOverlayShowing) hideOverlay()
            return
        }

        val brickEverything = prefs.getBoolean(MainActivity.KEY_BRICK_EVERYTHING, false)
        val shouldBlock = if (brickEverything) {
            true
        } else {
            val blockedApps = prefs.getStringSet(MainActivity.KEY_BLOCKED_APPS, emptySet()) ?: emptySet()
            val groupBlocked = GroupManager.getGroupBlockedPackages(prefs)
            blockedApps.contains(foregroundPkg) || groupBlocked.contains(foregroundPkg)
        }

        if (shouldBlock) {
            lastBlockedPackage = foregroundPkg
            showOverlay()
        } else {
            if (isOverlayShowing) hideOverlay()
        }
    }

    private fun getForegroundPackage(): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        // Query events for the last 5 seconds to find the most recent foreground app
        val events = usm.queryEvents(now - 5000, now)
        val event = UsageEvents.Event()
        var lastForegroundPkg: String? = null
        var lastTimestamp = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val isForeground = event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && event.eventType == UsageEvents.Event.ACTIVITY_RESUMED)
            if (isForeground && event.timeStamp > lastTimestamp) {
                lastForegroundPkg = event.packageName
                lastTimestamp = event.timeStamp
            }
        }

        return lastForegroundPkg
    }

    private fun showOverlay() {
        if (isOverlayShowing) return

        val inflater = LayoutInflater.from(this)
        overlayView = inflater.inflate(R.layout.window_blocker, null)

        // Make overlay tappable to open BlockerActivity for NFC unblock
        overlayView?.setOnClickListener {
            val intent = Intent(this, BlockerActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
            PixelFormat.OPAQUE
        )
        params.gravity = Gravity.CENTER

        try {
            windowManager.addView(overlayView, params)
            isOverlayShowing = true
            Log.d(TAG, "Overlay shown for: $lastBlockedPackage")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show overlay: ${e.message}")
        }
    }

    private fun hideOverlay() {
        if (!isOverlayShowing || overlayView == null) return
        try {
            windowManager.removeView(overlayView)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove overlay: ${e.message}")
        }
        overlayView = null
        isOverlayShowing = false
        lastBlockedPackage = null
        Log.d(TAG, "Overlay hidden")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Block Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors app usage to enforce blocking"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Block is active")
            .setContentText("Monitoring app usage")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}

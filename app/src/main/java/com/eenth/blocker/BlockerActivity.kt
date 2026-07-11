package com.eenth.blocker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class BlockerActivity : AppCompatActivity() {

    companion object {
        const val ACTION_CLOSE_BLOCKER = "com.eenth.blocker.ACTION_CLOSE_BLOCKER"
    }

    private val closeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_CLOSE_BLOCKER) {
                // Open Eenth main screen
                val mainIntent = Intent(this@BlockerActivity, MainActivity::class.java)
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(mainIntent)
                finishAndRemoveTask()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.window_blocker)
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(ACTION_CLOSE_BLOCKER)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(closeReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(closeReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(closeReceiver)
    }

    @Deprecated("Use onBackPressedDispatcher")
    override fun onBackPressed() {
        // Do nothing — prevent back from dismissing the blocker
    }
}

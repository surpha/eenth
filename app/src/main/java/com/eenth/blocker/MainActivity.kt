package com.eenth.blocker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    companion object {
        const val PREFS_NAME = "eenth_prefs"
        const val KEY_BLOCKED_APPS = "blocked_apps"
        const val KEY_IS_BRICKED = "is_bricked"
        const val ACTION_STATE_CHANGED = "com.eenth.blocker.ACTION_STATE_CHANGED"
    }

    private lateinit var prefs: SharedPreferences
    private lateinit var tvStatus: TextView
    private lateinit var tvStatusHint: TextView
    private lateinit var tvAppCount: TextView
    private var nfcAdapter: NfcAdapter? = null

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_STATE_CHANGED) {
                updateStatusBanner()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        tvStatus = findViewById(R.id.tvStatus)
        tvStatusHint = findViewById(R.id.tvStatusHint)
        tvAppCount = findViewById(R.id.tvAppCount)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        updateStatusBanner()
        setupAppList()
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(ACTION_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stateReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(stateReceiver, filter)
        }
        updateStatusBanner()

        // Enable NFC reader mode — intercepts ALL tag taps while app is visible
        // NOT skipping NDEF check so we can read/write NDEF to erase tag data
        val flags = NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B
        nfcAdapter?.enableReaderMode(this, this, flags, null)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(stateReceiver)
        nfcAdapter?.disableReaderMode(this)
    }

    override fun onTagDiscovered(tag: Tag?) {
        Log.d("EenthNfc", "NFC tag detected in MainActivity!")

        // Toggle bricked state
        val isBricked = prefs.getBoolean(KEY_IS_BRICKED, false)
        val newState = !isBricked
        prefs.edit().putBoolean(KEY_IS_BRICKED, newState).apply()

        // Erase any existing NDEF URL data from the tag (one-time cleanup)
        eraseTagData(tag)

        runOnUiThread {
            val message = if (newState) "BRICKED! Apps are now blocked." else "UNBRICKED! Apps unlocked."
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            updateStatusBanner()

            // Notify the service
            val stateIntent = Intent(ACTION_STATE_CHANGED)
            sendBroadcast(stateIntent)
        }
    }

    private fun eraseTagData(tag: Tag?) {
        if (tag == null) return
        try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                if (ndef.isWritable) {
                    // Write an empty NDEF message to clear the Brick URL
                    val emptyRecord = NdefRecord(NdefRecord.TNF_EMPTY, null, null, null)
                    val emptyMessage = NdefMessage(arrayOf(emptyRecord))
                    ndef.writeNdefMessage(emptyMessage)
                    Log.d("EenthNfc", "Tag erased successfully")
                }
                ndef.close()
            }
        } catch (e: Exception) {
            Log.d("EenthNfc", "Tag erase skipped: ${e.message}")
        }
    }

    private fun updateStatusBanner() {
        val isBricked = prefs.getBoolean(KEY_IS_BRICKED, false)
        if (isBricked) {
            tvStatus.text = "BRICKED"
            tvStatus.setTextColor(0xFFD32F2F.toInt())
            tvStatusHint.text = "Tap your NFC tag to unlock"
        } else {
            tvStatus.text = "UNBRICKED"
            tvStatus.setTextColor(0xFF4CAF50.toInt())
            tvStatusHint.text = "Tap your NFC tag to activate"
        }
    }

    private fun updateAppCount() {
        val count = prefs.getStringSet(KEY_BLOCKED_APPS, emptySet())?.size ?: 0
        tvAppCount.text = "$count selected"
    }

    private fun setupAppList() {
        val pm = packageManager

        // Get all apps that have a launcher intent (user-visible apps)
        val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val launchableApps = pm.queryIntentActivities(launchIntent, 0)

        val appList = launchableApps
            .map { it.activityInfo }
            .filter { it.packageName != packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }
            .map { activityInfo ->
                val blockedSet = prefs.getStringSet(KEY_BLOCKED_APPS, emptySet()) ?: emptySet()
                AppInfo(
                    name = activityInfo.loadLabel(pm).toString(),
                    packageName = activityInfo.packageName,
                    icon = activityInfo.loadIcon(pm),
                    isBlocked = blockedSet.contains(activityInfo.packageName)
                )
            }

        updateAppCount()

        val recyclerView = findViewById<RecyclerView>(R.id.rvApps)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = AppListAdapter(appList) { app, isChecked ->
            val current = prefs.getStringSet(KEY_BLOCKED_APPS, emptySet())?.toMutableSet() ?: mutableSetOf()
            if (isChecked) {
                current.add(app.packageName)
            } else {
                current.remove(app.packageName)
            }
            prefs.edit().putStringSet(KEY_BLOCKED_APPS, current).apply()
            updateAppCount()
        }
    }
}
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
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    companion object {
        const val PREFS_NAME = "eenth_prefs"
        const val KEY_BLOCKED_APPS = "blocked_apps"
        const val KEY_IS_BRICKED = "is_bricked"
        const val KEY_BRICK_EVERYTHING = "brick_everything"
        const val KEY_PAIRED_TAG_ID = "paired_tag_id"
        const val KEY_TAG_NAME = "tag_name"
        const val ACTION_STATE_CHANGED = "com.eenth.blocker.ACTION_STATE_CHANGED"
    }

    private lateinit var prefs: SharedPreferences
    private lateinit var tvStatus: TextView
    private lateinit var tvStatusHint: TextView
    private lateinit var tvAppCount: TextView
    private lateinit var tvTagName: TextView
    private lateinit var tvTagId: TextView
    private lateinit var btnRepair: TextView
    private lateinit var btnEditName: TextView
    private lateinit var tagUnpaired: LinearLayout
    private lateinit var tagPaired: LinearLayout
    private lateinit var switchBrickAll: SwitchMaterial
    private lateinit var tvBrickAllHint: TextView
    private lateinit var appsSection: LinearLayout
    private var nfcAdapter: NfcAdapter? = null
    private val tagRepo = TagRepository()
    private lateinit var deviceId: String

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
        tvTagName = findViewById(R.id.tvTagName)
        tvTagId = findViewById(R.id.tvTagId)
        btnRepair = findViewById(R.id.btnRepair)
        btnEditName = findViewById(R.id.btnEditName)
        tagUnpaired = findViewById(R.id.tagUnpaired)
        tagPaired = findViewById(R.id.tagPaired)
        switchBrickAll = findViewById(R.id.switchBrickAll)
        tvBrickAllHint = findViewById(R.id.tvBrickAllHint)
        appsSection = findViewById(R.id.appsSection)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        btnRepair.setOnClickListener {
            val oldTagId = prefs.getString(KEY_PAIRED_TAG_ID, null)
            prefs.edit()
                .remove(KEY_PAIRED_TAG_ID)
                .remove(KEY_TAG_NAME)
                .apply()
            if (oldTagId != null) {
                Thread { tagRepo.unpairTag(oldTagId, deviceId) }.start()
            }
            Toast.makeText(this, "Tag unpaired. Tap a new tag to pair.", Toast.LENGTH_SHORT).show()
            updateTagSection()
        }

        btnEditName.setOnClickListener {
            showNameDialog()
        }

        // Brick Everything toggle
        switchBrickAll.isChecked = prefs.getBoolean(KEY_BRICK_EVERYTHING, false)
        updateBrickAllState(switchBrickAll.isChecked)
        switchBrickAll.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_BRICK_EVERYTHING, isChecked).apply()
            updateBrickAllState(isChecked)
        }

        updateStatusBanner()
        updateTagSection()
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
        if (tag == null) return

        val tagId = tag.id.toHexString()
        val pairedId = prefs.getString(KEY_PAIRED_TAG_ID, null)

        if (pairedId == null) {
            // First tap — try to pair this tag via server
            runOnUiThread {
                Toast.makeText(this, "Pairing tag...", Toast.LENGTH_SHORT).show()
            }

            val result = tagRepo.pairTag(tagId, deviceId)
            when (result) {
                is PairResult.Success, is PairResult.AlreadyPairedToThis -> {
                    prefs.edit().putString(KEY_PAIRED_TAG_ID, tagId).apply()
                    eraseTagData(tag)
                    runOnUiThread {
                        Toast.makeText(this, "Tag paired!", Toast.LENGTH_SHORT).show()
                        updateTagSection()
                        showNameDialog()
                    }
                }
                is PairResult.AlreadyTakenByOther -> {
                    runOnUiThread {
                        Toast.makeText(this, "This tag is already registered to another device.", Toast.LENGTH_LONG).show()
                    }
                }
                is PairResult.Error -> {
                    prefs.edit().putString(KEY_PAIRED_TAG_ID, tagId).apply()
                    eraseTagData(tag)
                    runOnUiThread {
                        Toast.makeText(this, "Tag paired (offline).", Toast.LENGTH_SHORT).show()
                        updateTagSection()
                        showNameDialog()
                    }
                }
            }
            return
        }

        if (tagId != pairedId) {
            runOnUiThread {
                Toast.makeText(this, "Unrecognized tag. Only your paired tag works.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // Correct tag — toggle bricked state
        val isBricked = prefs.getBoolean(KEY_IS_BRICKED, false)
        val newState = !isBricked
        prefs.edit().putBoolean(KEY_IS_BRICKED, newState).apply()

        runOnUiThread {
            val message = if (newState) "BRICKED! Apps are now blocked." else "UNBRICKED! Apps unlocked."
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            updateStatusBanner()

            val stateIntent = Intent(ACTION_STATE_CHANGED)
            sendBroadcast(stateIntent)
        }
    }

    private fun ByteArray.toHexString(): String = joinToString("") { "%02X".format(it) }

    private fun eraseTagData(tag: Tag?) {
        if (tag == null) return
        try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                if (ndef.isWritable) {
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

    private fun updateTagSection() {
        val pairedId = prefs.getString(KEY_PAIRED_TAG_ID, null)
        if (pairedId != null) {
            tagUnpaired.visibility = View.GONE
            tagPaired.visibility = View.VISIBLE
            val name = prefs.getString(KEY_TAG_NAME, null) ?: "My Brick"
            tvTagName.text = name
            tvTagId.text = "ID: $pairedId"
        } else {
            tagUnpaired.visibility = View.VISIBLE
            tagPaired.visibility = View.GONE
        }
    }

    private fun showNameDialog() {
        val input = EditText(this)
        input.hint = "e.g. My Brick, Desk Tag, Focus Key"
        input.setText(prefs.getString(KEY_TAG_NAME, ""))
        input.setTextColor(0xFFEEEEEE.toInt())
        input.setHintTextColor(0xFF666666.toInt())
        input.setBackgroundColor(0xFF1A1A1A.toInt())
        input.setPadding(48, 32, 48, 32)

        AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog)
            .setTitle("Name your tag")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text.toString().trim().ifEmpty { "My Brick" }
                prefs.edit().putString(KEY_TAG_NAME, name).apply()
                updateTagSection()
                // Sync to server
                val tagUid = prefs.getString(KEY_PAIRED_TAG_ID, null)
                if (tagUid != null) {
                    Thread { tagRepo.updateTagName(tagUid, deviceId, name) }.start()
                }
            }
            .setNegativeButton("Skip", null)
            .show()
    }

    private fun updateAppCount() {
        val count = prefs.getStringSet(KEY_BLOCKED_APPS, emptySet())?.size ?: 0
        tvAppCount.text = "$count selected"
    }

    private fun updateBrickAllState(enabled: Boolean) {
        if (enabled) {
            tvBrickAllHint.text = "Every app will be blocked when bricked"
            appsSection.alpha = 0.3f
            appsSection.isEnabled = false
        } else {
            tvBrickAllHint.text = "Only selected apps will be blocked"
            appsSection.alpha = 1.0f
            appsSection.isEnabled = true
        }
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
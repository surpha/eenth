package com.eenth.blocker

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast

class NfcUnlockActivity : Activity(), NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var prefs: SharedPreferences
    private val tagRepo = TagRepository()
    private lateinit var deviceId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        if (nfcAdapter == null) {
            Toast.makeText(this, "No NFC Hardware found!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Handle if launched via system NFC dispatch
        if (intent != null && (
            NfcAdapter.ACTION_TAG_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action
        )) {
            val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            handleTag(tag)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent != null && (
            NfcAdapter.ACTION_TAG_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action
        )) {
            val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            handleTag(tag)
        }
    }

    override fun onResume() {
        super.onResume()
        val flags = NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK

        nfcAdapter?.enableReaderMode(this, this, flags, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    override fun onTagDiscovered(tag: Tag?) {
        Log.d("EenthNfc", "NFC TAG DETECTED via reader mode!")
        runOnUiThread { handleTag(tag) }
    }

    private fun handleTag(tag: Tag?) {
        if (tag == null) {
            finish()
            return
        }

        val tagId = tag.id.toHexString()
        val pairedId = prefs.getString(MainActivity.KEY_PAIRED_TAG_ID, null)

        if (pairedId == null) {
            runOnUiThread {
                Toast.makeText(this, "No tag paired yet. Open Eenth to pair.", Toast.LENGTH_SHORT).show()
            }
            finish()
            return
        }

        if (tagId != pairedId) {
            runOnUiThread {
                Toast.makeText(this, "Unrecognized tag.", Toast.LENGTH_SHORT).show()
            }
            finish()
            return
        }

        // Verify with server (in background thread — we're already off main thread from reader mode)
        val verifyResult = tagRepo.verifyTag(tagId, deviceId)
        when (verifyResult) {
            is VerifyResult.WrongDevice -> {
                runOnUiThread {
                    Toast.makeText(this, "This tag is registered to another device.", Toast.LENGTH_LONG).show()
                }
                finish()
                return
            }
            is VerifyResult.Error -> {
                // Allow offline toggle (local pairing still valid)
                Log.d("EenthNfc", "Server unreachable, allowing local toggle")
            }
            else -> { /* Valid or NotRegistered — proceed */ }
        }

        // Toggle bricked state
        val isBricked = prefs.getBoolean(MainActivity.KEY_IS_BRICKED, false)
        val newState = !isBricked
        prefs.edit().putBoolean(MainActivity.KEY_IS_BRICKED, newState).apply()

        runOnUiThread {
            val message = if (newState) "BLOCKED! Apps are now blocked." else "UNBLOCKED! Apps unlocked."
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()

            val stateIntent = Intent(MainActivity.ACTION_STATE_CHANGED)
            sendBroadcast(stateIntent)

            val closeIntent = Intent(BlockerActivity.ACTION_CLOSE_BLOCKER)
            sendBroadcast(closeIntent)

            if (!newState) {
                val mainIntent = Intent(this, MainActivity::class.java)
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(mainIntent)
            }

            Log.d("BlockNfc", "Toggled to: blocked=$newState")
            finish()
        }
    }

    private fun ByteArray.toHexString(): String = joinToString("") { "%02X".format(it) }
}
package com.eenth.blocker

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.widget.Toast

class NfcUnlockActivity : Activity(), NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (nfcAdapter == null) {
            Toast.makeText(this, "No NFC Hardware found!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Handle if launched via system NFC dispatch (TAG_DISCOVERED / TECH_DISCOVERED)
        if (intent != null && (
            NfcAdapter.ACTION_TAG_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action
        )) {
            handleToggle()
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
            handleToggle()
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
        runOnUiThread { handleToggle() }
    }

    private fun handleToggle() {
        val isBricked = prefs.getBoolean(MainActivity.KEY_IS_BRICKED, false)
        val newState = !isBricked
        prefs.edit().putBoolean(MainActivity.KEY_IS_BRICKED, newState).apply()

        val message = if (newState) "BRICKED! Apps are now blocked." else "UNBRICKED! Apps unlocked."
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

        // Notify service and MainActivity of the state change
        val stateIntent = Intent(EenthService.ACTION_STATE_CHANGED)
        sendBroadcast(stateIntent)

        // Close the blocker activity
        val closeIntent = Intent(BlockerActivity.ACTION_CLOSE_BLOCKER)
        sendBroadcast(closeIntent)

        // If unbricking, redirect to Eenth main screen
        if (!newState) {
            val mainIntent = Intent(this, MainActivity::class.java)
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(mainIntent)
        }

        Log.d("EenthNfc", "Toggled to: bricked=$newState")
        finish()
    }
}
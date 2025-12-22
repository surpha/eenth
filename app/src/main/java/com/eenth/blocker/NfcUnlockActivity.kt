package com.eenth.blocker

import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.widget.Toast

class NfcUnlockActivity : Activity(), NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        
        if (nfcAdapter == null) {
            Toast.makeText(this, "No NFC Hardware found!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Start listening for ANY NFC tag
        val flags = NfcAdapter.FLAG_READER_NFC_A or 
                    NfcAdapter.FLAG_READER_NFC_B or 
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
        
        nfcAdapter?.enableReaderMode(this, this, flags, null)
        Toast.makeText(this, "Ready to Scan...", Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        // Stop listening to save battery
        nfcAdapter?.disableReaderMode(this)
    }

    // This function fires AUTOMATICALLY when a tag touches the phone
    override fun onTagDiscovered(tag: Tag?) {
        Log.d("EenthSensor", "NFC TAG DETECTED!")

        // Go back to the main thread to update UI
        runOnUiThread {
            Toast.makeText(this, "KEY ACCEPTED! UNLOCKING...", Toast.LENGTH_LONG).show()
            
            // SEND SIGNAL TO UNLOCK (We will wire this to the service in the next step)
            val unlockIntent = Intent("com.eenth.blocker.ACTION_UNLOCK")
            sendBroadcast(unlockIntent)
            
            // Close this scanner screen
            finish()
        }
    }
}
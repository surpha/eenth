package com.eenth.blocker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class BlockerActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    companion object {
        const val ACTION_CLOSE_BLOCKER = "com.eenth.blocker.ACTION_CLOSE_BLOCKER"
    }

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var prefs: SharedPreferences
    private val tagRepo = TagRepository()
    private lateinit var deviceId: String

    private val closeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_CLOSE_BLOCKER) {
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
        prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(ACTION_CLOSE_BLOCKER)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(closeReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(closeReceiver, filter)
        }

        // Enable NFC reader mode — this takes priority over system NFC dispatch
        // so no chooser dialog will appear while the blocker is showing
        val flags = NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
        nfcAdapter?.enableReaderMode(this, this, flags, null)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(closeReceiver)
        nfcAdapter?.disableReaderMode(this)
    }

    override fun onTagDiscovered(tag: Tag?) {
        if (tag == null) return
        val tagId = tag.id.joinToString("") { "%02X".format(it) }
        val pairedId = prefs.getString(MainActivity.KEY_PAIRED_TAG_ID, null)

        if (pairedId == null || tagId != pairedId) {
            runOnUiThread {
                Toast.makeText(this, "Unrecognized tag.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // Verify with server
        val verifyResult = tagRepo.verifyTag(tagId, deviceId)
        if (verifyResult is VerifyResult.WrongDevice) {
            runOnUiThread {
                Toast.makeText(this, "Tag registered to another device.", Toast.LENGTH_LONG).show()
            }
            return
        }

        // Unbrick
        prefs.edit().putBoolean(MainActivity.KEY_IS_BRICKED, false).apply()
        Log.d("EenthNfc", "Unbricked via BlockerActivity NFC tap")

        runOnUiThread {
            Toast.makeText(this, "UNBRICKED! Apps unlocked.", Toast.LENGTH_LONG).show()
            sendBroadcast(Intent(EenthService.ACTION_STATE_CHANGED))
            sendBroadcast(Intent(ACTION_CLOSE_BLOCKER))

            val mainIntent = Intent(this, MainActivity::class.java)
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(mainIntent)
            finishAndRemoveTask()
        }
    }

    @Deprecated("Use onBackPressedDispatcher")
    override fun onBackPressed() {
        // Do nothing — prevent back from dismissing the blocker
    }
}

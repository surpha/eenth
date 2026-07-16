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
import android.os.Handler
import android.os.Looper
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
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.UUID

class MainActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    companion object {
        const val PREFS_NAME = "eenth_prefs"
        const val KEY_BLOCKED_APPS = "blocked_apps"
        const val KEY_IS_BRICKED = "is_bricked"
        const val KEY_BRICK_EVERYTHING = "brick_everything"
        const val KEY_PAIRED_TAG_ID = "paired_tag_id"
        const val KEY_TAG_NAME = "tag_name"
        const val KEY_BRICK_START_TIME = "brick_start_time"
        const val KEY_TODAY_FOCUS_MS = "today_focus_ms"
        const val KEY_TODAY_SESSIONS = "today_sessions"
        const val KEY_STAT_DATE = "stat_date"
        const val ACTION_STATE_CHANGED = "com.eenth.blocker.ACTION_STATE_CHANGED"
    }

    private lateinit var prefs: SharedPreferences
    private lateinit var tvBrand: TextView
    private lateinit var statusCard: LinearLayout
    private lateinit var statusDot: View
    private lateinit var tvStatus: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvStatusHint: TextView
    private lateinit var tvAppCount: TextView
    private lateinit var tvTagName: TextView
    private lateinit var btnRepair: TextView
    private lateinit var btnEditName: TextView
    private lateinit var tagUnpaired: LinearLayout
    private lateinit var tagPaired: LinearLayout
    private lateinit var switchBlockAll: SwitchMaterial
    private lateinit var appsSection: LinearLayout
    private lateinit var groupsSection: LinearLayout
    private lateinit var groupAdapter: GroupAdapter
    private lateinit var tvTodayTime: TextView
    private lateinit var tvSessions: TextView
    private var installedPackages: Set<String> = emptySet()
    private var nfcAdapter: NfcAdapter? = null
    private val tagRepo = TagRepository()
    private lateinit var deviceId: String
    private var adminMode = false

    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            updateTimerDisplay()
            timerHandler.postDelayed(this, 1000)
        }
    }

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
        tvBrand = findViewById(R.id.tvBrand)
        statusCard = findViewById(R.id.statusCard)
        statusDot = findViewById(R.id.statusDot)
        tvStatus = findViewById(R.id.tvStatus)
        tvTimer = findViewById(R.id.tvTimer)
        tvStatusHint = findViewById(R.id.tvStatusHint)
        tvAppCount = findViewById(R.id.tvAppCount)
        tvTagName = findViewById(R.id.tvTagName)
        btnRepair = findViewById(R.id.btnRepair)
        btnEditName = findViewById(R.id.btnEditName)
        tagUnpaired = findViewById(R.id.tagUnpaired)
        tagPaired = findViewById(R.id.tagPaired)
        switchBlockAll = findViewById(R.id.switchBlockAll)
        appsSection = findViewById(R.id.appsSection)
        groupsSection = findViewById(R.id.groupsSection)
        tvTodayTime = findViewById(R.id.tvTodayTime)
        tvSessions = findViewById(R.id.tvSessions)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        resetDailyStatsIfNeeded()

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

        // Block all apps toggle
        switchBlockAll.isChecked = prefs.getBoolean(KEY_BRICK_EVERYTHING, false)
        updateBlockAllVisibility(switchBlockAll.isChecked)
        switchBlockAll.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_BRICK_EVERYTHING, isChecked).apply()
            updateBlockAllVisibility(isChecked)
        }

        updateStatusBanner()
        updateTagSection()
        setupAppList()
        setupGroups()

        // Stats card → opens Insights
        findViewById<LinearLayout>(R.id.statsCard).setOnClickListener {
            startActivity(Intent(this, StatsActivity::class.java))
        }

        // Bottom nav
        findViewById<LinearLayout>(R.id.navInsights).setOnClickListener {
            startActivity(Intent(this, StatsActivity::class.java))
        }

        // Long-press brand logo 5 times within 3 seconds → toggle admin mode
        var tapCount = 0
        var firstTapTime = 0L
        tvBrand.setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - firstTapTime > 3000) {
                tapCount = 0
                firstTapTime = now
            }
            tapCount++
            if (tapCount >= 7) {
                tapCount = 0
                adminMode = !adminMode
                if (adminMode) {
                    showAdminSheet()
                } else {
                    Toast.makeText(this, "Admin mode OFF", Toast.LENGTH_SHORT).show()
                    updateStatusBanner()
                }
            }
        }
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
        updateStats()
        startTimerIfBricked()

        // Enable NFC reader mode — intercepts ALL tag taps while app is visible
        val flags = NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B
        nfcAdapter?.enableReaderMode(this, this, flags, null)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(stateReceiver)
        nfcAdapter?.disableReaderMode(this)
        timerHandler.removeCallbacks(timerRunnable)
    }

    override fun onTagDiscovered(tag: Tag?) {
        Log.d("BlockNfc", "NFC tag detected in MainActivity!")
        if (tag == null) return

        val tagId = tag.id.toHexString()

        // Admin mode: register tag as approved brick
        if (adminMode) {
            Thread {
                val success = tagRepo.registerApprovedTag(tagId)
                runOnUiThread {
                    addAdminTagEntry(tagId, success)
                }
            }.start()
            return
        }

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
                is PairResult.NotApproved -> {
                    runOnUiThread {
                        Toast.makeText(this, "This tag is not an approved Block tag.", Toast.LENGTH_LONG).show()
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

        if (newState) {
            // Bricking: record start time, increment sessions
            prefs.edit()
                .putBoolean(KEY_IS_BRICKED, true)
                .putLong(KEY_BRICK_START_TIME, System.currentTimeMillis())
                .putInt(KEY_TODAY_SESSIONS, prefs.getInt(KEY_TODAY_SESSIONS, 0) + 1)
                .apply()
        } else {
            // Unbricking: accumulate focus time
            val startTime = prefs.getLong(KEY_BRICK_START_TIME, 0L)
            val elapsed = if (startTime > 0) System.currentTimeMillis() - startTime else 0L
            val todayMs = prefs.getLong(KEY_TODAY_FOCUS_MS, 0L) + elapsed
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            prefs.edit()
                .putBoolean(KEY_IS_BRICKED, false)
                .putLong(KEY_BRICK_START_TIME, 0L)
                .putLong(KEY_TODAY_FOCUS_MS, todayMs)
                .putLong("focus_$today", todayMs)
                .apply()
        }

        runOnUiThread {
            val message = if (newState) "BLOCKED" else "UNBLOCKED"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            updateStatusBanner()
            updateStats()
            if (newState) startTimerIfBricked() else timerHandler.removeCallbacks(timerRunnable)

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
            tvBrand.text = "BLOCKIN"
            tvBrand.setTextColor(0xFFFF453A.toInt())
            statusCard.setBackgroundResource(R.drawable.bg_hero_bricked)
            statusDot.setBackgroundResource(R.drawable.bg_dot_red)
            tvStatus.text = "BLOCKED"
            tvStatus.setTextColor(0xFFFF453A.toInt())
            tvStatusHint.text = "tap block to unblock"
            updateTimerDisplay()
        } else {
            tvBrand.text = "BLOCK"
            tvBrand.setTextColor(0xFFFFFFFF.toInt())
            statusCard.setBackgroundResource(R.drawable.bg_hero_default)
            statusDot.setBackgroundResource(R.drawable.bg_dot_green)
            tvStatus.text = "UNBLOCKED"
            tvStatus.setTextColor(0xFF32D74B.toInt())
            tvStatusHint.text = "tap block to start a session"
            tvTimer.text = "0:00:00"
        }
    }

    private fun updateTimerDisplay() {
        val startTime = prefs.getLong(KEY_BRICK_START_TIME, 0L)
        if (startTime <= 0L) {
            tvTimer.text = "0:00:00"
            return
        }
        val elapsed = System.currentTimeMillis() - startTime
        tvTimer.text = formatTimer(elapsed)
    }

    private fun startTimerIfBricked() {
        timerHandler.removeCallbacks(timerRunnable)
        if (prefs.getBoolean(KEY_IS_BRICKED, false)) {
            timerRunnable.run()
        }
    }

    private fun updateStats() {
        resetDailyStatsIfNeeded()
        var todayMs = prefs.getLong(KEY_TODAY_FOCUS_MS, 0L)
        // If currently bricked, add ongoing session time
        val startTime = prefs.getLong(KEY_BRICK_START_TIME, 0L)
        if (prefs.getBoolean(KEY_IS_BRICKED, false) && startTime > 0L) {
            todayMs += System.currentTimeMillis() - startTime
        }
        tvTodayTime.text = formatDuration(todayMs)
        val sessions = prefs.getInt(KEY_TODAY_SESSIONS, 0)
        tvSessions.text = sessions.toString()
    }

    private fun resetDailyStatsIfNeeded() {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val savedDate = prefs.getString(KEY_STAT_DATE, null)
        if (savedDate != today) {
            prefs.edit()
                .putString(KEY_STAT_DATE, today)
                .putLong(KEY_TODAY_FOCUS_MS, 0L)
                .putInt(KEY_TODAY_SESSIONS, 0)
                .apply()
        }
    }

    private fun formatTimer(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return "${hours}:%02d:%02d".format(minutes, seconds)
    }

    private fun formatDuration(ms: Long): String {
        val totalMinutes = ms / 60000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "<1m"
        }
    }

    private fun updateTagSection() {
        val pairedId = prefs.getString(KEY_PAIRED_TAG_ID, null)
        if (pairedId != null) {
            tagUnpaired.visibility = View.GONE
            tagPaired.visibility = View.VISIBLE
            val name = prefs.getString(KEY_TAG_NAME, null) ?: "My Brick"
            tvTagName.text = name
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

    private var adminTagList: LinearLayout? = null
    private var adminTagCount: TextView? = null
    private var adminSheet: BottomSheetDialog? = null

    private fun showAdminSheet() {
        findViewById<TextView>(R.id.tvBrand).setTextColor(0xFFFF453A.toInt())

        val sheet = BottomSheetDialog(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF000000.toInt())
            setPadding(0, 0, 0, dpToPx(24))
        }

        // Handle
        val handle = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(40), dpToPx(4)).apply {
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                topMargin = dpToPx(12)
                bottomMargin = dpToPx(16)
            }
            setBackgroundResource(R.drawable.bg_handle)
        }
        root.addView(handle)

        // Title
        val title = TextView(this).apply {
            text = "Admin — Register Bricks"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(dpToPx(24), 0, dpToPx(24), dpToPx(4))
        }
        root.addView(title)

        // Subtitle with count
        val countTv = TextView(this).apply {
            text = "Scan NFC tags to register. 0 scanned."
            setTextColor(0xFF8E8E93.toInt())
            textSize = 13f
            setPadding(dpToPx(24), 0, dpToPx(24), dpToPx(16))
        }
        root.addView(countTv)
        adminTagCount = countTv

        // Divider
        root.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
            setBackgroundColor(0xFF1C1C1E.toInt())
        })

        // Scrollable tag list
        val scroll = android.widget.ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(300)
            )
        }
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(24), dpToPx(8), dpToPx(24), dpToPx(8))
        }
        scroll.addView(list)
        root.addView(scroll)

        // Empty state
        val emptyHint = TextView(this).apply {
            text = "Hold an NFC tag to the back of your phone..."
            setTextColor(0xFF48484A.toInt())
            textSize = 14f
            gravity = android.view.Gravity.CENTER
            setPadding(dpToPx(24), dpToPx(40), dpToPx(24), dpToPx(40))
            tag = "empty_hint"
        }
        list.addView(emptyHint)

        adminTagList = list
        adminSheet = sheet

        sheet.setContentView(root)
        sheet.setOnDismissListener {
            adminMode = false
            adminTagList = null
            adminTagCount = null
            adminSheet = null
            findViewById<TextView>(R.id.tvBrand).setTextColor(0xFFFFFFFF.toInt())
        }
        sheet.show()
    }

    private var adminScannedCount = 0

    private fun addAdminTagEntry(tagUid: String, success: Boolean) {
        val list = adminTagList ?: return

        // Remove empty hint on first scan
        val hint = list.findViewWithTag<View>("empty_hint")
        if (hint != null) list.removeView(hint)

        adminScannedCount++

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, dpToPx(10), 0, dpToPx(10))
        }

        val status = TextView(this).apply {
            text = if (success) "✓" else "✗"
            setTextColor(if (success) 0xFF30D158.toInt() else 0xFFFF3B30.toInt())
            textSize = 18f
            setPadding(0, 0, dpToPx(12), 0)
        }

        val uid = TextView(this).apply {
            text = tagUid
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 14f
            typeface = android.graphics.Typeface.MONOSPACE
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val label = TextView(this).apply {
            text = if (success) "Registered" else "Failed"
            setTextColor(if (success) 0xFF30D158.toInt() else 0xFFFF3B30.toInt())
            textSize = 12f
        }

        row.addView(status)
        row.addView(uid)
        row.addView(label)
        list.addView(row, 0) // newest at top

        adminTagCount?.text = "Scan NFC tags to register. $adminScannedCount scanned."
    }

    private fun updateAppCount() {
        val count = prefs.getStringSet(KEY_BLOCKED_APPS, emptySet())?.size ?: 0
        tvAppCount.text = count.toString()
    }

    private fun updateBlockAllVisibility(blockAll: Boolean) {
        if (blockAll) {
            groupsSection.visibility = View.GONE
            appsSection.visibility = View.GONE
        } else {
            groupsSection.visibility = View.VISIBLE
            appsSection.visibility = View.VISIBLE
        }
    }

    private fun setupGroups() {
        val rvGroups = findViewById<RecyclerView>(R.id.rvGroups)
        rvGroups.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val groups = GroupManager.loadGroups(prefs, installedPackages).toMutableList()
        groupAdapter = GroupAdapter(groups,
            onClick = { group ->
                showGroupDetail(group)
            },
            onLongPress = { group ->
                if (!group.isPreset) {
                    AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog)
                        .setTitle("Delete \"${group.name}\"?")
                        .setMessage("This group will be removed.")
                        .setPositiveButton("Delete") { _, _ ->
                            GroupManager.deleteCustomGroup(prefs, group.id)
                            refreshGroups()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
        )
        rvGroups.adapter = groupAdapter

        findViewById<TextView>(R.id.btnAddGroup).setOnClickListener {
            showCreateGroupDialog()
        }
    }

    private fun refreshGroups() {
        val groups = GroupManager.loadGroups(prefs, installedPackages)
        groupAdapter.updateGroups(groups)
    }

    private fun showGroupDetail(group: AppGroup) {
        val bottomSheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.sheet_group_detail, null)
        bottomSheet.setContentView(view)

        val pm = packageManager
        val tvEmoji = view.findViewById<TextView>(R.id.tvDetailEmoji)
        val tvName = view.findViewById<TextView>(R.id.tvDetailName)
        val switchBlock = view.findViewById<SwitchMaterial>(R.id.switchDetailBlock)
        val appsContainer = view.findViewById<LinearLayout>(R.id.appsContainer)
        val btnAddApps = view.findViewById<TextView>(R.id.btnAddApps)

        tvEmoji.text = group.emoji
        tvName.text = group.name
        switchBlock.isChecked = group.isSelected
        switchBlock.setOnCheckedChangeListener { _, isChecked ->
            group.isSelected = isChecked
            GroupManager.toggleGroup(prefs, group.id, isChecked)
        }

        fun rebuildAppList() {
            appsContainer.removeAllViews()
            val packages = GroupManager.getGroupPackages(prefs, group.id, installedPackages).sorted().toList()
            val columns = 4
            val iconSize = dpToPx(44)

            packages.chunked(columns).forEach { rowPkgs ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6))
                }

                rowPkgs.forEach { pkg ->
                    val cell = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = android.view.Gravity.CENTER
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        setPadding(0, dpToPx(8), 0, dpToPx(8))
                    }

                    val icon = android.widget.ImageView(this).apply {
                        layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
                        try { setImageDrawable(pm.getApplicationIcon(pkg)) }
                        catch (_: Exception) { setImageResource(android.R.drawable.sym_def_app_icon) }
                    }

                    val label = TextView(this).apply {
                        text = try { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }
                               catch (_: Exception) { pkg.substringAfterLast('.') }
                        setTextColor(0xFFBBBBBB.toInt())
                        textSize = 10f
                        maxLines = 1
                        gravity = android.view.Gravity.CENTER
                        ellipsize = android.text.TextUtils.TruncateAt.END
                        setPadding(0, dpToPx(4), 0, 0)
                    }

                    cell.addView(icon)
                    cell.addView(label)
                    cell.setOnClickListener {
                        GroupManager.removeFromGroup(prefs, group.id, pkg, installedPackages)
                        rebuildAppList()
                        refreshGroups()
                    }

                    row.addView(cell)
                }

                // Pad remaining cells for alignment
                repeat(columns - rowPkgs.size) {
                    row.addView(View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
                    })
                }

                appsContainer.addView(row)
            }

            if (packages.isEmpty()) {
                val empty = TextView(this).apply {
                    text = "No apps in this group"
                    setTextColor(0xFF48484A.toInt())
                    textSize = 14f
                    gravity = android.view.Gravity.CENTER
                    setPadding(dpToPx(24), dpToPx(24), dpToPx(24), dpToPx(16))
                }
                appsContainer.addView(empty)
            }
        }

        rebuildAppList()

        btnAddApps.setOnClickListener {
            val currentPkgs = GroupManager.getGroupPackages(prefs, group.id, installedPackages)
            val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val available = pm.queryIntentActivities(launchIntent, 0)
                .map { it.activityInfo }
                .filter { it.packageName != packageName && !currentPkgs.contains(it.packageName) }
                .distinctBy { it.packageName }
                .sortedBy { it.loadLabel(pm).toString().lowercase() }

            val appNames = available.map { it.loadLabel(pm).toString() }.toTypedArray()
            val appPackages = available.map { it.packageName }
            val checked = BooleanArray(appNames.size)

            AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog)
                .setTitle("Add to ${group.name}")
                .setMultiChoiceItems(appNames, checked) { _, which, isChecked ->
                    checked[which] = isChecked
                }
                .setPositiveButton("Add") { _, _ ->
                    appPackages.forEachIndexed { i, pkg ->
                        if (checked[i]) {
                            GroupManager.addToGroup(prefs, group.id, pkg, installedPackages)
                        }
                    }
                    rebuildAppList()
                    refreshGroups()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        bottomSheet.setOnDismissListener {
            refreshGroups()
        }

        bottomSheet.show()
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private fun showCreateGroupDialog() {
        val pm = packageManager
        val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val launchableApps = pm.queryIntentActivities(launchIntent, 0)
            .map { it.activityInfo }
            .filter { it.packageName != packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }

        val appNames = launchableApps.map { it.loadLabel(pm).toString() }.toTypedArray()
        val appPackages = launchableApps.map { it.packageName }
        val checked = BooleanArray(appNames.size)

        // First: name input
        val nameInput = EditText(this)
        nameInput.hint = "e.g. Work Apps, Focus Mode"
        nameInput.setTextColor(0xFFEEEEEE.toInt())
        nameInput.setHintTextColor(0xFF666666.toInt())
        nameInput.setBackgroundColor(0xFF1A1A1A.toInt())
        nameInput.setPadding(48, 32, 48, 32)

        AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog)
            .setTitle("New Group")
            .setView(nameInput)
            .setPositiveButton("Next") { _, _ ->
                val groupName = nameInput.text.toString().trim().ifEmpty { "My Group" }

                // Second: app picker
                AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog)
                    .setTitle("Select apps for \"$groupName\"")
                    .setMultiChoiceItems(appNames, checked) { _, which, isChecked ->
                        checked[which] = isChecked
                    }
                    .setPositiveButton("Create") { _, _ ->
                        val selectedPackages = appPackages.filterIndexed { i, _ -> checked[i] }.toSet()
                        if (selectedPackages.isNotEmpty()) {
                            val group = AppGroup(
                                id = "custom_${UUID.randomUUID()}",
                                name = groupName,
                                emoji = "📂",
                                packages = selectedPackages,
                                isPreset = false
                            )
                            GroupManager.saveCustomGroup(prefs, group)
                            refreshGroups()
                            Toast.makeText(this, "Group \"$groupName\" created!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "No apps selected.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupAppList() {
        val pm = packageManager

        // Get all apps that have a launcher intent (user-visible apps)
        val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val launchableApps = pm.queryIntentActivities(launchIntent, 0)

        val activityInfos = launchableApps
            .map { it.activityInfo }
            .filter { it.packageName != packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }

        installedPackages = activityInfos.map { it.packageName }.toSet()

        rebuildBlockedAppsGrid()

        findViewById<TextView>(R.id.btnAddApps).setOnClickListener {
            showAppPickerSheet()
        }
    }

    private fun rebuildBlockedAppsGrid() {
        val container = findViewById<LinearLayout>(R.id.blockedAppsContainer)
        container.removeAllViews()

        val pm = packageManager
        val blockedSet = prefs.getStringSet(KEY_BLOCKED_APPS, emptySet()) ?: emptySet()
        val blockedList = blockedSet.sorted()

        updateAppCount()

        if (blockedList.isEmpty()) {
            val empty = TextView(this).apply {
                text = "no apps blocked"
                setTextColor(0xFF48484A.toInt())
                textSize = 13f
                gravity = android.view.Gravity.CENTER
                setPadding(dpToPx(24), dpToPx(20), dpToPx(24), dpToPx(8))
            }
            container.addView(empty)
            return
        }

        val columns = 5
        val iconSize = dpToPx(40)

        blockedList.chunked(columns).forEach { rowPkgs ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
            }

            rowPkgs.forEach { pkg ->
                val cell = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = android.view.Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    setPadding(0, dpToPx(6), 0, dpToPx(6))
                }

                val icon = android.widget.ImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
                    try { setImageDrawable(pm.getApplicationIcon(pkg)) }
                    catch (_: Exception) { setImageResource(android.R.drawable.sym_def_app_icon) }
                }

                val label = TextView(this).apply {
                    text = try { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }
                           catch (_: Exception) { pkg.substringAfterLast('.') }
                    setTextColor(0xFF8E8E93.toInt())
                    textSize = 9f
                    maxLines = 1
                    gravity = android.view.Gravity.CENTER
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    setPadding(0, dpToPx(3), 0, 0)
                }

                cell.addView(icon)
                cell.addView(label)
                cell.setOnClickListener {
                    val current = prefs.getStringSet(KEY_BLOCKED_APPS, emptySet())?.toMutableSet() ?: mutableSetOf()
                    current.remove(pkg)
                    prefs.edit().putStringSet(KEY_BLOCKED_APPS, current).apply()
                    rebuildBlockedAppsGrid()
                }

                row.addView(cell)
            }

            repeat(columns - rowPkgs.size) {
                row.addView(View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
                })
            }

            container.addView(row)
        }
    }

    private fun showAppPickerSheet() {
        val pm = packageManager
        val blockedSet = prefs.getStringSet(KEY_BLOCKED_APPS, emptySet()) ?: emptySet()

        val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val allApps = pm.queryIntentActivities(launchIntent, 0)
            .map { it.activityInfo }
            .filter { it.packageName != packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }

        val sheet = BottomSheetDialog(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF000000.toInt())
            setPadding(0, 0, 0, dpToPx(24))
        }

        // Handle
        root.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(36), dpToPx(4)).apply {
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                topMargin = dpToPx(12)
                bottomMargin = dpToPx(16)
            }
            setBackgroundResource(R.drawable.bg_handle)
        })

        // Title
        root.addView(TextView(this).apply {
            text = "Add Apps"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 17f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(dpToPx(24), 0, dpToPx(24), dpToPx(4))
        })

        root.addView(TextView(this).apply {
            text = "tap to block"
            setTextColor(0xFF48484A.toInt())
            textSize = 12f
            setPadding(dpToPx(24), 0, dpToPx(24), dpToPx(12))
        })

        // Scrollable app list
        val scroll = android.widget.ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(400)
            )
        }
        val listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(20), 0, dpToPx(20), 0)
        }
        scroll.addView(listContainer)
        root.addView(scroll)

        fun buildPickerList() {
            listContainer.removeAllViews()
            val currentBlocked = prefs.getStringSet(KEY_BLOCKED_APPS, emptySet()) ?: emptySet()

            allApps.forEach { activityInfo ->
                val pkg = activityInfo.packageName
                val isBlocked = currentBlocked.contains(pkg)

                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(dpToPx(16), dpToPx(10), dpToPx(16), dpToPx(10))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = dpToPx(2) }
                    setBackgroundResource(R.drawable.bg_app_item)
                }

                val icon = android.widget.ImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(dpToPx(36), dpToPx(36))
                    setImageDrawable(activityInfo.loadIcon(pm))
                }

                val name = TextView(this).apply {
                    text = activityInfo.loadLabel(pm).toString()
                    setTextColor(if (isBlocked) 0xFFFFFFFF.toInt() else 0xFF8E8E93.toInt())
                    textSize = 14f
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginStart = dpToPx(14)
                    }
                }

                val indicator = TextView(this).apply {
                    text = if (isBlocked) "✓" else ""
                    setTextColor(0xFF32D74B.toInt())
                    textSize = 16f
                }

                row.addView(icon)
                row.addView(name)
                row.addView(indicator)

                row.setOnClickListener {
                    val current = prefs.getStringSet(KEY_BLOCKED_APPS, emptySet())?.toMutableSet() ?: mutableSetOf()
                    if (isBlocked) {
                        current.remove(pkg)
                    } else {
                        current.add(pkg)
                    }
                    prefs.edit().putStringSet(KEY_BLOCKED_APPS, current).apply()
                    buildPickerList()
                }

                listContainer.addView(row)
            }
        }

        buildPickerList()

        sheet.setOnDismissListener {
            rebuildBlockedAppsGrid()
        }

        sheet.setContentView(root)
        sheet.show()
    }
}
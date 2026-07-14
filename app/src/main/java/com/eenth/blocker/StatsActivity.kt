package com.eenth.blocker

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StatsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)

        val permHint = findViewById<TextView>(R.id.tvPermHint)
        permHint.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        // Bottom nav
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener { finish() }

        loadStats()
    }

    override fun onResume() {
        super.onResume()
        loadStats()
    }

    private fun hasUsagePermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun loadStats() {
        val hasPermission = hasUsagePermission()
        val permHint = findViewById<TextView>(R.id.tvPermHint)

        val focusMs = getFocusTimeToday()
        val sessions = prefs.getInt(MainActivity.KEY_TODAY_SESSIONS, 0)
        findViewById<TextView>(R.id.tvFocusTime).text = formatDuration(focusMs)
        findViewById<TextView>(R.id.tvStatSessions).text = sessions.toString()

        val streak = calculateStreak()
        findViewById<TextView>(R.id.tvStreak).text = streak.toString()

        loadFocusChart()

        if (hasPermission) {
            permHint.visibility = View.GONE
            loadUsageStats()
            loadScreenTimeChart()
            loadPickupsChart()
        } else {
            permHint.visibility = View.VISIBLE
            findViewById<TextView>(R.id.tvScreenTime).text = "—"
            findViewById<TextView>(R.id.tvPickups).text = "—"
        }
    }

    private fun getFocusTimeToday(): Long {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
        val savedDate = prefs.getString(MainActivity.KEY_STAT_DATE, null)
        if (savedDate != today) return 0L

        var totalMs = prefs.getLong(MainActivity.KEY_TODAY_FOCUS_MS, 0L)
        val startTime = prefs.getLong(MainActivity.KEY_BRICK_START_TIME, 0L)
        if (prefs.getBoolean(MainActivity.KEY_IS_BRICKED, false) && startTime > 0L) {
            totalMs += System.currentTimeMillis() - startTime
        }
        return totalMs
    }

    // ---- Focus Time Bar Chart (7 days) ----
    private fun loadFocusChart() {
        val chart = findViewById<BarChart>(R.id.chartFocus)
        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()
        val colors = mutableListOf<Int>()
        val dayFmt = SimpleDateFormat("EEE", Locale.US)
        val numFmt = SimpleDateFormat("d", Locale.US)
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayStr = dateFmt.format(java.util.Date())

        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = dateFmt.format(cal.time)
            var focusMin = prefs.getLong("focus_$dateStr", 0L) / 60000f
            if (dateStr == todayStr) focusMin = getFocusTimeToday() / 60000f
            entries.add(BarEntry((6 - i).toFloat(), focusMin))
            labels.add("${dayFmt.format(cal.time)}\n${numFmt.format(cal.time)}")
            colors.add(if (i == 0) Color.parseColor("#32D74B") else Color.parseColor("#1C1C1E"))
        }

        val dataSet = BarDataSet(entries, "").apply {
            this.colors = colors
            setDrawValues(false)
        }

        styleBarChart(chart, labels)
        chart.data = BarData(dataSet).apply { barWidth = 0.5f }
        chart.invalidate()
    }

    // ---- Screen Time Stacked Bar Chart (7 days, per-app breakdown) ----
    private val appColors = intArrayOf(
        Color.parseColor("#FF453A"), // red
        Color.parseColor("#FF9F0A"), // orange
        Color.parseColor("#FFD60A"), // yellow
        Color.parseColor("#32D74B"), // green
        Color.parseColor("#64D2FF"), // cyan
        Color.parseColor("#BF5AF2"), // purple
    )

    private fun loadScreenTimeChart() {
        val chart = findViewById<BarChart>(R.id.chartScreenTime)
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val pm = packageManager
        val dayFmt = SimpleDateFormat("EEE", Locale.US)
        val dateFmt = SimpleDateFormat("d", Locale.US)

        // Collect per-app usage for each of 7 days
        data class DayData(val label: String, val appMinutes: MutableMap<String, Float> = mutableMapOf())
        val days = mutableListOf<DayData>()
        val globalAppTotals = mutableMapOf<String, Float>()

        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val label = "${dayFmt.format(cal.time)}\n${dateFmt.format(cal.time)}"

            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val dayStart = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val dayEnd = cal.timeInMillis

            val dayData = DayData(label)
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, dayStart, dayEnd)
            stats?.forEach { s ->
                if (s.totalTimeInForeground > 60000 && s.packageName != packageName) {
                    val mins = s.totalTimeInForeground / 60000f
                    dayData.appMinutes[s.packageName] = mins
                    globalAppTotals[s.packageName] = (globalAppTotals[s.packageName] ?: 0f) + mins
                }
            }
            days.add(dayData)
        }

        // Top 5 apps by total usage across the week, rest lumped into "Other"
        val topApps = globalAppTotals.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }

        val stackLabels = topApps.map { pkg ->
            try { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }
            catch (_: Exception) { pkg.substringAfterLast('.') }
        }.toMutableList()
        stackLabels.add("Other")

        val stackCount = stackLabels.size // top 5 + Other = 6

        // Build stacked entries
        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()

        days.forEachIndexed { idx, day ->
            val vals = FloatArray(stackCount)
            topApps.forEachIndexed { appIdx, pkg ->
                vals[appIdx] = day.appMinutes[pkg] ?: 0f
            }
            // "Other" = everything not in top 5
            var other = 0f
            day.appMinutes.forEach { (pkg, mins) ->
                if (pkg !in topApps) other += mins
            }
            vals[stackCount - 1] = other

            entries.add(BarEntry(idx.toFloat(), vals))
            labels.add(day.label)
        }

        val colors = mutableListOf<Int>()
        for (i in 0 until stackCount - 1) {
            colors.add(appColors[i % appColors.size])
        }
        colors.add(Color.parseColor("#2C2C2E")) // "Other" gray

        val dataSet = BarDataSet(entries, "").apply {
            this.colors = colors
            this.stackLabels = stackLabels.toTypedArray()
            setDrawValues(false)
        }

        chart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBorders(false)
            setFitBars(true)
            setTouchEnabled(false)

            legend.apply {
                isEnabled = true
                textColor = Color.parseColor("#8E8E93")
                textSize = 10f
                form = Legend.LegendForm.CIRCLE
                formSize = 8f
                xEntrySpace = 12f
                yEntrySpace = 4f
                isWordWrapEnabled = true
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                yOffset = 8f
            }

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawAxisLine(false)
                textColor = Color.parseColor("#48484A")
                textSize = 10f
                granularity = 1f
                valueFormatter = IndexAxisValueFormatter(labels)
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#1A1A1A")
                setDrawAxisLine(false)
                textColor = Color.parseColor("#48484A")
                textSize = 9f
                axisMinimum = 0f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return if (value >= 60) "${(value / 60).toInt()}h" else "${value.toInt()}m"
                    }
                }
            }

            axisRight.isEnabled = false

            data = BarData(dataSet).apply { barWidth = 0.55f }
            animateY(600)
            invalidate()
        }
    }

    // ---- Pickups Bar Chart (7 days) ----
    private fun loadPickupsChart() {
        val chart = findViewById<BarChart>(R.id.chartPickups)
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()
        val dayFmt = SimpleDateFormat("EEE", Locale.US)
        val numFmt = SimpleDateFormat("d", Locale.US)

        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val dayStart = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val dayEnd = cal.timeInMillis

            var pickups = 0
            val events = usm.queryEvents(dayStart, dayEnd)
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.KEYGUARD_HIDDEN) pickups++
            }

            entries.add(BarEntry((6 - i).toFloat(), pickups.toFloat()))
            val labelCal = Calendar.getInstance()
            labelCal.add(Calendar.DAY_OF_YEAR, -i)
            labels.add("${dayFmt.format(labelCal.time)}\n${numFmt.format(labelCal.time)}")
        }

        val colors = MutableList(7) { Color.parseColor("#1C1C1E") }
        colors[6] = Color.parseColor("#FF9F0A")

        val dataSet = BarDataSet(entries, "").apply {
            this.colors = colors
            setDrawValues(false)
        }

        styleBarChart(chart, labels)
        chart.data = BarData(dataSet).apply { barWidth = 0.5f }
        chart.invalidate()
    }

    // ---- Top Apps Horizontal Bar Chart ----
    private fun loadUsageStats() {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis
        val now = System.currentTimeMillis()

        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startOfDay, now)
        val appUsage = mutableMapOf<String, Long>()
        var totalScreenTime = 0L

        stats?.forEach { stat ->
            if (stat.totalTimeInForeground > 0 && stat.packageName != packageName) {
                appUsage[stat.packageName] = stat.totalTimeInForeground
                totalScreenTime += stat.totalTimeInForeground
            }
        }

        findViewById<TextView>(R.id.tvScreenTime).text = formatDuration(totalScreenTime)

        // Pickups today
        var pickups = 0
        val events = usm.queryEvents(startOfDay, now)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.KEYGUARD_HIDDEN) pickups++
        }
        findViewById<TextView>(R.id.tvPickups).text = pickups.toString()

        // Top apps chart
        loadTopAppsChart(appUsage)
    }

    private fun loadTopAppsChart(appUsage: Map<String, Long>) {
        val chart = findViewById<HorizontalBarChart>(R.id.chartTopApps)
        val pm = packageManager

        val sorted = appUsage.entries
            .sortedByDescending { it.value }
            .take(6)
            .filter { it.value > 60000 }
            .reversed()

        if (sorted.isEmpty()) {
            chart.setNoDataText("no usage data yet")
            chart.setNoDataTextColor(Color.parseColor("#48484A"))
            chart.invalidate()
            return
        }

        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()

        sorted.forEachIndexed { i, (pkg, time) ->
            entries.add(BarEntry(i.toFloat(), time / 60000f))
            val name = try { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }
                       catch (_: Exception) { pkg.substringAfterLast('.') }
            labels.add(name)
        }

        val dataSet = BarDataSet(entries, "").apply {
            color = Color.parseColor("#32D74B")
            setDrawValues(true)
            valueTextColor = Color.parseColor("#8E8E93")
            valueTextSize = 10f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val h = (value / 60).toInt()
                    val m = (value % 60).toInt()
                    return if (h > 0) "${h}h ${m}m" else "${m}m"
                }
            }
        }

        chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setDrawGridBackground(false)
            setDrawBorders(false)
            setFitBars(true)
            setTouchEnabled(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawAxisLine(false)
                textColor = Color.parseColor("#8E8E93")
                textSize = 11f
                granularity = 1f
                valueFormatter = IndexAxisValueFormatter(labels)
            }

            axisLeft.apply {
                setDrawGridLines(false)
                setDrawAxisLine(false)
                setDrawLabels(false)
                axisMinimum = 0f
            }

            axisRight.apply {
                setDrawGridLines(false)
                setDrawAxisLine(false)
                setDrawLabels(false)
            }

            data = BarData(dataSet).apply { barWidth = 0.6f }
            animateY(600)
            invalidate()
        }
    }

    // ---- Chart styling helpers ----
    private fun styleBarChart(chart: BarChart, labels: List<String>) {
        chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setDrawGridBackground(false)
            setDrawBorders(false)
            setFitBars(true)
            setTouchEnabled(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawAxisLine(false)
                textColor = Color.parseColor("#48484A")
                textSize = 10f
                granularity = 1f
                valueFormatter = IndexAxisValueFormatter(labels)
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#1A1A1A")
                setDrawAxisLine(false)
                textColor = Color.parseColor("#48484A")
                textSize = 9f
                axisMinimum = 0f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return if (value >= 60) "${(value / 60).toInt()}h" else "${value.toInt()}m"
                    }
                }
            }

            axisRight.isEnabled = false
            animateY(600)
        }
    }

    private fun calculateStreak(): Int {
        val cal = Calendar.getInstance()
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        var streak = 0

        val todaySessions = prefs.getInt(MainActivity.KEY_TODAY_SESSIONS, 0)
        val todayStr = dateFmt.format(cal.time)
        val savedDate = prefs.getString(MainActivity.KEY_STAT_DATE, null)

        if (savedDate == todayStr && todaySessions > 0) {
            streak = 1
        } else {
            return 0
        }

        for (i in 1..365) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val dateStr = dateFmt.format(cal.time)
            val dayFocus = prefs.getLong("focus_$dateStr", 0L)
            if (dayFocus > 0) streak++ else break
        }

        return streak
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

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}

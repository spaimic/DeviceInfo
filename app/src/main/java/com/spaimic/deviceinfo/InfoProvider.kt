package com.spaimic.deviceinfo

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.text.format.Formatter
import java.io.File

typealias Row = Pair<String, String>

object InfoProvider {

    private fun read(path: String): String? =
        try { File(path).readText().trim().ifEmpty { null } } catch (_: Exception) { null }

    fun dashboard(c: Context): List<Row> {
        val am = c.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val st = StatFs(Environment.getDataDirectory().path)
        val bat = c.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = bat?.let {
            it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) * 100 /
                it.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        }
        val temp = bat?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)?.div(10.0)
        return listOf(
            "Model" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "Device" to Build.DEVICE,
            "Board" to Build.BOARD,
            "Android" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            "Security patch" to (Build.VERSION.SECURITY_PATCH),
            "Kernel" to (System.getProperty("os.version") ?: "N/A"),
            "RAM" to "${Formatter.formatFileSize(c, mi.availMem)} free / ${Formatter.formatFileSize(c, mi.totalMem)}",
            "Storage" to "${Formatter.formatFileSize(c, st.availableBytes)} free / ${Formatter.formatFileSize(c, st.totalBytes)}",
            "Battery" to "${level ?: "?"}%  ${temp ?: "?"}°C",
        )
    }

    fun cpu(): List<Row> {
        val rows = mutableListOf<Row>()
        val info = read("/proc/cpuinfo") ?: ""
        val hw = info.lineSequence().firstOrNull { it.startsWith("Hardware") }
            ?.substringAfter(":")?.trim()
        rows += "SoC" to (hw ?: if (Build.VERSION.SDK_INT >= 31) Build.SOC_MODEL else Build.HARDWARE)
        rows += "ABIs" to Build.SUPPORTED_ABIS.joinToString()
        val n = Runtime.getRuntime().availableProcessors()
        rows += "Cores" to n.toString()
        for (i in 0 until n) {
            val base = "/sys/devices/system/cpu/cpu$i/cpufreq"
            val cur = read("$base/scaling_cur_freq")?.toLongOrNull()?.let { "${it / 1000} MHz" }
            val max = read("$base/cpuinfo_max_freq")?.toLongOrNull()?.let { "${it / 1000} MHz" }
            rows += "Core $i" to if (cur == null) "offline / not readable" else "$cur (max ${max ?: "?"})"
        }
        rows += "Governor" to (read("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor") ?: "N/A")
        return rows
    }
}

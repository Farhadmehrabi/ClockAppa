package com.example.clockapp

import java.util.Calendar

/**
 * days: مقادیر Calendar.DAY_OF_WEEK (یکشنبه=1 ... شنبه=7).
 * اگر days خالی باشد، آلارم فقط یک‌بار زنگ می‌خورد.
 */
data class Alarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val enabled: Boolean = true,
    val days: Set<Int> = emptySet(),
    /** آدرس (URI) زنگ دلخواه؛ null یعنی زنگ پیش‌فرض آلارم گوشی */
    val ringtone: String? = null
) {
    fun nextTriggerMillis(from: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = from
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        repeat(8) {
            val okDay = days.isEmpty() || cal.get(Calendar.DAY_OF_WEEK) in days
            if (cal.timeInMillis > from && okDay) return cal.timeInMillis
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.timeInMillis
    }
}

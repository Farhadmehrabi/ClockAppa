package com.example.clockapp

import com.ibm.icu.util.PersianCalendar
import java.util.Calendar
import java.util.Date

object Jalali {
    private val months = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    private val weekdays = mapOf(
        Calendar.SATURDAY to "شنبه",
        Calendar.SUNDAY to "یکشنبه",
        Calendar.MONDAY to "دوشنبه",
        Calendar.TUESDAY to "سه‌شنبه",
        Calendar.WEDNESDAY to "چهارشنبه",
        Calendar.THURSDAY to "پنج‌شنبه",
        Calendar.FRIDAY to "جمعه"
    )

    fun toPersianDigits(s: String): String = s.map { c ->
        if (c in '0'..'9') '۰' + (c - '0') else c
    }.joinToString("")

    /** مثال: «یکشنبه ۱۹ مهر ۱۴۰۵» */
    fun format(date: Date): String {
        val pc = PersianCalendar()
        pc.time = date
        val year = pc.get(com.ibm.icu.util.Calendar.YEAR)
        val month = pc.get(com.ibm.icu.util.Calendar.MONTH)
        val day = pc.get(com.ibm.icu.util.Calendar.DAY_OF_MONTH)

        val g = Calendar.getInstance().apply { time = date }
        val weekday = weekdays[g.get(Calendar.DAY_OF_WEEK)].orEmpty()

        return toPersianDigits("$weekday $day ${months[month]} $year")
    }
}

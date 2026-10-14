package com.example.clockapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // بعد از ریستارت گوشی یا آپدیت اپ، آلارم‌ها دوباره زمان‌بندی می‌شوند
        AlarmScheduler.rescheduleAll(context)

        // اگر تایمری در حال اجرا بود، دوباره زمان‌بندی شود
        val timerEnd = TimerStore(context).endAt
        if (timerEnd > System.currentTimeMillis()) {
            AlarmScheduler.scheduleTimer(context, timerEnd)
        }
    }
}

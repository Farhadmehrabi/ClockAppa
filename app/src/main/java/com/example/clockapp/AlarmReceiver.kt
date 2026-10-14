package com.example.clockapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // پایان تایمر
        if (intent.getBooleanExtra(AlarmScheduler.EXTRA_TIMER, false)) {
            val svc = Intent(context, AlarmService::class.java)
                .setAction(AlarmService.ACTION_START)
                .putExtra("label", "زمان تایمر تمام شد")
            ContextCompat.startForegroundService(context, svc)
            return
        }

        val id = intent.getIntExtra(AlarmScheduler.EXTRA_ID, -1)
        val isSnooze = intent.getBooleanExtra(AlarmScheduler.EXTRA_SNOOZE, false)
        val store = AlarmStore(context)
        val alarm = store.get(id) ?: return

        val svc = Intent(context, AlarmService::class.java)
            .setAction(AlarmService.ACTION_START)
            .putExtra(AlarmScheduler.EXTRA_ID, id)
            .putExtra("label", alarm.label)
            .putExtra("ringtone", alarm.ringtone)
        ContextCompat.startForegroundService(context, svc)

        if (!isSnooze) {
            if (alarm.days.isEmpty()) {
                // آلارم یک‌بارمصرف: بعد از زنگ خاموش می‌شود
                store.save(store.load().map { if (it.id == id) it.copy(enabled = false) else it })
            } else {
                // آلارم تکرارشونده: نوبت بعدی را زمان‌بندی کن
                AlarmScheduler.schedule(context, alarm)
            }
        }
    }
}

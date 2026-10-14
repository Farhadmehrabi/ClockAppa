package com.example.clockapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object AlarmScheduler {

    const val EXTRA_ID = "alarm_id"
    const val EXTRA_SNOOZE = "is_snooze"
    const val EXTRA_TIMER = "is_timer"
    private const val SNOOZE_OFFSET = 1_000_000
    private const val TIMER_CODE = 2_000_000

    private fun manager(context: Context) =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun firePending(context: Context, requestCode: Int, id: Int, snooze: Boolean): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .putExtra(EXTRA_ID, id)
            .putExtra(EXTRA_SNOOZE, snooze)
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun showPending(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun setClock(context: Context, at: Long, op: PendingIntent) {
        // setAlarmClock دقیق است و نیازی به مجوز SCHEDULE_EXACT_ALARM ندارد
        manager(context).setAlarmClock(AlarmManager.AlarmClockInfo(at, showPending(context)), op)
    }

    fun schedule(context: Context, alarm: Alarm) {
        if (!alarm.enabled) {
            cancel(context, alarm.id)
            return
        }
        setClock(context, alarm.nextTriggerMillis(), firePending(context, alarm.id, alarm.id, false))
    }

    fun scheduleSnooze(context: Context, id: Int, minutes: Int = 10) {
        val at = System.currentTimeMillis() + minutes * 60_000L
        setClock(context, at, firePending(context, id + SNOOZE_OFFSET, id, true))
    }

    fun cancel(context: Context, id: Int) {
        manager(context).cancel(firePending(context, id, id, false))
        manager(context).cancel(firePending(context, id + SNOOZE_OFFSET, id, true))
    }

    private fun timerPending(context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).putExtra(EXTRA_TIMER, true)
        return PendingIntent.getBroadcast(
            context, TIMER_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun scheduleTimer(context: Context, atMillis: Long) {
        setClock(context, atMillis, timerPending(context))
    }

    fun cancelTimer(context: Context) {
        manager(context).cancel(timerPending(context))
    }

    fun rescheduleAll(context: Context) {
        AlarmStore(context).load().forEach { schedule(context, it) }
    }
}

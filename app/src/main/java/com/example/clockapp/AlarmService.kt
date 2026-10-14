package com.example.clockapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat

class AlarmService : Service() {

    companion object {
        const val ACTION_START = "com.example.clockapp.START"
        const val ACTION_STOP = "com.example.clockapp.STOP"
        const val ACTION_SNOOZE = "com.example.clockapp.SNOOZE"
        private const val CHANNEL_ID = "alarm_channel"
        private const val NOTIFICATION_ID = 42
    }

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var currentId = -1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopAlarm()
                return START_NOT_STICKY
            }
            ACTION_SNOOZE -> {
                val id = intent.getIntExtra(AlarmScheduler.EXTRA_ID, currentId)
                AlarmScheduler.scheduleSnooze(this, id, 10)
                stopAlarm()
                return START_NOT_STICKY
            }
        }

        currentId = intent?.getIntExtra(AlarmScheduler.EXTRA_ID, -1) ?: -1
        val label = intent?.getStringExtra("label").orEmpty()
        val ringtone = intent?.getStringExtra("ringtone")
        startForegroundCompat(buildNotification(label))
        startSound(ringtone)
        return START_NOT_STICKY
    }

    private fun buildNotification(label: String): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "آلارم", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null)
                enableVibration(false)
            }
        )

        fun action(act: String, req: Int) = PendingIntent.getService(
            this, req,
            Intent(this, AlarmService::class.java)
                .setAction(act)
                .putExtra(AlarmScheduler.EXTRA_ID, currentId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(if (currentId >= 0) "آلارم" else "تایمر")
            .setContentText(label.ifBlank { "وقت بیدار شدن است!" })
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setFullScreenIntent(open, true)
            .setContentIntent(open)
            .addAction(0, "توقف", action(ACTION_STOP, 1))
            .apply {
                // برای تایمر دکمه «۱۰ دقیقه بعد» معنی ندارد
                if (currentId >= 0) addAction(0, "۱۰ دقیقه بعد", action(ACTION_SNOOZE, 2))
            }
            .build()
    }

    private fun startForegroundCompat(n: Notification) {
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, n)
        }
    }

    private fun startSound(customUri: String?) {
        stopSound()
        val fallback = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        // اول زنگ انتخابی کاربر؛ اگر باز نشد (مثلاً فایل پاک شده)، زنگ پیش‌فرض
        val candidates = listOfNotNull(customUri?.let { Uri.parse(it) }, fallback)
        for (uri in candidates) {
            val mp = MediaPlayer()
            try {
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                mp.setDataSource(this, uri)
                mp.isLooping = true
                mp.prepare()
                mp.start()
                player = mp
                break
            } catch (e: Exception) {
                mp.release()
            }
        }
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 600, 600), 0))
    }

    private fun stopSound() {
        player?.runCatching { stop(); release() }
        player = null
        vibrator?.cancel()
    }

    private fun stopAlarm() {
        stopSound()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopSound()
        super.onDestroy()
    }
}

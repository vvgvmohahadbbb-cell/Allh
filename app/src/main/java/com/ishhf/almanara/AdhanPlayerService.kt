package com.ishhf.almanara

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager

/** يشغّل ملف الأذان الكامل أو تذكير "صلِّ على النبي صلى الله عليه وسلم" - صوت مسجّل بس، بدون رنين إشعار إضافي */
class AdhanPlayerService : Service() {

    companion object {
        private const val CHANNEL_ID = "adhan_channel_silent"
        private const val NOTIF_ID = 500
    }

    private var player: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val isSalawat = intent?.getBooleanExtra("salawat", false) ?: false
        val prayerName = intent?.getStringExtra("prayerName") ?: ""

        createSilentChannel()
        val title = if (isSalawat) "صلِّ على النبي صلى الله عليه وسلم" else "حان وقت صلاة $prayerName"
        startForeground(NOTIF_ID, buildNotification(title))

        acquireWakeLock()

        val resId = if (isSalawat) R.raw.salawat else R.raw.adhan
        try {
            player = MediaPlayer.create(this, resId)
            player?.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            player?.setOnCompletionListener { stopSelfCleanly() }
            player?.start()
        } catch (e: Exception) {
            stopSelfCleanly()
        }

        return START_NOT_STICKY
    }

    /** قناة إشعار بدون أي صوت نظام أو اهتزاز - الصوت يجي بس من ملف الأذان/الصلاة على النبي نفسه */
    private fun createSilentChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(CHANNEL_ID, "الأذان والتذكيرات", NotificationManager.IMPORTANCE_LOW)
            channel.setSound(null, null)
            channel.enableVibration(false)
            mgr.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle(title)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .build()
        }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AlManara:AdhanWakeLock")
        wakeLock?.acquire(6 * 60 * 1000L)
    }

    private fun stopSelfCleanly() {
        player?.release()
        player = null
        wakeLock?.let { if (it.isHeld) it.release() }
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSelfCleanly()
    }
}

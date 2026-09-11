package com.ishhf.almanara

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** يستقبل منبّه دخول وقت الصلاة ويشغّل الأذان الكامل */
class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra("prayerName") ?: "الصلاة"
        val serviceIntent = Intent(context, AdhanPlayerService::class.java)
        serviceIntent.putExtra("prayerName", prayerName)
        context.startService(serviceIntent)
    }
}

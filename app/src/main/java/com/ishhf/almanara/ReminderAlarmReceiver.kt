package com.ishhf.almanara

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** يستقبل منبّه "صلِّ على النبي"، ويعيد جدولة نفسه للمرة الجاية (إذا لسا مفعّل) */
class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("almanara_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("salawat_enabled", true)) {
            val serviceIntent = Intent(context, AdhanPlayerService::class.java)
            serviceIntent.putExtra("salawat", true)
            context.startService(serviceIntent)
        }
        AlarmScheduler.rescheduleSalawatNext(context)
    }
}

package com.ishhf.almanara

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** يستقبل منبّه "صلي على النبي" كل ١٠ دقايق، ويعيد جدولة نفسه للمرة الجاية */
class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, AdhanPlayerService::class.java)
        serviceIntent.putExtra("salawat", true)
        context.startService(serviceIntent)
        AlarmScheduler.rescheduleSalawatNext(context)
    }
}

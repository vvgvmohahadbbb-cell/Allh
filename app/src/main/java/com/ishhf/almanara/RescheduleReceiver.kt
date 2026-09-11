package com.ishhf.almanara

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** يشتغل كل منتصف ليل عشان يعيد حساب أوقات صلاة اليوم الجديد ويجدولها */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AlarmScheduler.scheduleAll(context)
    }
}

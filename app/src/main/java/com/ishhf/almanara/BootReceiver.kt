package com.ishhf.almanara

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** يعيد جدولة كل المنبهات بعد إعادة تشغيل الهاتف (المنبهات بتنمسح عند الريستارت) */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            AlarmScheduler.scheduleAll(context)
        }
    }
}

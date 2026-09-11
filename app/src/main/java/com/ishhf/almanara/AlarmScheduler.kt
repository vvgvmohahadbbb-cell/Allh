package com.ishhf.almanara

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

/**
 * يجدول كل منبهات الصلاة + منبّه "صلي على النبي" كل ١٠ دقايق عبر
 * AlarmManager - هاي منبهات محلية بالكامل، بتشتغل حتى لو التطبيق
 * مقفول تماماً وبدون أي اتصال إنترنت.
 */
object AlarmScheduler {

    private const val REQ_FAJR = 1001
    private const val REQ_SUNRISE = 1002
    private const val REQ_DHUHR = 1003
    private const val REQ_ASR = 1004
    private const val REQ_MAGHRIB = 1005
    private const val REQ_ISHA = 1006
    private const val REQ_SALAWAT = 2001
    private const val REQ_RESCHEDULE = 3001

    private const val SALAWAT_INTERVAL_MS = 10 * 60 * 1000L

    fun scheduleAll(context: Context) {
        val prefs = context.getSharedPreferences("almanara_prefs", Context.MODE_PRIVATE)
        val lat = prefs.getFloat("lat", Float.NaN)
        val lng = prefs.getFloat("lng", Float.NaN)
        if (lat.isNaN() || lng.isNaN()) return

        val cal = Calendar.getInstance()
        val timezone = cal.timeZone.rawOffset / (1000.0 * 60.0 * 60.0)
        val times = PrayerTimeCalculator.calculate(lat.toDouble(), lng.toDouble(), timezone, cal)

        schedulePrayer(context, REQ_FAJR, times.fajr, "الفجر")
        schedulePrayer(context, REQ_SUNRISE, times.sunrise, "الشروق")
        schedulePrayer(context, REQ_DHUHR, times.dhuhr, "الظهر")
        schedulePrayer(context, REQ_ASR, times.asr, "العصر")
        schedulePrayer(context, REQ_MAGHRIB, times.maghrib, "المغرب")
        schedulePrayer(context, REQ_ISHA, times.isha, "العشاء")

        scheduleMidnightReschedule(context)
        scheduleSalawat(context)
    }

    private fun schedulePrayer(context: Context, requestCode: Int, decimalHour: Double, name: String) {
        val cal = Calendar.getInstance()
        val hour = decimalHour.toInt()
        val minute = ((decimalHour - hour) * 60).toInt()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        if (cal.timeInMillis < System.currentTimeMillis()) return // الوقت فات اليوم، رح ينجدول بكرا تلقائياً

        val intent = Intent(context, PrayerAlarmReceiver::class.java)
        intent.putExtra("prayerName", name)
        val pending = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExactAlarm(context, cal.timeInMillis, pending)
    }

    private fun scheduleSalawat(context: Context) {
        val intent = Intent(context, ReminderAlarmReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, REQ_SALAWAT, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExactAlarm(context, System.currentTimeMillis() + SALAWAT_INTERVAL_MS, pending)
    }

    fun rescheduleSalawatNext(context: Context) {
        scheduleSalawat(context)
    }

    private fun scheduleMidnightReschedule(context: Context) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 1)
        cal.set(Calendar.SECOND, 0)

        val intent = Intent(context, RescheduleReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, REQ_RESCHEDULE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExactAlarm(context, cal.timeInMillis, pending)
    }

    private fun setExactAlarm(context: Context, timeMillis: Long, pending: PendingIntent) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, timeMillis, pending)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pending)
            }
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, timeMillis, pending)
        }
    }
}

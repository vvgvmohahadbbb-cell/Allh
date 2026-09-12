package com.ishhf.almanara

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

/**
 * يجدول كل منبهات الصلاة + منبّه "صلِّ على النبي" عبر AlarmManager - منبهات
 * محلية بالكامل، بتشتغل حتى لو التطبيق مقفول تماماً وبدون أي اتصال إنترنت.
 * تدعم تعديل يدوي (بالدقائق) لكل صلاة، وفترة مخصصة لتذكير الصلاة على النبي.
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

    fun scheduleAll(context: Context) {
        val prefs = context.getSharedPreferences("almanara_prefs", Context.MODE_PRIVATE)
        val lat = prefs.getFloat("lat", Float.NaN)
        val lng = prefs.getFloat("lng", Float.NaN)
        if (lat.isNaN() || lng.isNaN()) return

        val cal = Calendar.getInstance()
        val timezone = cal.timeZone.rawOffset / (1000.0 * 60.0 * 60.0)
        val times = PrayerTimeCalculator.calculate(lat.toDouble(), lng.toDouble(), timezone, cal)

        schedulePrayer(context, REQ_FAJR, times.fajr, "الفجر", "offset_fajr")
        schedulePrayer(context, REQ_SUNRISE, times.sunrise, "الشروق", "offset_sunrise")
        schedulePrayer(context, REQ_DHUHR, times.dhuhr, "الظهر", "offset_dhuhr")
        schedulePrayer(context, REQ_ASR, times.asr, "العصر", "offset_asr")
        schedulePrayer(context, REQ_MAGHRIB, times.maghrib, "المغرب", "offset_maghrib")
        schedulePrayer(context, REQ_ISHA, times.isha, "العشاء", "offset_isha")

        scheduleMidnightReschedule(context)
        if (prefs.getBoolean("salawat_enabled", true)) {
            scheduleSalawat(context)
        }
    }

    /** يرجع أوقات اليوم بعد تطبيق التعديلات اليدوية - تستخدمها الشاشة الرئيسية للعرض */
    fun getAdjustedTimes(context: Context, lat: Double, lng: Double, cal: Calendar): Map<String, Double> {
        val prefs = context.getSharedPreferences("almanara_prefs", Context.MODE_PRIVATE)
        val timezone = cal.timeZone.rawOffset / (1000.0 * 60.0 * 60.0)
        val times = PrayerTimeCalculator.calculate(lat, lng, timezone, cal)
        fun adjusted(base: Double, key: String) = base + prefs.getInt(key, 0) / 60.0
        return mapOf(
            "الفجر" to adjusted(times.fajr, "offset_fajr"),
            "الشروق" to adjusted(times.sunrise, "offset_sunrise"),
            "الظهر" to adjusted(times.dhuhr, "offset_dhuhr"),
            "العصر" to adjusted(times.asr, "offset_asr"),
            "المغرب" to adjusted(times.maghrib, "offset_maghrib"),
            "العشاء" to adjusted(times.isha, "offset_isha")
        )
    }

    private fun schedulePrayer(
        context: Context, requestCode: Int, baseDecimalHour: Double, name: String, offsetKey: String
    ) {
        val prefs = context.getSharedPreferences("almanara_prefs", Context.MODE_PRIVATE)
        val offsetMinutes = prefs.getInt(offsetKey, 0)
        val decimalHour = baseDecimalHour + offsetMinutes / 60.0

        val cal = Calendar.getInstance()
        val hour = decimalHour.toInt()
        val minute = ((decimalHour - hour) * 60).toInt()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        if (cal.timeInMillis < System.currentTimeMillis()) return

        val intent = Intent(context, PrayerAlarmReceiver::class.java)
        intent.putExtra("prayerName", name)
        val pending = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExactAlarm(context, cal.timeInMillis, pending)
    }

    private fun scheduleSalawat(context: Context) {
        val prefs = context.getSharedPreferences("almanara_prefs", Context.MODE_PRIVATE)
        val intervalMinutes = prefs.getInt("salawat_interval_min", 10)
        val intent = Intent(context, ReminderAlarmReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, REQ_SALAWAT, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val nextTime = System.currentTimeMillis() + intervalMinutes * 60 * 1000L
        prefs.edit().putLong("salawat_next_time", nextTime).apply()
        setExactAlarm(context, nextTime, pending)
    }

    fun rescheduleSalawatNext(context: Context) {
        val prefs = context.getSharedPreferences("almanara_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("salawat_enabled", true)) {
            scheduleSalawat(context)
        }
    }

    fun cancelSalawat(context: Context) {
        val intent = Intent(context, ReminderAlarmReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, REQ_SALAWAT, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pending)
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

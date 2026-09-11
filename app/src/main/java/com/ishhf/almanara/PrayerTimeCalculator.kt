package com.ishhf.almanara

import java.util.Calendar
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * حساب أوقات الصلاة فلكياً من الموقع والتاريخ، بدون أي اتصال إنترنت.
 * مبني على معادلات فلكية معيارية (ميل الشمس، معادلة الزمن) مستخدمة
 * بمعظم تطبيقات المواقيت. زاوية الفجر ١٨° وزاوية العشاء ١٧° (طريقة
 * رابطة العالم الإسلامي التقريبية) - يفضّل تتأكد من دقتها بمنطقتك
 * ومقارنتها بمواقيت مسجد قريب أول مرة.
 */
object PrayerTimeCalculator {

    private const val FAJR_ANGLE = 18.0
    private const val ISHA_ANGLE = 17.0

    data class PrayerTimes(
        val fajr: Double,
        val sunrise: Double,
        val dhuhr: Double,
        val asr: Double,
        val maghrib: Double,
        val isha: Double
    )

    fun calculate(latitude: Double, longitude: Double, timezone: Double, date: Calendar): PrayerTimes {
        val jd = julianDate(date)
        val dhuhrTime = fixHour(12.0 + timezone - longitude / 15.0 - equationOfTime(jd) / 60.0)
        val declination = sunDeclination(jd)

        val fajr = dhuhrTime - hourAngle(FAJR_ANGLE, latitude, declination) / 15.0
        val sunrise = dhuhrTime - hourAngle(0.833, latitude, declination) / 15.0
        val maghrib = dhuhrTime + hourAngle(0.833, latitude, declination) / 15.0
        val isha = dhuhrTime + hourAngle(ISHA_ANGLE, latitude, declination) / 15.0
        val asr = asrTime(1.0, latitude, declination, dhuhrTime)

        return PrayerTimes(
            fixHour(fajr), fixHour(sunrise), dhuhrTime,
            fixHour(asr), fixHour(maghrib), fixHour(isha)
        )
    }

    private fun julianDate(cal: Calendar): Double {
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun equationOfTime(jd: Double): Double {
        val d = jd - 2451545.0
        val g = normalizeAngle(357.529 + 0.98560028 * d)
        val q = normalizeAngle(280.459 + 0.98564736 * d)
        val l = normalizeAngle(q + 1.915 * sinDeg(g) + 0.020 * sinDeg(2 * g))
        val e = 23.439 - 0.00000036 * d
        var ra = Math.toDegrees(atan2(cosDeg(e) * sinDeg(l), cosDeg(l))) / 15.0
        ra = fixHour(ra)
        return (q / 15.0 - ra) * 60.0
    }

    private fun sunDeclination(jd: Double): Double {
        val d = jd - 2451545.0
        val g = normalizeAngle(357.529 + 0.98560028 * d)
        val q = normalizeAngle(280.459 + 0.98564736 * d)
        val l = normalizeAngle(q + 1.915 * sinDeg(g) + 0.020 * sinDeg(2 * g))
        val e = 23.439 - 0.00000036 * d
        return Math.toDegrees(asin(sinDeg(e) * sinDeg(l)))
    }

    private fun hourAngle(angle: Double, latitude: Double, declination: Double): Double {
        val term = (-sinDeg(angle) - sinDeg(latitude) * sinDeg(declination)) /
            (cosDeg(latitude) * cosDeg(declination))
        return Math.toDegrees(acos(term.coerceIn(-1.0, 1.0)))
    }

    private fun asrTime(shadowFactor: Double, latitude: Double, declination: Double, dhuhrTime: Double): Double {
        val angle = -Math.toDegrees(atan(1.0 / (shadowFactor + tan(Math.toRadians(abs(latitude - declination))))))
        return dhuhrTime + hourAngle(angle, latitude, declination) / 15.0
    }

    private fun sinDeg(deg: Double) = sin(Math.toRadians(deg))
    private fun cosDeg(deg: Double) = cos(Math.toRadians(deg))

    private fun normalizeAngle(deg: Double): Double {
        var d = deg % 360.0
        if (d < 0) d += 360.0
        return d
    }

    private fun fixHour(hour: Double): Double {
        var h = hour % 24.0
        if (h < 0) h += 24.0
        return h
    }

    fun formatHour(decimalHour: Double): String {
        val totalMinutes = Math.round(decimalHour * 60.0)
        val h = (totalMinutes / 60) % 24
        val m = totalMinutes % 60
        return String.format("%02d:%02d", h, m)
    }
}

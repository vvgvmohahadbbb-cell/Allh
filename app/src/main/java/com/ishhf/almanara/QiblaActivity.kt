package com.ishhf.almanara

import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** بوصلة اتجاه القبلة - تحسب الاتجاه فلكياً من موقعك، وتدور مع دوران الهاتف فعلياً */
class QiblaActivity : AppCompatActivity(), SensorEventListener {

    companion object {
        private const val KAABA_LAT = 21.4225
        private const val KAABA_LNG = 39.8262
    }

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private var haveGravity = false
    private var haveGeomagnetic = false

    private lateinit var compassView: QiblaCompassView
    private var qiblaBearing = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qibla)
        compassView = findViewById(R.id.compassView)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val prefs = getSharedPreferences("almanara_prefs", MODE_PRIVATE)
        val lat = prefs.getFloat("lat", Float.NaN)
        val lng = prefs.getFloat("lng", Float.NaN)
        if (lat.isNaN() || lng.isNaN()) {
            findViewById<TextView>(R.id.qiblaInfo).text = "فعّل مواقيت الصلاة أول مرة من الشاشة الرئيسية عشان نحدد موقعك"
            return
        }
        qiblaBearing = calculateBearing(lat.toDouble(), lng.toDouble(), KAABA_LAT, KAABA_LNG)
        findViewById<TextView>(R.id.qiblaInfo).text = "اتجاه القبلة: ${qiblaBearing.toInt()}° من الشمال"
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        magnetometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    private fun calculateBearing(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLng = Math.toRadians(lng2 - lng1)
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val y = sin(dLng) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(dLng)
        var bearing = Math.toDegrees(atan2(y, x))
        if (bearing < 0) bearing += 360.0
        return bearing
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, gravity, 0, 3)
            haveGravity = true
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, geomagnetic, 0, 3)
            haveGeomagnetic = true
        }
        if (haveGravity && haveGeomagnetic) {
            val r = FloatArray(9)
            val i = FloatArray(9)
            if (SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(r, orientation)
                val azimuth = Math.toDegrees(orientation[0].toDouble())
                val normalizedAzimuth = (azimuth + 360) % 360
                compassView.rotationToQibla = (qiblaBearing - normalizedAzimuth).toFloat()
                compassView.invalidate()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

class QiblaCompassView(context: android.content.Context, attrs: AttributeSet?) : View(context, attrs) {
    var rotationToQibla = 0f

    private val circlePaint = Paint().apply { color = Color.parseColor("#1E1E1E"); style = Paint.Style.FILL }
    private val arrowPaint = Paint().apply { color = Color.parseColor("#4CAF50"); style = Paint.Style.FILL }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(width, height) / 2.5f
        canvas.drawCircle(cx, cy, radius, circlePaint)

        canvas.save()
        canvas.rotate(rotationToQibla, cx, cy)
        val path = android.graphics.Path()
        path.moveTo(cx, cy - radius + 20)
        path.lineTo(cx - 25f, cy + 20f)
        path.lineTo(cx + 25f, cy + 20f)
        path.close()
        canvas.drawPath(path, arrowPaint)
        canvas.drawCircle(cx, cy, 12f, arrowPaint)
        canvas.restore()
    }
}

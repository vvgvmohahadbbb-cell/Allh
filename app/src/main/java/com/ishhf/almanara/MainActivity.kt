package com.ishhf.almanara

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var timesContainer: LinearLayout
    private lateinit var tasksContainer: LinearLayout
    private lateinit var updateBanner: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences("almanara_prefs", MODE_PRIVATE)

        timesContainer = findViewById(R.id.timesContainer)
        tasksContainer = findViewById(R.id.tasksContainer)
        updateBanner = findViewById(R.id.updateBanner)

        findViewById<Button>(R.id.btnEnableLocation).setOnClickListener {
            requestLocationAndSchedule()
        }
        findViewById<Button>(R.id.btnAddTask).setOnClickListener {
            val input = findViewById<EditText>(R.id.newTaskInput)
            val title = input.text.toString().trim()
            if (title.isNotEmpty()) {
                DailyTasksManager.addCustomTask(this, title)
                input.setText("")
                renderTasks()
            }
        }

        findViewById<Button>(R.id.btnShareApp).setOnClickListener { shareApp() }
        findViewById<Button>(R.id.btnFeedback).setOnClickListener { openWhatsAppFeedback() }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 50)
        }

        ensureSignedInAndCheckUpdate()
        renderPrayerTimes()
        renderTasks()

        if (prefs.contains("lat")) {
            AlarmScheduler.scheduleAll(this)
        }
    }

    override fun onResume() {
        super.onResume()
        renderTasks()
    }

    private fun ensureSignedInAndCheckUpdate() {
        FirebaseAuth.getInstance().signInAnonymously()
            .addOnCompleteListener {
                val currentVersionCode = try {
                    packageManager.getPackageInfo(packageName, 0).let {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.longVersionCode.toInt() else @Suppress("DEPRECATION") it.versionCode
                    }
                } catch (e: Exception) {
                    1
                }
                UpdateChecker.check(
                    currentVersionCode,
                    onUpdateAvailable = { versionName, apkUrl -> showUpdateBanner(versionName, apkUrl) },
                    onUpToDate = { updateBanner.visibility = View.GONE }
                )
            }
    }

    private fun showUpdateBanner(versionName: String, apkUrl: String) {
        updateBanner.visibility = View.VISIBLE
        findViewById<TextView>(R.id.updateText).text = "في نسخة جديدة متوفرة ($versionName)"
        findViewById<Button>(R.id.btnUpdateNow).setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl)))
            } catch (e: Exception) {
                Toast.makeText(this, "تعذر فتح رابط التحديث", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestLocationAndSchedule() {
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 51)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
        }
        val fused = LocationServices.getFusedLocationProviderClient(this)
        fused.lastLocation.addOnSuccessListener { location: Location? ->
            if (location == null) {
                Toast.makeText(this, "ما قدرنا نحدد موقعك، جرب برا أو فعّل GPS", Toast.LENGTH_LONG).show()
                return@addOnSuccessListener
            }
            prefs.edit()
                .putFloat("lat", location.latitude.toFloat())
                .putFloat("lng", location.longitude.toFloat())
                .apply()
            AlarmScheduler.scheduleAll(this)
            renderPrayerTimes()
            Toast.makeText(this, "تم تفعيل مواقيت الصلاة والتذكيرات ✅", Toast.LENGTH_LONG).show()
        }
    }

    private fun renderPrayerTimes() {
        timesContainer.removeAllViews()
        val lat = prefs.getFloat("lat", Float.NaN)
        val lng = prefs.getFloat("lng", Float.NaN)
        if (lat.isNaN() || lng.isNaN()) {
            addTimeRow("اضغط \"تفعيل المواقيت\" أول مرة لتحديد موقعك", "")
            return
        }
        val cal = Calendar.getInstance()
        val timezone = cal.timeZone.rawOffset / (1000.0 * 60.0 * 60.0)
        val times = PrayerTimeCalculator.calculate(lat.toDouble(), lng.toDouble(), timezone, cal)

        addTimeRow("الفجر", PrayerTimeCalculator.formatHour(times.fajr))
        addTimeRow("الشروق", PrayerTimeCalculator.formatHour(times.sunrise))
        addTimeRow("الظهر", PrayerTimeCalculator.formatHour(times.dhuhr))
        addTimeRow("العصر", PrayerTimeCalculator.formatHour(times.asr))
        addTimeRow("المغرب", PrayerTimeCalculator.formatHour(times.maghrib))
        addTimeRow("العشاء", PrayerTimeCalculator.formatHour(times.isha))
    }

    private fun addTimeRow(name: String, time: String) {
        val row = LayoutInflater.from(this).inflate(R.layout.item_prayer_time, timesContainer, false)
        row.findViewById<TextView>(R.id.prayerName).text = name
        row.findViewById<TextView>(R.id.prayerTime).text = time
        timesContainer.addView(row)
    }

    private fun shareApp() {
        val shareText = "جرب تطبيق المنارة 🕌 (مواقيت صلاة تلقائية بدون نت + تذكيرات):\nhttps://github.com/vvgvmohahadbbb-cell/Allh"
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, shareText)
        startActivity(Intent.createChooser(intent, "شارك التطبيق عبر"))
    }

    private fun openWhatsAppFeedback() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/447578209156"))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر فتح واتساب", Toast.LENGTH_SHORT).show()
        }
    }

    private fun renderTasks() {
        tasksContainer.removeAllViews()
        val tasks = DailyTasksManager.getTodayTasks(this)
        for (task in tasks) {
            val checkBox = CheckBox(this)
            checkBox.text = task.title
            checkBox.isChecked = task.done
            checkBox.setTextColor(android.graphics.Color.WHITE)
            checkBox.setOnCheckedChangeListener { _, _ ->
                DailyTasksManager.toggleTask(this, task.id)
            }
            tasksContainer.addView(checkBox)
        }
    }
}

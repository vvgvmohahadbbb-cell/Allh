package com.ishhf.almanara

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

        findViewById<Button>(R.id.btnEnableLocation).setOnClickListener { requestLocationAndSchedule() }
        findViewById<Button>(R.id.btnBatteryOptimization).setOnClickListener { requestIgnoreBatteryOptimization() }

        findViewById<Button>(R.id.btnAddTask).setOnClickListener {
            val input = findViewById<EditText>(R.id.newTaskInput)
            val title = input.text.toString().trim()
            if (title.isNotEmpty()) {
                DailyTasksManager.addCustomTask(this, title)
                input.setText("")
                renderTasks()
            }
        }

        setupSalawatSection()

        findViewById<Button>(R.id.btnQibla).setOnClickListener { startActivity(Intent(this, QiblaActivity::class.java)) }
        findViewById<Button>(R.id.btnQuranQuiz).setOnClickListener { startActivity(Intent(this, QuranQuizActivity::class.java)) }
        findViewById<Button>(R.id.btnQAGuide).setOnClickListener { startActivity(Intent(this, QAGuideActivity::class.java)) }

        findViewById<Button>(R.id.btnShareApp).setOnClickListener { shareAppApk() }
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
        updateSalawatStatusText()
    }

    // ===== صلِّ على النبي: تفعيل/تعطيل + فترة مخصصة + عرض الوقت الجاي =====

    private fun setupSalawatSection() {
        val enabledCheckbox = findViewById<CheckBox>(R.id.salawatEnabledCheckbox)
        enabledCheckbox.isChecked = prefs.getBoolean("salawat_enabled", true)
        enabledCheckbox.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("salawat_enabled", checked).apply()
            if (checked) AlarmScheduler.rescheduleSalawatNext(this) else AlarmScheduler.cancelSalawat(this)
            updateSalawatStatusText()
        }

        val intervalInput = findViewById<EditText>(R.id.salawatIntervalInput)
        intervalInput.setText(prefs.getInt("salawat_interval_min", 10).toString())

        findViewById<Button>(R.id.btnSaveSalawatInterval).setOnClickListener {
            val minutes = intervalInput.text.toString().toIntOrNull()
            if (minutes == null || minutes < 1) {
                Toast.makeText(this, "اكتب رقم دقايق صحيح (١ أو أكتر)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            prefs.edit().putInt("salawat_interval_min", minutes).apply()
            if (prefs.getBoolean("salawat_enabled", true)) AlarmScheduler.rescheduleSalawatNext(this)
            Toast.makeText(this, "تم حفظ الفترة الجديدة ✅", Toast.LENGTH_SHORT).show()
            updateSalawatStatusText()
        }

        updateSalawatStatusText()
    }

    private fun updateSalawatStatusText() {
        val statusText = findViewById<TextView>(R.id.salawatStatusText)
        if (!prefs.getBoolean("salawat_enabled", true)) {
            statusText.text = "التذكير موقف حالياً"
            return
        }
        val nextTime = prefs.getLong("salawat_next_time", 0)
        if (nextTime == 0L) {
            statusText.text = "رح يبلش أول ما تفعّل مواقيت الصلاة"
        } else {
            val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
            statusText.text = "التذكير الجاي حوالي الساعة ${fmt.format(Date(nextTime))}"
        }
    }

    // ===== تحديث التطبيق =====

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

    // ===== البطارية =====

    private fun requestIgnoreBatteryOptimization() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "افتح إعدادات البطارية يدوياً واستثنِ هالتطبيق", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "التطبيق مستثنى أصلاً من توفير الطاقة ✅", Toast.LENGTH_SHORT).show()
        }
    }

    // ===== الموقع والمواقيت =====

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
            updateSalawatStatusText()
            Toast.makeText(this, "تم تفعيل مواقيت الصلاة والتذكيرات ✅", Toast.LENGTH_LONG).show()
        }
    }

    private fun renderPrayerTimes() {
        timesContainer.removeAllViews()
        val lat = prefs.getFloat("lat", Float.NaN)
        val lng = prefs.getFloat("lng", Float.NaN)
        if (lat.isNaN() || lng.isNaN()) {
            addTimeRow("اضغط \"تفعيل المواقيت\" أول مرة لتحديد موقعك", "", null)
            return
        }
        val times = AlarmScheduler.getAdjustedTimes(this, lat.toDouble(), lng.toDouble(), Calendar.getInstance())
        val offsetKeys = mapOf(
            "الفجر" to "offset_fajr", "الشروق" to "offset_sunrise", "الظهر" to "offset_dhuhr",
            "العصر" to "offset_asr", "المغرب" to "offset_maghrib", "العشاء" to "offset_isha"
        )
        val order = listOf("الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء")
        for (name in order) {
            val hour = times[name] ?: continue
            addTimeRow(name, PrayerTimeCalculator.formatHour(hour), offsetKeys[name])
        }
    }

    private fun addTimeRow(name: String, time: String, offsetKey: String?) {
        val row = LayoutInflater.from(this).inflate(R.layout.item_prayer_time, timesContainer, false)
        row.findViewById<TextView>(R.id.prayerName).text = name
        row.findViewById<TextView>(R.id.prayerTime).text = time
        val editButton = row.findViewById<View>(R.id.btnEditOffset)
        if (offsetKey != null) {
            editButton.visibility = View.VISIBLE
            editButton.setOnClickListener { showOffsetDialog(name, offsetKey) }
        } else {
            editButton.visibility = View.GONE
        }
        timesContainer.addView(row)
    }

    private fun showOffsetDialog(prayerName: String, offsetKey: String) {
        val input = EditText(this)
        input.hint = "بالدقايق، مثلاً 5 أو -5-"
        input.setText(prefs.getInt(offsetKey, 0).toString())
        AlertDialog.Builder(this)
            .setTitle("تعديل وقت $prayerName يدوياً (بالدقايق)")
            .setView(input)
            .setPositiveButton("حفظ") { _, _ ->
                val minutes = input.text.toString().toIntOrNull() ?: 0
                prefs.edit().putInt(offsetKey, minutes).apply()
                AlarmScheduler.scheduleAll(this)
                renderPrayerTimes()
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    // ===== المهمات =====

    private fun renderTasks() {
        tasksContainer.removeAllViews()
        val tasks = DailyTasksManager.getTodayTasks(this)
        for (task in tasks) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL

            val checkBox = CheckBox(this)
            checkBox.text = task.title
            checkBox.isChecked = task.done
            checkBox.setTextColor(android.graphics.Color.WHITE)
            checkBox.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            checkBox.setOnCheckedChangeListener { _, _ ->
                DailyTasksManager.toggleTask(this, task.id)
            }
            row.addView(checkBox)

            if (task.contentType != null) {
                val openButton = Button(this)
                openButton.text = "📖"
                openButton.setOnClickListener {
                    val intent = Intent(this, AzkarContentActivity::class.java)
                    intent.putExtra(AzkarContentActivity.EXTRA_TYPE, task.contentType)
                    startActivity(intent)
                }
                row.addView(openButton)
            }

            tasksContainer.addView(row)
        }
    }

    // ===== مشاركة التطبيق (ملف APK حقيقي، مش رابط) =====

    private fun shareAppApk() {
        try {
            val apkFile = File(applicationInfo.sourceDir)
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", apkFile)
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "application/vnd.android.package-archive"
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(intent, "شارك ملف التطبيق عبر"))
        } catch (e: Exception) {
            Toast.makeText(this, "تعذرت مشاركة الملف مباشرة، جرب مشاركة الرابط بدلها", Toast.LENGTH_LONG).show()
        }
    }

    private fun openWhatsAppFeedback() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/447578209156"))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر فتح واتساب", Toast.LENGTH_SHORT).show()
        }
    }
}

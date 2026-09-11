package com.ishhf.almanara

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DailyTask(val id: String, val title: String, var done: Boolean, val custom: Boolean)

/**
 * قائمة مهام يومية روحية (أذكار، تسبيح، شكر...) تتصفّر كل يوم جديد،
 * وتقدر تضيف مهامك الخاصة (تنضاف دايماً، مش بس اليوم).
 * كل شي مخزّن محلياً بالجهاز (SharedPreferences)، بدون نت.
 */
object DailyTasksManager {

    private val defaultTasks = listOf(
        "أذكار الصباح",
        "أذكار المساء",
        "١٠٠ تسبيحة (سبحان الله وبحمده)",
        "الاستغفار ١٠٠ مرة",
        "الحمد لله والشكر على نعمه",
        "قراءة ورد من القرآن",
        "الصلاة على النبي ﷺ"
    )

    fun getTodayTasks(context: Context): List<DailyTask> {
        val prefs = context.getSharedPreferences("almanara_prefs", Context.MODE_PRIVATE)
        val today = todayKey()
        val lastDate = prefs.getString("tasks_date", "")

        val customTitles = loadCustomTitles(prefs)
        val allTitles = defaultTasks + customTitles

        if (lastDate != today) {
            // يوم جديد: نصفّر حالة الإنجاز بس نحافظ على قائمة المهام المخصصة
            val fresh = allTitles.mapIndexed { i, title ->
                DailyTask(i.toString(), title, false, i >= defaultTasks.size)
            }
            saveTasks(prefs, fresh, today)
            return fresh
        }

        return loadTasks(prefs, allTitles)
    }

    fun toggleTask(context: Context, taskId: String) {
        val prefs = context.getSharedPreferences("almanara_prefs", Context.MODE_PRIVATE)
        val tasks = getTodayTasks(context).toMutableList()
        val index = tasks.indexOfFirst { it.id == taskId }
        if (index != -1) {
            tasks[index] = tasks[index].copy(done = !tasks[index].done)
            saveTasks(prefs, tasks, todayKey())
        }
    }

    fun addCustomTask(context: Context, title: String) {
        val prefs = context.getSharedPreferences("almanara_prefs", Context.MODE_PRIVATE)
        val customTitles = loadCustomTitles(prefs).toMutableList()
        customTitles.add(title)
        saveCustomTitles(prefs, customTitles)
        // نجبر إعادة بناء القائمة عشان المهمة الجديدة تنضاف فوراً
        prefs.edit().putString("tasks_date", "").apply()
    }

    private fun loadCustomTitles(prefs: android.content.SharedPreferences): List<String> {
        val raw = prefs.getString("custom_titles", "[]") ?: "[]"
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { arr.getString(it) }
    }

    private fun saveCustomTitles(prefs: android.content.SharedPreferences, titles: List<String>) {
        val arr = JSONArray()
        titles.forEach { arr.put(it) }
        prefs.edit().putString("custom_titles", arr.toString()).apply()
    }

    private fun saveTasks(prefs: android.content.SharedPreferences, tasks: List<DailyTask>, date: String) {
        val arr = JSONArray()
        tasks.forEach { t ->
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("title", t.title)
            obj.put("done", t.done)
            obj.put("custom", t.custom)
            arr.put(obj)
        }
        prefs.edit()
            .putString("tasks_today", arr.toString())
            .putString("tasks_date", date)
            .apply()
    }

    private fun loadTasks(prefs: android.content.SharedPreferences, fallbackTitles: List<String>): List<DailyTask> {
        val raw = prefs.getString("tasks_today", null) ?: return fallbackTitles.mapIndexed { i, t ->
            DailyTask(i.toString(), t, false, false)
        }
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            DailyTask(obj.getString("id"), obj.getString("title"), obj.getBoolean("done"), obj.getBoolean("custom"))
        }
    }

    private fun todayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}

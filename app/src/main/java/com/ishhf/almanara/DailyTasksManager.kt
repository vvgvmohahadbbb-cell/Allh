package com.ishhf.almanara

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DailyTask(val id: String, val title: String, var done: Boolean, val custom: Boolean, val contentType: String? = null)

/**
 * قائمة مهام يومية روحية (أذكار، تسبيح، شكر...) تتصفّر كل يوم جديد،
 * وتقدر تضيف مهامك الخاصة (تنضاف دايماً، مش بس اليوم).
 * كل شي مخزّن محلياً بالجهاز (SharedPreferences)، بدون نت.
 */
object DailyTasksManager {

    // (العنوان، نوع المحتوى القابل للفتح - null يعني مجرد صندوق اختيار عادي)
    private val defaultTasks = listOf(
        "أذكار الصباح" to "morning",
        "أذكار المساء" to "evening",
        "١٠٠ تسبيحة (سبحان الله وبحمده)" to null,
        "الاستغفار ١٠٠ مرة" to null,
        "الحمد لله والشكر على نعمه" to null,
        "قراءة ورد من القرآن" to null,
        "الصلاة على النبي صلى الله عليه وسلم" to null
    )

    fun getTodayTasks(context: Context): List<DailyTask> {
        val prefs = context.getSharedPreferences("almanara_prefs", Context.MODE_PRIVATE)
        val today = todayKey()
        val lastDate = prefs.getString("tasks_date", "")

        val customTitles = loadCustomTitles(prefs)

        if (lastDate != today) {
            val fresh = mutableListOf<DailyTask>()
            defaultTasks.forEachIndexed { i, (title, type) ->
                fresh.add(DailyTask(i.toString(), title, false, false, type))
            }
            customTitles.forEachIndexed { i, title ->
                fresh.add(DailyTask((defaultTasks.size + i).toString(), title, false, true, null))
            }
            saveTasks(prefs, fresh, today)
            return fresh
        }

        return loadTasks(prefs)
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
            obj.put("contentType", t.contentType ?: JSONObject.NULL)
            arr.put(obj)
        }
        prefs.edit()
            .putString("tasks_today", arr.toString())
            .putString("tasks_date", date)
            .apply()
    }

    private fun loadTasks(prefs: android.content.SharedPreferences): List<DailyTask> {
        val raw = prefs.getString("tasks_today", null) ?: return emptyList()
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            DailyTask(
                obj.getString("id"), obj.getString("title"), obj.getBoolean("done"), obj.getBoolean("custom"),
                if (obj.isNull("contentType")) null else obj.getString("contentType")
            )
        }
    }

    private fun todayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}

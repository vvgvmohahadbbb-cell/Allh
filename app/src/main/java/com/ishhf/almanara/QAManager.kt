package com.ishhf.almanara

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class QAEntry(val question: String, val answer: String, val custom: Boolean)

/** دليل أسئلة وأجوبة محلي بالكامل - بدون نت وبدون أي ذكاء اصطناعي حقيقي.
 *  المستخدم يقدر يضيف أسئلته وأجوبته الخاصة وتنحفظ بجهازه فقط. */
object QAManager {

    private val seeded = listOf(
        "كم عدد الصلوات المفروضة باليوم؟" to "خمس صلوات: الفجر، الظهر، العصر، المغرب، والعشاء.",
        "ما هي أركان الإسلام؟" to "الشهادتان، إقامة الصلاة، إيتاء الزكاة، صوم رمضان، وحج البيت لمن استطاع.",
        "متى يبدأ وقت صلاة الفجر؟" to "من طلوع الفجر الصادق إلى قبيل شروق الشمس.",
        "هل يجوز الجمع بين الصلوات؟" to "يجوز الجمع بين الظهر والعصر، والمغرب والعشاء، في حالات مثل السفر أو المرض حسب تفصيل الفقهاء."
    )

    fun getAll(context: Context): List<QAEntry> {
        val prefs = context.getSharedPreferences("almanara_prefs", Context.MODE_PRIVATE)
        val raw = prefs.getString("qa_custom", "[]") ?: "[]"
        val arr = JSONArray(raw)
        val custom = (0 until arr.length()).map {
            val obj = arr.getJSONObject(it)
            QAEntry(obj.getString("q"), obj.getString("a"), true)
        }
        val seededEntries = seeded.map { QAEntry(it.first, it.second, false) }
        return seededEntries + custom
    }

    fun addCustom(context: Context, question: String, answer: String) {
        val prefs = context.getSharedPreferences("almanara_prefs", Context.MODE_PRIVATE)
        val raw = prefs.getString("qa_custom", "[]") ?: "[]"
        val arr = JSONArray(raw)
        val obj = JSONObject()
        obj.put("q", question)
        obj.put("a", answer)
        arr.put(obj)
        prefs.edit().putString("qa_custom", arr.toString()).apply()
    }

    fun search(context: Context, query: String): List<QAEntry> {
        if (query.isBlank()) return getAll(context)
        return getAll(context).filter {
            it.question.contains(query, ignoreCase = true) || it.answer.contains(query, ignoreCase = true)
        }
    }
}

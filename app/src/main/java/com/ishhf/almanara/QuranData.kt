package com.ishhf.almanara

import android.content.Context
import org.json.JSONObject

data class Ayah(val surahNumber: Int, val surahName: String, val ayahNumber: Int, val text: String)

/**
 * يقرأ نص القرآن من ملف assets/quran_simple_clean.json يلي لازم تجيبه إنت
 * من tanzil.net (مصدر موثوق ومرخّص). ما بنكتب أي آية من الذاكرة هون.
 */
object QuranData {
    private var cache: List<Ayah>? = null

    fun isAvailable(context: Context): Boolean {
        return try {
            context.assets.open("quran_simple_clean.json").close()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun loadAll(context: Context): List<Ayah> {
        cache?.let { return it }
        return try {
            val json = context.assets.open("quran_simple_clean.json")
                .bufferedReader(Charsets.UTF_8).use { it.readText() }
            val root = JSONObject(json)
            val surahsArray = root.getJSONArray("surahs")
            val list = mutableListOf<Ayah>()
            for (i in 0 until surahsArray.length()) {
                val surah = surahsArray.getJSONObject(i)
                val surahNumber = surah.getInt("number")
                val surahName = surah.getString("name")
                val ayahsArray = surah.getJSONArray("ayahs")
                for (j in 0 until ayahsArray.length()) {
                    val ayah = ayahsArray.getJSONObject(j)
                    list.add(Ayah(surahNumber, surahName, ayah.getInt("number"), ayah.getString("text")))
                }
            }
            cache = list
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun surahNames(context: Context): List<Pair<Int, String>> {
        return loadAll(context).map { it.surahNumber to it.surahName }.distinct()
    }

    fun ayahsFor(context: Context, surahNumber: Int): List<Ayah> {
        return loadAll(context).filter { it.surahNumber == surahNumber }
    }
}

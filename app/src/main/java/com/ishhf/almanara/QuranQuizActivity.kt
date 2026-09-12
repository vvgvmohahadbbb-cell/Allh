package com.ishhf.almanara

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * اختبار تسميع بسيط: يعرض آية، تقرأها، والتطبيق يلوّن كل كلمة تعرّف عليها صح
 * أخضر وكل كلمة ما طابقت أحمر. دقة على مستوى الكلمة فقط (تقنية التعرف على
 * الكلام العادية بأندرويد، مش مصممة لدقة تجويد أو حركات).
 */
class QuranQuizActivity : AppCompatActivity() {

    private var recognizer: SpeechRecognizer? = null
    private var listening = false
    private var currentAyahs: List<Ayah> = emptyList()
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quran_quiz)

        if (!QuranData.isAvailable(this)) {
            findViewById<TextView>(R.id.quizAyahText).text =
                "ملف نص القرآن مش موجود بعد. راجع README لتحميله من tanzil.net وحطه بمجلد assets."
            return
        }

        val surahSpinner = findViewById<Spinner>(R.id.surahSpinner)
        val surahs = QuranData.surahNames(this)
        surahSpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            surahs.map { "${it.first}. ${it.second}" }
        )

        findViewById<Button>(R.id.btnStartSurah).setOnClickListener {
            val selected = surahs.getOrNull(surahSpinner.selectedItemPosition) ?: return@setOnClickListener
            currentAyahs = QuranData.ayahsFor(this, selected.first)
            currentIndex = 0
            showCurrentAyah()
        }

        findViewById<Button>(R.id.btnMicToggle).setOnClickListener {
            if (listening) stopListening() else startListening()
        }

        findViewById<Button>(R.id.btnNextAyah).setOnClickListener {
            if (currentIndex < currentAyahs.size - 1) {
                currentIndex++
                showCurrentAyah()
            }
        }
    }

    private fun showCurrentAyah() {
        val ayah = currentAyahs.getOrNull(currentIndex) ?: return
        findViewById<TextView>(R.id.quizAyahText).text = ayah.text
        findViewById<TextView>(R.id.quizAyahText).setTextColor(android.graphics.Color.WHITE)
    }

    private fun startListening() {
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 60)
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "التعرف على الكلام غير متوفر بهالجهاز", Toast.LENGTH_LONG).show()
            return
        }
        listening = true
        findViewById<Button>(R.id.btnMicToggle).text = "⏹ إيقاف الاستماع"
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spoken = matches?.firstOrNull() ?: ""
                highlightComparison(spoken)
                if (listening) startListeningInternal()
            }

            override fun onError(error: Int) {
                if (listening) startListeningInternal()
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        startListeningInternal()
    }

    private fun startListeningInternal() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar")
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        try {
            recognizer?.startListening(intent)
        } catch (e: Exception) {
        }
    }

    private fun stopListening() {
        listening = false
        findViewById<Button>(R.id.btnMicToggle).text = "🎤 ابدأ الاستماع"
        recognizer?.stopListening()
        recognizer?.destroy()
        recognizer = null
    }

    private fun normalize(word: String): String {
        return word.replace(Regex("[\\u064B-\\u065F\\u0670]"), "") // إزالة الحركات للمقارنة
            .replace("أ", "ا").replace("إ", "ا").replace("آ", "ا")
            .replace("ة", "ه")
            .trim()
    }

    private fun highlightComparison(spoken: String) {
        val ayah = currentAyahs.getOrNull(currentIndex) ?: return
        val expectedWords = ayah.text.split(" ").filter { it.isNotBlank() }
        val spokenWords = spoken.split(" ").filter { it.isNotBlank() }.map { normalize(it) }

        val builder = SpannableStringBuilder()
        for ((i, word) in expectedWords.withIndex()) {
            val matched = i < spokenWords.size && normalize(word) == spokenWords[i]
            val start = builder.length
            builder.append(word)
            val end = builder.length
            val color = if (matched) android.graphics.Color.parseColor("#4CAF50") else android.graphics.Color.parseColor("#F44336")
            builder.setSpan(ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.append(" ")
        }
        findViewById<TextView>(R.id.quizAyahText).text = builder
    }

    override fun onDestroy() {
        super.onDestroy()
        recognizer?.destroy()
    }
}

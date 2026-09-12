package com.ishhf.almanara

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/** يعرض نص الأذكار الفعلي (صباح/مساء) لما تضغط عالمهمة من الشاشة الرئيسية */
class AzkarContentActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TYPE = "type" // "morning" or "evening"
    }

    private val morningAzkar = """
        🌅 أذكار الصباح

        أصبحنا وأصبح الملك لله، والحمد لله، لا إله إلا الله وحده لا شريك له، له الملك وله الحمد وهو على كل شيء قدير.

        اللهم بك أصبحنا، وبك أمسينا، وبك نحيا، وبك نموت، وإليك النشور.

        آية الكرسي: اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَّهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ...

        سورة الإخلاص، الفلق، والناس (ثلاث مرات).

        اللهم إني أصبحت أشهدك، وأشهد حملة عرشك، وملائكتك، وجميع خلقك، أنك أنت الله لا إله إلا أنت وحدك لا شريك لك، وأن محمداً عبدك ورسولك.

        سبحان الله وبحمده (١٠٠ مرة).
    """.trimIndent()

    private val eveningAzkar = """
        🌙 أذكار المساء

        أمسينا وأمسى الملك لله، والحمد لله، لا إله إلا الله وحده لا شريك له، له الملك وله الحمد وهو على كل شيء قدير.

        اللهم بك أمسينا، وبك أصبحنا، وبك نحيا، وبك نموت، وإليك المصير.

        آية الكرسي: اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَّهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ...

        سورة الإخلاص، الفلق، والناس (ثلاث مرات).

        اللهم إني أمسيت أشهدك، وأشهد حملة عرشك، وملائكتك، وجميع خلقك، أنك أنت الله لا إله إلا أنت وحدك لا شريك لك، وأن محمداً عبدك ورسولك.

        أعوذ بكلمات الله التامات من شر ما خلق.
    """.trimIndent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_azkar_content)
        val type = intent.getStringExtra(EXTRA_TYPE) ?: "morning"
        val text = if (type == "morning") morningAzkar else eveningAzkar
        findViewById<TextView>(R.id.azkarContentText).text = text
    }
}

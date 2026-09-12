package com.ishhf.almanara

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class QAGuideActivity : AppCompatActivity() {

    private lateinit var adapter: QAAdapter
    private val items = mutableListOf<QAEntry>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qa_guide)

        val recyclerView = findViewById<RecyclerView>(R.id.qaRecyclerView)
        adapter = QAAdapter(items)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val searchInput = findViewById<EditText>(R.id.qaSearchInput)
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                refresh(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        findViewById<Button>(R.id.btnAddQA).setOnClickListener {
            val q = findViewById<EditText>(R.id.qaNewQuestion).text.toString().trim()
            val a = findViewById<EditText>(R.id.qaNewAnswer).text.toString().trim()
            if (q.isNotEmpty() && a.isNotEmpty()) {
                QAManager.addCustom(this, q, a)
                findViewById<EditText>(R.id.qaNewQuestion).setText("")
                findViewById<EditText>(R.id.qaNewAnswer).setText("")
                refresh(searchInput.text.toString())
            }
        }

        refresh("")
    }

    private fun refresh(query: String) {
        items.clear()
        items.addAll(QAManager.search(this, query))
        adapter.notifyDataSetChanged()
    }
}

class QAAdapter(private val items: List<QAEntry>) : RecyclerView.Adapter<QAAdapter.QAViewHolder>() {
    class QAViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val question: TextView = view.findViewById(R.id.qaQuestion)
        val answer: TextView = view.findViewById(R.id.qaAnswer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QAViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_qa, parent, false)
        return QAViewHolder(view)
    }

    override fun onBindViewHolder(holder: QAViewHolder, position: Int) {
        val entry = items[position]
        holder.question.text = (if (entry.custom) "📝 " else "❓ ") + entry.question
        holder.answer.text = entry.answer
    }

    override fun getItemCount(): Int = items.size
}

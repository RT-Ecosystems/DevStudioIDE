package com.devstudio.ide.editor

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.*
import com.devstudio.ide.R

class SearchReplaceDialog(
    context: Context,
    private val currentText: String,
    private val onReplace: (newText: String, count: Int) -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_search_replace)
        setTitle("Search & Replace")

        val etSearch      = findViewById<EditText>(R.id.etSearch)
        val etReplace     = findViewById<EditText>(R.id.etReplace)
        val swMatchCase   = findViewById<Switch>(R.id.swMatchCase)
        val tvResultCount = findViewById<TextView>(R.id.tvResultCount)
        val btnFind       = findViewById<Button>(R.id.btnFind)
        val btnReplace    = findViewById<Button>(R.id.btnReplace)
        val btnReplaceAll = findViewById<Button>(R.id.btnReplaceAll)
        val btnClose      = findViewById<Button>(R.id.btnClose)

        // Find - show count
        btnFind.setOnClickListener {
            val query = etSearch.text.toString()
            if (query.isEmpty()) {
                tvResultCount.text = "Enter search text"
                return@setOnClickListener
            }
            val count = if (swMatchCase.isChecked) {
                currentText.split(query).size - 1
            } else {
                currentText.lowercase().split(query.lowercase()).size - 1
            }
            tvResultCount.text = if (count > 0) "$count match(es) found" else "No matches found"
        }

        // Replace first occurrence
        btnReplace.setOnClickListener {
            val query   = etSearch.text.toString()
            val replace = etReplace.text.toString()
            if (query.isEmpty()) return@setOnClickListener

            val newText = if (swMatchCase.isChecked) {
                currentText.replaceFirst(query, replace)
            } else {
                val idx = currentText.lowercase().indexOf(query.lowercase())
                if (idx >= 0) {
                    currentText.substring(0, idx) + replace +
                    currentText.substring(idx + query.length)
                } else currentText
            }
            if (newText != currentText) {
                onReplace(newText, 1)
                tvResultCount.text = "Replaced 1 occurrence"
            } else {
                tvResultCount.text = "No match found"
            }
        }

        // Replace all
        btnReplaceAll.setOnClickListener {
            val query   = etSearch.text.toString()
            val replace = etReplace.text.toString()
            if (query.isEmpty()) return@setOnClickListener

            val count = if (swMatchCase.isChecked) {
                currentText.split(query).size - 1
            } else {
                currentText.lowercase().split(query.lowercase()).size - 1
            }

            val newText = if (swMatchCase.isChecked) {
                currentText.replace(query, replace)
            } else {
                currentText.replace(query, replace, ignoreCase = true)
            }

            if (count > 0) {
                onReplace(newText, count)
                tvResultCount.text = "Replaced $count occurrence(s)"
            } else {
                tvResultCount.text = "No matches found"
            }
        }

        btnClose.setOnClickListener { dismiss() }
    }
}

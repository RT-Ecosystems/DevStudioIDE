package com.devstudio.ide.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.devstudio.ide.R

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = getSharedPreferences("devstudio_prefs", Context.MODE_PRIVATE)

        val btnBack        = findViewById<ImageView>(R.id.btnSettingsBack)
        val sbFontSize     = findViewById<SeekBar>(R.id.sbFontSize)
        val tvFontPreview  = findViewById<TextView>(R.id.tvFontSizePreview)
        val swWordWrap     = findViewById<Switch>(R.id.swWordWrap)
        val swLineNumbers  = findViewById<Switch>(R.id.swLineNumbers)
        val swAutoSave     = findViewById<Switch>(R.id.swAutoSave)
        val spinnerTheme   = findViewById<Spinner>(R.id.spinnerTheme)

        // Font size: range 10–24sp, default 14
        val savedFont = prefs.getInt("font_size", 14)
        sbFontSize.max = 14   // 10 + max = 24
        sbFontSize.progress = savedFont - 10
        tvFontPreview.text = "${savedFont}sp"
        tvFontPreview.textSize = savedFont.toFloat()

        sbFontSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val size = progress + 10
                tvFontPreview.text = "${size}sp"
                tvFontPreview.textSize = size.toFloat()
                prefs.edit().putInt("font_size", size).apply()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // Word wrap
        swWordWrap.isChecked = prefs.getBoolean("word_wrap", true)
        swWordWrap.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("word_wrap", checked).apply()
        }

        // Line numbers
        swLineNumbers.isChecked = prefs.getBoolean("line_numbers", true)
        swLineNumbers.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("line_numbers", checked).apply()
        }

        // Auto save
        swAutoSave.isChecked = prefs.getBoolean("auto_save", false)
        swAutoSave.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("auto_save", checked).apply()
        }

        // Theme spinner
        val themes = arrayOf("Light (Default)", "Dark", "Monokai", "Solarized")
        spinnerTheme.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            themes
        )
        val savedTheme = prefs.getInt("editor_theme", 0)
        spinnerTheme.setSelection(savedTheme)
        spinnerTheme.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, pos: Int, id: Long) {
                prefs.edit().putInt("editor_theme", pos).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnBack.setOnClickListener { finish() }
    }
}

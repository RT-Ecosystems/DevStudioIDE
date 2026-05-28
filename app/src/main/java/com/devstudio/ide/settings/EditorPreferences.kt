package com.devstudio.ide.settings

import android.content.Context

object EditorPreferences {

    private const val PREFS_NAME = "devstudio_prefs"

    fun getFontSize(context: Context): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt("font_size", 14)

    fun getWordWrap(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean("word_wrap", true)

    fun getLineNumbers(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean("line_numbers", true)

    fun getAutoSave(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean("auto_save", false)

    fun getEditorTheme(context: Context): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt("editor_theme", 0)
}

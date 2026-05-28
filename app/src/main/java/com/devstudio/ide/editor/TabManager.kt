package com.devstudio.ide.editor

import java.io.File

data class EditorTab(
    val file: File,
    var content: String,
    var isModified: Boolean = false
)

class TabManager {

    private val tabs = mutableListOf<EditorTab>()
    private var activeIndex = -1

    fun openFile(file: File): Int {
        // Check if already open
        val existing = tabs.indexOfFirst { it.file.absolutePath == file.absolutePath }
        if (existing >= 0) {
            activeIndex = existing
            return activeIndex
        }
        val content = if (file.exists()) file.readText() else ""
        tabs.add(EditorTab(file, content))
        activeIndex = tabs.size - 1
        return activeIndex
    }

    fun getActiveTab(): EditorTab? =
        if (activeIndex >= 0 && activeIndex < tabs.size) tabs[activeIndex] else null

    fun setActiveIndex(index: Int) {
        if (index in tabs.indices) activeIndex = index
    }

    fun updateContent(content: String) {
        getActiveTab()?.let {
            it.content = content
            it.isModified = true
        }
    }

    fun saveActiveTab(): Boolean {
        val tab = getActiveTab() ?: return false
        return try {
            tab.file.writeText(tab.content)
            tab.isModified = false
            true
        } catch (e: Exception) {
            false
        }
    }

    fun closeTab(index: Int) {
        if (index in tabs.indices) {
            tabs.removeAt(index)
            activeIndex = when {
                tabs.isEmpty()       -> -1
                activeIndex >= tabs.size -> tabs.size - 1
                else                 -> activeIndex
            }
        }
    }

    fun getTabs(): List<EditorTab> = tabs.toList()
    fun getActiveIndex(): Int = activeIndex
    fun hasUnsavedChanges(): Boolean = tabs.any { it.isModified }
}

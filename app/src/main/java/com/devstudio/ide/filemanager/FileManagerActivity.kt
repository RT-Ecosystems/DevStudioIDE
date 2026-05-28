package com.devstudio.ide.filemanager

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.devstudio.ide.R
import com.devstudio.ide.editor.SearchReplaceDialog
import com.devstudio.ide.editor.TabAdapter
import com.devstudio.ide.editor.TabManager
import com.devstudio.ide.settings.EditorPreferences
import com.devstudio.ide.settings.SettingsActivity
import io.github.rosemoe.sora.langs.EmptyLanguage
import io.github.rosemoe.sora.widget.CodeEditor
import org.apache.commons.io.FileUtils
import java.io.File

class FileManagerActivity : AppCompatActivity() {

    private lateinit var rvFiles: RecyclerView
    private lateinit var rvTabs: RecyclerView
    private lateinit var editorContainer: LinearLayout
    private lateinit var tvPath: TextView
    private lateinit var editor: CodeEditor
    private lateinit var currentPath: String
    private lateinit var rootPath: String

    private val tabManager = TabManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_manager)

        rootPath = intent.getStringExtra("PATH") ?: run { finish(); return }
        currentPath = rootPath

        tvPath        = findViewById(R.id.tvCurrentPath)
        rvFiles       = findViewById(R.id.rvFiles)
        rvTabs        = findViewById(R.id.rvTabs)
        editorContainer = findViewById(R.id.editorContainer)
        editor        = findViewById(R.id.codeEditor)

        rvFiles.layoutManager = LinearLayoutManager(this)
        rvTabs.layoutManager  = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        applyEditorPreferences()

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { handleBackPress() }
        findViewById<ImageView>(R.id.btnNewFile).setOnClickListener { showCreateDialog(false) }
        findViewById<ImageView>(R.id.btnNewFolder).setOnClickListener { showCreateDialog(true) }
        findViewById<ImageView>(R.id.btnSave).setOnClickListener { saveCurrentTab() }
        findViewById<ImageView>(R.id.btnSearch).setOnClickListener { showSearchReplace() }
        findViewById<ImageView>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        loadFiles(currentPath)
    }

    override fun onResume() {
        super.onResume()
        // Re-apply preferences if changed in settings
        applyEditorPreferences()
    }

    private fun applyEditorPreferences() {
        val fontSize    = EditorPreferences.getFontSize(this)
        val wordWrap    = EditorPreferences.getWordWrap(this)
        val lineNumbers = EditorPreferences.getLineNumbers(this)

        editor.setTextSize(fontSize.toFloat())
        editor.isWordwrap = wordWrap
        editor.isLineNumberEnabled = lineNumbers
    }

    private fun loadFiles(path: String) {
        currentPath = path
        tvPath.text = path.removePrefix(rootPath).ifEmpty { "/" }

        val dir = File(path)
        val items = mutableListOf<FileItem>()

        if (path != rootPath && dir.parentFile != null) {
            items.add(FileItem("..", dir.parentFile!!.absolutePath, true))
        }

        dir.listFiles()
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?.forEach { items.add(FileItem(it.name, it.absolutePath, it.isDirectory)) }

        val adapter = FileAdapter(
            items,
            onClick = { item ->
                if (item.name == "..") loadFiles(item.path)
                else if (item.isDirectory) loadFiles(item.path)
                else openInEditor(File(item.path))
            },
            onLongClick = { item, view ->
                if (item.name != "..") showFileOptions(item, view)
            }
        )
        rvFiles.adapter = adapter
        rvFiles.visibility = View.VISIBLE
        editorContainer.visibility = View.GONE
        rvTabs.visibility = View.GONE
    }

    private fun openInEditor(file: File) {
        val index = tabManager.openFile(file)
        refreshTabs()

        val tab = tabManager.getActiveTab() ?: return
        editor.setEditorLanguage(EmptyLanguage())
        editor.setText(tab.content)
        applyEditorPreferences()

        rvFiles.visibility = View.GONE
        rvTabs.visibility = View.VISIBLE
        editorContainer.visibility = View.VISIBLE
        tvPath.text = file.name
    }

    private fun refreshTabs() {
        val adapter = TabAdapter(
            tabs        = tabManager.getTabs(),
            activeIndex = tabManager.getActiveIndex(),
            onTabClick  = { index ->
                // Save current before switching
                tabManager.updateContent(editor.text.toString())
                tabManager.setActiveIndex(index)
                val tab = tabManager.getActiveTab() ?: return@TabAdapter
                editor.setText(tab.content)
                tvPath.text = tab.file.name
                refreshTabs()
            },
            onTabClose  = { index ->
                val tab = tabManager.getTabs()[index]
                if (tab.isModified) {
                    AlertDialog.Builder(this)
                        .setTitle("Unsaved Changes")
                        .setMessage("Save '${tab.file.name}' before closing?")
                        .setPositiveButton("Save") { _, _ ->
                            tabManager.setActiveIndex(index)
                            tabManager.updateContent(editor.text.toString())
                            tabManager.saveActiveTab()
                            tabManager.closeTab(index)
                            afterTabClose()
                        }
                        .setNegativeButton("Discard") { _, _ ->
                            tabManager.closeTab(index)
                            afterTabClose()
                        }
                        .setNeutralButton("Cancel", null)
                        .show()
                } else {
                    tabManager.closeTab(index)
                    afterTabClose()
                }
            }
        )
        rvTabs.adapter = adapter
    }

    private fun afterTabClose() {
        val tab = tabManager.getActiveTab()
        if (tab == null) {
            // No tabs left - go back to file list
            rvFiles.visibility = View.VISIBLE
            rvTabs.visibility = View.GONE
            editorContainer.visibility = View.GONE
            loadFiles(currentPath)
        } else {
            editor.setText(tab.content)
            tvPath.text = tab.file.name
            refreshTabs()
        }
    }

    private fun saveCurrentTab() {
        tabManager.updateContent(editor.text.toString())
        if (tabManager.saveActiveTab()) {
            val name = tabManager.getActiveTab()?.file?.name ?: ""
            Toast.makeText(this, "Saved: $name", Toast.LENGTH_SHORT).show()
            refreshTabs()
        } else {
            Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSearchReplace() {
        val currentText = editor.text.toString()
        SearchReplaceDialog(this, currentText) { newText, count ->
            editor.setText(newText)
            tabManager.updateContent(newText)
            Toast.makeText(this, "Replaced $count occurrence(s)", Toast.LENGTH_SHORT).show()
        }.show()
    }

    private fun showFileOptions(item: FileItem, view: View) {
        val popup = PopupMenu(this, view)
        popup.menu.add(0, 0, 0, "Copy Path")
        popup.menu.add(0, 1, 1, "Rename")
        popup.menu.add(0, 2, 2, "Delete")
        popup.setOnMenuItemClickListener { menuItem ->
            val file = File(item.path)
            when (menuItem.itemId) {
                0 -> {
                    val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cb.setPrimaryClip(ClipData.newPlainText("Path", file.absolutePath))
                    Toast.makeText(this, "Path copied", Toast.LENGTH_SHORT).show()
                }
                1 -> {
                    val input = EditText(this).apply { setText(file.name) }
                    AlertDialog.Builder(this).setTitle("Rename").setView(input)
                        .setPositiveButton("Rename") { _, _ ->
                            val newName = input.text.toString().trim()
                            if (newName.isNotEmpty()) {
                                file.renameTo(File(file.parent, newName))
                                loadFiles(currentPath)
                            }
                        }.setNegativeButton("Cancel", null).show()
                }
                2 -> {
                    AlertDialog.Builder(this).setTitle("Delete")
                        .setMessage("Delete '${file.name}'?")
                        .setPositiveButton("Delete") { _, _ ->
                            try {
                                if (file.isDirectory) FileUtils.deleteDirectory(file)
                                else file.delete()
                                loadFiles(currentPath)
                            } catch (e: Exception) {
                                Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show()
                            }
                        }.setNegativeButton("Cancel", null).show()
                }
            }
            true
        }
        popup.show()
    }

    private fun showCreateDialog(isFolder: Boolean) {
        val input = EditText(this)
        input.hint = if (isFolder) "Folder name" else "File name (e.g. Main.kt)"
        AlertDialog.Builder(this)
            .setTitle(if (isFolder) "New Folder" else "New File")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) return@setPositiveButton
                val target = File(currentPath, name)
                if (target.exists()) {
                    Toast.makeText(this, "Already exists", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (isFolder) target.mkdirs() else target.createNewFile()
                loadFiles(currentPath)
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun handleBackPress() {
        if (editorContainer.visibility == View.VISIBLE) {
            if (tabManager.hasUnsavedChanges()) {
                AlertDialog.Builder(this)
                    .setTitle("Unsaved Changes")
                    .setMessage("You have unsaved changes. Save before going back?")
                    .setPositiveButton("Save All") { _, _ ->
                        tabManager.updateContent(editor.text.toString())
                        tabManager.saveActiveTab()
                        goBackToFiles()
                    }
                    .setNegativeButton("Discard") { _, _ -> goBackToFiles() }
                    .setNeutralButton("Cancel", null)
                    .show()
            } else {
                goBackToFiles()
            }
        } else {
            val dir = File(currentPath)
            if (currentPath != rootPath && dir.parentFile != null) {
                loadFiles(dir.parentFile!!.absolutePath)
            } else {
                finish()
            }
        }
    }

    private fun goBackToFiles() {
        rvFiles.visibility = View.VISIBLE
        rvTabs.visibility = View.GONE
        editorContainer.visibility = View.GONE
        loadFiles(currentPath)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        handleBackPress()
    }
}

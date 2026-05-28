package com.devstudio.ide.filemanager

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import io.github.rosemoe.sora.langs.EmptyLanguage
import io.github.rosemoe.sora.widget.CodeEditor
import org.apache.commons.io.FileUtils
import java.io.File

class FileManagerActivity : AppCompatActivity() {

    private lateinit var rvFiles: RecyclerView
    private lateinit var editorContainer: LinearLayout
    private lateinit var tvPath: TextView
    private lateinit var editor: CodeEditor
    private lateinit var currentPath: String
    private lateinit var rootPath: String
    private var currentFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_manager)

        rootPath = intent.getStringExtra("PATH") ?: run {
            finish()
            return
        }
        currentPath = rootPath

        tvPath = findViewById(R.id.tvCurrentPath)
        rvFiles = findViewById(R.id.rvFiles)
        editorContainer = findViewById(R.id.editorContainer)
        editor = findViewById(R.id.codeEditor)

        rvFiles.layoutManager = LinearLayoutManager(this)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { handleBackPress() }
        findViewById<ImageView>(R.id.btnNewFile).setOnClickListener { showCreateDialog(false) }
        findViewById<ImageView>(R.id.btnNewFolder).setOnClickListener { showCreateDialog(true) }
        findViewById<ImageView>(R.id.btnSave).setOnClickListener { saveCurrentFile() }

        loadFiles(currentPath)
    }

    private fun loadFiles(path: String) {
        currentPath = path
        tvPath.text = path.removePrefix(rootPath).ifEmpty { "/" }

        val dir = File(path)
        val items = mutableListOf<FileItem>()

        // Add parent dir entry if not at root
        if (path != rootPath && dir.parentFile != null) {
            items.add(FileItem("..", dir.parentFile!!.absolutePath, true))
        }

        dir.listFiles()
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?.forEach { items.add(FileItem(it.name, it.absolutePath, it.isDirectory)) }

        val adapter = FileAdapter(
            items,
            onClick = { item ->
                if (item.name == "..") {
                    loadFiles(item.path)
                } else if (item.isDirectory) {
                    loadFiles(item.path)
                } else {
                    openInEditor(File(item.path))
                }
            },
            onLongClick = { item, view ->
                if (item.name != "..") showFileOptions(item, view)
            }
        )
        rvFiles.adapter = adapter
        rvFiles.visibility = View.VISIBLE
        editorContainer.visibility = View.GONE
    }

    private fun openInEditor(file: File) {
        currentFile = file
        // FIX: EmptyLanguage set before setText - was missing before
        editor.setEditorLanguage(EmptyLanguage())
        editor.setText(file.readText())
        rvFiles.visibility = View.GONE
        editorContainer.visibility = View.VISIBLE
        tvPath.text = file.name
    }

    private fun saveCurrentFile() {
        currentFile?.let { file ->
            try {
                file.writeText(editor.text.toString())
                Toast.makeText(this, "Saved: ${file.name}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } ?: Toast.makeText(this, "No file open", Toast.LENGTH_SHORT).show()
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
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Path", file.absolutePath))
                    Toast.makeText(this, "Path copied", Toast.LENGTH_SHORT).show()
                }
                1 -> {
                    val input = EditText(this).apply { setText(file.name) }
                    AlertDialog.Builder(this)
                        .setTitle("Rename")
                        .setView(input)
                        .setPositiveButton("Rename") { _, _ ->
                            val newName = input.text.toString().trim()
                            if (newName.isNotEmpty()) {
                                file.renameTo(File(file.parent, newName))
                                loadFiles(currentPath)
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
                2 -> {
                    AlertDialog.Builder(this)
                        .setTitle("Delete")
                        .setMessage("Delete '${file.name}'?")
                        .setPositiveButton("Delete") { _, _ ->
                            try {
                                if (file.isDirectory) FileUtils.deleteDirectory(file)
                                else file.delete()
                                loadFiles(currentPath)
                            } catch (e: Exception) {
                                Toast.makeText(this, "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
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
                if (name.isEmpty()) {
                    Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val newTarget = File(currentPath, name)
                if (newTarget.exists()) {
                    Toast.makeText(this, "Already exists", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (isFolder) newTarget.mkdirs() else newTarget.createNewFile()
                loadFiles(currentPath)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleBackPress() {
        if (editorContainer.visibility == View.VISIBLE) {
            rvFiles.visibility = View.VISIBLE
            editorContainer.visibility = View.GONE
            loadFiles(currentPath)
        } else {
            val dir = File(currentPath)
            if (currentPath != rootPath && dir.parentFile != null) {
                loadFiles(dir.parentFile!!.absolutePath)
            } else {
                finish()
            }
        }
    }

    // FIX: Deprecated annotation added
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        handleBackPress()
    }
}

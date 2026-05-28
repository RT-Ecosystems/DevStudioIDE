package com.devstudio.ide

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.devstudio.ide.filemanager.FileAdapter
import com.devstudio.ide.filemanager.FileItem
import com.devstudio.ide.filemanager.FileManagerActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var rvProjects: RecyclerView
    private lateinit var tvNoProjects: TextView
    private lateinit var projectsDir: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvProjects = findViewById(R.id.rvRecentProjects)
        tvNoProjects = findViewById(R.id.tvNoProjects)
        rvProjects.layoutManager = LinearLayoutManager(this)

        // FIX: New Project button
        findViewById<ImageView>(R.id.btnNewProject).setOnClickListener {
            if (::projectsDir.isInitialized) showCreateProjectDialog()
            else Toast.makeText(this, "Please grant storage permission first", Toast.LENGTH_SHORT).show()
        }

        // FIX: Open button - opens existing folder as project
        findViewById<Button>(R.id.btnOpen).setOnClickListener {
            if (::projectsDir.isInitialized) loadProjects()
            else Toast.makeText(this, "Please grant storage permission first", Toast.LENGTH_SHORT).show()
        }

        // FIX: Build button placeholder
        findViewById<Button>(R.id.btnBuild).setOnClickListener {
            Toast.makeText(this, "Build feature coming in next update!", Toast.LENGTH_SHORT).show()
        }

        checkPermissionsAndLoad()
    }

    // FIX: onResume correctly handles permission return
    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                if (!::projectsDir.isInitialized) setupProjectsDir()
                else loadProjects()
            }
        } else {
            if (::projectsDir.isInitialized && projectsDir.exists()) {
                loadProjects()
            }
        }
    }

    // FIX: onRequestPermissionsResult added - was completely missing before
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupProjectsDir()
        } else if (requestCode == 100) {
            Toast.makeText(this, "Storage permission is required", Toast.LENGTH_LONG).show()
        }
    }

    // FIX: Deprecated annotation added
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
    }

    private fun checkPermissionsAndLoad() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } else {
                setupProjectsDir()
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ),
                    100
                )
            } else {
                setupProjectsDir()
            }
        }
    }

    private fun setupProjectsDir() {
        projectsDir = File(Environment.getExternalStorageDirectory(), "DevStudioProjects")
        if (!projectsDir.exists()) projectsDir.mkdirs()
        loadProjects()
    }

    private fun loadProjects() {
        if (!::projectsDir.isInitialized) return
        val items = projectsDir.listFiles()
            ?.filter { it.isDirectory }
            ?.sortedBy { it.name.lowercase() }
            ?.map { FileItem(it.name, it.absolutePath, true) }
            ?: emptyList()

        if (items.isEmpty()) {
            tvNoProjects.visibility = android.view.View.VISIBLE
            rvProjects.visibility = android.view.View.GONE
        } else {
            tvNoProjects.visibility = android.view.View.GONE
            rvProjects.visibility = android.view.View.VISIBLE
        }

        val adapter = FileAdapter(
            items,
            onClick = { item ->
                val intent = Intent(this, FileManagerActivity::class.java)
                intent.putExtra("PATH", item.path)
                startActivity(intent)
            },
            onLongClick = { item, _ ->
                showProjectOptions(item)
            }
        )
        rvProjects.adapter = adapter
    }

    private fun showCreateProjectDialog() {
        val input = android.widget.EditText(this)
        input.hint = "Project name"
        AlertDialog.Builder(this)
            .setTitle("New Project")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "Project name cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val newProject = File(projectsDir, name)
                if (newProject.exists()) {
                    Toast.makeText(this, "Project already exists", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (newProject.mkdirs()) {
                    File(newProject, "main.kt").writeText("fun main() {\n    println(\"Hello, Dev Studio!\")\n}")
                    loadProjects()
                    Toast.makeText(this, "Project '$name' created!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showProjectOptions(item: FileItem) {
        val options = arrayOf("Open", "Rename", "Delete")
        AlertDialog.Builder(this)
            .setTitle(item.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(this, FileManagerActivity::class.java)
                        intent.putExtra("PATH", item.path)
                        startActivity(intent)
                    }
                    1 -> showRenameDialog(item)
                    2 -> showDeleteDialog(item)
                }
            }.show()
    }

    private fun showRenameDialog(item: FileItem) {
        val input = android.widget.EditText(this)
        input.setText(item.name)
        AlertDialog.Builder(this)
            .setTitle("Rename Project")
            .setView(input)
            .setPositiveButton("Rename") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    File(item.path).renameTo(File(projectsDir, newName))
                    loadProjects()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteDialog(item: FileItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete Project")
            .setMessage("Delete '${item.name}'? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                File(item.path).deleteRecursively()
                loadProjects()
                Toast.makeText(this, "Project deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

package com.devstudio.ide.build

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.devstudio.ide.R
import com.devstudio.ide.compiler.BuildManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class BuildActivity : AppCompatActivity() {

    private lateinit var pbBuild: ProgressBar
    private lateinit var tvStep: TextView
    private lateinit var tvLog: TextView
    private lateinit var tvErrors: TextView
    private lateinit var btnInstall: Button
    private lateinit var btnShare: Button
    private lateinit var scrollLog: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_build)

        pbBuild   = findViewById(R.id.pbBuild)
        tvStep    = findViewById(R.id.tvStep)
        tvLog     = findViewById(R.id.tvLog)
        tvErrors  = findViewById(R.id.tvErrors)
        btnInstall = findViewById(R.id.btnInstall)
        btnShare  = findViewById(R.id.btnShare)
        scrollLog = findViewById(R.id.scrollLog)

        // Android native text selection
        tvLog.setTextIsSelectable(true)
        tvErrors.setTextIsSelectable(true)
        tvErrors.setTextColor(Color.RED)

        pbBuild.max = 4

        val sourceDir    = intent.getStringExtra("SOURCE_DIR") ?: run {
            tvErrors.text = "Error: SOURCE_DIR not provided"
            tvErrors.visibility = View.VISIBLE
            return
        }
        val resDir       = intent.getStringExtra("RES_DIR") ?: run {
            tvErrors.text = "Error: RES_DIR not provided"
            tvErrors.visibility = View.VISIBLE
            return
        }
        val manifestPath = intent.getStringExtra("MANIFEST_PATH") ?: run {
            tvErrors.text = "Error: MANIFEST_PATH not provided"
            tvErrors.visibility = View.VISIBLE
            return
        }

        val outputDir = File(cacheDir, "build_output").absolutePath

        startBuild(sourceDir, resDir, manifestPath, outputDir)
    }

    private fun startBuild(
        sourceDir: String,
        resDir: String,
        manifestPath: String,
        outputDir: String
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            BuildManager.build(
                context      = this@BuildActivity,
                sourceDir    = sourceDir,
                resDir       = resDir,
                manifestPath = manifestPath,
                outputDir    = outputDir,
                onProgress   = { step, message ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        pbBuild.progress = step
                        tvStep.text = message
                        tvLog.append("[$step/4] $message\n")
                        scrollLog.post { scrollLog.fullScroll(View.FOCUS_DOWN) }
                    }
                },
                onComplete   = { success, apkPath, errors ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        if (success && apkPath != null) {
                            tvStep.text = "✅ Build Successful!"
                            tvLog.append("\nAPK saved at:\n$apkPath\n")
                            setupButtons(apkPath)
                        } else {
                            tvStep.text = "❌ Build Failed"
                            tvErrors.visibility = View.VISIBLE
                            tvErrors.text = errors.joinToString("\n")
                        }
                    }
                }
            )
        }
    }

    private fun setupButtons(apkPath: String) {
        btnInstall.visibility = View.VISIBLE
        btnShare.visibility   = View.VISIBLE

        val apkFile = File(apkPath)
        val apkUri  = FileProvider.getUriForFile(
            this,
            "$packageName.provider",
            apkFile
        )

        btnInstall.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }

        btnShare.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, apkUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share APK via"))
        }
    }
}

package com.devstudio.ide.webtoapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.devstudio.ide.R
import com.devstudio.ide.build.BuildActivity
import java.io.File

class WebToApkActivity : AppCompatActivity() {

    private lateinit var etUrl: EditText
    private lateinit var etAppName: EditText
    private lateinit var etPackageName: EditText
    private lateinit var ivIconPreview: ImageView
    private lateinit var spinnerColor: Spinner
    private lateinit var btnBuild: Button

    // FIX: ActivityResultLauncher instead of deprecated onActivityResult
    private val iconPicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { ivIconPreview.setImageURI(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_to_apk)

        etUrl         = findViewById(R.id.etUrl)
        etAppName     = findViewById(R.id.etAppName)
        etPackageName = findViewById(R.id.etPackageName)
        ivIconPreview = findViewById(R.id.ivIconPreview)
        spinnerColor  = findViewById(R.id.spinnerColor)
        btnBuild      = findViewById(R.id.btnBuildWebApp)

        val colors = arrayOf("Blue", "Green", "Red", "Purple", "Orange")
        spinnerColor.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            colors
        )

        // Auto-fill package name from app name
        etAppName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val formatted = s.toString()
                    .lowercase()
                    .replace(Regex("[^a-z0-9]"), "")
                if (formatted.isNotEmpty()) {
                    etPackageName.setText("com.example.$formatted")
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        findViewById<Button>(R.id.btnChooseIcon).setOnClickListener {
            iconPicker.launch("image/*")
        }

        btnBuild.setOnClickListener {
            generateAndBuild()
        }
    }

    private fun generateAndBuild() {
        val url      = etUrl.text.toString().trim().ifEmpty { "https://example.com" }
        val appName  = etAppName.text.toString().trim().ifEmpty { "My Web App" }
        val pkgName  = etPackageName.text.toString().trim().ifEmpty { "com.example.mywebapp" }
        val color    = spinnerColor.selectedItem.toString()

        // Theme color hex map
        val colorHex = when (color) {
            "Green"  -> "#388E3C"
            "Red"    -> "#D32F2F"
            "Purple" -> "#7B1FA2"
            "Orange" -> "#F57C00"
            else     -> "#1565C0" // Blue default
        }

        // Generate project in cache directory
        val projectDir = File(cacheDir, "webapp_${System.currentTimeMillis()}")
        projectDir.deleteRecursively()

        val pkgPath = pkgName.replace('.', '/')
        val srcDir  = File(projectDir, "src/$pkgPath").also { it.mkdirs() }
        val resDir  = File(projectDir, "res").also { it.mkdirs() }
        File(resDir, "values").mkdirs()
        File(resDir, "layout").mkdirs()

        // Generate MainActivity.java
        val mainActivityCode = "package " + pkgName + ";\n\n" +
            "import android.app.Activity;\n" +
            "import android.os.Bundle;\n" +
            "import android.webkit.WebSettings;\n" +
            "import android.webkit.WebView;\n" +
            "import android.webkit.WebViewClient;\n\n" +
            "public class MainActivity extends Activity {\n\n" +
            "    private WebView webView;\n\n" +
            "    @Override\n" +
            "    protected void onCreate(Bundle savedInstanceState) {\n" +
            "        super.onCreate(savedInstanceState);\n" +
            "        webView = new WebView(this);\n" +
            "        setContentView(webView);\n" +
            "        WebSettings settings = webView.getSettings();\n" +
            "        settings.setJavaScriptEnabled(true);\n" +
            "        settings.setDomStorageEnabled(true);\n" +
            "        settings.setLoadWithOverviewMode(true);\n" +
            "        settings.setUseWideViewPort(true);\n" +
            "        webView.setWebViewClient(new WebViewClient());\n" +
            "        webView.loadUrl(\"" + url + "\");\n" +
            "    }\n\n" +
            "    @Override\n" +
            "    public void onBackPressed() {\n" +
            "        if (webView.canGoBack()) {\n" +
            "            webView.goBack();\n" +
            "        } else {\n" +
            "            super.onBackPressed();\n" +
            "        }\n" +
            "    }\n" +
            "}"
        File(srcDir, "MainActivity.java").writeText(mainActivityCode)

        // Generate AndroidManifest.xml
        val manifestFile = File(projectDir, "AndroidManifest.xml")
        val manifestCode = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
            "    package=\"" + pkgName + "\">\n\n" +
            "    <uses-permission android:name=\"android.permission.INTERNET\" />\n\n" +
            "    <application\n" +
            "        android:label=\"" + appName + "\"\n" +
            "        android:allowBackup=\"true\"\n" +
            "        android:theme=\"@android:style/Theme.NoTitleBar.Fullscreen\">\n\n" +
            "        <activity\n" +
            "            android:name=\".MainActivity\"\n" +
            "            android:exported=\"true\"\n" +
            "            android:configChanges=\"orientation|screenSize\">\n" +
            "            <intent-filter>\n" +
            "                <action android:name=\"android.intent.action.MAIN\" />\n" +
            "                <category android:name=\"android.intent.category.LAUNCHER\" />\n" +
            "            </intent-filter>\n" +
            "        </activity>\n\n" +
            "    </application>\n" +
            "</manifest>"
        manifestFile.writeText(manifestCode)

        // Generate colors.xml
        val colorsCode = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<resources>\n" +
            "    <color name=\"primary\">" + colorHex + "</color>\n" +
            "    <color name=\"white\">#FFFFFF</color>\n" +
            "</resources>"
        File(resDir, "values/colors.xml").writeText(colorsCode)

        // Launch BuildActivity with all required paths
        val intent = Intent(this, BuildActivity::class.java).apply {
            putExtra("SOURCE_DIR",    srcDir.absolutePath)
            putExtra("RES_DIR",       resDir.absolutePath)
            putExtra("MANIFEST_PATH", manifestFile.absolutePath)
        }
        startActivity(intent)
    }
}

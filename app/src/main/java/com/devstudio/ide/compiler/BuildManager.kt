package com.devstudio.ide.compiler

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object BuildManager {

    suspend fun build(
        context: Context,
        sourceDir: String,
        resDir: String,
        manifestPath: String,
        outputDir: String,
        onProgress: (Int, String) -> Unit,
        onComplete: (Boolean, String?, List<String>) -> Unit
    ) = withContext(Dispatchers.IO) {

        try {
            File(outputDir).mkdirs()

            // Step 1: Compile
            onProgress(1, "Compiling Java sources...")
            val classDir = "$outputDir/classes"
            File(classDir).mkdirs()

            val compileResult = JavaCompiler.compile(sourceDir, classDir)
            if (!compileResult.success) {
                onComplete(false, null, compileResult.errors)
                return@withContext
            }

            // Step 2: Convert to DEX
            onProgress(2, "Converting to DEX...")
            val dexPath = "$outputDir/classes.dex"

            val dexResult = DexConverter.convert(classDir, dexPath)
            if (!dexResult.success) {
                onComplete(false, null, listOf(dexResult.error ?: "DEX conversion failed"))
                return@withContext
            }

            // Step 3: Build APK
            onProgress(3, "Building APK...")
            val unsignedApk = "$outputDir/app-unsigned.apk"

            val buildResult = ApkBuilder.build(dexPath, resDir, manifestPath, unsignedApk)
            if (!buildResult.success) {
                onComplete(false, null, listOf(buildResult.error ?: "APK build failed"))
                return@withContext
            }

            // Step 4: Sign APK
            onProgress(4, "Signing APK...")
            val signedApk = "$outputDir/app-signed.apk"

            val signResult = ApkSigner.sign(context, unsignedApk, signedApk)
            if (!signResult.success) {
                onComplete(false, null, listOf(signResult.error ?: "APK signing failed"))
                return@withContext
            }

            onComplete(true, signResult.signedApkPath, emptyList())

        } catch (e: Exception) {
            onComplete(false, null, listOf("BuildManager error: ${e.message}"))
        }
    }
}

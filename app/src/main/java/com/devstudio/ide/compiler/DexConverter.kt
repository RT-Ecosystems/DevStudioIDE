package com.devstudio.ide.compiler

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class DexResult(
    val success: Boolean,
    val error: String?
)

object DexConverter {

    suspend fun convert(
        classDir: String,
        outputDexPath: String
    ): DexResult = withContext(Dispatchers.IO) {

        try {
            val outputFile = File(outputDexPath)
            outputFile.parentFile?.let { if (!it.exists()) it.mkdirs() }

            val classFiles = File(classDir)
                .walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .map { it.absolutePath }
                .toList()

            if (classFiles.isEmpty()) {
                return@withContext DexResult(
                    success = false,
                    error = "No .class files found in $classDir"
                )
            }

            // Use dx tool which is bundled with Android build tools
            val dxArgs = mutableListOf(
                "dx",
                "--dex",
                "--output=${outputFile.absolutePath}"
            )
            dxArgs.addAll(classFiles)

            val process = ProcessBuilder(dxArgs)
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode == 0 && outputFile.exists()) {
                DexResult(success = true, error = null)
            } else {
                DexResult(
                    success = false,
                    error = "dx conversion failed (exit $exitCode): $output"
                )
            }

        } catch (e: Exception) {
            DexResult(
                success = false,
                error = "DexConverter error: ${e.message}"
            )
        }
    }
}

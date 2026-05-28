package com.devstudio.ide.compiler

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jdt.internal.compiler.batch.Main
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

data class CompileResult(
    val success: Boolean,
    val errors: List<String>
)

object JavaCompiler {

    suspend fun compile(
        sourceDir: String,
        outputDir: String
    ): CompileResult = withContext(Dispatchers.IO) {

        try {
            val outDir = File(outputDir)
            if (!outDir.exists()) outDir.mkdirs()

            // Collect all .java files recursively
            val sourceFiles = File(sourceDir)
                .walkTopDown()
                .filter { it.isFile && it.extension == "java" }
                .map { it.absolutePath }
                .toList()

            if (sourceFiles.isEmpty()) {
                return@withContext CompileResult(
                    success = true,
                    errors = listOf("No .java files found in $sourceDir")
                )
            }

            val outWriter = StringWriter()
            val errWriter = StringWriter()

            val compiler = Main(
                PrintWriter(outWriter),
                PrintWriter(errWriter),
                false,
                null,
                null
            )

            val args = mutableListOf(
                "-source", "1.8",
                "-target", "1.8",
                "-encoding", "UTF-8",
                "-d", outputDir,
                "-classpath", android.os.Build.VERSION.SDK_INT.toString()
            )
            args.addAll(sourceFiles)

            val success = compiler.compile(args.toTypedArray())

            val errors = errWriter.toString()
                .split("\n")
                .filter { it.isNotBlank() }

            CompileResult(success, errors)

        } catch (e: Exception) {
            CompileResult(
                success = false,
                errors = listOf("Compiler exception: ${e.message}")
            )
        }
    }
}

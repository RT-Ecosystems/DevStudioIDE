package com.devstudio.ide.compiler

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class BuildResult(
    val success: Boolean,
    val apkPath: String?,
    val error: String?
)

object ApkBuilder {

    suspend fun build(
        dexPath: String,
        resPath: String,
        manifestPath: String,
        outputApkPath: String
    ): BuildResult = withContext(Dispatchers.IO) {

        try {
            val outputApk = File(outputApkPath)
            outputApk.parentFile?.let { if (!it.exists()) it.mkdirs() }

            ZipOutputStream(FileOutputStream(outputApk)).use { zos ->

                // Add AndroidManifest.xml
                val manifestFile = File(manifestPath)
                if (manifestFile.exists()) {
                    addToZip(zos, manifestFile, "AndroidManifest.xml")
                } else {
                    return@withContext BuildResult(
                        false, null, "AndroidManifest.xml not found at $manifestPath"
                    )
                }

                // Add classes.dex
                val dexFile = File(dexPath)
                if (dexFile.exists()) {
                    addToZip(zos, dexFile, "classes.dex")
                } else {
                    return@withContext BuildResult(
                        false, null, "classes.dex not found at $dexPath"
                    )
                }

                // Add resources
                val resDir = File(resPath)
                if (resDir.exists() && resDir.isDirectory) {
                    resDir.walkTopDown()
                        .filter { it.isFile }
                        .forEach { file ->
                            val relativePath = file.relativeTo(resDir).path
                                .replace(File.separatorChar, '/')
                            addToZip(zos, file, "res/$relativePath")
                        }
                }
            }

            BuildResult(
                success = true,
                apkPath = outputApk.absolutePath,
                error = null
            )

        } catch (e: Exception) {
            BuildResult(
                success = false,
                apkPath = null,
                error = "ApkBuilder error: ${e.message}"
            )
        }
    }

    private fun addToZip(zos: ZipOutputStream, file: File, entryName: String) {
        zos.putNextEntry(ZipEntry(entryName))
        FileInputStream(file).use { it.copyTo(zos) }
        zos.closeEntry()
    }
}

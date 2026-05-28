package com.devstudio.ide.filemanager

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean
) {
    val extension: String
        get() = if (name.contains(".")) name.substringAfterLast(".").lowercase() else ""
}

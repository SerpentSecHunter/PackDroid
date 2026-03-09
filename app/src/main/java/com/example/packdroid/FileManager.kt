package com.example.packdroid

import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class FileItem(
    val file      : File,
    val name      : String = file.name,
    val path      : String = file.absolutePath,
    val isDirectory: Boolean = file.isDirectory,
    val size      : Long = if (file.isDirectory) 0L else file.length(),
    val lastModified: Long = file.lastModified(),
    val extension : String = file.extension.lowercase()
)

object FileManager {

    val archiveExtensions = setOf(
        "zip", "rar", "7z", "tar", "gz", "tgz",
        "iso", "cab", "arj", "z01", "001"
    )

    fun listFiles(path: String): List<FileItem> {
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        return dir.listFiles()
            ?.map { FileItem(it) }
            ?.sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenBy { it.name.lowercase() })
            ?: emptyList()
    }

    fun isArchive(file: File): Boolean {
        return file.extension.lowercase() in archiveExtensions
    }

    fun isImage(file: File): Boolean {
        val ext = file.extension.lowercase()
        return ext in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "ico", "tiff", "tif")
    }

    fun isVideo(file: File): Boolean {
        val ext = file.extension.lowercase()
        return ext in listOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "3gp", "3g2")
    }

    fun isAudio(file: File): Boolean {
        val ext = file.extension.lowercase()
        return ext in listOf("mp3", "wav", "flac", "aac", "ogg", "m4a", "wma", "opus", "amr")
    }

    fun copyFile(source: File, destDir: File): Boolean {
        return try {
            val dest = File(destDir, source.name)
            if (source.isDirectory) source.copyRecursively(dest, overwrite = true)
            else source.copyTo(dest, overwrite = true)
            true
        } catch (_: Exception) { false }
    }

    fun moveFile(source: File, destDir: File): Boolean {
        return try {
            val dest = File(destDir, source.name)
            source.renameTo(dest)
        } catch (_: Exception) { false }
    }

    fun deleteFile(file: File): Boolean {
        return try {
            if (file.isDirectory) file.deleteRecursively() else file.delete()
        } catch (_: Exception) { false }
    }

    fun renameFile(file: File, newName: String): Boolean {
        return try {
            val dest = File(file.parent ?: return false, newName)
            file.renameTo(dest)
        } catch (_: Exception) { false }
    }

    fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id")).format(Date(timestamp))
    }

    fun getFileIcon(item: FileItem): String {
        return when {
            item.isDirectory                   -> "📁"
            item.extension in setOf("zip", "z01") -> "🗜️"
            item.extension in setOf("rar")     -> "📦"
            item.extension == "7z"             -> "🗃️"
            item.extension in setOf("tar","gz","tgz") -> "📋"
            item.extension == "iso"            -> "💿"
            item.extension in setOf("cab","arj") -> "🗂️"
            item.extension in setOf("jpg","jpeg","png","gif","webp") -> "🖼️"
            item.extension in setOf("mp4","mkv","avi","mov") -> "🎬"
            item.extension in setOf("mp3","m4a","flac","aac") -> "🎵"
            item.extension in setOf("pdf")     -> "📄"
            item.extension in setOf("apk")     -> "📱"
            item.extension in setOf("txt","log","md") -> "📝"
            else                               -> "📃"
        }
    }
}
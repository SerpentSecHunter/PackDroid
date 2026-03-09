package com.example.packdroid

import java.io.File

enum class ClipboardAction { COPY, CUT }

data class ClipboardEntry(
    val files     : List<File>,
    val action    : ClipboardAction,
    val timestamp : Long = System.currentTimeMillis()
)

object AppClipboard {
    private val history = mutableListOf<ClipboardEntry>()
    var current: ClipboardEntry? = null
        private set

    fun copy(files: List<File>) {
        val entry = ClipboardEntry(files, ClipboardAction.COPY)
        current   = entry
        history.add(0, entry)
        if (history.size > 20) history.removeLastOrNull()
    }

    fun cut(files: List<File>) {
        val entry = ClipboardEntry(files, ClipboardAction.CUT)
        current   = entry
        history.add(0, entry)
        if (history.size > 20) history.removeLastOrNull()
    }

    fun getHistory(): List<ClipboardEntry> = history.toList()

    fun paste(destDir: File): Boolean {
        val entry = current ?: return false
        return try {
            entry.files.forEach { file ->
                val dest = File(destDir, file.name)
                if (entry.action == ClipboardAction.CUT) {
                    file.renameTo(dest)
                } else {
                    if (file.isDirectory) file.copyRecursively(dest, true)
                    else file.copyTo(dest, true)
                }
            }
            if (entry.action == ClipboardAction.CUT) current = null
            true
        } catch (_: Exception) { false }
    }

    fun clear() {
        current = null
        history.clear()
    }
}
package com.example.packdroid

import android.content.Context
import com.google.gson.Gson
import java.io.File
import java.util.Date

data class RecycleItem(
    val originalPath : String,
    val name         : String,
    val deletedAt    : Long = System.currentTimeMillis(),
    val size         : Long = 0L,
    val isDirectory  : Boolean = false
)

object RecycleBin {
    private const val PREF_NAME  = "recycle_bin"
    private const val KEY_ITEMS  = "items"
    private const val MAX_DAYS   = 30L
    private val gson             = Gson()

    private fun getBinDir(context: Context): File {
        val dir = File(context.filesDir, ".recycle_bin")
        dir.mkdirs()
        return dir
    }

    private fun getItems(context: Context): MutableList<RecycleItem> {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = pref.getString(KEY_ITEMS, "[]") ?: "[]"
        return try {
            gson.fromJson(json, Array<RecycleItem>::class.java).toMutableList()
        } catch (_: Exception) { mutableListOf() }
    }

    private fun saveItems(context: Context, items: List<RecycleItem>) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_ITEMS, gson.toJson(items)).apply()
    }

    fun moveToRecycleBin(context: Context, file: File): Boolean {
        return try {
            val binDir  = getBinDir(context)
            val dest    = File(binDir, "${System.currentTimeMillis()}_${file.name}")
            val success = if (file.isDirectory) file.copyRecursively(dest, true) else file.copyTo(dest, true) != null
            if (success) {
                val item = RecycleItem(
                    originalPath = file.absolutePath,
                    name         = file.name,
                    size         = if (file.isDirectory) 0L else file.length(),
                    isDirectory  = file.isDirectory
                )
                val items = getItems(context)
                items.add(item)
                saveItems(context, items)
                if (file.isDirectory) file.deleteRecursively() else file.delete()
            }
            success
        } catch (_: Exception) { false }
    }

    fun getRecycleItems(context: Context): List<RecycleItem> {
        cleanExpired(context)
        return getItems(context)
    }

    fun restore(context: Context, item: RecycleItem): Boolean {
        return try {
            val binDir   = getBinDir(context)
            val binFiles = binDir.listFiles() ?: return false
            val binFile  = binFiles.firstOrNull { it.name.endsWith(item.name) } ?: return false
            val dest     = File(item.originalPath)
            dest.parentFile?.mkdirs()
            val success  = if (item.isDirectory) binFile.copyRecursively(dest, true)
                           else binFile.copyTo(dest, true) != null
            if (success) {
                if (item.isDirectory) binFile.deleteRecursively() else binFile.delete()
                val items = getItems(context)
                items.removeAll { it.originalPath == item.originalPath && it.deletedAt == item.deletedAt }
                saveItems(context, items)
            }
            success
        } catch (_: Exception) { false }
    }

    fun deletePermanent(context: Context, item: RecycleItem): Boolean {
        return try {
            val binDir   = getBinDir(context)
            val binFiles = binDir.listFiles() ?: return false
            val binFile  = binFiles.firstOrNull { it.name.endsWith(item.name) }
            binFile?.let { if (it.isDirectory) it.deleteRecursively() else it.delete() }
            val items    = getItems(context)
            items.removeAll { it.originalPath == item.originalPath && it.deletedAt == item.deletedAt }
            saveItems(context, items)
            true
        } catch (_: Exception) { false }
    }

    fun emptyRecycleBin(context: Context) {
        getBinDir(context).listFiles()?.forEach {
            if (it.isDirectory) it.deleteRecursively() else it.delete()
        }
        saveItems(context, emptyList())
    }

    private fun cleanExpired(context: Context) {
        val now      = System.currentTimeMillis()
        val maxMs    = MAX_DAYS * 24 * 60 * 60 * 1000L
        val items    = getItems(context)
        val expired  = items.filter { now - it.deletedAt > maxMs }
        expired.forEach { deletePermanent(context, it) }
    }

    fun formatDeletedDate(timestamp: Long): String {
        val diff  = System.currentTimeMillis() - timestamp
        val days  = diff / (24 * 60 * 60 * 1000L)
        return when {
            days == 0L -> "Hari ini"
            days == 1L -> "Kemarin"
            else       -> "$days hari lalu (${30 - days} hari tersisa)"
        }
    }
}
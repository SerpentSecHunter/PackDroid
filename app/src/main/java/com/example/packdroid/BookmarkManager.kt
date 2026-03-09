package com.example.packdroid

import android.content.Context
import com.google.gson.Gson

data class Bookmark(
    val name : String,
    val path : String,
    val emoji: String = "📁"
)

object BookmarkManager {
    private const val PREF_NAME = "bookmarks"
    private const val KEY       = "list"
    private val gson            = Gson()

    fun getAll(context: Context): List<Bookmark> {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = pref.getString(KEY, "[]") ?: "[]"
        return try {
            gson.fromJson(json, Array<Bookmark>::class.java).toList()
        } catch (_: Exception) { emptyList() }
    }

    fun add(context: Context, bookmark: Bookmark) {
        val list = getAll(context).toMutableList()
        if (list.none { it.path == bookmark.path }) {
            list.add(bookmark)
            save(context, list)
        }
    }

    fun remove(context: Context, path: String) {
        val list = getAll(context).filter { it.path != path }
        save(context, list)
    }

    fun isBookmarked(context: Context, path: String): Boolean {
        return getAll(context).any { it.path == path }
    }

    private fun save(context: Context, list: List<Bookmark>) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY, gson.toJson(list)).apply()
    }
}
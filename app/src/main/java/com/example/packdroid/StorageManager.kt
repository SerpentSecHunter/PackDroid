package com.example.packdroid

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.provider.Settings
import java.io.File

data class StorageVolume2(
    val name        : String,
    val path        : String,
    val totalBytes  : Long,
    val freeBytes   : Long,
    val isRemovable : Boolean,
    val isAvailable : Boolean
)

object StorageHelper {

    // Cek apakah punya akses penuh storage
    fun hasFullAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true // Android 10 ke bawah pakai legacy permission
        }
    }

    // Buka settings untuk minta MANAGE_EXTERNAL_STORAGE
    fun requestFullAccess(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                context.startActivity(intent)
            }
        }
    }

    // Ambil semua storage yang tersedia (internal + SD Card)
    fun getStorageVolumes(context: Context): List<StorageVolume2> {
        val volumes = mutableListOf<StorageVolume2>()

        // Internal Storage
        val internalPath = Environment.getExternalStorageDirectory()
        if (internalPath.exists()) {
            val stat = StatFs(internalPath.absolutePath)
            volumes.add(
                StorageVolume2(
                    name        = "Memori Internal",
                    path        = internalPath.absolutePath,
                    totalBytes  = stat.totalBytes,
                    freeBytes   = stat.freeBytes,
                    isRemovable = false,
                    isAvailable = true
                )
            )
        }

        // SD Card — cari semua external storage
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val sm = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
                val svList = sm.storageVolumes
                for (sv in svList) {
                    if (sv.isRemovable) {
                        val path = getVolumePath(sv) ?: continue
                        if (File(path).exists()) {
                            val stat = StatFs(path)
                            volumes.add(
                                StorageVolume2(
                                    name        = sv.getDescription(context),
                                    path        = path,
                                    totalBytes  = stat.totalBytes,
                                    freeBytes   = stat.freeBytes,
                                    isRemovable = true,
                                    isAvailable = sv.state == android.os.Environment.MEDIA_MOUNTED
                                )
                            )
                        }
                    }
                }
            } else {
                // Fallback untuk Android < 7
                getSdCardPathFallback()?.let { path ->
                    val file = File(path)
                    if (file.exists() && file.isDirectory) {
                        val stat = StatFs(path)
                        volumes.add(
                            StorageVolume2(
                                name        = "Kartu SD",
                                path        = path,
                                totalBytes  = stat.totalBytes,
                                freeBytes   = stat.freeBytes,
                                isRemovable = true,
                                isAvailable = true
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        return volumes
    }

    @Suppress("DiscouragedPrivateApi")
    private fun getVolumePath(sv: StorageVolume): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                sv.directory?.absolutePath
            } else {
                val method = sv.javaClass.getDeclaredMethod("getPath")
                method.isAccessible = true
                method.invoke(sv) as? String
            }
        } catch (_: Exception) { null }
    }

    private fun getSdCardPathFallback(): String? {
        val candidates = listOf(
            "/storage/sdcard1",
            "/storage/extsdcard",
            "/storage/sdcard0/external_sdcard",
            "/mnt/sdcard/external_sd",
            "/mnt/external_sd",
            "/mnt/sdcard1",
            "/sdcard1",
            "/sdcard2"
        )
        return candidates.firstOrNull { File(it).exists() && File(it).isDirectory }
    }

    fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024L                -> "$bytes B"
            bytes < 1024L * 1024         -> "%.1f KB".format(bytes / 1024.0)
            bytes < 1024L * 1024 * 1024  -> "%.1f MB".format(bytes / (1024.0 * 1024))
            else                         -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
        }
    }

    fun getUsedPercent(vol: StorageVolume2): Float {
        if (vol.totalBytes == 0L) return 0f
        return ((vol.totalBytes - vol.freeBytes).toFloat() / vol.totalBytes.toFloat())
    }
}
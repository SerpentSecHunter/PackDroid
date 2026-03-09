package com.example.packdroid

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile

object SplitMergeEngine {

    data class SplitResult(
        val success : Boolean,
        val parts   : List<File> = emptyList(),
        val message : String = ""
    )

    data class MergeResult(
        val success    : Boolean,
        val outputFile : File? = null,
        val message    : String = ""
    )

    // Split file menjadi beberapa bagian
    fun splitFile(
        sourceFile  : File,
        destDir     : File,
        partSizeMB  : Int,
        onProgress  : (Int) -> Unit = {}
    ): SplitResult {
        return try {
            destDir.mkdirs()
            val partSizeBytes = partSizeMB.toLong() * 1024 * 1024
            val totalParts    = Math.ceil(sourceFile.length().toDouble() / partSizeBytes).toInt()
            val parts         = mutableListOf<File>()
            val baseName      = sourceFile.nameWithoutExtension
            val ext           = if (sourceFile.extension.isNotEmpty()) ".${sourceFile.extension}" else ""

            FileInputStream(sourceFile).use { fis ->
                val buffer = ByteArray(8192)
                for (i in 0 until totalParts) {
                    val partFile = File(destDir, "$baseName.part${(i + 1).toString().padStart(3, '0')}$ext")
                    var written  = 0L
                    FileOutputStream(partFile).use { fos ->
                        while (written < partSizeBytes) {
                            val toRead = minOf(buffer.size.toLong(), partSizeBytes - written).toInt()
                            val read   = fis.read(buffer, 0, toRead)
                            if (read == -1) break
                            fos.write(buffer, 0, read)
                            written += read
                        }
                    }
                    parts.add(partFile)
                    onProgress(((i + 1) * 100) / totalParts)
                }
            }
            SplitResult(true, parts, "Berhasil dipecah menjadi $totalParts bagian")
        } catch (e: Exception) {
            SplitResult(false, message = e.message ?: "Gagal memecah file")
        }
    }

    // Merge file dari beberapa bagian
    fun mergeFiles(
        parts      : List<File>,
        outputFile : File,
        onProgress : (Int) -> Unit = {}
    ): MergeResult {
        return try {
            outputFile.parentFile?.mkdirs()
            val sortedParts = parts.sortedBy { it.name }
            val buffer      = ByteArray(8192)
            var totalWritten = 0L
            val totalSize   = sortedParts.sumOf { it.length() }

            FileOutputStream(outputFile).use { fos ->
                sortedParts.forEachIndexed { index, part ->
                    FileInputStream(part).use { fis ->
                        var read = fis.read(buffer)
                        while (read != -1) {
                            fos.write(buffer, 0, read)
                            totalWritten += read
                            if (totalSize > 0) onProgress((totalWritten * 100 / totalSize).toInt())
                            read = fis.read(buffer)
                        }
                    }
                }
            }
            MergeResult(true, outputFile, "Berhasil digabung: ${outputFile.name}")
        } catch (e: Exception) {
            MergeResult(false, message = e.message ?: "Gagal menggabungkan file")
        }
    }

    // Auto-detect part files dari satu file part
    fun detectParts(partFile: File): List<File> {
        val dir      = partFile.parentFile ?: return listOf(partFile)
        val baseName = partFile.name.replace(Regex("\\.part\\d+"), "")
        return dir.listFiles()
            ?.filter { it.name.matches(Regex("${Regex.escape(baseName.substringBeforeLast("."))}\\.part\\d+.*")) }
            ?.sortedBy { it.name }
            ?: listOf(partFile)
    }

    fun formatPartSize(bytes: Long): String = ArchiveEngine.formatSize(bytes)
}
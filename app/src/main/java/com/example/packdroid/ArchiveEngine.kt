package com.example.packdroid

import net.sf.sevenzipjbinding.*
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import com.github.junrar.Archive
import java.io.*
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry

// ── DATA CLASSES ────────────────────────────────────────────

data class ArchiveEntry(
    val name     : String,
    val size     : Long,
    val isFolder : Boolean,
    val path     : String
)

data class ExtractResult(
    val success       : Boolean,
    val message       : String,
    val filesExtracted: Int = 0
)

data class CompressResult(
    val success   : Boolean,
    val message   : String,
    val outputPath: String = ""
)

enum class ArchiveFormat { ZIP, RAR, SEVEN_Z, TAR, TAR_GZ, ISO, CAB, ARJ, UNKNOWN }

// ── ARCHIVE ENGINE ───────────────────────────────────────────

object ArchiveEngine {

    fun detectFormat(filePath: String): ArchiveFormat {
        val lower = filePath.lowercase()
        return when {
            lower.endsWith(".zip") || lower.endsWith(".z01")                       -> ArchiveFormat.ZIP
            lower.endsWith(".rar") || lower.matches(Regex(".*\\.part\\d+\\.rar$"))-> ArchiveFormat.RAR
            lower.endsWith(".7z")  || lower.matches(Regex(".*\\.7z\\.\\d+$"))     -> ArchiveFormat.SEVEN_Z
            lower.endsWith(".tar")                                                 -> ArchiveFormat.TAR
            lower.endsWith(".tar.gz") || lower.endsWith(".tgz")                   -> ArchiveFormat.TAR_GZ
            lower.endsWith(".iso")                                                 -> ArchiveFormat.ISO
            lower.endsWith(".cab")                                                 -> ArchiveFormat.CAB
            lower.endsWith(".arj")                                                 -> ArchiveFormat.ARJ
            else                                                                   -> ArchiveFormat.UNKNOWN
        }
    }

    fun listEntries(filePath: String, password: String? = null): List<ArchiveEntry> {
        return try {
            when (detectFormat(filePath)) {
                ArchiveFormat.ZIP    -> listZip(filePath)
                ArchiveFormat.RAR    -> listRar(filePath, password)
                ArchiveFormat.TAR    -> listTar(filePath)
                ArchiveFormat.TAR_GZ -> listTarGz(filePath)
                ArchiveFormat.SEVEN_Z,
                ArchiveFormat.ISO,
                ArchiveFormat.CAB,
                ArchiveFormat.ARJ    -> listSevenZip(filePath)
                else                 -> emptyList()
            }
        } catch (e: Exception) { emptyList() }
    }

    fun extract(
        filePath   : String,
        destFolder : String,
        password   : String? = null,
        onProgress : (Int) -> Unit = {}
    ): ExtractResult {
        return try {
            File(destFolder).mkdirs()
            val entries = listEntries(filePath, password)
            val target  = determineExtractTarget(File(filePath), File(destFolder), entries.map { it.name })
            when (detectFormat(filePath)) {
                ArchiveFormat.ZIP    -> extractZip(filePath, target.absolutePath, onProgress)
                ArchiveFormat.RAR    -> extractRar(filePath, target.absolutePath, password, onProgress)
                ArchiveFormat.TAR    -> extractTar(filePath, target.absolutePath, onProgress)
                ArchiveFormat.TAR_GZ -> extractTarGz(filePath, target.absolutePath, onProgress)
                ArchiveFormat.SEVEN_Z,
                ArchiveFormat.ISO,
                ArchiveFormat.CAB,
                ArchiveFormat.ARJ    -> extractSevenZip(filePath, target.absolutePath, onProgress)
                else -> ExtractResult(false, "Format tidak didukung")
            }
        } catch (e: Exception) {
            ExtractResult(false, e.message ?: "Error tidak diketahui")
        }
    }

    fun compressToZip(
        sources    : List<File>,
        outputPath : String,
        password   : String? = null,
        onProgress : (Int) -> Unit = {}
    ): CompressResult {
        return try {
            val outFile = File(outputPath)
            outFile.parentFile?.mkdirs()
            ZipOutputStream(BufferedOutputStream(FileOutputStream(outFile))).use { zos ->
                sources.forEachIndexed { index, file ->
                    addToZip(zos, file, file.parent ?: "")
                    onProgress(((index + 1) * 100) / sources.size.coerceAtLeast(1))
                }
            }
            CompressResult(true, "Kompresi berhasil", outputPath)
        } catch (e: Exception) {
            CompressResult(false, e.message ?: "Gagal mengompres")
        }
    }

    // Kompres ke 7z — gunakan fallback ZIP jika SevenZipJBinding gagal init
    fun compressTo7z(
        sources    : List<File>,
        outputPath : String,
        password   : String? = null,
        onProgress : (Int) -> Unit = {}
    ): CompressResult {
        // SevenZipJBinding compress API berbeda per versi, gunakan ZIP sebagai fallback
        val zipOutput = outputPath.removeSuffix(".7z") + ".zip"
        return compressToZip(sources, zipOutput, password, onProgress).copy(
            message = "Disimpan sebagai ZIP (7z compress tidak tersedia di versi ini)"
        )
    }

    // ── LIST HELPERS ──────────────────────────────────────────

    private fun listZip(path: String): List<ArchiveEntry> {
        val entries = mutableListOf<ArchiveEntry>()
        ZipArchiveInputStream(BufferedInputStream(FileInputStream(path))).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entries.add(ArchiveEntry(entry.name, entry.size, entry.isDirectory, entry.name))
                entry = zis.nextEntry
            }
        }
        return entries
    }

    private fun listRar(path: String, password: String?): List<ArchiveEntry> {
        val entries = mutableListOf<ArchiveEntry>()
        try {
            Archive(File(path), password).use { arc ->
                arc.fileHeaders.forEach { h ->
                    entries.add(ArchiveEntry(h.fileName, h.dataSize, h.isDirectory, h.fileName))
                }
            }
        } catch (_: Exception) {}
        return entries
    }

    private fun listTar(path: String): List<ArchiveEntry> {
        val entries = mutableListOf<ArchiveEntry>()
        TarArchiveInputStream(BufferedInputStream(FileInputStream(path))).use { tis ->
            var entry = tis.nextEntry
            while (entry != null) {
                entries.add(ArchiveEntry(entry.name, entry.size, entry.isDirectory, entry.name))
                entry = tis.nextEntry
            }
        }
        return entries
    }

    private fun listTarGz(path: String): List<ArchiveEntry> {
        val entries = mutableListOf<ArchiveEntry>()
        TarArchiveInputStream(
            GzipCompressorInputStream(BufferedInputStream(FileInputStream(path)))
        ).use { tis ->
            var entry = tis.nextEntry
            while (entry != null) {
                entries.add(ArchiveEntry(entry.name, entry.size, entry.isDirectory, entry.name))
                entry = tis.nextEntry
            }
        }
        return entries
    }

    private fun listSevenZip(path: String): List<ArchiveEntry> {
        val entries = mutableListOf<ArchiveEntry>()
        try {
            RandomAccessFile(path, "r").use { raf ->
                SevenZip.openInArchive(null, RandomAccessFileInStream(raf)).use { arc ->
                    val count = arc.numberOfItems
                    for (i in 0 until count) {
                        val name  = arc.getStringProperty(i, PropID.PATH) ?: ""
                        val size  = (arc.getProperty(i, PropID.SIZE) as? Long) ?: 0L
                        val isDir = (arc.getProperty(i, PropID.IS_FOLDER) as? Boolean) ?: false
                        entries.add(ArchiveEntry(name, size, isDir, name))
                    }
                }
            }
        } catch (_: Exception) {}
        return entries
    }

    // ── EXTRACT HELPERS ───────────────────────────────────────

    private fun extractZip(
        path: String, dest: String, onProgress: (Int) -> Unit
    ): ExtractResult {
        var count = 0
        ZipArchiveInputStream(BufferedInputStream(FileInputStream(path))).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(dest, entry.name)
                if (entry.isDirectory) outFile.mkdirs()
                else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos -> zis.copyTo(fos) }
                    count++
                    onProgress(count)
                }
                entry = zis.nextEntry
            }
        }
        return ExtractResult(true, "Berhasil mengekstrak $count file", count)
    }

    private fun extractRar(
        path: String, dest: String, password: String?, onProgress: (Int) -> Unit
    ): ExtractResult {
        var count = 0
        try {
            Archive(File(path), password).use { arc ->
                arc.fileHeaders.forEach { header ->
                    val outFile = File(dest, header.fileName)
                    if (header.isDirectory) outFile.mkdirs()
                    else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos -> arc.extractFile(header, fos) }
                        count++
                        onProgress(count)
                    }
                }
            }
        } catch (e: Exception) {
            return ExtractResult(false, e.message ?: "Gagal ekstrak RAR")
        }
        return ExtractResult(true, "Berhasil mengekstrak $count file", count)
    }

    private fun extractTar(
        path: String, dest: String, onProgress: (Int) -> Unit
    ): ExtractResult {
        var count = 0
        TarArchiveInputStream(BufferedInputStream(FileInputStream(path))).use { tis ->
            var entry = tis.nextEntry
            while (entry != null) {
                val outFile = File(dest, entry.name)
                if (entry.isDirectory) outFile.mkdirs()
                else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos -> tis.copyTo(fos) }
                    count++
                    onProgress(count)
                }
                entry = tis.nextEntry
            }
        }
        return ExtractResult(true, "Berhasil mengekstrak $count file", count)
    }

    private fun extractTarGz(
        path: String, dest: String, onProgress: (Int) -> Unit
    ): ExtractResult {
        var count = 0
        TarArchiveInputStream(
            GzipCompressorInputStream(BufferedInputStream(FileInputStream(path)))
        ).use { tis ->
            var entry = tis.nextEntry
            while (entry != null) {
                val outFile = File(dest, entry.name)
                if (entry.isDirectory) outFile.mkdirs()
                else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos -> tis.copyTo(fos) }
                    count++
                    onProgress(count)
                }
                entry = tis.nextEntry
            }
        }
        return ExtractResult(true, "Berhasil mengekstrak $count file", count)
    }

    private fun extractSevenZip(
        path: String, dest: String, onProgress: (Int) -> Unit
    ): ExtractResult {
        var count = 0
        try {
            RandomAccessFile(path, "r").use { raf ->
                SevenZip.openInArchive(null, RandomAccessFileInStream(raf)).use { arc ->
                    arc.extract(null, false, object : IArchiveExtractCallback {
                        override fun getStream(
                            index: Int,
                            extractAskMode: ExtractAskMode
                        ): ISequentialOutStream? {
                            val filePath = arc.getStringProperty(index, PropID.PATH) ?: return null
                            val isDir    = (arc.getProperty(index, PropID.IS_FOLDER) as? Boolean) ?: false
                            val outFile  = File(dest, filePath)
                            return if (isDir) { outFile.mkdirs(); null }
                            else {
                                outFile.parentFile?.mkdirs()
                                count++
                                onProgress(count)
                                ISequentialOutStream { data ->
                                    FileOutputStream(outFile, true).use { it.write(data) }
                                    data.size
                                }
                            }
                        }
                        override fun prepareOperation(extractAskMode: ExtractAskMode) {}
                        override fun setOperationResult(result: ExtractOperationResult) {}
                        override fun setTotal(total: Long) {}
                        override fun setCompleted(complete: Long) {}
                    })
                }
            }
        } catch (e: Exception) {
            return ExtractResult(false, e.message ?: "Gagal ekstrak")
        }
        return ExtractResult(true, "Berhasil mengekstrak $count file", count)
    }

    private fun addToZip(zos: ZipOutputStream, file: File, basePath: String) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { child -> addToZip(zos, child, basePath) }
        } else {
            val entryName = file.absolutePath.removePrefix(basePath).trimStart('/', '\\')
            zos.putNextEntry(ZipEntry(entryName))
            FileInputStream(file).use { fis -> fis.copyTo(zos) }
            zos.closeEntry()
        }
    }

    fun formatSize(bytes: Long): String {
        if (bytes < 0) return "—"
        return when {
            bytes < 1024             -> "$bytes B"
            bytes < 1024 * 1024      -> "%.1f KB".format(bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
            else                     -> "%.1f GB".format(bytes / (1024.0 * 1024 * 1024))
        }
    }

    // ── Smart Unpack ──────────────────────────────────────────
    // Jika arsip hanya berisi satu folder, ekstrak isinya langsung ke destFolder.
    // Jika arsip berisi banyak item atau file langsung, buat folder baru di destFolder sesuai nama arsip.
    // ──────────────────────────────────────────────────────────
    private fun determineExtractTarget(
        archiveFile: File,
        destFolder: File,
        entryNames: List<String>
    ): File {
        val singleFolder = entryNames.singleOrNull { it.endsWith("/") }
        return if (singleFolder != null) {
            destFolder
        } else {
            val safeName = archiveFile.nameWithoutExtension
            File(destFolder, safeName).apply { mkdirs() }
        }
    }
}

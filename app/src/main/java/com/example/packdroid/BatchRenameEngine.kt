package com.example.packdroid

import java.io.File

data class RenamePreview(
    val original : String,
    val newName  : String,
    val file     : File
)

object BatchRenameEngine {

    // Generate preview rename sebelum eksekusi
    fun preview(
        files     : List<File>,
        template  : String,    // contoh: "Foto Liburan_{NNN}"
        startNum  : Int = 1,
        extension : String = "" // kosong = pakai ekstensi asli
    ): List<RenamePreview> {
        return files.mapIndexed { index, file ->
            val num    = (startNum + index).toString().padStart(3, '0')
            val ext    = if (extension.isNotBlank()) ".$extension" else if (file.extension.isNotBlank()) ".${file.extension}" else ""
            val newName = template
                .replace("{NNN}", num)
                .replace("{NN}",  (startNum + index).toString().padStart(2, '0'))
                .replace("{N}",   (startNum + index).toString())
                .replace("{NAME}", file.nameWithoutExtension)
                .replace("{EXT}", file.extension) + ext
            RenamePreview(file.name, newName, file)
        }
    }

    // Eksekusi rename
    fun execute(previews: List<RenamePreview>): Pair<Int, Int> {
        var success = 0
        var failed  = 0
        previews.forEach { p ->
            val dest = File(p.file.parent ?: return@forEach, p.newName)
            if (p.file.renameTo(dest)) success++ else failed++
        }
        return Pair(success, failed)
    }

    // Contoh template
    val templates = listOf(
        "{NAME}_{NNN}"         to "NamaAsli_001, NamaAsli_002",
        "Foto Liburan_{NNN}"   to "Foto Liburan_001, Foto Liburan_002",
        "IMG_{NNN}"            to "IMG_001, IMG_002",
        "File_{NNN}"           to "File_001, File_002",
        "{NNN}_{NAME}"         to "001_NamaAsli, 002_NamaAsli"
    )
}
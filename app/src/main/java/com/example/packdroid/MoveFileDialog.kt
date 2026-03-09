package com.example.packdroid

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun MoveFileDialog(
    files     : List<File>,
    isCopy    : Boolean = false,
    onDismiss : () -> Unit,
    onDone    : (String) -> Unit   // path tujuan
) {
    var currentPath by remember { mutableStateOf("/storage/emulated/0") }
    var folders     by remember { mutableStateOf<List<File>>(emptyList()) }
    var isMoving    by remember { mutableStateOf(false) }
    var resultMsg   by remember { mutableStateOf("") }
    var isSuccess   by remember { mutableStateOf(false) }
    val scope       = rememberCoroutineScope()

    LaunchedEffect(currentPath) {
        folders = withContext(Dispatchers.IO) {
            File(currentPath).listFiles()
                ?.filter { it.isDirectory && !it.isHidden }
                ?.sortedBy { it.name }
                ?: emptyList()
        }
    }

    Dialog(onDismissRequest = { if (!isMoving) onDismiss() }) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(20.dp).height(500.dp)) {

                // Header
                Text(
                    text       = if (isCopy) "📋 Salin ke..." else "✂️ Pindahkan ke...",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp
                )

                Spacer(Modifier.height(4.dp))

                // Info file yang dipindah
                Text(
                    text     = "${files.size} item dipilih",
                    fontSize = 12.sp,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Spacer(Modifier.height(12.dp))

                // Path bar
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick  = { File(currentPath).parent?.let { currentPath = it } },
                        enabled  = File(currentPath).parent != null
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali",
                            modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text     = currentPath,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                // Daftar folder
                if (folders.isEmpty()) {
                    Box(
                        modifier         = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Folder kosong", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(folders) { folder ->
                            Row(
                                modifier          = Modifier
                                    .fillMaxWidth()
                                    .clickable { currentPath = folder.absolutePath }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📁", fontSize = 20.sp)
                                Spacer(Modifier.width(10.dp))
                                Text(folder.name, fontSize = 14.sp, modifier = Modifier.weight(1f),
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Icon(Icons.Default.ChevronRight, null,
                                    modifier = Modifier.size(16.dp),
                                    tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Path tujuan saat ini
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text     = "Tujuan: $currentPath",
                        fontSize = 11.sp,
                        modifier = Modifier.padding(8.dp),
                        color    = MaterialTheme.colorScheme.primary,
                        maxLines = 2
                    )
                }

                Spacer(Modifier.height(8.dp))

                if (isMoving) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(if (isCopy) "Menyalin..." else "Memindahkan...", fontSize = 12.sp)
                }

                if (resultMsg.isNotEmpty()) {
                    Text(
                        text  = resultMsg,
                        color = if (isSuccess) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick  = { if (!isMoving) onDismiss() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Batal") }

                    Button(
                        onClick  = {
                            scope.launch {
                                isMoving  = true
                                resultMsg = ""
                                val dest  = currentPath
                                val result = withContext(Dispatchers.IO) {
                                    try {
                                        files.forEach { file ->
                                            val target = File(dest, file.name)
                                            if (isCopy) {
                                                if (file.isDirectory) file.copyRecursively(target, true)
                                                else file.copyTo(target, overwrite = true)
                                            } else {
                                                file.renameTo(target)
                                            }
                                        }
                                        true
                                    } catch (_: Exception) { false }
                                }
                                isMoving  = false
                                isSuccess = result
                                resultMsg = if (result)
                                    "✅ ${files.size} file berhasil ${if (isCopy) "disalin" else "dipindahkan"}"
                                else "❌ Gagal, coba lagi"
                                if (result) {
                                    kotlinx.coroutines.delay(1000)
                                    onDone(dest)
                                }
                            }
                        },
                        enabled  = !isMoving,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isCopy) "Salin Di Sini" else "Pindah Di Sini",
                            fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
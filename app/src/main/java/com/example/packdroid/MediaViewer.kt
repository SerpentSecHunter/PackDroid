package com.example.packdroid

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import java.io.File

// ── TIPE MEDIA ────────────────────────────────────────────────

enum class MediaType { IMAGE, VIDEO, AUDIO, UNSUPPORTED }

fun detectMediaType(file: File): MediaType {
    val ext = file.extension.lowercase()
    return when (ext) {
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif" -> MediaType.IMAGE
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "3gp", "webm"   -> MediaType.VIDEO
        "mp3", "aac", "wav", "flac", "ogg", "m4a", "opus", "wma"  -> MediaType.AUDIO
        else -> MediaType.UNSUPPORTED
    }
}

// ── IMAGE VIEWER ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewer(file: File, onBack: () -> Unit) {
    var scale       by remember { mutableStateOf(1f) }
    var offsetX     by remember { mutableStateOf(0f) }
    var offsetY     by remember { mutableStateOf(0f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 16.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { scale = 1f; offsetX = 0f; offsetY = 0f }) {
                        Icon(Icons.Default.FitScreen, "Reset Zoom")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier         = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale   = (scale * zoom).coerceIn(0.5f, 5f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(file)
                    .crossfade(true)
                    .build(),
                contentDescription = file.name,
                contentScale       = ContentScale.Fit,
                modifier           = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX       = scale,
                        scaleY       = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
            )

            // Info ukuran file
            Box(
                modifier         = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    "${ArchiveEngine.formatSize(file.length())} · Cubit untuk zoom",
                    color    = Color.White,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// ── VIDEO PLAYER ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayer(file: File, onBack: () -> Unit) {
    val context = LocalContext.current
    val player  = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(file.toUri()))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = { player.pause(); onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier         = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory  = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// ── MUSIC PLAYER ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayer(file: File, onBack: () -> Unit) {
    val context    = LocalContext.current
    val player     = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(file.toUri()))
            prepare()
            playWhenReady = true
        }
    }
    var isPlaying  by remember { mutableStateOf(true) }
    var duration   by remember { mutableStateOf(0L) }
    var position   by remember { mutableStateOf(0L) }

    // Update posisi setiap detik
    LaunchedEffect(player) {
        while (true) {
            isPlaying = player.isPlaying
            position  = player.currentPosition
            duration  = player.duration.takeIf { it > 0 } ?: 0L
            delay(500)
        }
    }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pemutar Musik") },
                navigationIcon = {
                    IconButton(onClick = { player.pause(); onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(Modifier.weight(1f))

            // Album art placeholder
            Box(
                modifier        = Modifier
                    .size(200.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("🎵", fontSize = 72.sp)
            }

            Spacer(Modifier.height(8.dp))

            // Nama file
            Text(
                text       = file.nameWithoutExtension,
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis
            )
            Text(
                text  = file.extension.uppercase(),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            // Progress bar
            Column {
                Slider(
                    value         = if (duration > 0) position.toFloat() / duration.toFloat() else 0f,
                    onValueChange = { player.seekTo((it * duration).toLong()) },
                    modifier      = Modifier.fillMaxWidth()
                )
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatDuration(position), fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text(formatDuration(duration), fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }

            // Kontrol
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                IconButton(onClick = { player.seekTo(0) }) {
                    Icon(Icons.Default.SkipPrevious, "Ulang", modifier = Modifier.size(36.dp))
                }
                FloatingActionButton(
                    onClick        = {
                        if (player.isPlaying) player.pause() else player.play()
                        isPlaying = player.isPlaying
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier       = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier    = Modifier.size(32.dp),
                        tint        = Color.White
                    )
                }
                IconButton(onClick = { player.seekTo(player.duration) }) {
                    Icon(Icons.Default.SkipNext, "Lewati", modifier = Modifier.size(36.dp))
                }
            }

            Spacer(Modifier.weight(1f))

            Text(
                ArchiveEngine.formatSize(file.length()),
                fontSize = 12.sp,
                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSec = ms / 1000
    val min      = totalSec / 60
    val sec      = totalSec % 60
    return "$min:${sec.toString().padStart(2, '0')}"
}
package com.example.packdroid

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// ── MEDIA TYPE DETECTION ──────────────────────────────────────

// ── THEME ─────────────────────────────────────────────────────

private val DarkColors = darkColorScheme(
    primary         = Color(0xFF64B5F6),
    secondary       = Color(0xFF81C784),
    tertiary        = Color(0xFFFFB74D),
    background      = Color(0xFF0D1117),
    surface         = Color(0xFF161B22),
    surfaceVariant  = Color(0xFF21262D),
    onBackground    = Color(0xFFE6EDF3),
    onSurface       = Color(0xFFE6EDF3),
    error           = Color(0xFFFF7B72)
)

private val LightColors = lightColorScheme(
    primary         = Color(0xFF1565C0),
    secondary       = Color(0xFF2E7D32),
    tertiary        = Color(0xFFE65100),
    background      = Color(0xFFF0F2F5),
    surface         = Color(0xFFFFFFFF),
    surfaceVariant  = Color(0xFFE8EAED),
    onBackground    = Color(0xFF1C1C1E),
    onSurface       = Color(0xFF1C1C1E),
    error           = Color(0xFFD32F2F)
)

// ── ACTIVITY ──────────────────────────────────────────────────

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context      = LocalContext.current
            val themePref    = remember { ThemePreference(context) }
            val themeMode    by themePref.themeFlow.collectAsStateWithLifecycle(initialValue = "auto")
            val scope        = rememberCoroutineScope()
            val isSystemDark = isSystemInDarkTheme()
            val useDark      = when (themeMode) {
                "dark"  -> true
                "light" -> false
                else    -> isSystemDark
            }
            MaterialTheme(colorScheme = if (useDark) DarkColors else LightColors) {
                PackDroidApp(
                    themeMode     = themeMode,
                    onThemeChange = { mode -> scope.launch { themePref.saveTheme(mode) } }
                )
            }
        }
    }
}

// ── ENUM ──────────────────────────────────────────────────────

enum class AppPage { HOME, BROWSER, ARCHIVES, SETTINGS, RECYCLE_BIN, BATCH_RENAME, SPLIT_MERGE, IMAGE_VIEWER, VIDEO_PLAYER, MUSIC_PLAYER }

// ── ROOT APP ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackDroidApp(
    themeMode     : String,
    onThemeChange : (String) -> Unit
) {
    var currentPage    by remember { mutableStateOf(AppPage.HOME) }
    var browserPath    by remember { mutableStateOf("") }
    var mediaFile      by remember { mutableStateOf<java.io.File?>(null) }
    var showThemeMenu  by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📦", fontSize = 22.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("PackDroid", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                },
                navigationIcon = {
                    if (currentPage == AppPage.BROWSER) {
                        IconButton(onClick = { currentPage = AppPage.HOME }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                        }
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showThemeMenu = true }) {
                            Icon(
                                imageVector = when (themeMode) {
                                    "dark"  -> Icons.Default.DarkMode
                                    "light" -> Icons.Default.LightMode
                                    else    -> Icons.Default.BrightnessAuto
                                },
                                contentDescription = "Tema"
                            )
                        }
                        DropdownMenu(
                            expanded         = showThemeMenu,
                            onDismissRequest = { showThemeMenu = false }
                        ) {
                            DropdownMenuItem(text = { Text("🌙 Gelap") },
                                onClick = { onThemeChange("dark"); showThemeMenu = false })
                            DropdownMenuItem(text = { Text("☀️ Terang") },
                                onClick = { onThemeChange("light"); showThemeMenu = false })
                            DropdownMenuItem(text = { Text("🔄 Otomatis") },
                                onClick = { onThemeChange("auto"); showThemeMenu = false })
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                listOf(
                    Triple(AppPage.HOME,     Icons.Default.Home,     "Beranda"),
                    Triple(AppPage.BROWSER,  Icons.Default.Folder,   "File"),
                    Triple(AppPage.ARCHIVES, Icons.Default.Archive,  "Arsip"),
                    Triple(AppPage.SETTINGS, Icons.Default.Settings, "Pengaturan")
                ).forEach { (page, icon, label) ->
                    NavigationBarItem(
                        selected = currentPage == page,
                        onClick  = { currentPage = page },
                        icon     = { Icon(icon, contentDescription = label) },
                        label    = { Text(label, fontSize = 10.sp) }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentPage) {
                AppPage.HOME     -> HomePage(
                    onNavigate = { path ->
                        browserPath  = path
                        currentPage  = AppPage.BROWSER
                    }
                )
                AppPage.BROWSER  -> FileBrowserPage(
                    initialPath = browserPath,
                    onOpenMedia = { file, type ->
                        mediaFile   = file
                        currentPage = when (type) {
                            MediaType.IMAGE -> AppPage.IMAGE_VIEWER
                            MediaType.VIDEO -> AppPage.VIDEO_PLAYER
                            MediaType.AUDIO -> AppPage.MUSIC_PLAYER
                            else            -> AppPage.BROWSER
                        }
                    }
                )
                AppPage.ARCHIVES -> ArchivePage()
                AppPage.SETTINGS -> SettingsPage(themeMode, onThemeChange)
                AppPage.RECYCLE_BIN  -> RecycleBinPage(context = LocalContext.current)
                AppPage.IMAGE_VIEWER -> mediaFile?.let {
                    ImageViewer(file = it, onBack = { currentPage = AppPage.BROWSER })
                }
                AppPage.VIDEO_PLAYER -> mediaFile?.let {
                    VideoPlayer(file = it, onBack = { currentPage = AppPage.BROWSER })
                }
                AppPage.MUSIC_PLAYER -> mediaFile?.let {
                    MusicPlayer(file = it, onBack = { currentPage = AppPage.BROWSER })
                }
                AppPage.BATCH_RENAME -> BatchRenamePage(files = emptyList<FileItem>(), onDone = { currentPage = AppPage.HOME })
                else -> {}
            }
        }
    }
}

// ── HOME PAGE (Storage List) ──────────────────────────────────

@Composable
fun HomePage(onNavigate: (String) -> Unit) {
    val context      = LocalContext.current
    val scope        = rememberCoroutineScope()
    var volumes      by remember { mutableStateOf<List<StorageVolume2>>(emptyList()) }
    var hasAccess    by remember { mutableStateOf(StorageHelper.hasFullAccess()) }

    // Muat storage saat halaman dibuka
    LaunchedEffect(hasAccess) {
        volumes = withContext(Dispatchers.IO) {
            StorageHelper.getStorageVolumes(context)
        }
    }

    // Cek ulang akses saat resume
    LaunchedEffect(Unit) {
        while (true) {
            val newAccess = StorageHelper.hasFullAccess()
            if (newAccess != hasAccess) {
                hasAccess = newAccess
            }
            kotlinx.coroutines.delay(1000)
        }
    }

    LazyColumn(
        modifier            = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Text(
                    "Penyimpanan",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        }

        // Banner izin jika belum punya akses penuh (Android 11+)
        if (!hasAccess && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier          = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Izin diperlukan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Untuk akses semua file, aktifkan izin 'Semua File'",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { StorageHelper.requestFullAccess(context) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) { Text("Aktifkan", fontSize = 12.sp) }
                    }
                }
            }
        }

        // Daftar storage
        items(volumes) { vol ->
            StorageVolumeItem(volume = vol, onClick = { onNavigate(vol.path) })
            HorizontalDivider(
                modifier = Modifier.padding(start = 72.dp),
                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
            )
        }

        // Quick access section
        item {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Text(
                    "Akses Cepat",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        }

        // Quick access items
        val quickAccess = listOf(
            Triple("📥", "Download",  "/storage/emulated/0/Download"),
            Triple("📸", "DCIM",      "/storage/emulated/0/DCIM"),
            Triple("🎵", "Music",     "/storage/emulated/0/Music"),
            Triple("🎬", "Movies",    "/storage/emulated/0/Movies"),
            Triple("📄", "Documents", "/storage/emulated/0/Documents"),
            Triple("📱", "Android",   "/storage/emulated/0/Android")
        )

        items(quickAccess) { (emoji, name, path) ->
            val dir = File(path)
            if (dir.exists()) {
                QuickAccessItem(
                    emoji   = emoji,
                    name    = name,
                    path    = path,
                    onClick = { onNavigate(path) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 72.dp),
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ── STORAGE VOLUME ITEM ───────────────────────────────────────

@Composable
fun StorageVolumeItem(volume: StorageVolume2, onClick: () -> Unit) {
    val usedPercent = StorageHelper.getUsedPercent(volume)
    val usedBytes   = volume.totalBytes - volume.freeBytes

    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        color    = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier        = Modifier
                    .size(44.dp)
                    .background(
                        color = if (volume.isRemovable)
                            Color(0xFF4CAF50).copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text     = if (volume.isRemovable) "💾" else "📱",
                    fontSize = 22.sp
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = volume.name,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))

                // Progress bar storage
                LinearProgressIndicator(
                    progress         = { usedPercent },
                    modifier         = Modifier.fillMaxWidth().height(4.dp),
                    color            = when {
                        usedPercent > 0.9f -> MaterialTheme.colorScheme.error
                        usedPercent > 0.7f -> Color(0xFFFF9800)
                        else               -> MaterialTheme.colorScheme.primary
                    },
                    trackColor       = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    strokeCap        = StrokeCap.Round
                )

                Spacer(Modifier.height(4.dp))
                Text(
                    text  = "${StorageHelper.formatBytes(usedBytes)} / ${StorageHelper.formatBytes(volume.totalBytes)} · Bebas ${StorageHelper.formatBytes(volume.freeBytes)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
        }
    }
}

// ── QUICK ACCESS ITEM ─────────────────────────────────────────

@Composable
fun QuickAccessItem(emoji: String, name: String, path: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        color    = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier        = Modifier
                    .size(44.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) { Text(emoji, fontSize = 22.sp) }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(path, fontSize = 11.sp,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Icon(Icons.Default.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
        }
    }
}

// ── FILE BROWSER PAGE ─────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileBrowserPage(
    initialPath   : String = "/storage/emulated/0",
    onOpenMedia   : (java.io.File, MediaType) -> Unit = { _, _ -> }
) {
    val scope         = rememberCoroutineScope()
    var currentPath   by remember { mutableStateOf(initialPath.ifBlank { "/storage/emulated/0" }) }
    var files         by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var selectedFile  by remember { mutableStateOf<FileItem?>(null) }
    var showExtDialog by remember { mutableStateOf(false) }
    var showCmpDialog by remember { mutableStateOf(false) }
    var showMenuFor   by remember { mutableStateOf<FileItem?>(null) }
    var isLoading     by remember { mutableStateOf(false) }
    var pathHistory   by remember { mutableStateOf(listOf(initialPath.ifBlank { "/storage/emulated/0" })) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showCopyDialog by remember { mutableStateOf(false) }
    var moveTargetFile by remember { mutableStateOf<FileItem?>(null) }

    fun navigateTo(path: String) {
        currentPath = path
        pathHistory = pathHistory + path
    }

    fun navigateBack() {
        if (pathHistory.size > 1) {
            val newHistory = pathHistory.dropLast(1)
            pathHistory    = newHistory
            currentPath    = newHistory.last()
        }
    }

    LaunchedEffect(currentPath) {
        isLoading = true
        files     = withContext(Dispatchers.IO) { FileManager.listFiles(currentPath) }
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // ── Path Bar ──────────────────────────────────────────
        Surface(
            modifier       = Modifier.fillMaxWidth(),
            color          = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column {
                Row(
                    modifier          = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick  = { navigateBack() },
                        enabled  = pathHistory.size > 1
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali",
                            tint = if (pathHistory.size > 1)
                                MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    }
                    Text(
                        text     = currentPath,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    // Tombol kompres
                    IconButton(onClick = { showCmpDialog = true }) {
                        Icon(Icons.Default.CreateNewFolder, "Kompres",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
                } else {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                }
            }
        }

        // ── File count info ───────────────────────────────────
        if (files.isNotEmpty()) {
            val folderCount = files.count { it.isDirectory }
            val fileCount   = files.count { !it.isDirectory }
            Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(
                    text     = buildString {
                        if (folderCount > 0) append("$folderCount folder")
                        if (folderCount > 0 && fileCount > 0) append(", ")
                        if (fileCount > 0) append("$fileCount file")
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    fontSize = 11.sp,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        // ── File List ─────────────────────────────────────────
        if (!isLoading && files.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📂", fontSize = 52.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Folder kosong",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(files, key = { it.path }) { item ->
                    FileListItem(
                        item        = item,
                        onClick     = {
                            if (item.isDirectory) {
                                navigateTo(item.path)
                            } else if (FileManager.isArchive(item.file)) {
                                selectedFile  = item
                                showExtDialog = true
                            } else {
                                val mediaType = detectMediaType(item.file)
                                if (mediaType != MediaType.UNSUPPORTED) {
                                    onOpenMedia(item.file, mediaType)
                                }
                            }
                        },
                        onLongClick = { showMenuFor = item }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 72.dp),
                        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    )
                }
            }
        }
    }

    // Dialogs
    if (showExtDialog && selectedFile != null) {
        ExtractDialog(file = selectedFile!!.file, onDismiss = { showExtDialog = false })
    }
    if (showCmpDialog) {
        CompressDialog(currentPath = currentPath, files = files, onDismiss = { showCmpDialog = false })
    }
    val ctx = LocalContext.current
    showMenuFor?.let { item ->
        FileContextMenu(
            item         = item,
            context      = ctx,
            onDismiss    = { showMenuFor = null },
            onDelete     = {
                scope.launch(Dispatchers.IO) {
                    RecycleBin.moveToRecycleBin(ctx, item.file)
                    files = FileManager.listFiles(currentPath)
                }
                showMenuFor = null
            },
            onRename     = { newName ->
                scope.launch(Dispatchers.IO) {
                    FileManager.renameFile(item.file, newName)
                    files = FileManager.listFiles(currentPath)
                }
            },
            onCopy       = { AppClipboard.copy(listOf(item.file)) },
            onCut        = { AppClipboard.cut(listOf(item.file)) },
            onBookmark   = {
                if (BookmarkManager.isBookmarked(ctx, item.path)) {
                    BookmarkManager.remove(ctx, item.path)
                } else {
                    BookmarkManager.add(ctx, Bookmark(item.name, item.path,
                        FileManager.getFileIcon(item)))
                }
            },
            isBookmarked = BookmarkManager.isBookmarked(ctx, item.path),
            onMove       = {
                moveTargetFile = item
                showMoveDialog = true
                showMenuFor    = null
            },
            onCopyTo     = {
                moveTargetFile = item
                showCopyDialog = true
                showMenuFor    = null
            }
        )
    }

    // Move dialog
    if (showMoveDialog && moveTargetFile != null) {
        MoveFileDialog(
            files     = listOf(moveTargetFile!!.file),
            isCopy    = false,
            onDismiss = { showMoveDialog = false },
            onDone    = { showMoveDialog = false; scope.launch(Dispatchers.IO) { files = FileManager.listFiles(currentPath) } }
        )
    }

    // Copy dialog
    if (showCopyDialog && moveTargetFile != null) {
        MoveFileDialog(
            files     = listOf(moveTargetFile!!.file),
            isCopy    = true,
            onDismiss = { showCopyDialog = false },
            onDone    = { showCopyDialog = false; scope.launch(Dispatchers.IO) { files = FileManager.listFiles(currentPath) } }
        )
    }
}

// ── ARCHIVE PAGE ──────────────────────────────────────────────

@Composable
fun ArchivePage() {
    var archivePath by remember { mutableStateOf("") }
    var entries     by remember { mutableStateOf<List<ArchiveEntry>>(emptyList()) }
    var isLoading   by remember { mutableStateOf(false) }
    var errorMsg    by remember { mutableStateOf("") }
    val scope       = rememberCoroutineScope()

    Column(
        modifier            = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Lihat Isi Arsip", fontSize = 18.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)

        OutlinedTextField(
            value         = archivePath,
            onValueChange = { archivePath = it; errorMsg = "" },
            label         = { Text("Path file arsip") },
            placeholder   = { Text("/storage/emulated/0/Download/file.zip") },
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth(),
            shape         = RoundedCornerShape(8.dp),
            trailingIcon  = {
                IconButton(onClick = {
                    if (archivePath.isNotBlank()) {
                        scope.launch {
                            isLoading = true
                            errorMsg  = ""
                            entries   = withContext(Dispatchers.IO) {
                                ArchiveEngine.listEntries(archivePath)
                            }
                            isLoading = false
                            if (entries.isEmpty()) errorMsg = "Tidak ada isi atau format tidak didukung"
                        }
                    }
                }) { Icon(Icons.Default.Search, "Cari") }
            }
        )

        if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        if (errorMsg.isNotEmpty()) {
            Text(errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }
        if (entries.isNotEmpty()) {
            Text("${entries.size} item ditemukan",
                fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(entries) { entry ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                        Row(
                            modifier          = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (entry.isFolder) "📁" else "📄", fontSize = 20.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.name, fontSize = 13.sp, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis)
                                if (!entry.isFolder) {
                                    Text(ArchiveEngine.formatSize(entry.size), fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── SETTINGS PAGE ─────────────────────────────────────────────

@Composable
fun SettingsPage(themeMode: String, onThemeChange: (String) -> Unit) {
    val context   = LocalContext.current
    val themePref = remember { ThemePreference(context) }
    val langMode  by themePref.langFlow.collectAsStateWithLifecycle(initialValue = "id")
    val scope     = rememberCoroutineScope()

    LazyColumn(
        modifier            = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Pengaturan", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
        }
        item {
            SettingsCard(title = "Tema Tampilan", icon = "🎨") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("dark" to "🌙 Gelap", "light" to "☀️ Terang", "auto" to "🔄 Auto")
                        .forEach { (mode, label) ->
                            FilterChip(selected = themeMode == mode,
                                onClick = { onThemeChange(mode) },
                                label   = { Text(label, fontSize = 12.sp) })
                        }
                }
            }
        }
        item {
            SettingsCard(title = "Bahasa / Language", icon = "🌐") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("id" to "🇮🇩 Indonesia", "en" to "🇬🇧 English")
                        .forEach { (lang, label) ->
                            FilterChip(selected = langMode == lang,
                                onClick = { scope.launch { themePref.saveLang(lang) } },
                                label   = { Text(label, fontSize = 12.sp) })
                        }
                }
            }
        }
        item {
            SettingsCard(title = "Izin Akses", icon = "🔐") {
                val hasAccess = StorageHelper.hasFullAccess()
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Akses Semua File", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(
                            if (hasAccess) "✅ Aktif" else "❌ Tidak aktif",
                            fontSize = 12.sp,
                            color    = if (hasAccess) MaterialTheme.colorScheme.secondary
                                       else MaterialTheme.colorScheme.error
                        )
                    }
                    if (!hasAccess && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Button(
                            onClick = { StorageHelper.requestFullAccess(context) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) { Text("Aktifkan", fontSize = 12.sp) }
                    }
                }
            }
        }
        item {
            SettingsCard(title = "Format Didukung", icon = "📋") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "ZIP, Z01"      to "ZIP & Multi-volume",
                        "RAR, part.rar" to "RAR v4/v5 + Multi-volume",
                        "7z, 7z.001"    to "7-Zip + Multi-volume",
                        "TAR, TAR.GZ"   to "Unix archive",
                        "ISO"           to "Image disk CD/DVD",
                        "CAB"           to "Windows Cabinet",
                        "ARJ"           to "ARJ archive"
                    ).forEach { (fmt, desc) ->
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(fmt, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary)
                            Text(desc, fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
        item {
            SettingsCard(title = "Tentang PackDroid", icon = "ℹ️") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    InfoRow("Versi",        "1.0.0")
                    InfoRow("Developer",   "Ade Pratama")
                    InfoRow("Email",       "luarnegriakun702@gmail.com")
                    InfoRow("Lisensi",     "MIT")
                    InfoRow("Min Android", "Android 8.0 (API 26)")
                }
            }
        }
    }
}

// ── DIALOGS ───────────────────────────────────────────────────

@Composable
fun ExtractDialog(file: File, onDismiss: () -> Unit) {
    var destPath     by remember { mutableStateOf(file.parent ?: "/storage/emulated/0/") }
    var password     by remember { mutableStateOf("") }
    var isExtracting by remember { mutableStateOf(false) }
    var resultMsg    by remember { mutableStateOf("") }
    var isSuccess    by remember { mutableStateOf(false) }
    val scope        = rememberCoroutineScope()

    Dialog(onDismissRequest = { if (!isExtracting) onDismiss() }) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier            = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("🗜️ Ekstrak Arsip", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(file.name, fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                OutlinedTextField(
                    value = destPath, onValueChange = { destPath = it },
                    label = { Text("Folder Tujuan") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text("Kata Sandi (opsional)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)
                )
                if (isExtracting) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Sedang mengekstrak...", fontSize = 13.sp)
                }
                if (resultMsg.isNotEmpty()) {
                    Text(resultMsg,
                        color = if (isSuccess) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.error,
                        fontSize = 13.sp)
                }
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick  = { if (!isExtracting) onDismiss() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Batal") }
                    Button(
                        onClick  = {
                            scope.launch {
                                isExtracting = true
                                resultMsg    = ""
                                val result   = withContext(Dispatchers.IO) {
                                    ArchiveEngine.extract(
                                        filePath   = file.absolutePath,
                                        destFolder = destPath,
                                        password   = password.ifBlank { null }
                                    )
                                }
                                isExtracting = false
                                isSuccess    = result.success
                                resultMsg    = if (result.success)
                                    "✅ ${result.filesExtracted} file diekstrak"
                                else "❌ ${result.message}"
                            }
                        },
                        enabled  = !isExtracting,
                        modifier = Modifier.weight(1f)
                    ) { Text("Ekstrak") }
                }
            }
        }
    }
}

@Composable
fun CompressDialog(currentPath: String, files: List<FileItem>, onDismiss: () -> Unit) {
    var outputName    by remember { mutableStateOf("arsip_baru") }
    var format        by remember { mutableStateOf("zip") }
    var password      by remember { mutableStateOf("") }
    var isCompressing by remember { mutableStateOf(false) }
    var resultMsg     by remember { mutableStateOf("") }
    var isSuccess     by remember { mutableStateOf(false) }
    val scope         = rememberCoroutineScope()

    Dialog(onDismissRequest = { if (!isCompressing) onDismiss() }) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier            = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("📦 Kompres File", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                OutlinedTextField(
                    value = outputName, onValueChange = { outputName = it },
                    label = { Text("Nama File Output") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)
                )
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Format:", fontSize = 13.sp)
                    listOf("zip", "7z").forEach { fmt ->
                        FilterChip(
                            selected = format == fmt,
                            onClick  = { format = fmt },
                            label    = { Text(fmt.uppercase()) }
                        )
                    }
                }
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text("Kata Sandi (opsional)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)
                )
                if (isCompressing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Sedang mengompres...", fontSize = 13.sp)
                }
                if (resultMsg.isNotEmpty()) {
                    Text(resultMsg,
                        color = if (isSuccess) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.error,
                        fontSize = 13.sp)
                }
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick  = { if (!isCompressing) onDismiss() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Batal") }
                    Button(
                        onClick  = {
                            scope.launch {
                                isCompressing = true
                                resultMsg     = ""
                                val sources   = files.map { it.file }
                                val outputPath = "$currentPath/$outputName.$format"
                                val result    = withContext(Dispatchers.IO) {
                                    if (format == "7z")
                                        ArchiveEngine.compressTo7z(sources, outputPath, password.ifBlank { null })
                                    else
                                        ArchiveEngine.compressToZip(sources, outputPath, password.ifBlank { null })
                                }
                                isCompressing = false
                                isSuccess     = result.success
                                resultMsg     = if (result.success)
                                    "✅ Tersimpan: ${result.outputPath}"
                                else "❌ ${result.message}"
                            }
                        },
                        enabled  = !isCompressing,
                        modifier = Modifier.weight(1f)
                    ) { Text("Kompres") }
                }
            }
        }
    }
}

@Composable
fun FileContextMenu(
    item      : FileItem,
    context   : android.content.Context,
    onDismiss : () -> Unit,
    onDelete  : () -> Unit,
    onRename  : (String) -> Unit,
    onCopy    : () -> Unit,
    onCut     : () -> Unit,
    onBookmark: () -> Unit,
    isBookmarked: Boolean,
    onMove    : () -> Unit,
    onCopyTo  : () -> Unit
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var showPropsDialog  by remember { mutableStateOf(false) }
    var newName          by remember { mutableStateOf(item.name) }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("✏️ Ganti Nama") },
            text  = {
                OutlinedTextField(
                    value         = newName,
                    onValueChange = { newName = it },
                    label         = { Text("Nama baru") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newName.isNotBlank()) {
                        onRename(newName)
                        showRenameDialog = false
                        onDismiss()
                    }
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Batal") }
            }
        )
        return
    }

    if (showPropsDialog) {
        AlertDialog(
            onDismissRequest = { showPropsDialog = false },
            title = { Text("ℹ️ Properti") },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    InfoRow("Nama",     item.name)
                    InfoRow("Lokasi",   item.path)
                    InfoRow("Tipe",     if (item.isDirectory) "Folder" else item.extension.uppercase())
                    if (!item.isDirectory) InfoRow("Ukuran", ArchiveEngine.formatSize(item.size))
                    InfoRow("Diubah",  FileManager.formatDate(item.lastModified))
                }
            },
            confirmButton = {
                TextButton(onClick = { showPropsDialog = false }) { Text("Tutup") }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(FileManager.getFileIcon(item), fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                // Salin
                ContextMenuItem(Icons.Default.ContentCopy, "Salin") { onCopy(); onDismiss() }
                // Potong
                ContextMenuItem(Icons.Default.ContentCut, "Potong") { onCut(); onDismiss() }
                // Pindah
                ContextMenuItem(Icons.Default.DriveFileMove, "Pindah") { onMove(); onDismiss() }
                // Salin ke
                ContextMenuItem(Icons.Default.FileCopy, "Salin ke") { onCopyTo(); onDismiss() }
                // Rename
                ContextMenuItem(Icons.Default.DriveFileRenameOutline, "Ganti Nama") {
                    showRenameDialog = true
                }
                // Bookmark
                ContextMenuItem(
                    if (isBookmarked) Icons.Default.BookmarkRemove else Icons.Default.BookmarkAdd,
                    if (isBookmarked) "Hapus Bookmark" else "Tambah Bookmark"
                ) { onBookmark(); onDismiss() }
                // Properti
                ContextMenuItem(Icons.Default.Info, "Properti") { showPropsDialog = true }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                // Hapus
                ContextMenuItem(Icons.Default.Delete, "Hapus ke Tempat Sampah",
                    color = MaterialTheme.colorScheme.error) {
                    onDelete()
                    onDismiss()
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

@Composable
fun ContextMenuItem(
    icon   : androidx.compose.ui.graphics.vector.ImageVector,
    label  : String,
    color  : Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier          = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, fontSize = 14.sp, color = color)
    }
}

// ── REUSABLE COMPOSABLES ──────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(item: FileItem, onClick: () -> Unit, onLongClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().combinedClickable(
            onClick     = onClick,
            onLongClick = onLongClick
        ),
        color = Color.Transparent
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier        = Modifier
                    .size(44.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(FileManager.getFileIcon(item), fontSize = 22.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, fontSize = 14.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                Text(
                    text     = if (item.isDirectory) "Folder"
                               else "${ArchiveEngine.formatSize(item.size)} · ${FileManager.formatDate(item.lastModified)}",
                    fontSize = 12.sp,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            if (FileManager.isArchive(item.file)) {
                Icon(Icons.Default.Archive, null, modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary)
            } else if (item.isDirectory) {
                Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            }
        }
    }
}

@Composable
fun SettingsCard(title: String, icon: String, content: @Composable () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            content()
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

// -- RECYCLE BIN PAGE ------------------------------------------

@Composable
fun RecycleBinPage(context: android.content.Context) {
    var items by remember { mutableStateOf(RecycleBin.getRecycleItems(context)) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text("Tempat Sampah", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
            if (items.isNotEmpty()) {
                TextButton(onClick = {
                    scope.launch(Dispatchers.IO) {
                        RecycleBin.emptyRecycleBin(context)
                        withContext(Dispatchers.Main) {
                            items = RecycleBin.getRecycleItems(context)
                        }
                    }
                }) { Text("Kosongkan", color = MaterialTheme.colorScheme.error) }
            }
        }

        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("X", fontSize = 52.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Tempat sampah kosong",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items) { item ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                        Row(
                            modifier          = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (item.isDirectory) "DIR" else "FILE", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(RecycleBin.formatDeletedDate(item.deletedAt),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                            IconButton(onClick = {
                                scope.launch(Dispatchers.IO) {
                                    RecycleBin.restore(context, item)
                                    withContext(Dispatchers.Main) {
                                        items = RecycleBin.getRecycleItems(context)
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Restore, "Pulihkan",
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = {
                                scope.launch(Dispatchers.IO) {
                                    RecycleBin.deletePermanent(context, item)
                                    withContext(Dispatchers.Main) {
                                        items = RecycleBin.getRecycleItems(context)
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Delete, "Hapus",
                                    tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -- BATCH RENAME PAGE -----------------------------------------

@Composable
fun BatchRenamePage(files: List<FileItem>, onDone: () -> Unit) {
    var template  by remember { mutableStateOf("{NAME}_{NNN}") }
    var startNum  by remember { mutableStateOf("1") }
    var previews  by remember { mutableStateOf<List<RenamePreview>>(emptyList()) }
    var resultMsg by remember { mutableStateOf("") }
    val scope     = rememberCoroutineScope()

    LaunchedEffect(template, startNum) {
        val num  = startNum.toIntOrNull() ?: 1
        previews = BatchRenameEngine.preview(files.map { it.file }, template, num)
    }

    Column(
        modifier            = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Ganti Nama Massal", fontSize = 18.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)

        OutlinedTextField(
            value         = template,
            onValueChange = { template = it },
            label         = { Text("Template") },
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth(),
            shape         = RoundedCornerShape(8.dp)
        )

        Text("{NNN}=001 | {NAME}=nama asli | {EXT}=ekstensi",
            fontSize = 11.sp,
            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))

        OutlinedTextField(
            value         = startNum,
            onValueChange = { startNum = it },
            label         = { Text("Nomor Mulai") },
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth(),
            shape         = RoundedCornerShape(8.dp)
        )

        if (resultMsg.isNotEmpty()) {
            Text(resultMsg, color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp)
        }

        Text("Preview (${previews.size} file):", fontWeight = FontWeight.Medium)

        LazyColumn(
            modifier            = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(previews) { p ->
                Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(p.original, fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("-> ${p.newName}", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        Button(
            onClick  = {
                scope.launch(Dispatchers.IO) {
                    val (ok, fail) = BatchRenameEngine.execute(previews)
                    withContext(Dispatchers.Main) {
                        resultMsg = "$ok berhasil${if (fail > 0) ", $fail gagal" else ""}"
                        onDone()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled  = previews.isNotEmpty()
        ) { Text("Jalankan Rename", fontWeight = FontWeight.Bold) }
    }
}

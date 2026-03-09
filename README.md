# 📦 PackDroid

### Pengelola Arsip & File Manager Andal untuk Android

![Version](https://img.shields.io/badge/Versi-1.0-blue)
![Android](https://img.shields.io/badge/Android-26%2B-brightgreen?logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple?logo=kotlin)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.12.01-blue?logo=jetpackcompose)
![AGP](https://img.shields.io/badge/AGP-8.7.3-orange)
![License](https://img.shields.io/badge/Lisensi-MIT-yellow)
![Build](https://img.shields.io/badge/Build-Passing-success)
[![Stars](https://img.shields.io/github/stars/SerpentSecHunter/PackDroid?style=social)](https://github.com/SerpentSecHunter/PackDroid/stargazers)

## 📥 Download

**Versi Terbaru: v1.0.0**

Aplikasi sudah tersedia dalam dua format. Silakan unduh sesuai kebutuhan:

| Nama File | Deskripsi | Tautan Unduh |
|-----------|-----------|--------------|
| `app-release.apk` | File APK untuk instalasi langsung di perangkat Android. | [⬇️ Unduh APK](./releases/app-release.apk) |
| `app-release.aab` | Android App Bundle untuk developer atau upload ke Google Play Console. | [⬇️ Unduh AAB](./releases/app-release.aab) |

**Cara Install APK:**
1. Unduh file `app-release.apk` dari tautan di atas.
2. Buka file tersebut di perangkat Android Anda.
3. Jika diminta, izinkan instalasi dari sumber tidak dikenal.
4. Ikuti petunjuk instalasi hingga selesai.
5. Buka aplikasi **PackDroid** yang telah terpasang.

---

## ✨ Fitur Utama

### 🗂️ Manajemen File
- Browser file lengkap dengan navigasi folder dan history back
- Tampilkan **Memori Internal** dan **Kartu SD** otomatis saat buka app
- Info kapasitas storage dengan progress bar (mirip ZArchiver)
- Akses Cepat: Download, DCIM, Music, Movies, Documents, Android
- **Tap file** → buka langsung sesuai jenis (gambar/video/musik)
- **Long press** → context menu lengkap
- Bookmark folder favorit untuk akses cepat
- Clipboard app: salin, potong, tempel ke folder manapun

### 🗜️ Arsip — Format yang Didukung

| Format | Keterangan |
|--------|-----------|
| ZIP, Z01 | ZIP & Multi-volume |
| RAR, .part.rar | RAR v4/v5 + Multi-volume |
| 7z, 7z.001 | 7-Zip + Multi-volume |
| TAR, TAR.GZ, TGZ | Unix archive |
| ISO | Image disk CD/DVD |
| CAB | Windows Cabinet |
| ARJ | ARJ archive |

- Ekstrak arsip dengan atau tanpa password
- Kompres file ke **ZIP** atau **7z**
- **Smart Unpack** — jika ZIP hanya berisi 1 folder, ekstrak isinya langsung (tidak bersarang)
- Lihat isi arsip tanpa mengekstrak

### 🎬 Media Viewer
- 🖼️ **Image Viewer** — zoom pinch, pan, reset zoom
- 🎵 **Music Player** — play/pause, progress bar, seek, info durasi
- 📹 **Video Player** — ExoPlayer dengan kontrol lengkap
- Format gambar: JPG, PNG, GIF, WebP, HEIC, BMP
- Format video: MP4, MKV, AVI, MOV, WMV, 3GP, WebM
- Format audio: MP3, AAC, WAV, FLAC, OGG, M4A, OPUS

### 📋 Operasi File
- Ganti nama (rename) file dan folder
- Salin, potong, tempel file
- **Pindahkan ke...** — browse dan pilih folder tujuan
- **Salin ke...** — salin file ke folder manapun
- Properti file: ukuran, tanggal, lokasi, tipe
- **Batch Rename** — ganti nama banyak file dengan template

### ✂️ Split & Merge File
- Pecah file besar menjadi beberapa bagian tanpa mengubah format
- Gabungkan kembali file yang terpecah
- Ukuran partisi bisa dikustomisasi
- Cocok untuk video 4GB+, ISO besar, dll

### 🗑️ Recycle Bin (Tempat Sampah)
- File yang dihapus **tidak langsung hilang** — masuk tempat sampah dulu
- Pulihkan file dalam **30 hari**
- Hapus permanen jika sudah yakin
- Tampilkan tanggal penghapusan dan sisa waktu

### ⚙️ Pengaturan
- Tema: **Gelap**, **Terang**, atau **Otomatis** (ikut sistem)
- Bahasa: **Indonesia** 🇮🇩 dan **English** 🇬🇧
- Izin akses `MANAGE_EXTERNAL_STORAGE` untuk Android 11+
- Info versi dan developer

---

## 🛠️ Teknologi

| Komponen | Versi / Keterangan |
|----------|-------------------|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material3 |
| Compose BOM | 2024.12.01 |
| Min SDK | API 26 (Android 8.0) |
| Target SDK | API 35 |
| AGP | 8.7.3 |
| Archive | Apache Commons Compress + junrar + SevenZipJBinding |
| Media | ExoPlayer (Media3) |
| Image | Coil Compose |
| Storage | MANAGE_EXTERNAL_STORAGE + Environment API |
| Preferences | DataStore |
| JSON | Gson |

---

## 📂 Struktur Project

```
PackDroid/
├── app/src/main/java/com/example/packdroid/
│   ├── MainActivity.kt        # UI utama, navigasi, semua halaman
│   ├── ArchiveEngine.kt       # Ekstrak & kompres semua format arsip
│   ├── FileManager.kt         # Browse, salin, hapus, rename file
│   ├── MediaViewer.kt         # Image Viewer, Video Player, Music Player
│   ├── MoveFileDialog.kt      # Dialog pindah & salin file
│   ├── StorageManager.kt      # Deteksi Internal storage & SD Card
│   ├── RecycleBin.kt          # Tempat sampah 30 hari
│   ├── AppClipboard.kt        # Clipboard history salin/potong
│   ├── BookmarkManager.kt     # Bookmark folder favorit
│   ├── BatchRenameEngine.kt   # Ganti nama massal dengan template
│   ├── SplitMergeEngine.kt    # Split & merge file
│   └── ThemePreference.kt     # Preferensi tema & bahasa
└── app/build.gradle.kts       # Konfigurasi build & dependencies
```

---

## 🚀 Cara Build

### Prasyarat
- Android Studio Hedgehog atau lebih baru
- JDK 11+
- Android SDK 35
- Perangkat/emulator Android 26+

### Clone & Build
```bash
# Clone repository
git clone https://github.com/SerpentSecHunter/PackDroid.git
cd PackDroid

# Build debug APK
.\gradlew app:assembleDebug

# Install ke perangkat
.\gradlew app:installDebug
```

### Lokasi APK
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 📖 Cara Penggunaan

### Jelajahi File
1. Buka app → **Beranda** tampil storage otomatis
2. Tap storage → masuk file browser
3. Tap folder untuk masuk, tombol ← untuk kembali
4. Tap file gambar/video/musik → langsung dibuka
5. Tap file arsip → dialog ekstrak
6. **Long press** file → menu: Ganti Nama, Salin, Potong, Pindah, Hapus, Bookmark

### Ekstrak Arsip
1. Tap file arsip (ZIP, RAR, 7z, dll)
2. Tentukan folder tujuan
3. Masukkan password jika diperlukan
4. Tap **Ekstrak**

### Kompres File
1. Di file browser, tap ikon 📁+ kanan atas
2. Masukkan nama output, pilih format ZIP/7z
3. Masukkan password jika ingin diproteksi
4. Tap **Kompres**

### Pindah / Salin File
1. Long press file → tap **Pindahkan ke...** atau **Salin ke...**
2. Browse folder tujuan
3. Tap **Pindah Di Sini** atau **Salin Di Sini**

### Batch Rename
1. Buka tab Pengaturan → Batch Rename
2. Masukkan template: `Foto_{NNN}`, `IMG_{NNN}`, dll
3. Preview hasil rename
4. Tap **Jalankan Rename**

---

## 🗺️ Roadmap

### ✅ Selesai
- [x] File browser dengan Internal + SD Card
- [x] Ekstrak ZIP, RAR, 7z, TAR, ISO, CAB, ARJ
- [x] Kompres ke ZIP & 7z
- [x] Image Viewer, Music Player, Video Player
- [x] Pindah & Salin file dengan folder picker
- [x] Rename, Salin, Potong, Tempel
- [x] Batch Rename dengan template
- [x] Smart Unpack
- [x] Recycle Bin 30 hari
- [x] Clipboard History
- [x] Bookmark Folder
- [x] Split & Merge file
- [x] Dark/Light/Auto theme
- [x] Bahasa Indonesia & Inggris

### 🔜 Akan Datang
- [ ] FTP Server (transfer via WiFi)
- [ ] Vault / Safe Box (kunci file dengan PIN/sidik jari)
- [ ] Enkripsi nama file AES-256
- [ ] Auto-Delete setelah kompres
- [ ] Dual Panel / Split View
- [ ] Cloud Storage (Google Drive, Dropbox)
- [ ] Stealth Mode (ikon palsu)

---

## 📄 Lisensi

```
MIT License

Copyright (c) 2026 Ade Pratama

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
```

---

## © Hak Cipta

```
Copyright (c) 2026 Ade Pratama
Email : luarnegriakun702@gmail.com
GitHub: https://github.com/SerpentSecHunter

Seluruh hak cipta dilindungi undang-undang.
Dilarang menyalin, mendistribusikan, atau memodifikasi
aplikasi ini tanpa izin tertulis dari pengembang.
```

---

## 👨‍💻 Developer

<div align="center">

| | |
|---|---|
| **Nama** | Ade Pratama |
| **Email** | luarnegriakun702@gmail.com |
| **GitHub** | [@SerpentSecHunter](https://github.com/SerpentSecHunter) |
| **Project** | [PackDroid](https://github.com/SerpentSecHunter/PackDroid) |

Dibuat dengan ❤️ menggunakan **Kotlin** + **Jetpack Compose**

---

⭐ Jika aplikasi ini bermanfaat, beri bintang di GitHub!

**[github.com/SerpentSecHunter/PackDroid](https://github.com/SerpentSecHunter/PackDroid)**

© 2026 **Ade Pratama** · luarnegriakun702@gmail.com

</div>
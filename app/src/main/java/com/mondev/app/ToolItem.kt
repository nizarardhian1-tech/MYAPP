package com.mondev.app

/**
 * type values:
 *   "apk"    → Android APK, dicek installed, ada progress download, tombol Install/Update
 *   "script" → File teks/lua/sh, tidak dicek installed, tombol Download + Open
 *   "zip"    → Source code / asset zip, tidak dicek installed, tombol Download + Open
 *   "binary" → Executable binary, tidak dicek installed, tombol Download
 *   "link"   → Hanya URL eksternal, tombol "Buka di Browser"
 */
data class ToolItem(
    val name:        String,
    val shortDesc:   String,
    val desc:        String,
    val version:     String,
    val packageName: String,           // hanya relevan untuk type="apk"
    val apkUrl:      String,           // URL file (apk/script/zip/binary) atau URL link
    val iconUrl:     String,
    val category:    String = "Tools",
    val developer:   String = "Unknown",
    val size:        String = "",
    val changelog:   String = "",
    val tags:        List<String> = emptyList(),
    val screenshots: List<String> = emptyList(),
    val forceUpdate: Boolean = false,  // hanya relevan untuk type="apk"
    val type:        String = "apk",
    val needsUpdate: Boolean = false,  // field opsional dari JSON untuk non-apk
    val latestVersion: String = ""     // versi terbaru untuk non-apk dari JSON
) {
    val isApk: Boolean    get() = type == "apk"
    val isScript: Boolean get() = type == "script"
    val isZip: Boolean    get() = type == "zip"
    val isBinary: Boolean get() = type == "binary"
    val isLink: Boolean   get() = type == "link"
    val isNonApk: Boolean get() = !isApk
}

package com.mondev.app

data class ToolItem(
    val name: String,
    val shortDesc: String,
    val desc: String,
    val version: String,
    val packageName: String,
    val apkUrl: String,
    val iconUrl: String,
    val category: String = "Tools",
    val developer: String = "Unknown",
    val size: String = "",
    val changelog: String = "",
    val tags: List<String> = emptyList(),
    val screenshots: List<String> = emptyList(),
    val forceUpdate: Boolean = false,
    val type: String = "apk" // "apk", "script", "binary"
)

package com.mondev.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import java.net.HttpURLConnection
import java.net.URL

object ToolsRepository {

    suspend fun fetchRawJson(url: String): String = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout    = 10_000
        conn.requestMethod  = "GET"
        conn.setRequestProperty("Cache-Control", "no-cache")

        val code = conn.responseCode
        if (code != HttpURLConnection.HTTP_OK) {
            conn.disconnect()
            throw Exception("HTTP $code")
        }
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()

        // Validasi dulu sebelum return
        try { JSONArray(body) } catch (e: JSONException) {
            throw JSONException("Format JSON tidak valid: ${e.message}")
        }
        body
    }

    fun parseToolsJson(json: String): List<ToolItem> {
        val list = mutableListOf<ToolItem>()
        val arr  = JSONArray(json)

        for (i in 0 until arr.length()) {
            val obj  = arr.getJSONObject(i)
            val name = obj.optString("name", "").trim()
            if (name.isEmpty()) continue

            val tags = buildList {
                val tj = obj.optJSONArray("tags")
                if (tj != null) for (t in 0 until tj.length()) add(tj.getString(t))
            }
            val screenshots = buildList {
                val sj = obj.optJSONArray("screenshots")
                if (sj != null) for (s in 0 until sj.length()) add(sj.getString(s))
            }

            list.add(ToolItem(
                name          = name,
                shortDesc     = obj.optString("short_desc", ""),
                desc          = obj.optString("desc", ""),
                version       = obj.optString("version", "1.0"),
                packageName   = obj.optString("package_name", ""),
                apkUrl        = obj.optString("apk_url", ""),
                iconUrl       = obj.optString("icon_url", ""),
                category      = obj.optString("category", "Tools"),
                developer     = obj.optString("developer", "Unknown"),
                size          = obj.optString("size", ""),
                changelog     = obj.optString("changelog", ""),
                tags          = tags,
                screenshots   = screenshots,
                forceUpdate   = obj.optBoolean("force_update", false),
                type          = obj.optString("type", "apk").lowercase(),
                needsUpdate   = obj.optBoolean("needs_update", false),
                latestVersion = obj.optString("latest_version", "")
            ))
        }
        return list
    }
}

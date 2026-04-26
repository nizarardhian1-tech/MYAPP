package com.mondev.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

object ToolsRepository {

    suspend fun fetchTools(url: String): List<ToolItem> = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 8000
        connection.readTimeout = 8000
        connection.requestMethod = "GET"
        connection.setRequestProperty("Cache-Control", "no-cache")

        val json = connection.inputStream.bufferedReader().use { it.readText() }
        connection.disconnect()
        parseTools(json)
    }

    private fun parseTools(json: String): List<ToolItem> {
        val list = mutableListOf<ToolItem>()
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)

            // Parse tags array
            val tagsJson = obj.optJSONArray("tags")
            val tags = mutableListOf<String>()
            if (tagsJson != null) {
                for (t in 0 until tagsJson.length()) tags.add(tagsJson.getString(t))
            }

            // Parse screenshots array
            val ssJson = obj.optJSONArray("screenshots")
            val screenshots = mutableListOf<String>()
            if (ssJson != null) {
                for (s in 0 until ssJson.length()) screenshots.add(ssJson.getString(s))
            }

            list.add(
                ToolItem(
                    name        = obj.getString("name"),
                    shortDesc   = obj.optString("short_desc", ""),
                    desc        = obj.optString("desc", ""),
                    version     = obj.optString("version", "1.0"),
                    packageName = obj.optString("package_name", ""),
                    apkUrl      = obj.optString("apk_url", ""),
                    iconUrl     = obj.optString("icon_url", ""),
                    category    = obj.optString("category", "Tools"),
                    developer   = obj.optString("developer", "Unknown"),
                    size        = obj.optString("size", ""),
                    changelog   = obj.optString("changelog", ""),
                    tags        = tags,
                    screenshots = screenshots
                )
            )
        }
        return list
    }
}

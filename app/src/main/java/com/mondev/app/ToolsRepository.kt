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

        val stream = connection.inputStream
        val jsonString = stream.bufferedReader().use { it.readText() }
        stream.close()
        connection.disconnect()

        parseTools(jsonString)
    }

    private fun parseTools(json: String): List<ToolItem> {
        val list = mutableListOf<ToolItem>()
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(ToolItem(
                name = obj.getString("name"),
                shortDesc = obj.optString("short_desc", ""),
                desc = obj.optString("desc", ""),
                version = obj.optString("version", ""),
                packageName = obj.optString("package_name", ""),
                apkUrl = obj.optString("apk_url", ""),
                iconUrl = obj.optString("icon_url", "")
            ))
        }
        return list
    }
}
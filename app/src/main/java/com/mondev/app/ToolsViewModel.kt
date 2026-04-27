package com.mondev.app

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ToolsViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        const val JSON_URL = "https://raw.githubusercontent.com/Moniop12/APK/refs/heads/main/tools.json"

        // Cache 24 jam
        const val CACHE_VALIDITY_MS = 24L * 60 * 60 * 1000
        const val PREF_NAME        = "tools_cache"
        const val KEY_JSON         = "cached_json"
        const val KEY_FETCH_TIME   = "last_fetch_time"
    }

    private val prefs = app.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // ─── State ───────────────────────────────────────────────────────────────
    private val _allTools         = MutableStateFlow<List<ToolItem>>(emptyList())
    private val _searchQuery      = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow("All")

    val isLoading    = MutableStateFlow(false)
    val error        = MutableStateFlow<String?>(null)
    val lastUpdated  = MutableStateFlow(0L)        // epoch ms dari cache atau server
    val isFromCache  = MutableStateFlow(false)     // true = data dari cache lokal

    val allTools: StateFlow<List<ToolItem>> = _allTools

    val tools: StateFlow<List<ToolItem>> = combine(
        _allTools, _searchQuery, _selectedCategory
    ) { list, query, cat ->
        list.filter { tool ->
            val matchesCat   = cat == "All" || tool.category.equals(cat, ignoreCase = true)
            val matchesQuery = query.isBlank() ||
                    tool.name.contains(query, ignoreCase = true) ||
                    tool.shortDesc.contains(query, ignoreCase = true) ||
                    tool.tags.any { it.contains(query, ignoreCase = true) }
            matchesCat && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val categories: StateFlow<List<String>> = _allTools.map { list ->
        listOf("All") + list.map { it.category }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, listOf("All"))

    val selectedCategory: StateFlow<String> = _selectedCategory

    fun setSearch(query: String) { _searchQuery.value = query }
    fun setCategory(cat: String) { _selectedCategory.value = cat }

    // ─── Fetch (dengan logika cache) ─────────────────────────────────────────
    /**
     * forceRefresh = false → pakai cache jika < 24 jam
     * forceRefresh = true  → langsung ke server (dipanggil oleh refresh() / checkForUpdates())
     */
    fun fetchTools(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            isLoading.value = true
            error.value     = null

            val savedJson   = prefs.getString(KEY_JSON, null)
            val lastFetch   = prefs.getLong(KEY_FETCH_TIME, 0L)
            val cacheAge    = System.currentTimeMillis() - lastFetch
            val cacheValid  = cacheAge < CACHE_VALIDITY_MS && !savedJson.isNullOrBlank()

            // Gunakan cache jika valid dan tidak dipaksa refresh
            if (!forceRefresh && cacheValid) {
                try {
                    _allTools.value    = ToolsRepository.parseToolsJson(savedJson!!)
                    lastUpdated.value  = lastFetch
                    isFromCache.value  = true
                    isLoading.value    = false
                    return@launch
                } catch (_: Exception) {
                    // Cache corrupt → lanjut fetch dari server
                }
            }

            // ─── Fetch dari server ────────────────────────────────────────
            try {
                val json           = ToolsRepository.fetchRawJson(JSON_URL)
                _allTools.value    = ToolsRepository.parseToolsJson(json)

                // Simpan ke cache
                prefs.edit()
                    .putString(KEY_JSON, json)
                    .putLong(KEY_FETCH_TIME, System.currentTimeMillis())
                    .apply()

                lastUpdated.value  = System.currentTimeMillis()
                isFromCache.value  = false
                error.value        = null

            } catch (e: UnknownHostException) {
                handleOffline(savedJson, lastFetch, "Tidak ada koneksi internet")

            } catch (e: SocketTimeoutException) {
                handleOffline(savedJson, lastFetch, "Koneksi timeout")

            } catch (e: JSONException) {
                // JSON dari server tidak valid — jangan tampilkan raw JSON ke user!
                handleOffline(savedJson, lastFetch, "Format data di server tidak valid.\nCek tools.json Anda")

            } catch (e: Exception) {
                val msg = when {
                    e.message?.contains("HTTP 404") == true -> "File tools.json tidak ditemukan di repo (404)"
                    e.message?.contains("HTTP 403") == true -> "Akses repo ditolak (403). Cek visibility repo"
                    e.message?.contains("HTTP 5")   == true -> "Server GitHub sedang bermasalah, coba lagi"
                    else -> "Gagal memuat tools"
                }
                handleOffline(savedJson, lastFetch, msg)
            }

            isLoading.value = false
        }
    }

    /** Fallback ke cache lama jika ada, atau set error tanpa data. */
    private fun handleOffline(savedJson: String?, lastFetch: Long, reason: String) {
        if (!savedJson.isNullOrBlank()) {
            try {
                _allTools.value   = ToolsRepository.parseToolsJson(savedJson)
                lastUpdated.value = lastFetch
                isFromCache.value = true
                // Error non-fatal: ada data dari cache
                error.value = "$reason — menampilkan data terakhir"
            } catch (_: Exception) {
                error.value = reason
            }
        } else {
            // Tidak ada cache sama sekali
            error.value = reason
        }
    }

    /** Dipanggil saat user swipe-refresh (paksa ke server). */
    fun refresh() = fetchTools(forceRefresh = true)

    /** Dipanggil saat user klik "Cek Update" (sama dengan refresh). */
    fun checkForUpdates() = fetchTools(forceRefresh = true)
}

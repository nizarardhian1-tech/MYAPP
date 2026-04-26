package com.mondev.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ToolsViewModel : ViewModel() {

    companion object {
        const val JSON_URL =
            "https://raw.githubusercontent.com/Moniop12/APK/refs/heads/main/tools.json"
    }

    // Raw list from network
    private val _allTools = MutableStateFlow<List<ToolItem>>(emptyList())

    // UI state
    private val _searchQuery     = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow("All")

    val isLoading = MutableStateFlow(false)
    val error     = MutableStateFlow<String?>(null)

    /** Filtered list shown in HomeFragment */
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

    /** All tools without filter (used by Dashboard) */
    val allTools: StateFlow<List<ToolItem>> = _allTools

    /** Distinct categories derived from data */
    val categories: StateFlow<List<String>> = _allTools.map { list ->
        listOf("All") + list.map { it.category }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, listOf("All"))

    fun setSearch(query: String) { _searchQuery.value = query }
    fun setCategory(cat: String) { _selectedCategory.value = cat }
    val selectedCategory: StateFlow<String> = _selectedCategory

    fun fetchTools(url: String = JSON_URL) {
        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            try {
                _allTools.value = ToolsRepository.fetchTools(url)
            } catch (e: Exception) {
                error.value = "Failed to load tools: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun refresh() = fetchTools()
}

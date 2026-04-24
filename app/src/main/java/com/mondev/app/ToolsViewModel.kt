package com.mondev.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ToolsViewModel : ViewModel() {
    private val _tools = MutableStateFlow<List<ToolItem>>(emptyList())
    val tools: StateFlow<List<ToolItem>> = _tools

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun fetchTools(url: String = "https://raw.githubusercontent.com/Moniop12/android-c-Compiler/refs/heads/main/tools.json") {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _tools.value = ToolsRepository.fetchTools(url)
            } catch (e: Exception) {
                _error.value = "Failed to load tools: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
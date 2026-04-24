package com.mondev.app

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ToolsViewModel : ViewModel() {
    private val _tools = MutableLiveData<List<ToolItem>>()
    val tools: LiveData<List<ToolItem>> = _tools

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

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
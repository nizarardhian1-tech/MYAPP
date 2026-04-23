package com.tes.jk

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * MainViewModel — ViewModel untuk MainActivity.
 * TODO: Tambah data dan business logic di sini
 */
class MainViewModel : ViewModel() {

    private val _statusText = MutableLiveData("Ready")
    val statusText: LiveData<String> get() = _statusText

    fun updateStatus(text: String) { _statusText.value = text }
}

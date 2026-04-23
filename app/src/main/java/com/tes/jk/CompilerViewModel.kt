package com.tes.jk

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * CompilerViewModel — Menangani proses instalasi NDK dan kompilasi.
 */
class CompilerViewModel(application: Application) : AndroidViewModel(application) {

    private val _compileLog = MutableLiveData<String>()
    val compileLog: LiveData<String> = _compileLog

    private val _ndkStatus = MutableLiveData<Boolean>(NdkManager.isInstalled(application))
    val ndkStatus: LiveData<Boolean> = _ndkStatus

    /**
     * Install NDK dari file ZIP.
     */
    fun installNdk(uri: Uri) {
        viewModelScope.launch {
            _compileLog.value = "⏳ Mengekstrak NDK... Mohon tunggu."
            val result = NdkManager.installFromUri(getApplication(), uri)
            result.onSuccess { msg ->
                _compileLog.value = "✅ $msg"
                _ndkStatus.value = true
            }.onFailure { err ->
                _compileLog.value = "❌ Instalasi gagal: ${err.message}"
                _ndkStatus.value = false
            }
        }
    }

    /**
     * Compile kode C++ yang diberikan.
     * @param code String kode C++ dari EditText
     */
    fun compileCode(code: String) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val ndkRoot = NdkManager.getNdkRoot(context)

            // Buat file sementara dari kode
            val srcFile = java.io.File(context.cacheDir, "temp.cpp")
            srcFile.writeText(code)

            val compiler = CppCompiler(ndkRoot)
            val (success, log) = compiler.compile(
                sourceFile = srcFile,
                buildDir = context.cacheDir,
                outputFileName = "liboutput.so"
            )

            _compileLog.value = log
        }
    }

    /**
     * Update status NDK saat fragment dibuka.
     */
    fun refreshNdkStatus() {
        _ndkStatus.value = NdkManager.isInstalled(getApplication())
    }
}
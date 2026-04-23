package com.tes.jk

import android.content.Context
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

object NdkManager {

    private const val NDK_DIR = "ndk"

    fun isInstalled(context: Context): Boolean {
        return getClangBinary(context)?.exists() == true
    }

    fun getClangBinary(context: Context): File? {
        val ndkRoot = File(context.filesDir, NDK_DIR)
        if (!ndkRoot.exists()) return null

        // Cari clang++ di semua kemungkinan path prebuilt
        val prebuiltDir = File(ndkRoot, "toolchains/llvm/prebuilt")
        if (prebuiltDir.exists()) {
            prebuiltDir.listFiles()?.forEach { hostDir ->
                if (hostDir.isDirectory) {
                    val clang = File(hostDir, "bin/clang++")
                    if (clang.exists()) return clang
                }
            }
        }
        
        // Jika tidak ditemukan di prebuilt, coba cari langsung di path lain
        return null
    }

    fun getNdkRoot(context: Context): File {
        return File(context.filesDir, NDK_DIR)
    }

    suspend fun installFromUri(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val ndkDir = File(context.filesDir, NDK_DIR)
            if (ndkDir.exists()) ndkDir.deleteRecursively()
            ndkDir.mkdirs()

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                extractZip(inputStream, ndkDir)
            } ?: throw Exception("Tidak bisa membuka file ZIP")

            setExecutableRecursive(ndkDir)

            if (isInstalled(context)) {
                Result.success("NDK berhasil diinstall")
            } else {
                // Beri pesan error yang lebih jelas
                val foundPath = getClangBinary(context)?.absolutePath
                val message = if (foundPath != null) {
                    "Struktur NDK tidak valid. clang++ ditemukan di $foundPath, tapi mungkin tidak executable."
                } else {
                    "Struktur NDK tidak valid. Pastikan ZIP berisi folder toolchains/llvm/prebuilt/<host>/bin/clang++"
                }
                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractZip(inputStream: InputStream, destDir: File) {
        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val file = File(destDir, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    file.outputStream().use { output ->
                        zis.copyTo(output)
                    }
                }
                entry = zis.nextEntry
            }
        }
    }

    private fun setExecutableRecursive(dir: File) {
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                setExecutableRecursive(file)
            } else {
                // Set executable untuk semua file di dalam folder bin/
                if (file.parentFile?.name == "bin" || 
                    file.nameWithoutExtension in listOf("clang", "clang++", "ld", "ld.lld", "llvm-strip", "llvm-ar")) {
                    file.setExecutable(true, false)
                }
            }
        }
    }
}
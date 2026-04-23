package com.tes.jk

import android.content.Context
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * NdkManager — Mengelola instalasi NDK dari file ZIP eksternal.
 * NDK akan diekstrak ke context.filesDir/ndk/
 */
object NdkManager {

    private const val NDK_DIR = "ndk"

    /**
     * Cek apakah NDK sudah terinstall dan binary clang++ tersedia.
     */
    fun isInstalled(context: Context): Boolean {
        val clang = getClangBinary(context)
        return clang.exists() && clang.canExecute()
    }

    /**
     * Mendapatkan File binary clang++ dari NDK yang terinstall.
     */
    fun getClangBinary(context: Context): File {
        val arch = getHostArch()
        return File(context.filesDir, "$NDK_DIR/toolchains/llvm/prebuilt/linux-$arch/bin/clang++")
    }

    /**
     * Mendapatkan folder root NDK yang terinstall.
     */
    fun getNdkRoot(context: Context): File {
        return File(context.filesDir, NDK_DIR)
    }

    /**
     * Install NDK dari URI file ZIP. Membersihkan instalasi lama jika ada.
     * Mengembalikan Result.success jika berhasil, Result.failure jika gagal.
     */
    suspend fun installFromUri(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val ndkDir = File(context.filesDir, NDK_DIR)

            // Hapus NDK lama jika ada
            if (ndkDir.exists()) {
                ndkDir.deleteRecursively()
            }
            ndkDir.mkdirs()

            // Ekstrak ZIP
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                extractZip(inputStream, ndkDir)
            } ?: throw Exception("Tidak bisa membuka file ZIP")

            // Setelah ekstrak, set executable permission untuk binary
            setExecutableRecursive(ndkDir)

            if (isInstalled(context)) {
                Result.success("NDK berhasil diinstall")
            } else {
                Result.failure(Exception("Struktur NDK tidak valid. Pastikan ZIP berisi folder toolchains/llvm/prebuilt/linux-<arch>/bin/clang++"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ekstrak ZIP ke folder tujuan.
     */
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

    /**
     * Set executable permission (chmod 755) untuk semua file di dalam folder bin/ secara rekursif.
     */
    private fun setExecutableRecursive(dir: File) {
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                setExecutableRecursive(file)
            } else if (file.extension.isBlank() || file.name in listOf("clang", "clang++", "ld", "ld.lld", "llvm-strip", "llvm-ar")) {
                file.setExecutable(true, false)
            }
        }
    }

    /**
     * Mendapatkan arsitektur host (arm64-v8a, armeabi-v7a, x86_64, dll.)
     */
    private fun getHostArch(): String {
        return when (Build.SUPPORTED_ABIS.firstOrNull()) {
            "arm64-v8a" -> "aarch64"
            "armeabi-v7a" -> "arm"
            "x86_64" -> "x86_64"
            "x86" -> "x86"
            else -> "aarch64" // default
        }
    }
}
package com.tes.jk

import android.content.Context
import java.io.File

/**
 * CppCompiler — Kompilasi file C++ menjadi shared library (.so).
 * Menerima Context untuk mencari binary clang++ via NdkManager.
 */
class CppCompiler(private val context: Context) {

    /**
     * Compile file sumber C++ menjadi shared library.
     * @param sourceFile File .cpp yang akan dikompilasi
     * @param buildDir Folder untuk output sementara (biasanya cacheDir)
     * @param outputFileName Nama file output .so (default: liboutput.so)
     * @return Pair<Boolean, String> → (sukses/gagal, log kompilasi)
     */
    fun compile(
        sourceFile: File,
        buildDir: File,
        outputFileName: String = "liboutput.so"
    ): Pair<Boolean, String> {

        val clangpp: File = NdkManager.getClangBinary(context)
            ?: return Pair(false, "ERROR: clang++ tidak ditemukan. Pastikan NDK sudah terinstall dengan benar.")

        val binDir = clangpp.parentFile
            ?: return Pair(false, "ERROR: Tidak bisa menentukan folder bin dari $clangpp")

        // Target triple berdasarkan ABI device
        val (target, apiLevel) = when (android.os.Build.SUPPORTED_ABIS.firstOrNull()) {
            "arm64-v8a" -> "aarch64-linux-android" to "21"
            "armeabi-v7a" -> "armv7a-linux-androideabi" to "21"
            "x86_64" -> "x86_64-linux-android" to "21"
            "x86" -> "i686-linux-android" to "21"
            else -> "aarch64-linux-android" to "21"
        }

        // Cari sysroot
        val sysroot = findSysroot(NdkManager.getNdkRoot(context))
            ?: return Pair(false, "ERROR: sysroot tidak ditemukan di dalam NDK")

        val outputFile = File(buildDir, outputFileName)

        val commands = arrayOf(
            clangpp.absolutePath,
            "-shared",
            "-fPIC",
            "-std=c++17",
            "-static-libstdc++",
            "-target", "$target$apiLevel",
            "--sysroot=$sysroot",
            "-o", outputFile.absolutePath,
            sourceFile.absolutePath
        )

        return try {
            val processBuilder = ProcessBuilder(*commands)
                .directory(buildDir)
                .redirectErrorStream(true)
                .apply {
                    environment()["PATH"] = binDir.absolutePath + ":" + (System.getenv("PATH") ?: "")
                }

            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            val log = buildString {
                appendLine("=== Compile Command ===")
                appendLine(commands.joinToString(" "))
                appendLine()
                appendLine("=== Compile Output ===")
                append(output)
                appendLine()
                appendLine("=== Exit Code: $exitCode ===")
                if (exitCode == 0) {
                    appendLine("✅ Compile SUCCESS")
                    appendLine("Output: ${outputFile.absolutePath}")
                } else {
                    appendLine("❌ Compile FAILED")
                }
            }

            Pair(exitCode == 0, log)
        } catch (e: Exception) {
            Pair(false, "ERROR: ${e.message}\n${e.stackTraceToString()}")
        }
    }

    /**
     * Mencari folder sysroot di dalam NDK yang sudah diekstrak.
     */
    private fun findSysroot(ndkRoot: File): String? {
        val prebuiltDir = File(ndkRoot, "toolchains/llvm/prebuilt")
        if (prebuiltDir.exists()) {
            prebuiltDir.listFiles()?.forEach { hostDir ->
                if (hostDir.isDirectory) {
                    // Coba path: .../prebuilt/<host>/sysroot
                    val sysroot = File(hostDir, "sysroot")
                    if (sysroot.exists()) return sysroot.absolutePath
                }
            }
        }
        // Alternatif: sysroot di root NDK
        val altSysroot = File(ndkRoot, "sysroot")
        if (altSysroot.exists()) return altSysroot.absolutePath
        return null
    }
}
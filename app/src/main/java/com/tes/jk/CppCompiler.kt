package com.tes.jk

import android.content.Context
import android.os.Build
import java.io.File

/**
 * CppCompiler — Kompilasi file C++ menjadi shared library (.so).
 */
class CppCompiler(private val ndkRoot: File) {

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
        val arch = getHostArch()
        val binDir = File(ndkRoot, "toolchains/llvm/prebuilt/linux-$arch/bin")
        val clangpp = File(binDir, "clang++")

        if (!clangpp.exists()) {
            return Pair(false, "ERROR: clang++ tidak ditemukan di ${clangpp.absolutePath}")
        }

        // Target triple berdasarkan ABI
        val (target, apiLevel) = when (Build.SUPPORTED_ABIS.firstOrNull()) {
            "arm64-v8a" -> "aarch64-linux-android" to "21"
            "armeabi-v7a" -> "armv7a-linux-androideabi" to "21"
            "x86_64" -> "x86_64-linux-android" to "21"
            "x86" -> "i686-linux-android" to "21"
            else -> "aarch64-linux-android" to "21"
        }

        // Cari sysroot
        val sysroot = findSysroot(ndkRoot, arch, apiLevel)
            ?: return Pair(false, "ERROR: sysroot tidak ditemukan di NDK")

        val outputFile = File(buildDir, outputFileName)

        // Perintah kompilasi
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
     * Mencari folder sysroot di dalam NDK.
     */
    private fun findSysroot(ndkRoot: File, arch: String, apiLevel: String): String? {
        // Beberapa kemungkinan struktur sysroot
        val candidates = listOf(
            File(ndkRoot, "toolchains/llvm/prebuilt/linux-$arch/sysroot"),
            File(ndkRoot, "sysroot"),
            File(ndkRoot, "platforms/android-$apiLevel/arch-${if (arch == "aarch64") "arm64" else arch}")
        )
        return candidates.firstOrNull { it.exists() && it.isDirectory }?.absolutePath
    }

    private fun getHostArch(): String = when (Build.SUPPORTED_ABIS.firstOrNull()) {
        "arm64-v8a" -> "aarch64"
        "armeabi-v7a" -> "arm"
        "x86_64" -> "x86_64"
        "x86" -> "x86"
        else -> "aarch64"
    }
}
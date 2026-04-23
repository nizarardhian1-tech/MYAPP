package com.tes.jk

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import java.io.File

/**
 * CompilerFragment — Tab utama untuk menulis kode C++ dan compile menjadi .so
 */
class CompilerFragment : Fragment() {

    private val viewModel: CompilerViewModel by viewModels()

    private lateinit var etCode: EditText
    private lateinit var btnCompile: Button
    private lateinit var btnInstallNdk: Button
    private lateinit var btnSaveSo: Button
    private lateinit var tvLog: TextView
    private lateinit var tvNdkStatus: TextView
    private lateinit var scrollLog: ScrollView

    // File picker untuk memilih NDK ZIP
    private val ndkPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.installNdk(it)
        }
    }

    // File picker untuk menyimpan hasil .so
    private val saveSoLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        uri?.let {
            saveSoToUri(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_compiler, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bind view
        etCode = view.findViewById(R.id.etCode)
        btnCompile = view.findViewById(R.id.btnCompile)
        btnInstallNdk = view.findViewById(R.id.btnInstallNdk)
        btnSaveSo = view.findViewById(R.id.btnSaveSo)
        tvLog = view.findViewById(R.id.tvLog)
        tvNdkStatus = view.findViewById(R.id.tvNdkStatus)
        scrollLog = view.findViewById(R.id.scrollLog)

        // Observasi status NDK
        viewModel.ndkStatus.observe(viewLifecycleOwner) { installed ->
            tvNdkStatus.text = if (installed) "✅ NDK Terinstall" else "❌ NDK Belum Terinstall"
            btnInstallNdk.visibility = if (installed) View.GONE else View.VISIBLE
            btnCompile.isEnabled = installed
        }

        // Observasi log compile
        viewModel.compileLog.observe(viewLifecycleOwner) { log ->
            tvLog.text = log
            // Auto-scroll ke bawah
            scrollLog.post {
                scrollLog.fullScroll(View.FOCUS_DOWN)
            }
        }

        // Tombol Install NDK
        btnInstallNdk.setOnClickListener {
            ndkPickerLauncher.launch(arrayOf("application/zip"))
        }

        // Tombol Compile
        btnCompile.setOnClickListener {
            val code = etCode.text.toString().trim()
            if (code.isBlank()) {
                Toast.makeText(context, "Kode C++ tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.compileCode(code)
        }

        // Tombol Simpan .so
        btnSaveSo.setOnClickListener {
            val soFile = File(requireContext().cacheDir, "liboutput.so")
            if (!soFile.exists()) {
                Toast.makeText(context, "File .so belum ada. Compile dulu.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Buka file picker untuk menyimpan
            saveSoLauncher.launch("liboutput.so")
        }

        // Refresh status NDK saat fragment dibuka
        viewModel.refreshNdkStatus()
    }

    /**
     * Menyimpan file .so ke URI yang dipilih user.
     */
    private fun saveSoToUri(destUri: Uri) {
        try {
            val soFile = File(requireContext().cacheDir, "liboutput.so")
            requireContext().contentResolver.openOutputStream(destUri)?.use { output ->
                soFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }
            Toast.makeText(context, "✅ File .so berhasil disimpan", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "❌ Gagal menyimpan: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
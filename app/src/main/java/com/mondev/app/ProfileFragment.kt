package com.mondev.app

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*

class ProfileFragment : Fragment() {

    private val viewModel: ToolsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Versi app
        val tvAppVersion = view.findViewById<TextView>(R.id.tvAppVersion)
        try {
            val info = requireContext().packageManager
                .getPackageInfo(requireContext().packageName, 0)
            tvAppVersion.text = "v${info.versionName}"
        } catch (_: Exception) {}

        // Cek update — pakai ViewModel + Repository baru
        view.findViewById<TextView>(R.id.tvCheckUpdate).setOnClickListener {
            checkForUpdates()
        }

        // Telegram
        view.findViewById<TextView>(R.id.tvTelegram).setOnClickListener {
            openLink("https://t.me/modfreew")
        }

        // GitHub
        view.findViewById<TextView>(R.id.tvGithub)?.setOnClickListener {
            openLink("https://github.com/Moniop12/APK.git")
        }

        // Refresh data (paksa fetch ulang ke server)
        view.findViewById<TextView>(R.id.tvRefreshData)?.setOnClickListener {
            viewModel.refresh()
            Toast.makeText(requireContext(), "Memuat ulang daftar tools…", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkForUpdates() {
        lifecycleScope.launch {
            try {
                // Gunakan fetchRawJson + parseToolsJson sesuai API baru
                val json  = withContext(Dispatchers.IO) {
                    ToolsRepository.fetchRawJson(ToolsViewModel.JSON_URL)
                }
                val tools = ToolsRepository.parseToolsJson(json)

                val updates = tools.filter { tool ->
                    when {
                        tool.packageName.isEmpty() -> false
                        tool.forceUpdate           -> true
                        else -> {
                            try {
                                val info = requireContext().packageManager
                                    .getPackageInfo(tool.packageName, 0)
                                info.versionName != tool.version
                            } catch (_: PackageManager.NameNotFoundException) { false }
                        }
                    }
                }

                if (updates.isEmpty()) {
                    Toast.makeText(requireContext(), "✓ Semua tools sudah terbaru", Toast.LENGTH_SHORT).show()
                } else {
                    val msg = updates.joinToString("\n") { tool ->
                        "• ${tool.name} → v${tool.version}" +
                            if (tool.changelog.isNotBlank()) "\n  ${tool.changelog}" else ""
                    }
                    AlertDialog.Builder(requireContext())
                        .setTitle("${updates.size} Update Tersedia")
                        .setMessage("$msg\n\nBuka tab Home untuk install.")
                        .setPositiveButton("OK", null)
                        .show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(),
                    "Cek update gagal: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openLink(url: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (_: Exception) {
            Toast.makeText(requireContext(), "Tidak ada browser", Toast.LENGTH_SHORT).show()
        }
    }
}

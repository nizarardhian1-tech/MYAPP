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
import kotlinx.coroutines.*

class ProfileFragment : Fragment() {

    private val viewModel: ToolsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // App version
        val tvAppVersion = view.findViewById<TextView>(R.id.tvAppVersion)
        try {
            val info = requireContext().packageManager
                .getPackageInfo(requireContext().packageName, 0)
            tvAppVersion.text = "v${info.versionName}"
        } catch (_: Exception) {}

        // Check updates
        view.findViewById<TextView>(R.id.tvCheckUpdate).setOnClickListener {
            checkForUpdates()
        }

        // Telegram
        view.findViewById<TextView>(R.id.tvTelegram).setOnClickListener {
            openLink("https://t.me/modfreew")
        }

        // GitHub
        view.findViewById<TextView>(R.id.tvGithub)?.setOnClickListener {
            openLink("https://github.com/Moniop12/android-c-Compiler")
        }

        // Refresh JSON
        view.findViewById<TextView>(R.id.tvRefreshData)?.setOnClickListener {
            viewModel.refresh()
            Toast.makeText(requireContext(), "Refreshing tool list…", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkForUpdates() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val tools = withContext(Dispatchers.IO) {
                    ToolsRepository.fetchTools(ToolsViewModel.JSON_URL)
                }
                val updates = tools.filter { tool ->
                    if (tool.packageName.isEmpty()) false
                    else {
                        try {
                            val info = requireContext().packageManager
                                .getPackageInfo(tool.packageName, 0)
                            info.versionName != tool.version
                        } catch (_: PackageManager.NameNotFoundException) { false }
                    }
                }
                if (updates.isEmpty()) {
                    Toast.makeText(requireContext(), "✓ All tools up to date", Toast.LENGTH_SHORT).show()
                } else {
                    val msg = updates.joinToString("\n") {
                        "• ${it.name} → v${it.version}" +
                                if (it.changelog.isNotBlank()) "\n  ${it.changelog}" else ""
                    }
                    AlertDialog.Builder(requireContext())
                        .setTitle("${updates.size} Update(s) Available")
                        .setMessage("$msg\n\nGo to Home tab to install.")
                        .setPositiveButton("OK", null)
                        .show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Update check failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openLink(url: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (_: Exception) { Toast.makeText(requireContext(), "No browser found", Toast.LENGTH_SHORT).show() }
    }
}

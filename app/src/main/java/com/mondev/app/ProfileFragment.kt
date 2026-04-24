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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment() {

    private val viewModel: ToolsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.tvCheckUpdate).setOnClickListener {
            checkForUpdates()
        }

        view.findViewById<TextView>(R.id.tvTelegram).setOnClickListener {
            openTelegram()
        }
    }

    private fun checkForUpdates() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val tools = withContext(Dispatchers.IO) {
                    ToolsRepository.fetchTools("https://raw.githubusercontent.com/Moniop12/android-c-Compiler/refs/heads/main/tools.json")
                }
                val updates = tools.filter { tool ->
                    if (tool.packageName.isEmpty()) false
                    else {
                        try {
                            val info = requireContext().packageManager.getPackageInfo(tool.packageName, 0)
                            info.versionName != tool.version
                        } catch (e: PackageManager.NameNotFoundException) { false }
                    }
                }
                if (updates.isEmpty()) {
                    Toast.makeText(requireContext(), "All tools up to date", Toast.LENGTH_SHORT).show()
                } else {
                    val names = updates.joinToString("\n") { "${it.name} (${it.version})" }
                    AlertDialog.Builder(requireContext())
                        .setTitle("Updates Available")
                        .setMessage(names + "\n\nOpen Home tab to install.")
                        .setPositiveButton("OK", null)
                        .show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Update check failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openTelegram() {
        val url = "https://t.me/modfreew"
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Telegram app not found", Toast.LENGTH_SHORT).show()
        }
    }
}
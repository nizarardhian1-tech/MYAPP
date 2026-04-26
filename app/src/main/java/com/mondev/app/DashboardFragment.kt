package com.mondev.app

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.io.File

class DashboardFragment : Fragment() {

    private val viewModel: ToolsViewModel by activityViewModels()
    private lateinit var toolAdapter: ToolAdapter
    private lateinit var downloadManager: DownloadManager

    private val downloadIds = mutableMapOf<Long, ToolItem>()
    private lateinit var progressHandler: android.os.Handler
    private var pendingInstallFile: File? = null

    private val progressRunnable = object : Runnable {
        override fun run() {
            updateAllProgress()
            if (downloadIds.isNotEmpty()) progressHandler.postDelayed(this, 800)
        }
    }

    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: return
            downloadIds.remove(id)?.let { tool ->
                toolAdapter.clearProgress(tool.packageName)
                toolAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        downloadManager = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        requireActivity().registerReceiver(
            onDownloadComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressHandler = android.os.Handler(android.os.Looper.getMainLooper())

        val rvInstalled      = view.findViewById<RecyclerView>(R.id.rvInstalled)
        val tvEmpty          = view.findViewById<TextView>(R.id.tvEmpty)
        val tvUpdateBadge    = view.findViewById<TextView>(R.id.tvUpdateBadge)
        val tvInstalledCount = view.findViewById<TextView>(R.id.tvInstalledCount)

        // Adapter lengkap dengan callback download & install
        toolAdapter = ToolAdapter(
            onDownload  = { tool -> startDownload(tool) },
            onInstall   = { file, tool -> installApk(file) },
            onCardClick = { tool ->
                if (tool.changelog.isNotBlank())
                    Toast.makeText(requireContext(), "Terbaru: ${tool.changelog}", Toast.LENGTH_LONG).show()
            }
        )
        rvInstalled.layoutManager = LinearLayoutManager(requireContext())
        rvInstalled.adapter = toolAdapter

        lifecycleScope.launch {
            viewModel.allTools.collect { tools ->
                val installed = tools.filter { isPackageInstalled(it.packageName) }
                val updates   = installed.filter { hasUpdate(it) }

                tvInstalledCount.text = "${installed.size} app${if (installed.size != 1) "s" else ""} terinstall"

                if (updates.isNotEmpty()) {
                    tvUpdateBadge.visibility = View.VISIBLE
                    tvUpdateBadge.text = "${updates.size} update tersedia"
                } else {
                    tvUpdateBadge.visibility = View.GONE
                }

                if (installed.isEmpty()) {
                    rvInstalled.visibility = View.GONE
                    tvEmpty.visibility = View.VISIBLE
                } else {
                    rvInstalled.visibility = View.VISIBLE
                    tvEmpty.visibility = View.GONE
                    toolAdapter.submitList(installed)
                }
            }
        }

        if (viewModel.allTools.value.isEmpty()) viewModel.fetchTools()
    }

    override fun onResume() {
        super.onResume()
        // Coba install jika izin baru diberikan
        pendingInstallFile?.let { file ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                requireContext().packageManager.canRequestPackageInstalls()) {
                installApk(file)
                pendingInstallFile = null
            }
        }
        toolAdapter.notifyDataSetChanged()
        if (downloadIds.isNotEmpty()) progressHandler.post(progressRunnable)
    }

    override fun onPause() {
        super.onPause()
        progressHandler.removeCallbacks(progressRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        progressHandler.removeCallbacks(progressRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        requireActivity().unregisterReceiver(onDownloadComplete)
    }

    // ─── Download ────────────────────────────────────────────────
    private fun startDownload(tool: ToolItem) {
        val fileName = "${tool.name.replace(" ", "_")}.apk"
        val file = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (file.exists()) file.delete()

        val request = DownloadManager.Request(Uri.parse(tool.apkUrl))
            .setTitle("Mengunduh ${tool.name}")
            .setDescription("v${tool.version}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(file))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadId = downloadManager.enqueue(request)
        downloadIds[downloadId] = tool
        toolAdapter.setProgress(tool.packageName, 0)
        progressHandler.post(progressRunnable)
        Toast.makeText(requireContext(), "Mengunduh ${tool.name}\u2026", Toast.LENGTH_SHORT).show()
    }

    private fun updateAllProgress() {
        downloadIds.forEach { (id, tool) ->
            val query  = DownloadManager.Query().setFilterById(id)
            val cursor = downloadManager.query(query)
            if (cursor.moveToFirst()) {
                val total    = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                val received = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                if (total > 0) toolAdapter.setProgress(tool.packageName, (received * 100 / total).toInt())
            }
            cursor.close()
        }
    }

    // ─── Install ─────────────────────────────────────────────────
    private fun installApk(file: File) {
        if (!file.exists()) {
            Toast.makeText(requireContext(), "File tidak ditemukan.", Toast.LENGTH_SHORT).show()
            return
        }
        val ctx = requireContext()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !ctx.packageManager.canRequestPackageInstalls()) {
            pendingInstallFile = file
            startActivity(Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${ctx.packageName}")
            })
            return
        }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        pendingInstallFile = null
    }

    // ─── Helper ──────────────────────────────────────────────────
    private fun isPackageInstalled(pkg: String): Boolean {
        if (pkg.isEmpty()) return false
        return try { requireContext().packageManager.getPackageInfo(pkg, 0); true }
        catch (_: PackageManager.NameNotFoundException) { false }
    }

    private fun hasUpdate(tool: ToolItem): Boolean {
        if (tool.packageName.isEmpty()) return false
        if (tool.forceUpdate) return true
        return try {
            val info = requireContext().packageManager.getPackageInfo(tool.packageName, 0)
            info.versionName != tool.version
        } catch (_: PackageManager.NameNotFoundException) { false }
    }
}

package com.mondev.app

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.*
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.io.File

class HomeFragment : Fragment() {

    private val viewModel: ToolsViewModel by activityViewModels()
    private lateinit var adapter: ToolAdapter
    private val downloadIds = mutableMapOf<Long, ToolItem>()

    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
            if (id != -1L && downloadIds.containsKey(id)) {
                // Download selesai, refresh daftar
                refreshAdapter()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().registerReceiver(
            onDownloadComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rvTools)
        adapter = ToolAdapter(
            onDownload = { tool -> startDownload(tool) },
            onInstall = { file -> installApk(file) }
        )
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        lifecycleScope.launch {
            viewModel.tools.collect { tools ->
                adapter.submitList(tools)
            }
        }
        lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                }
            }
        }

        if (viewModel.tools.value.isEmpty()) {
            viewModel.fetchTools()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshAdapter()
    }

    override fun onDestroy() {
        super.onDestroy()
        requireActivity().unregisterReceiver(onDownloadComplete)
    }

    private fun refreshAdapter() {
        adapter.notifyDataSetChanged()
    }

    private fun startDownload(tool: ToolItem) {
        val fileName = "${tool.name}.apk"
        val request = DownloadManager.Request(Uri.parse(tool.apkUrl))
            .setTitle("Downloading ${tool.name}")
            .setDescription("APK file")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            request.setRequiresCharging(false)
        }

        val manager = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = manager.enqueue(request)
        downloadIds[downloadId] = tool
        Toast.makeText(requireContext(), "Download started...", Toast.LENGTH_SHORT).show()
    }

    private fun installApk(file: File) {
        val context = requireContext()
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                Toast.makeText(context, "Allow install from unknown sources", Toast.LENGTH_LONG).show()
                startActivity(Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:" + context.packageName)
                })
                return
            }
        }
        startActivity(intent)
    }
}
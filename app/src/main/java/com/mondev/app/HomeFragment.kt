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
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.launch
import java.io.File

class HomeFragment : Fragment() {

    private val viewModel: ToolsViewModel by activityViewModels()
    private lateinit var toolAdapter: ToolAdapter
    private lateinit var categoryAdapter: CategoryAdapter

    private val downloadIds = mutableMapOf<Long, ToolItem>()
    private lateinit var downloadManager: DownloadManager
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
    ): View? = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressHandler = android.os.Handler(android.os.Looper.getMainLooper())

        // Search bar
        val etSearch = view.findViewById<EditText>(R.id.etSearch)
        etSearch.setHintTextColor(resources.getColor(R.color.text_hint, requireContext().theme))
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearch(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Category chips
        val rvCategories = view.findViewById<RecyclerView>(R.id.rvCategories)
        categoryAdapter = CategoryAdapter { cat -> viewModel.setCategory(cat) }
        rvCategories.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvCategories.adapter = categoryAdapter

        // Tools list
        val rvTools = view.findViewById<RecyclerView>(R.id.rvTools)
        toolAdapter = ToolAdapter(
            onDownload  = { tool -> startDownload(tool) },
            onInstall   = { file, tool -> installApk(file) },
            onCardClick = { tool -> showChangelog(tool) }
        )
        rvTools.layoutManager = LinearLayoutManager(requireContext())
        rvTools.adapter = toolAdapter

        // Swipe refresh
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        swipeRefresh.setColorSchemeResources(R.color.primary)
        swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.bg_card)
        swipeRefresh.setOnRefreshListener { viewModel.refresh() }

        val tvEmpty = view.findViewById<TextView>(R.id.tvEmpty)

        // Observers
        lifecycleScope.launch {
            viewModel.tools.collect { tools ->
                toolAdapter.submitList(tools)
                tvEmpty.visibility = if (tools.isEmpty()) View.VISIBLE else View.GONE
            }
        }
        lifecycleScope.launch {
            viewModel.categories.collect { cats ->
                categoryAdapter.submitList(cats, viewModel.selectedCategory.value)
            }
        }
        lifecycleScope.launch {
            viewModel.selectedCategory.collect { cat ->
                categoryAdapter.submitList(viewModel.categories.value, cat)
            }
        }
        lifecycleScope.launch {
            viewModel.isLoading.collect { loading ->
                swipeRefresh.isRefreshing = loading
            }
        }
        lifecycleScope.launch {
            viewModel.error.collect { err ->
                err?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
            }
        }

        if (viewModel.allTools.value.isEmpty()) viewModel.fetchTools()
    }

    override fun onResume() {
        super.onResume()
        // Coba instal jika ada pending dan izin sudah diberikan
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

    private fun startDownload(tool: ToolItem) {
        val fileName = "${tool.name.replace(" ", "_")}.apk"
        val file = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (file.exists()) file.delete()

        val request = DownloadManager.Request(Uri.parse(tool.apkUrl))
            .setTitle("Downloading ${tool.name}")
            .setDescription("v${tool.version}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(file))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadId = downloadManager.enqueue(request)
        downloadIds[downloadId] = tool
        toolAdapter.setProgress(tool.packageName, 0)
        progressHandler.post(progressRunnable)
        Toast.makeText(requireContext(), "Downloading ${tool.name}\u2026", Toast.LENGTH_SHORT).show()
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

    private fun installApk(file: File) {
        if (!file.exists()) {
            Toast.makeText(requireContext(), "File not found.", Toast.LENGTH_SHORT).show()
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

    private fun showChangelog(tool: ToolItem) {
        if (tool.changelog.isNotBlank()) {
            Toast.makeText(requireContext(), "What's new: ${tool.changelog}", Toast.LENGTH_LONG).show()
        }
    }
}

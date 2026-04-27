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
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private val viewModel: ToolsViewModel by activityViewModels()
    private lateinit var toolAdapter: ToolAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var downloadManager: DownloadManager

    // key = downloadId, value = ToolItem
    private val downloadIds   = mutableMapOf<Long, ToolItem>()
    private lateinit var progressHandler: android.os.Handler
    private var pendingInstallFile: File? = null

    private lateinit var tvCacheStatus: TextView
    private lateinit var btnCheckUpdate: TextView

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
                val key = if (tool.isApk) tool.packageName else tool.name
                toolAdapter.clearProgress(key)
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

        // ── Referensi view ───────────────────────────────────────────────────
        tvCacheStatus  = view.findViewById(R.id.tvCacheStatus)
        btnCheckUpdate = view.findViewById(R.id.btnCheckUpdate)

        // ── Search ───────────────────────────────────────────────────────────
        view.findViewById<EditText>(R.id.etSearch).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                viewModel.setSearch(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // ── Category chips ───────────────────────────────────────────────────
        val rvCategories = view.findViewById<RecyclerView>(R.id.rvCategories)
        categoryAdapter  = CategoryAdapter { cat -> viewModel.setCategory(cat) }
        rvCategories.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvCategories.adapter = categoryAdapter

        // ── Tools list ───────────────────────────────────────────────────────
        val rvTools = view.findViewById<RecyclerView>(R.id.rvTools)
        toolAdapter = ToolAdapter(
            onDownload  = { tool -> startDownload(tool) },
            onInstall   = { file, _  -> installApk(file) },
            onOpen      = { file, tool -> openFile(file, tool) },
            onCardClick = { tool -> showDetail(tool) }
        )
        rvTools.layoutManager = LinearLayoutManager(requireContext())
        rvTools.adapter       = toolAdapter

        // ── Swipe refresh ────────────────────────────────────────────────────
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        swipeRefresh.setColorSchemeResources(R.color.primary)
        swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.bg_card)
        swipeRefresh.setOnRefreshListener { viewModel.refresh() }

        // ── Check update button ───────────────────────────────────────────────
        btnCheckUpdate.setOnClickListener {
            viewModel.checkForUpdates()
        }

        // ── Observers ────────────────────────────────────────────────────────
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmpty)

        lifecycleScope.launch {
            viewModel.tools.collect { tools ->
                toolAdapter.submitList(tools)
                tvEmpty.visibility = if (tools.isEmpty() && !viewModel.isLoading.value)
                    View.VISIBLE else View.GONE
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
                if (loading) tvEmpty.visibility = View.GONE
            }
        }
        lifecycleScope.launch {
            viewModel.error.collect { err ->
                err?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                }
            }
        }
        lifecycleScope.launch {
            viewModel.lastUpdated.collect { ts -> updateCacheLabel(ts) }
        }
        lifecycleScope.launch {
            viewModel.isFromCache.collect { fromCache ->
                btnCheckUpdate.visibility = if (fromCache) View.VISIBLE else View.GONE
            }
        }

        // Fetch awal
        if (viewModel.allTools.value.isEmpty()) viewModel.fetchTools()
    }

    // ── Cache label ──────────────────────────────────────────────────────────
    private fun updateCacheLabel(ts: Long) {
        if (ts == 0L) { tvCacheStatus.visibility = View.GONE; return }
        tvCacheStatus.visibility = View.VISIBLE
        val sdf = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
        tvCacheStatus.text = "Data: ${sdf.format(Date(ts))}"
    }

    // ── onResume ─────────────────────────────────────────────────────────────
    override fun onResume() {
        super.onResume()
        pendingInstallFile?.let { file ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                requireContext().packageManager.canRequestPackageInstalls()) {
                installApk(file); pendingInstallFile = null
            }
        }
        toolAdapter.notifyDataSetChanged()
        if (downloadIds.isNotEmpty()) progressHandler.post(progressRunnable)
    }

    override fun onPause() { super.onPause(); progressHandler.removeCallbacks(progressRunnable) }
    override fun onDestroyView() { super.onDestroyView(); progressHandler.removeCallbacks(progressRunnable) }
    override fun onDestroy() { super.onDestroy(); requireActivity().unregisterReceiver(onDownloadComplete) }

    // ── Download (APK + file) ────────────────────────────────────────────────
    private fun startDownload(tool: ToolItem) {
        val ext = when (tool.type) {
            "apk"    -> ".apk"
            "script" -> {
                val urlLower = tool.apkUrl.lowercase()
                when {
                    urlLower.endsWith(".lua") -> ".lua"
                    urlLower.endsWith(".sh")  -> ".sh"
                    urlLower.endsWith(".py")  -> ".py"
                    else -> ".script"
                }
            }
            "zip"    -> ".zip"
            "binary" -> ".bin"
            else     -> ".file"
        }
        val safeName = tool.name.replace(" ", "_").replace("/", "_")
        val fileName = if (tool.isApk) "${safeName}.apk" else "${safeName}${ext}"
        val dir      = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val file     = File(dir, fileName)
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
        val key = if (tool.isApk) tool.packageName else tool.name
        toolAdapter.setProgress(key, 0)
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
                val key      = if (tool.isApk) tool.packageName else tool.name
                if (total > 0) toolAdapter.setProgress(key, (received * 100 / total).toInt())
            }
            cursor.close()
        }
    }

    // ── Install APK ──────────────────────────────────────────────────────────
    private fun installApk(file: File) {
        if (!file.exists()) { Toast.makeText(requireContext(), "File tidak ditemukan.", Toast.LENGTH_SHORT).show(); return }
        val ctx = requireContext()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !ctx.packageManager.canRequestPackageInstalls()) {
            pendingInstallFile = file
            startActivity(Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${ctx.packageName}")
            })
            return
        }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        pendingInstallFile = null
    }

    // ── Buka file non-APK ────────────────────────────────────────────────────
    private fun openFile(file: File, tool: ToolItem) {
        val ctx  = requireContext()
        val mime = when (tool.type) {
            "script" -> "text/plain"
            "zip"    -> "application/zip"
            "binary" -> "application/octet-stream"
            else     -> "*/*"
        }
        try {
            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Toast.makeText(ctx, "Tidak ada aplikasi untuk membuka file ini", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Detail / changelog ───────────────────────────────────────────────────
    private fun showDetail(tool: ToolItem) {
        if (tool.changelog.isNotBlank())
            Toast.makeText(requireContext(), "Terbaru: ${tool.changelog}", Toast.LENGTH_LONG).show()
    }
}

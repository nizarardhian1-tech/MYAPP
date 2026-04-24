package com.mondev.app

import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import android.content.Intent
import android.net.Uri

class HomeFragment : Fragment() {

    private val viewModel: ToolsViewModel by activityViewModels()
    private lateinit var adapter: ToolAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rvTools)
        adapter = ToolAdapter { tool -> installTool(tool) }
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

    private fun installTool(tool: ToolItem) {
        if (tool.apkUrl.isEmpty()) {
            Toast.makeText(requireContext(), "APK URL not available", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            Toast.makeText(requireContext(), "Downloading ${tool.name}...", Toast.LENGTH_SHORT).show()
            val file = withContext(Dispatchers.IO) {
                downloadApk(tool.apkUrl, tool.name + ".apk")
            }
            if (file != null) {
                installApk(file)
            } else {
                Toast.makeText(requireContext(), "Download failed", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun downloadApk(url: String, fileName: String): File? {
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null

            val file = File(requireContext().externalCacheDir, fileName)
            val fos = FileOutputStream(file)
            val input = connection.inputStream
            val buffer = ByteArray(4096)
            var len: Int
            while (input.read(buffer).also { len = it } > 0) {
                fos.write(buffer, 0, len)
            }
            fos.close()
            input.close()
            connection.disconnect()
            file
        } catch (e: Exception) {
            null
        }
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
                Toast.makeText(context, "Please allow install from unknown sources", Toast.LENGTH_LONG).show()
                val settingsIntent = android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES.let { action ->
                    Intent(action).apply {
                        data = Uri.parse("package:" + context.packageName)
                    }
                }
                startActivity(settingsIntent)
                return
            }
        }
        startActivity(intent)
    }
}
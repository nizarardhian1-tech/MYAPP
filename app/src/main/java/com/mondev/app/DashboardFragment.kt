package com.mondev.app

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private val viewModel: ToolsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvInstalled    = view.findViewById<RecyclerView>(R.id.rvInstalled)
        val tvEmpty        = view.findViewById<TextView>(R.id.tvEmpty)
        val tvUpdateBadge  = view.findViewById<TextView>(R.id.tvUpdateBadge)
        val tvInstalledCount = view.findViewById<TextView>(R.id.tvInstalledCount)

        val adapter = ToolAdapter()
        rvInstalled.layoutManager = LinearLayoutManager(requireContext())
        rvInstalled.adapter = adapter

        lifecycleScope.launch {
            viewModel.allTools.collect { tools ->
                val installed = tools.filter { isPackageInstalled(it.packageName) }
                val updates   = installed.filter { hasUpdate(it) }

                tvInstalledCount.text = "${installed.size} app${if (installed.size != 1) "s" else ""} installed"

                if (updates.isNotEmpty()) {
                    tvUpdateBadge.visibility = View.VISIBLE
                    tvUpdateBadge.text = "${updates.size} update${if (updates.size != 1) "s" else ""} available"
                } else {
                    tvUpdateBadge.visibility = View.GONE
                }

                if (installed.isEmpty()) {
                    rvInstalled.visibility = View.GONE
                    tvEmpty.visibility = View.VISIBLE
                } else {
                    rvInstalled.visibility = View.VISIBLE
                    tvEmpty.visibility = View.GONE
                    adapter.submitList(installed)
                }
            }
        }

        if (viewModel.allTools.value.isEmpty()) viewModel.fetchTools()
    }

    private fun isPackageInstalled(pkg: String): Boolean {
        if (pkg.isEmpty()) return false
        return try { requireContext().packageManager.getPackageInfo(pkg, 0); true }
        catch (_: PackageManager.NameNotFoundException) { false }
    }

    private fun hasUpdate(tool: ToolItem): Boolean {
        if (tool.packageName.isEmpty()) return false
        return try {
            val info = requireContext().packageManager.getPackageInfo(tool.packageName, 0)
            info.versionName != tool.version
        } catch (_: PackageManager.NameNotFoundException) { false }
    }
}

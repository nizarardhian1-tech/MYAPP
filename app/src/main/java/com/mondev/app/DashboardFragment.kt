package com.mondev.app

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DashboardFragment : Fragment() {

    private val viewModel: ToolsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rvInstalled)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmpty)

        val adapter = ToolAdapter(null) // no action needed
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        viewModel.tools.observe(viewLifecycleOwner) { tools ->
            val installed = tools.filter { isPackageInstalled(it.packageName) }
            if (installed.isEmpty()) {
                rv.visibility = View.GONE
                tvEmpty.visibility = View.VISIBLE
            } else {
                rv.visibility = View.VISIBLE
                tvEmpty.visibility = View.GONE
                adapter.submitList(installed)
            }
        }

        if (viewModel.tools.value.isEmpty()) {
            viewModel.fetchTools()
        }
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        if (packageName.isEmpty()) return false
        return try {
            requireContext().packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
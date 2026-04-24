package com.mondev.app

import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.io.File

class ToolAdapter(
    private val onDownload: ((ToolItem) -> Unit)? = null,
    private val onInstall: ((File) -> Unit)? = null
) : ListAdapter<ToolItem, ToolAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvShortDesc: TextView = view.findViewById(R.id.tvShortDesc)
        val tvDesc: TextView = view.findViewById(R.id.tvDesc)
        val tvVersion: TextView = view.findViewById(R.id.tvVersion)
        val btnAction: TextView = view.findViewById(R.id.btnAction)

        fun bind(tool: ToolItem) {
            tvName.text = tool.name
            tvShortDesc.text = tool.shortDesc
            tvDesc.text = HtmlCompat.fromHtml(tool.desc, HtmlCompat.FROM_HTML_MODE_LEGACY)
            tvVersion.text = "v${tool.version}"

            if (tool.iconUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(tool.iconUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .into(ivIcon)
            } else {
                ivIcon.setImageResource(R.drawable.ic_launcher_foreground)
            }

            val ctx = itemView.context

            if (tool.apkUrl.isBlank()) {
                btnAction.text = "Coming Soon"
                btnAction.isEnabled = false
                btnAction.alpha = 0.5f
                return
            }

            val isInstalled = isPackageInstalled(ctx, tool.packageName)
            val fileName = "${tool.name}.apk"
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)

            when {
                isInstalled -> {
                    btnAction.text = "Installed"
                    btnAction.isEnabled = false
                    btnAction.alpha = 0.5f
                }
                file.exists() -> {
                    btnAction.text = "Install"
                    btnAction.isEnabled = true
                    btnAction.alpha = 1.0f
                    btnAction.setOnClickListener { onInstall?.invoke(file) }
                }
                else -> {
                    btnAction.text = "Download"
                    btnAction.isEnabled = true
                    btnAction.alpha = 1.0f
                    btnAction.setOnClickListener { onDownload?.invoke(tool) }
                }
            }
        }

        private fun isPackageInstalled(context: Context, packageName: String): Boolean {
            if (packageName.isEmpty()) return false
            return try {
                context.packageManager.getPackageInfo(packageName, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tool, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<ToolItem>() {
        override fun areItemsTheSame(oldItem: ToolItem, newItem: ToolItem): Boolean {
            return oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(oldItem: ToolItem, newItem: ToolItem): Boolean {
            return oldItem == newItem
        }
    }
}
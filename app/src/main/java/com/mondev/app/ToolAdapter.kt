package com.mondev.app

import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.io.File

class ToolAdapter(
    private val onDownload: ((ToolItem) -> Unit)? = null,
    private val onInstall:  ((File, ToolItem) -> Unit)? = null,
    private val onCardClick: ((ToolItem) -> Unit)? = null
) : ListAdapter<ToolItem, ToolAdapter.ViewHolder>(DiffCallback()) {

    // Map packageName -> download progress 0-100 (-1 = not downloading)
    private val progressMap = mutableMapOf<String, Int>()

    fun setProgress(packageName: String, progress: Int) {
        progressMap[packageName] = progress
        val idx = currentList.indexOfFirst { it.packageName == packageName }
        if (idx >= 0) notifyItemChanged(idx)
    }

    fun clearProgress(packageName: String) {
        progressMap.remove(packageName)
        val idx = currentList.indexOfFirst { it.packageName == packageName }
        if (idx >= 0) notifyItemChanged(idx)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon:       ImageView   = view.findViewById(R.id.ivIcon)
        val tvName:       TextView    = view.findViewById(R.id.tvName)
        val tvDeveloper:  TextView    = view.findViewById(R.id.tvDeveloper)
        val tvShortDesc:  TextView    = view.findViewById(R.id.tvShortDesc)
        val tvDesc:       TextView    = view.findViewById(R.id.tvDesc)
        val tvVersion:    TextView    = view.findViewById(R.id.tvVersion)
        val tvSize:       TextView    = view.findViewById(R.id.tvSize)
        val tvCategory:   TextView    = view.findViewById(R.id.tvCategory)
        val tagContainer: LinearLayout = view.findViewById(R.id.tagContainer)
        val btnAction:    TextView    = view.findViewById(R.id.btnAction)
        val progressBar:  ProgressBar = view.findViewById(R.id.downloadProgress)

        fun bind(tool: ToolItem) {
            tvName.text      = tool.name
            tvDeveloper.text = tool.developer.ifBlank { "Unknown" }
            tvShortDesc.text = tool.shortDesc
            tvDesc.text      = HtmlCompat.fromHtml(tool.desc, HtmlCompat.FROM_HTML_MODE_LEGACY)
            tvVersion.text   = "v${tool.version}"
            tvSize.text      = if (tool.size.isNotBlank()) tool.size else ""
            tvCategory.text  = tool.category

            tvSize.visibility = if (tool.size.isNotBlank()) View.VISIBLE else View.GONE

            // Tags
            tagContainer.removeAllViews()
            tool.tags.take(3).forEach { tag ->
                val chip = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.item_tag_chip, tagContainer, false) as TextView
                chip.text = tag
                tagContainer.addView(chip)
            }
            tagContainer.visibility = if (tool.tags.isEmpty()) View.GONE else View.VISIBLE

            // Icon
            if (tool.iconUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(tool.iconUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .into(ivIcon)
            } else {
                ivIcon.setImageResource(R.drawable.ic_launcher_foreground)
            }

            // Card click
            itemView.setOnClickListener { onCardClick?.invoke(tool) }

            // Progress state
            val progress = progressMap[tool.packageName] ?: -1
            if (progress in 0..99) {
                progressBar.visibility = View.VISIBLE
                progressBar.progress   = progress
                btnAction.text         = "$progress%"
                btnAction.isEnabled    = false
                btnAction.alpha        = 0.6f
                return
            } else {
                progressBar.visibility = View.GONE
            }

            // Coming soon
            if (tool.apkUrl.isBlank()) {
                btnAction.text      = "Coming Soon"
                btnAction.isEnabled = false
                btnAction.alpha     = 0.4f
                return
            }

            val ctx        = itemView.context
            val isInstalled = isPackageInstalled(ctx, tool.packageName)
            val fileName   = "${tool.name.replace(" ", "_")}.apk"
            val file       = File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)

            when {
                isInstalled -> {
                    btnAction.text      = "✓ Installed"
                    btnAction.isEnabled = false
                    btnAction.alpha     = 0.5f
                }
                file.exists() -> {
                    btnAction.text      = "Install"
                    btnAction.isEnabled = true
                    btnAction.alpha     = 1.0f
                    btnAction.setOnClickListener { onInstall?.invoke(file, tool) }
                }
                else -> {
                    btnAction.text      = "Download"
                    btnAction.isEnabled = true
                    btnAction.alpha     = 1.0f
                    btnAction.setOnClickListener { onDownload?.invoke(tool) }
                }
            }
        }

        private fun isPackageInstalled(ctx: Context, pkg: String): Boolean {
            if (pkg.isEmpty()) return false
            return try { ctx.packageManager.getPackageInfo(pkg, 0); true }
            catch (_: PackageManager.NameNotFoundException) { false }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_tool, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<ToolItem>() {
        override fun areItemsTheSame(a: ToolItem, b: ToolItem) = a.packageName == b.packageName && a.name == b.name
        override fun areContentsTheSame(a: ToolItem, b: ToolItem) = a == b
    }
}

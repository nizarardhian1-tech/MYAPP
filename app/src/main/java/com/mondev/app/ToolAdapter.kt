package com.mondev.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.text.HtmlCompat
import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import java.io.File

class ToolAdapter(
    private val onDownload:  ((ToolItem) -> Unit)? = null,
    private val onInstall:   ((File, ToolItem) -> Unit)? = null,
    private val onOpen:      ((File, ToolItem) -> Unit)? = null,
    private val onCardClick: ((ToolItem) -> Unit)? = null
) : ListAdapter<ToolItem, ToolAdapter.ViewHolder>(DiffCallback()) {

    // State expand disimpan di adapter level agar tidak hilang saat recycle
    private val expandedKeys = mutableSetOf<String>()
    private val progressMap  = mutableMapOf<String, Int>()

    private fun itemKey(tool: ToolItem) = "${tool.type}_${tool.packageName}_${tool.name}"

    fun setProgress(packageName: String, progress: Int) {
        progressMap[packageName] = progress
        currentList.indexOfFirst { it.packageName == packageName }
            .takeIf { it >= 0 }?.let { notifyItemChanged(it) }
    }

    fun clearProgress(packageName: String) {
        progressMap.remove(packageName)
        currentList.indexOfFirst { it.packageName == packageName }
            .takeIf { it >= 0 }?.let { notifyItemChanged(it) }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon:       ImageView      = view.findViewById(R.id.ivIcon)
        val tvName:       TextView       = view.findViewById(R.id.tvName)
        val tvDeveloper:  TextView       = view.findViewById(R.id.tvDeveloper)
        val tvShortDesc:  TextView       = view.findViewById(R.id.tvShortDesc)
        val tvDesc:       TextView       = view.findViewById(R.id.tvDesc)
        val tvMore:       TextView       = view.findViewById(R.id.tvMore)
        val tvVersion:    TextView       = view.findViewById(R.id.tvVersion)
        val tvSize:       TextView       = view.findViewById(R.id.tvSize)
        val tvCategory:   TextView       = view.findViewById(R.id.tvCategory)
        val tagContainer: LinearLayout   = view.findViewById(R.id.tagContainer)
        val btnAction:    MaterialButton = view.findViewById(R.id.btnAction)
        val progressBar:  ProgressBar    = view.findViewById(R.id.downloadProgress)

        fun bind(tool: ToolItem) {
            val key = itemKey(tool)

            // ── Info dasar ──────────────────────────────────────────────────
            tvName.text      = tool.name
            tvDeveloper.text = tool.developer.ifBlank { "Unknown" }
            tvShortDesc.text = tool.shortDesc
            tvDesc.text      = HtmlCompat.fromHtml(tool.desc, HtmlCompat.FROM_HTML_MODE_LEGACY)
            tvSize.text      = tool.size
            tvSize.visibility = if (tool.size.isNotBlank()) View.VISIBLE else View.GONE
            tvCategory.text  = tool.category

            // Versi: untuk non-apk tampilkan latest_version jika ada
            tvVersion.text = when {
                tool.isNonApk && tool.latestVersion.isNotBlank() ->
                    "v${tool.version}  →  v${tool.latestVersion}"
                else -> "v${tool.version}"
            }

            // ── Badge tipe (APK / SCRIPT / ZIP / BINARY / LINK) ────────────
            val typeBadge = when (tool.type) {
                "script" -> "\uD83D\uDCDC Script"
                "zip"    -> "\uD83D\uDCC6 ZIP"
                "binary" -> "\u2699\uFE0F Binary"
                "link"   -> "\uD83C\uDF10 Link"
                else     -> null
            }
            // Sisipkan tipe ke tvCategory jika bukan APK
            if (typeBadge != null) {
                tvCategory.text = "${tool.category}  \u2022  $typeBadge"
            }

            // ── Tags ────────────────────────────────────────────────────────
            tagContainer.removeAllViews()
            tool.tags.take(4).forEach { tag ->
                val chip = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.item_tag_chip, tagContainer, false) as TextView
                chip.text = tag
                tagContainer.addView(chip)
            }
            tagContainer.visibility = if (tool.tags.isEmpty()) View.GONE else View.VISIBLE

            // ── Icon ────────────────────────────────────────────────────────
            if (tool.iconUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(tool.iconUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(ivIcon)
            } else {
                ivIcon.setImageResource(R.drawable.ic_launcher_foreground)
            }

            // ── Expand / Collapse deskripsi (Play Store style) ──────────────
            val isExpanded = expandedKeys.contains(key)
            if (isExpanded) {
                tvDesc.maxLines = Int.MAX_VALUE
                tvMore.text = "\u25b2 Sembunyikan"
                tvMore.visibility = View.VISIBLE
            } else {
                tvDesc.maxLines = 3
                tvMore.text = "\u25bc Selengkapnya"
                tvMore.visibility = View.GONE
            }
            tvDesc.doOnLayout {
                if (tvDesc.lineCount > 3 || isExpanded)
                    tvMore.visibility = View.VISIBLE
            }
            val toggleExpand = View.OnClickListener {
                if (expandedKeys.contains(key)) {
                    expandedKeys.remove(key)
                    tvDesc.maxLines = 3
                    tvMore.text = "\u25bc Selengkapnya"
                } else {
                    expandedKeys.add(key)
                    tvDesc.maxLines = Int.MAX_VALUE
                    tvMore.text = "\u25b2 Sembunyikan"
                }
            }
            tvDesc.setOnClickListener(toggleExpand)
            tvMore.setOnClickListener(toggleExpand)

            // Card click → changelog / detail
            itemView.setOnClickListener { onCardClick?.invoke(tool) }

            // ── Progress bar download ───────────────────────────────────────
            val progress = progressMap[tool.packageName.ifBlank { tool.name }] ?: -1
            if (progress in 0..99) {
                progressBar.visibility = View.VISIBLE
                progressBar.progress   = progress
                btnAction.text         = "$progress%"
                btnAction.isEnabled    = false
                return
            }
            progressBar.visibility = View.GONE

            // ── Tombol aksi per tipe file ───────────────────────────────────
            when {
                tool.apkUrl.isBlank() -> {
                    btnAction.text      = "Segera Hadir"
                    btnAction.isEnabled = false
                    btnAction.setOnClickListener(null)
                }

                // ── APK ─────────────────────────────────────────────────────
                tool.isApk -> bindApkButton(tool)

                // ── LINK (eksternal browser) ─────────────────────────────────
                tool.isLink -> {
                    btnAction.text      = "\uD83C\uDF10 Buka"
                    btnAction.isEnabled = true
                    btnAction.setOnClickListener {
                        itemView.context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(tool.apkUrl))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }

                // ── SCRIPT / ZIP / BINARY ───────────────────────────────────
                else -> bindFileButton(tool)
            }
        }

        // ── APK: Installed / Update / Install / Download ───────────────────
        private fun bindApkButton(tool: ToolItem) {
            val ctx         = itemView.context
            val isInstalled = isPackageInstalled(ctx, tool.packageName)
            val hasUpdate   = isInstalled && (tool.forceUpdate || isNewerVersion(ctx, tool))
            val fileName    = "${tool.name.replace(" ", "_")}.apk"
            val file        = File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)

            when {
                hasUpdate -> {
                    btnAction.text      = "\u2191 Update"
                    btnAction.isEnabled = true
                    btnAction.setOnClickListener { onDownload?.invoke(tool) }
                }
                isInstalled -> {
                    btnAction.text      = "\u2713 Installed"
                    btnAction.isEnabled = false
                    btnAction.setOnClickListener(null)
                }
                file.exists() -> {
                    btnAction.text      = "Install"
                    btnAction.isEnabled = true
                    btnAction.setOnClickListener { onInstall?.invoke(file, tool) }
                }
                else -> {
                    btnAction.text      = "Download"
                    btnAction.isEnabled = true
                    btnAction.setOnClickListener { onDownload?.invoke(tool) }
                }
            }
        }

        // ── Non-APK: Download / Update / Open ──────────────────────────────
        private fun bindFileButton(tool: ToolItem) {
            val ctx      = itemView.context
            val ext      = when (tool.type) {
                "script" -> ".lua"    // bisa juga .sh — ambil dari URL jika perlu
                "zip"    -> ".zip"
                "binary" -> ".bin"
                else     -> ".file"
            }
            val safeName = tool.name.replace(" ", "_").replace("/", "_")
            val file     = File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "$safeName$ext")

            val progressKey = tool.name // non-apk pakai name sebagai key
            val inProgress  = progressMap[progressKey]

            if (inProgress != null && inProgress in 0..99) {
                btnAction.text      = "$inProgress%"
                btnAction.isEnabled = false
                return
            }

            when {
                // Ada update dari JSON (needs_update: true atau latest_version berbeda)
                file.exists() && (tool.needsUpdate ||
                        (tool.latestVersion.isNotBlank() && tool.latestVersion != tool.version)) -> {
                    btnAction.text      = "\u2191 Update"
                    btnAction.isEnabled = true
                    btnAction.setOnClickListener { onDownload?.invoke(tool) }
                }
                file.exists() -> {
                    btnAction.text      = "\uD83D\uDCC2 Buka"
                    btnAction.isEnabled = true
                    btnAction.setOnClickListener { onOpen?.invoke(file, tool) }
                }
                else -> {
                    btnAction.text      = "\u2193 Download"
                    btnAction.isEnabled = true
                    btnAction.setOnClickListener { onDownload?.invoke(tool) }
                }
            }
        }

        private fun isPackageInstalled(ctx: Context, pkg: String): Boolean {
            if (pkg.isEmpty()) return false
            return try { ctx.packageManager.getPackageInfo(pkg, 0); true }
            catch (_: PackageManager.NameNotFoundException) { false }
        }

        private fun isNewerVersion(ctx: Context, tool: ToolItem): Boolean {
            return try {
                val info = ctx.packageManager.getPackageInfo(tool.packageName, 0)
                info.versionName != tool.version
            } catch (_: PackageManager.NameNotFoundException) { false }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_tool, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<ToolItem>() {
        override fun areItemsTheSame(a: ToolItem, b: ToolItem) =
            a.type == b.type && a.packageName == b.packageName && a.name == b.name
        override fun areContentsTheSame(a: ToolItem, b: ToolItem) = a == b
    }
}

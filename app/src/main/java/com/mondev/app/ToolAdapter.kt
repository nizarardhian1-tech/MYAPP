package com.mondev.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ToolAdapter(private val onInstallClick: ((ToolItem) -> Unit)?) :
    ListAdapter<ToolItem, ToolAdapter.ViewHolder>(DiffCallback()) {

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
            tvDesc.text = tool.desc
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

            if (onInstallClick != null) {
                btnAction.visibility = View.VISIBLE
                btnAction.text = "Install"
                btnAction.setOnClickListener { onInstallClick?.invoke(tool) }
            } else {
                btnAction.visibility = View.GONE
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
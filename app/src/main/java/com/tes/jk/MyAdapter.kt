package com.tes.jk

import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * MyAdapter — Adapter RecyclerView.
 * TODO: Ganti String dengan model data Anda
 */
class MyAdapter(
    private val dataList: List<String>,
    private val onItemClick: (String, Int) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<MyAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView    = view.findViewById(R.id.tvTitle)
        val tvSubtitle: TextView = view.findViewById(R.id.tvSubtitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataList[position]
        holder.tvTitle.text    = item             // TODO: sesuaikan
        holder.tvSubtitle.text = "Item #${position + 1}" // TODO: sesuaikan
        holder.itemView.setOnClickListener { onItemClick(item, position) }
    }

    override fun getItemCount() = dataList.size
}

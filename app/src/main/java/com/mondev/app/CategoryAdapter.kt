package com.mondev.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CategoryAdapter(
    private val onSelect: (String) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.VH>() {

    private var items: List<String> = emptyList()
    private var selected: String = "All"

    fun submitList(list: List<String>, active: String) {
        items    = list
        selected = active
        notifyDataSetChanged()
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val chip: TextView = view.findViewById(R.id.tvCategory)
        fun bind(cat: String) {
            chip.text = cat
            chip.isSelected = (cat == selected)
            chip.setOnClickListener { onSelect(cat) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_category_chip, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size
}

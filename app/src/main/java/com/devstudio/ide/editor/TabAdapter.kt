package com.devstudio.ide.editor

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.devstudio.ide.R

class TabAdapter(
    private val tabs: List<EditorTab>,
    private val activeIndex: Int,
    private val onTabClick: (Int) -> Unit,
    private val onTabClose: (Int) -> Unit
) : RecyclerView.Adapter<TabAdapter.TabViewHolder>() {

    class TabViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTabName: TextView  = view.findViewById(R.id.tvTabName)
        val btnClose: ImageView  = view.findViewById(R.id.btnCloseTab)
        val dot: View            = view.findViewById(R.id.viewUnsavedDot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tab, parent, false)
        return TabViewHolder(view)
    }

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        val tab = tabs[position]
        holder.tvTabName.text = tab.file.name
        // Unsaved dot indicator
        holder.dot.visibility = if (tab.isModified) View.VISIBLE else View.GONE
        // Active tab highlight
        holder.itemView.setBackgroundColor(
            if (position == activeIndex) Color.parseColor("#E3F2FD")
            else Color.parseColor("#FFFFFF")
        )
        holder.itemView.setOnClickListener { onTabClick(position) }
        holder.btnClose.setOnClickListener { onTabClose(position) }
    }

    override fun getItemCount(): Int = tabs.size
}

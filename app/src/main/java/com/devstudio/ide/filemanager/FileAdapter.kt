package com.devstudio.ide.filemanager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.devstudio.ide.R

class FileAdapter(
    private val items: List<FileItem>,
    private val onClick: (FileItem) -> Unit,
    private val onLongClick: (FileItem, View) -> Unit
) : RecyclerView.Adapter<FileAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.ivFileIcon)
        val name: TextView = view.findViewById(R.id.tvFileName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.name.text = item.name

        // FIX: Using custom vector drawables instead of deprecated system icons
        val iconRes = when {
            item.name == ".." -> R.drawable.ic_folder
            item.isDirectory -> R.drawable.ic_folder
            item.name.endsWith(".kt") || item.name.endsWith(".java") -> R.drawable.ic_code
            item.name.endsWith(".xml") || item.name.endsWith(".html") -> R.drawable.ic_file_xml
            else -> R.drawable.ic_file
        }
        holder.icon.setImageResource(iconRes)

        holder.itemView.setOnClickListener { onClick(item) }
        // FIX: Long press restored
        holder.itemView.setOnLongClickListener {
            onLongClick(item, it)
            true
        }
    }

    override fun getItemCount(): Int = items.size
}

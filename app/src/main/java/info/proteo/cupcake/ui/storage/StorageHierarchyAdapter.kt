package info.proteo.cupcake.ui.storage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.R

class StorageHierarchyAdapter(
    private val hierarchyItems: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<StorageHierarchyAdapter.HierarchyViewHolder>() {

    class HierarchyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewHierarchyItem: TextView = itemView.findViewById(R.id.textViewHierarchyItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HierarchyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_storage_hierarchy, parent, false)
        return HierarchyViewHolder(view)
    }

    override fun onBindViewHolder(holder: HierarchyViewHolder, position: Int) {
        val item = hierarchyItems[position]
        holder.textViewHierarchyItem.text = item
        
        // Add indentation based on hierarchy level
        val indentationLevel = position
        val padding = 16 + (indentationLevel * 24)
        holder.textViewHierarchyItem.setPadding(padding, 16, 16, 16)
        
        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = hierarchyItems.size
}
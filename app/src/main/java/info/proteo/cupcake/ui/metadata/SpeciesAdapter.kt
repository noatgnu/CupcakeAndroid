package info.proteo.cupcake.ui.metadata

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.R
import info.proteo.cupcake.data.local.entity.metadatacolumn.SpeciesEntity
import androidx.recyclerview.widget.DiffUtil

class SpeciesAdapter : ListAdapter<SpeciesEntity, SpeciesAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_metadata, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.titleText)
        private val subtitle: TextView = view.findViewById(R.id.subtitleText)
        private val description: TextView = view.findViewById(R.id.descriptionText)

        fun bind(item: SpeciesEntity) {
            title.text = item.code ?: item.taxon ?: "Unknown"
            subtitle.text = item.officialName ?: item.commonName ?: ""
            description.text = item.synonym ?: ""
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<SpeciesEntity>() {
        override fun areItemsTheSame(oldItem: SpeciesEntity, newItem: SpeciesEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SpeciesEntity, newItem: SpeciesEntity): Boolean {
            return oldItem == newItem
        }
    }
}
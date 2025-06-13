package info.proteo.cupcake.ui.metadata

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.R
import info.proteo.cupcake.data.local.entity.metadatacolumn.HumanDiseaseEntity

class HumanDiseaseAdapter : ListAdapter<HumanDiseaseEntity, HumanDiseaseAdapter.ViewHolder>(DiffCallback()) {

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

        fun bind(item: HumanDiseaseEntity) {
            val titleText = item.accession.let { "$it ${item.identifier}" }
            title.text = titleText
            subtitle.text = item.acronym ?: ""
            description.text = item.definition ?: item.synonyms ?: ""
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<HumanDiseaseEntity>() {
        override fun areItemsTheSame(oldItem: HumanDiseaseEntity, newItem: HumanDiseaseEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HumanDiseaseEntity, newItem: HumanDiseaseEntity): Boolean {
            return oldItem == newItem
        }
    }
}
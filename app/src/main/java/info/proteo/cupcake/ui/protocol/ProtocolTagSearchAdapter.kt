package info.proteo.cupcake.ui.protocol

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.R
import info.proteo.cupcake.shared.data.model.protocol.ProtocolTag

class ProtocolTagSearchAdapter(
    private var tags: List<ProtocolTag>,
    private val onTagSelected: (ProtocolTag) -> Unit
) : RecyclerView.Adapter<ProtocolTagSearchAdapter.ViewHolder>() {

    fun updateTags(newTags: List<ProtocolTag>) {
        tags = newTags
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tag, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(tags[position])
    }

    override fun getItemCount() = tags.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tagText: TextView = itemView.findViewById(R.id.tagText)

        fun bind(tag: ProtocolTag) {
            tagText.text = tag.tag.tag
            itemView.setOnClickListener {
                onTagSelected(tag)
            }
        }
    }
}
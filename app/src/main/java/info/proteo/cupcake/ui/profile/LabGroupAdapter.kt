package info.proteo.cupcake.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.databinding.ItemLabGroupBinding
import info.proteo.cupcake.shared.data.model.user.LabGroup
import java.text.SimpleDateFormat
import java.util.*

class LabGroupAdapter(
    private val onLabGroupClick: (LabGroup) -> Unit = {}
) : ListAdapter<LabGroup, LabGroupAdapter.LabGroupViewHolder>(LabGroupDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabGroupViewHolder {
        val binding = ItemLabGroupBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LabGroupViewHolder(binding, onLabGroupClick)
    }

    override fun onBindViewHolder(holder: LabGroupViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class LabGroupViewHolder(
        private val binding: ItemLabGroupBinding,
        private val onLabGroupClick: (LabGroup) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(labGroup: LabGroup) {
            binding.apply {
                tvLabGroupName.text = labGroup.name

                // Handle description
                if (labGroup.description.isNullOrBlank()) {
                    tvLabGroupDescription.visibility = View.GONE
                } else {
                    tvLabGroupDescription.visibility = View.VISIBLE
                    tvLabGroupDescription.text = labGroup.description
                }

                // Handle professional badge
                if (labGroup.isProfessional) {
                    chipProfessional.visibility = View.VISIBLE
                } else {
                    chipProfessional.visibility = View.GONE
                }

                // Handle dates
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val datesText = buildString {
                    labGroup.createdAt?.let { createdAt ->
                        append("Created: ")
                        try {
                            // Parse ISO date string and format it
                            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
                            val date = isoFormat.parse(createdAt)
                            append(dateFormat.format(date ?: Date()))
                        } catch (e: Exception) {
                            // Fallback: try simpler format
                            try {
                                val simpleFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val date = simpleFormat.parse(createdAt)
                                append(dateFormat.format(date ?: Date()))
                            } catch (e2: Exception) {
                                append(createdAt)
                            }
                        }
                    }

                    labGroup.updatedAt?.let { updatedAt ->
                        if (isNotEmpty()) append(" • ")
                        append("Updated: ")
                        try {
                            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
                            val date = isoFormat.parse(updatedAt)
                            append(dateFormat.format(date ?: Date()))
                        } catch (e: Exception) {
                            try {
                                val simpleFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val date = simpleFormat.parse(updatedAt)
                                append(dateFormat.format(date ?: Date()))
                            } catch (e2: Exception) {
                                append(updatedAt)
                            }
                        }
                    }
                }

                if (datesText.isNotBlank()) {
                    tvDates.text = datesText
                    tvDates.visibility = View.VISIBLE
                } else {
                    tvDates.visibility = View.GONE
                }

                // Set click listener
                root.setOnClickListener {
                    onLabGroupClick(labGroup)
                }
            }
        }
    }

    class LabGroupDiffCallback : DiffUtil.ItemCallback<LabGroup>() {
        override fun areItemsTheSame(oldItem: LabGroup, newItem: LabGroup): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: LabGroup, newItem: LabGroup): Boolean {
            return oldItem == newItem
        }
    }
}
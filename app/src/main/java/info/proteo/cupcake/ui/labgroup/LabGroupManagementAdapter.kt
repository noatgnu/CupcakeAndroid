package info.proteo.cupcake.ui.labgroup

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.R
import info.proteo.cupcake.databinding.ItemLabGroupManagementBinding
import info.proteo.cupcake.shared.data.model.user.LabGroup
import java.text.SimpleDateFormat
import java.util.*

class LabGroupManagementAdapter(
    private val onLabGroupClick: (LabGroup) -> Unit = {}
) : ListAdapter<LabGroup, LabGroupManagementAdapter.LabGroupViewHolder>(LabGroupDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabGroupViewHolder {
        val binding = ItemLabGroupManagementBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LabGroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LabGroupViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class LabGroupViewHolder(
        private val binding: ItemLabGroupManagementBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(labGroup: LabGroup) {
            binding.apply {
                // Basic info
                tvLabGroupName.text = labGroup.name

                // Description
                if (labGroup.description.isNullOrBlank()) {
                    tvLabGroupDescription?.visibility = View.GONE
                } else {
                    tvLabGroupDescription?.visibility = View.VISIBLE
                    tvLabGroupDescription?.text = labGroup.description
                }

                // Professional badge
                chipProfessional.visibility = if (labGroup.isProfessional) View.VISIBLE else View.GONE

                // Storage info for professional groups
                if (labGroup.isProfessional && labGroup.serviceStorage != null) {
                    iconStorage?.visibility = View.VISIBLE
                    tvStorageInfo?.visibility = View.VISIBLE
                    tvStorageInfo?.text = "Service Storage: ${labGroup.serviceStorage!!.objectName}"
                } else if (labGroup.isProfessional) {
                    iconStorage?.visibility = View.VISIBLE
                    tvStorageInfo?.visibility = View.VISIBLE
                    tvStorageInfo?.text = "Service Storage: Not Set"
                    tvStorageInfo?.setTextColor(itemView.context.getColor(R.color.danger))
                } else {
                    iconStorage?.visibility = View.GONE
                    tvStorageInfo?.visibility = View.GONE
                }

                // Members count - hide for now since we don't have the data
                tvMembersCount?.text = "Click to view members"

                // Dates
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val datesText = buildString {
                    labGroup.createdAt?.let { createdAt ->
                        append("Created: ")
                        try {
                            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
                            val date = isoFormat.parse(createdAt)
                            append(dateFormat.format(date ?: Date()))
                        } catch (e: Exception) {
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

                tvDates?.text = datesText

                // Click listeners
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
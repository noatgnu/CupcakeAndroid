package info.proteo.cupcake.ui.maintenance

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.R
import info.proteo.cupcake.databinding.ItemMaintenanceLogBinding
import info.proteo.cupcake.shared.data.model.maintenance.MaintenanceConstants
import info.proteo.cupcake.shared.data.model.maintenance.MaintenanceLog
import java.text.SimpleDateFormat
import java.util.Locale

class MaintenanceLogAdapter(
    private val onItemClick: (MaintenanceLog) -> Unit,
    private val onStatusChange: (MaintenanceLog, String) -> Unit
) : ListAdapter<MaintenanceLog, MaintenanceLogAdapter.MaintenanceLogViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MaintenanceLogViewHolder {
        val binding = ItemMaintenanceLogBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MaintenanceLogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MaintenanceLogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MaintenanceLogViewHolder(
        private val binding: ItemMaintenanceLogBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(maintenanceLog: MaintenanceLog) {
            binding.apply {
                // Format and display date
                textMaintenanceDate.text = formatDate(maintenanceLog.maintenanceDate)
                
                // Display maintenance type with proper formatting
                textMaintenanceType.text = formatMaintenanceType(maintenanceLog.maintenanceType)
                
                // Display description
                textDescription.text = maintenanceLog.maintenanceDescription ?: "No description"
                
                // Display status with color coding
                textStatus.text = formatStatus(maintenanceLog.status)
                textStatus.setTextColor(getStatusColor(maintenanceLog.status))
                
                // Display created by information
                textCreatedBy.text = maintenanceLog.createdByUser?.username ?: "Unknown"
                
                // Show template indicator
                chipTemplate.visibility = if (maintenanceLog.isTemplate) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }
                
                // Display notes if available
                if (!maintenanceLog.maintenanceNotes.isNullOrBlank()) {
                    textNotes.text = maintenanceLog.maintenanceNotes
                    textNotes.visibility = android.view.View.VISIBLE
                } else {
                    textNotes.visibility = android.view.View.GONE
                }

                // Set click listeners
                root.setOnClickListener {
                    onItemClick(maintenanceLog)
                }

                // Status change button (for non-completed logs)
                if (maintenanceLog.status != MaintenanceConstants.Statuses.COMPLETED &&
                    maintenanceLog.status != MaintenanceConstants.Statuses.CANCELLED) {
                    
                    buttonChangeStatus.visibility = android.view.View.VISIBLE
                    buttonChangeStatus.text = when (maintenanceLog.status) {
                        MaintenanceConstants.Statuses.PENDING -> "Start"
                        MaintenanceConstants.Statuses.IN_PROGRESS -> "Complete"
                        MaintenanceConstants.Statuses.REQUESTED -> "Approve"
                        else -> "Update"
                    }
                    
                    buttonChangeStatus.setOnClickListener {
                        val newStatus = when (maintenanceLog.status) {
                            MaintenanceConstants.Statuses.PENDING -> MaintenanceConstants.Statuses.IN_PROGRESS
                            MaintenanceConstants.Statuses.IN_PROGRESS -> MaintenanceConstants.Statuses.COMPLETED
                            MaintenanceConstants.Statuses.REQUESTED -> MaintenanceConstants.Statuses.IN_PROGRESS
                            else -> MaintenanceConstants.Statuses.COMPLETED
                        }
                        onStatusChange(maintenanceLog, newStatus)
                    }
                } else {
                    buttonChangeStatus.visibility = android.view.View.GONE
                }
            }
        }

        private fun formatDate(dateString: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val date = inputFormat.parse(dateString)
                date?.let { outputFormat.format(it) } ?: dateString
            } catch (e: Exception) {
                dateString
            }
        }

        private fun formatMaintenanceType(type: String): String {
            return when (type) {
                MaintenanceConstants.Types.ROUTINE -> "Routine"
                MaintenanceConstants.Types.EMERGENCY -> "Emergency"
                MaintenanceConstants.Types.OTHER -> "Other"
                else -> type.replaceFirstChar { it.uppercase() }
            }
        }

        private fun formatStatus(status: String): String {
            return when (status) {
                MaintenanceConstants.Statuses.PENDING -> "Pending"
                MaintenanceConstants.Statuses.IN_PROGRESS -> "In Progress"
                MaintenanceConstants.Statuses.COMPLETED -> "Completed"
                MaintenanceConstants.Statuses.REQUESTED -> "Requested"
                MaintenanceConstants.Statuses.CANCELLED -> "Cancelled"
                else -> status.replace("_", " ").replaceFirstChar { it.uppercase() }
            }
        }

        private fun getStatusColor(status: String): Int {
            val context = binding.root.context
            return when (status) {
                MaintenanceConstants.Statuses.PENDING -> ContextCompat.getColor(context, R.color.status_pending)
                MaintenanceConstants.Statuses.IN_PROGRESS -> ContextCompat.getColor(context, R.color.status_in_progress)
                MaintenanceConstants.Statuses.COMPLETED -> ContextCompat.getColor(context, R.color.status_completed)
                MaintenanceConstants.Statuses.REQUESTED -> ContextCompat.getColor(context, R.color.status_requested)
                MaintenanceConstants.Statuses.CANCELLED -> ContextCompat.getColor(context, R.color.status_cancelled)
                else -> ContextCompat.getColor(context, android.R.color.black)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<MaintenanceLog>() {
        override fun areItemsTheSame(oldItem: MaintenanceLog, newItem: MaintenanceLog): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MaintenanceLog, newItem: MaintenanceLog): Boolean {
            return oldItem == newItem
        }
    }
}
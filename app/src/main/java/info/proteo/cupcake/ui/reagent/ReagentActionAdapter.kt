package info.proteo.cupcake.ui.reagent

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.R
import info.proteo.cupcake.data.remote.model.reagent.ReagentAction
import info.proteo.cupcake.databinding.ItemReagentActionBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ReagentActionAdapter : ListAdapter<ReagentAction, ReagentActionAdapter.ViewHolder>(ReagentActionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReagentActionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemReagentActionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(action: ReagentAction) {
            binding.textViewActionType.text = formatActionType(action.actionType)

            binding.textViewQuantity.setTextColor(
                if (action.actionType == "reserve")
                    ContextCompat.getColor(binding.root.context, R.color.negative_action)
                else
                    ContextCompat.getColor(binding.root.context, R.color.positive_action)
            )

            binding.textViewQuantity.text = buildString {
                if (action.actionType == "reserve") append("-") else append("+")
                append(action.quantity)
            }

            binding.textViewNotes.visibility = if (action.notes.isNullOrEmpty()) View.GONE else View.VISIBLE
            binding.textViewNotes.text = action.notes

            binding.textViewUser.text = "By: ${action.user}"
            binding.textViewTimestamp.text = formatDate(action.createdAt)
        }

        private fun formatActionType(actionType: String?): String {
            return when(actionType) {
                "reserve" -> "Reserve"
                "add" -> "Add"
                else -> actionType?.capitalize(Locale.getDefault()) ?: "Unknown"
            }
        }

        private fun formatDate(timestamp: String?): String {
            if (timestamp.isNullOrEmpty()) return ""

            try {
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                format.timeZone = TimeZone.getTimeZone("UTC")
                val date = format.parse(timestamp) ?: return timestamp
                val diffMs = System.currentTimeMillis() - date.time

                return when {
                    diffMs < 60 * 1000 -> "Just now"
                    diffMs < 60 * 60 * 1000 -> "${diffMs / (60 * 1000)} min ago"
                    diffMs < 24 * 60 * 60 * 1000 -> "${diffMs / (60 * 60 * 1000)} hours ago"
                    diffMs < 7 * 24 * 60 * 60 * 1000 -> "${diffMs / (24 * 60 * 60 * 1000)} days ago"
                    else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
                }
            } catch (e: Exception) {
                return timestamp
            }
        }
    }

    private class ReagentActionDiffCallback : DiffUtil.ItemCallback<ReagentAction>() {
        override fun areItemsTheSame(oldItem: ReagentAction, newItem: ReagentAction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ReagentAction, newItem: ReagentAction): Boolean {
            return oldItem == newItem
        }
    }
}
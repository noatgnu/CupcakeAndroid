package info.proteo.cupcake.ui.message

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.R
import info.proteo.cupcake.data.remote.model.message.Message
import info.proteo.cupcake.data.remote.model.message.ThreadMessage
import info.proteo.cupcake.databinding.ItemMessageBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class MessageAdapter : ListAdapter<ThreadMessage, MessageAdapter.MessageViewHolder>(MessageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MessageViewHolder(private val binding: ItemMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ThreadMessage) {
            binding.textViewSender.text = message.sender.username
            binding.textViewTimestamp.text = formatDate(message.createdAt)

            binding.textViewContent.text = HtmlCompat.fromHtml(
                message.content.toString(),
                HtmlCompat.FROM_HTML_MODE_COMPACT
            )
            if (!message.messageType.isNullOrEmpty() && message.messageType != "user_message") {
                binding.messageTypeIndicator.visibility = View.VISIBLE
                binding.messageTypeIndicator.text = message.messageType

                // Apply styling based on type
                when (message.messageType) {
                    "system_notification" -> binding.messageTypeIndicator.setBackgroundResource(R.drawable.bg_system_notification)
                    "alert" -> binding.messageTypeIndicator.setBackgroundResource(R.drawable.bg_alert)
                    "announcement" -> binding.messageTypeIndicator.setBackgroundResource(R.drawable.bg_announcement)
                }
            } else {
                binding.messageTypeIndicator.visibility = View.GONE
            }

            if (!message.priority.isNullOrEmpty() && message.priority != "normal") {
                binding.priorityIndicator.visibility = View.VISIBLE

                when (message.priority) {
                    "high" -> {
                        binding.priorityIndicator.setBackgroundResource(R.drawable.bg_priority_high)
                        binding.priorityIndicator.text = "High Priority"
                    }
                    "urgent" -> {
                        binding.priorityIndicator.setBackgroundResource(R.drawable.bg_priority_urgent)
                        binding.priorityIndicator.text = "Urgent"
                    }
                }
            } else {
                binding.priorityIndicator.visibility = View.GONE
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
                    else -> {
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                        dateFormat.timeZone = TimeZone.getDefault() // Use device timezone for display
                        dateFormat.format(date)
                    }
                }
            } catch (e: Exception) {
                return timestamp
            }
        }


    }

    class MessageDiffCallback : DiffUtil.ItemCallback<ThreadMessage>() {
        override fun areItemsTheSame(oldItem: ThreadMessage, newItem: ThreadMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ThreadMessage, newItem: ThreadMessage): Boolean {
            return oldItem == newItem
        }
    }
}
package info.proteo.cupcake.ui.message

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.data.remote.model.message.Message
import info.proteo.cupcake.data.remote.model.message.ThreadMessage
import info.proteo.cupcake.databinding.ItemMessageBinding

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
        }

        private fun formatDate(timestamp: String?): String {
            return timestamp ?: ""
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
package info.proteo.cupcake.ui.message

import android.os.Build
import android.text.Html
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.data.remote.model.message.MessageThread
import info.proteo.cupcake.databinding.ItemMessageThreadBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageThreadAdapter(
    private val onThreadClick: (MessageThread) -> Unit
) : ListAdapter<MessageThread, MessageThreadAdapter.ThreadViewHolder>(ThreadDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThreadViewHolder {
        val binding = ItemMessageThreadBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ThreadViewHolder(binding, onThreadClick)
    }

    override fun onBindViewHolder(holder: ThreadViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ThreadViewHolder(
        private val binding: ItemMessageThreadBinding,
        private val onThreadClick: (MessageThread) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(thread: MessageThread) {
            binding.textViewThreadTitle.text = thread.title

            if (thread.latestMessage != null) {
                binding.textViewLastMessage.text = fromHtml(thread.latestMessage.content.toString())
                binding.textViewLastMessage.visibility = View.VISIBLE

                thread.latestMessage.createdAt?.let { timestamp ->
                    binding.textViewTimestamp.text = formatTimestamp(timestamp)
                    binding.textViewTimestamp.visibility = View.VISIBLE
                } ?: run {
                    binding.textViewTimestamp.visibility = View.GONE
                }
            } else {
                binding.textViewLastMessage.visibility = View.GONE
                binding.textViewTimestamp.visibility = View.GONE
            }

            if (thread.unreadCount > 0) {
                binding.textViewUnreadCount.text = thread.unreadCount.toString()
                binding.textViewUnreadCount.visibility = View.VISIBLE
            } else {
                binding.textViewUnreadCount.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                onThreadClick(thread)
            }
        }

        private fun fromHtml(html: String): Spanned {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(html)
            }
        }

        private fun formatTimestamp(timestamp: String): String {
            try {
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                val date = format.parse(timestamp) ?: return timestamp

                // Get difference in milliseconds
                val diffMs = Date().time - date.time

                return when {
                    diffMs < 60 * 1000 -> "Just now"
                    diffMs < 60 * 60 * 1000 -> "${diffMs / (60 * 1000)} min ago"
                    diffMs < 24 * 60 * 60 * 1000 -> "${diffMs / (60 * 60 * 1000)} hours ago"
                    diffMs < 7 * 24 * 60 * 60 * 1000 -> "${diffMs / (24 * 60 * 60 * 1000)} days ago"
                    else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
                }
            } catch (e: Exception) {
                return timestamp
            }
        }
    }

    class ThreadDiffCallback : DiffUtil.ItemCallback<MessageThread>() {
        override fun areItemsTheSame(oldItem: MessageThread, newItem: MessageThread): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MessageThread, newItem: MessageThread): Boolean {
            return oldItem == newItem
        }
    }
}
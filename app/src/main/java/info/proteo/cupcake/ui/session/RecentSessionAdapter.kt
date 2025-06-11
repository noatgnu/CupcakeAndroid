package info.proteo.cupcake.ui.session

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.R
import info.proteo.cupcake.data.local.entity.protocol.RecentSessionEntity
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class RecentSessionAdapter(
    private val onItemClick: (RecentSessionEntity) -> Unit
) : ListAdapter<RecentSessionEntity, RecentSessionAdapter.ViewHolder>(RecentSessionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_session, parent, false)
        return ViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        itemView: View,
        private val onItemClick: (RecentSessionEntity) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val sessionIdTextView: TextView = itemView.findViewById(R.id.textViewSessionId)
        private val lastAccessedTextView: TextView = itemView.findViewById(R.id.textViewLastAccessed)

        fun bind(recentSession: RecentSessionEntity) {
            if (recentSession.sessionName != null) {
                sessionIdTextView.text = recentSession.sessionName
            } else {
                sessionIdTextView.text = "Session #${recentSession.sessionUniqueId}"
            }

            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US)

            try {
                val date = inputFormat.parse(recentSession.lastAccessed)
                lastAccessedTextView.text = date?.let { outputFormat.format(it) } ?: recentSession.lastAccessed
            } catch (e: Exception) {
                lastAccessedTextView.text = recentSession.lastAccessed
            }

            itemView.setOnClickListener {
                onItemClick(recentSession)
            }
        }
    }

    private class RecentSessionDiffCallback : DiffUtil.ItemCallback<RecentSessionEntity>() {
        override fun areItemsTheSame(oldItem: RecentSessionEntity, newItem: RecentSessionEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RecentSessionEntity, newItem: RecentSessionEntity): Boolean {
            return oldItem == newItem
        }
    }
}
package info.proteo.cupcake.ui.session

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.R
import info.proteo.cupcake.data.remote.model.protocol.Session
import info.proteo.cupcake.databinding.ItemSessionBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class SessionAdapter(
    private val onSessionClicked: (Session) -> Unit
) : RecyclerView.Adapter<SessionAdapter.SessionViewHolder>() {

    private var sessions: List<Session> = emptyList()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    init {
        dateFormat.timeZone = TimeZone.getDefault()
    }

    fun updateSessions(newSessions: List<Session>) {
        Log.d("SessionAdapter", "Updating sessions: ${newSessions.size}")
        sessions = newSessions
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val binding = ItemSessionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SessionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(sessions[position])
    }

    override fun getItemCount(): Int = sessions.size

    inner class SessionViewHolder(
        private val binding: ItemSessionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onSessionClicked(sessions[position])
                }
            }
        }

        fun bind(session: Session) {
            // Use name if available, otherwise use uniqueId
            binding.sessionName.text = session.name.takeIf { !it.isNullOrBlank() }
                ?: session.uniqueId

            // Set status icon
            binding.sessionStatus.visibility = View.VISIBLE
            binding.sessionStatus.setImageResource(
                if (session.enabled) R.drawable.ic_check_circle else R.drawable.ic_disabled
            )

            // Format created at date
            binding.sessionCreatedAt.text = session.createdAt?.let { createdAt ->
                try {
                    val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
                        .apply { timeZone = TimeZone.getTimeZone("UTC") }
                        .parse(createdAt)
                    "Created: ${dateFormat.format(date)}"
                } catch (e: Exception) {
                    Log.e("SessionAdapter", "Error parsing date: $createdAt", e)
                    "Created: Unknown"
                }
            } ?: "Created: Unknown"

            // Format start and end dates if available
            val startDate = session.startedAt
            val endDate = session.endedAt

            if (!startDate.isNullOrBlank() || !endDate.isNullOrBlank()) {
                val startFormatted = if (!startDate.isNullOrBlank()) {
                    try {
                        val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                            .apply { timeZone = TimeZone.getTimeZone("UTC") }
                            .parse(startDate)
                        dateFormat.format(date)
                    } catch (e: Exception) {
                        "Unknown"
                    }
                } else "N/A"

                val endFormatted = if (!endDate.isNullOrBlank()) {
                    try {
                        val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                            .apply { timeZone = TimeZone.getTimeZone("UTC") }
                            .parse(endDate)
                        dateFormat.format(date)
                    } catch (e: Exception) {
                        "Unknown"
                    }
                } else "N/A"

                binding.sessionDates.text = "Period: $startFormatted - $endFormatted"
                binding.sessionDates.visibility = View.VISIBLE
            } else {
                binding.sessionDates.visibility = View.GONE
            }
        }
    }
}
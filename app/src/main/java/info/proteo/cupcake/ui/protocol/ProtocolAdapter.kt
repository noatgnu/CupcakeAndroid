package info.proteo.cupcake.ui.protocol

import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.shared.data.model.protocol.ProtocolModel
import info.proteo.cupcake.shared.data.model.protocol.Session
import info.proteo.cupcake.data.remote.service.SessionMinimal
import info.proteo.cupcake.databinding.ItemProtocolBinding
import info.proteo.cupcake.databinding.ItemSessionBinding
import info.proteo.cupcake.ui.protocol.ProtocolListViewModel.ProtocolWithSessions
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ProtocolAdapter(
    private val onProtocolClick: (ProtocolModel) -> Unit,
    private val onSessionClick: (Session) -> Unit
) : ListAdapter<ProtocolWithSessions, ProtocolAdapter.ProtocolViewHolder>(ProtocolDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProtocolViewHolder {
        val binding = ItemProtocolBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProtocolViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProtocolViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProtocolViewHolder(
        private val binding: ItemProtocolBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        fun bind(item: ProtocolWithSessions) {
            val protocol = item.protocol

            val sectionsCount = protocol.sections?.size ?: 0
            val stepsCount = protocol.steps?.size ?: 0

            binding.apply {
                protocolStats.text = "Sections: $sectionsCount | Steps: $stepsCount"
                protocolStats.visibility = View.VISIBLE
                protocolTitle.text = protocol.protocolTitle
                protocolDescription.text = getPlainTextFromHtml(protocol.protocolDescription, 300)

                protocol.protocolCreatedOn?.let {
                    try {
                        val parsedDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault()).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }.parse(it)

                        creationDate.text = "Created: ${dateFormat.format(parsedDate)}"
                    } catch (e: Exception) {
                        Log.e("ProtocolAdapter", "Error parsing date: $it", e)
                        creationDate.text = "Created: Unknown date"
                    }
                    creationDate.visibility = View.VISIBLE
                } ?: run {
                    creationDate.visibility = View.GONE
                }

                val sessions = item.sessions
                if (sessions.isEmpty()) {
                    sessionSection.visibility = View.GONE
                } else {
                    sessionSection.visibility = View.VISIBLE
                    sessionsLabel.text = "Associated Sessions (${sessions.size})"

                    sessionContainer.removeAllViews()
                    sessions.forEach { session ->
                        val sessionBinding = ItemSessionBinding.inflate(
                            LayoutInflater.from(root.context),
                            sessionContainer,
                            false
                        )

                        sessionBinding.sessionName.text = session.name.takeIf { !it.isNullOrBlank() }
                            ?: session.uniqueId

                        sessionBinding.sessionCreatedAt.text = session.createdAt?.let { createdAt ->
                            try {
                                val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
                                    .apply { timeZone = TimeZone.getTimeZone("UTC") }
                                    .parse(createdAt)
                                "Created: ${dateFormat.format(date)}"
                            } catch (e: Exception) {
                                Log.e("ProtocolAdapter", "Error parsing date: $createdAt", e)
                                "Created: Unknown"
                            }
                        } ?: "Created: Unknown"

                        sessionBinding.sessionDates.visibility = View.GONE

                        sessionBinding.root.setOnClickListener {
                            onSessionClick(session)
                        }

                        sessionContainer.addView(sessionBinding.root)
                    }
                }

                root.setOnClickListener {
                    onProtocolClick(protocol)
                }

                expandSessions.setOnClickListener {
                    val isVisible = sessionContainer.visibility == View.VISIBLE
                    sessionContainer.visibility = if (isVisible) View.GONE else View.VISIBLE
                    expandSessions.rotation = if (isVisible) 0f else 180f
                }
            }
        }
    }

    fun getPlainTextFromHtml(htmlString: String?, maxLength: Int): String {
        if (htmlString.isNullOrEmpty()) return ""

        val plainText = Html.fromHtml(htmlString, Html.FROM_HTML_MODE_COMPACT).toString()
        val trimmed = plainText.trim()
        return if (trimmed.length <= maxLength) {
            trimmed
        } else {
            "${trimmed.substring(0, maxLength)}..."
        }
    }

    private class ProtocolDiffCallback : DiffUtil.ItemCallback<ProtocolWithSessions>() {
        override fun areItemsTheSame(
            oldItem: ProtocolWithSessions,
            newItem: ProtocolWithSessions
        ): Boolean {
            return oldItem.protocol.id == newItem.protocol.id
        }

        override fun areContentsTheSame(
            oldItem: ProtocolWithSessions,
            newItem: ProtocolWithSessions
        ): Boolean {
            return oldItem.protocol == newItem.protocol &&
                    oldItem.sessions.size == newItem.sessions.size &&
                    oldItem.sessions.zip(newItem.sessions).all { (a, b) -> a == b }
        }
    }
}
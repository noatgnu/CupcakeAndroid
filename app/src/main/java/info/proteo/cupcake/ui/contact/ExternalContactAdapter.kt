package info.proteo.cupcake.ui.contact

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.R
import info.proteo.cupcake.shared.data.model.instrument.ExternalContact
import androidx.recyclerview.widget.DiffUtil
import com.google.android.material.snackbar.Snackbar

class ExternalContactAdapter(
    private val onRemoveClick: (contactId: Int) -> Unit,
    private val onEditClick: (contact: ExternalContact) -> Unit
) : ListAdapter<ExternalContact, ExternalContactAdapter.ViewHolder>(DiffCallback()) {

    private val expandedItems = HashSet<Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_external_contact, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = getItem(position)
        val isExpanded = expandedItems.contains(contact.id)
        holder.bind(contact, isExpanded, onRemoveClick, onEditClick) {
            toggleExpansion(contact.id, holder)
        }
    }

    private fun toggleExpansion(contactId: Int?, holder: ViewHolder) {
        contactId?.let {
            val isCurrentlyExpanded = expandedItems.contains(it)
            if (isCurrentlyExpanded) {
                expandedItems.remove(it)
            } else {
                expandedItems.add(it)
            }
            notifyItemChanged(holder.bindingAdapterPosition)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val contactNameTextView: TextView = itemView.findViewById(R.id.textViewContactName)
        private val editButton: ImageButton = itemView.findViewById(R.id.buttonEditContact)
        private val removeButton: ImageButton = itemView.findViewById(R.id.buttonRemoveContact)
        private val detailsContainer: ViewGroup = itemView.findViewById(R.id.detailsContainer)

        private fun copyToClipboard(context: Context, text: String) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Contact Detail", text)
            clipboard.setPrimaryClip(clip)
        }

        fun bind(
            contact: ExternalContact,
            isExpanded: Boolean,
            onRemoveClick: (contactId: Int) -> Unit,
            onEditClick: (contact: ExternalContact) -> Unit,
            onItemClick: () -> Unit
        ) {
            contactNameTextView.text = contact.contactName ?: "N/A"
            removeButton.setOnClickListener { contact.id?.let { onRemoveClick(it) } }
            editButton.setOnClickListener { onEditClick(contact) }

            itemView.setOnClickListener { onItemClick() }

            detailsContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE

            if (isExpanded) {
                detailsContainer.removeAllViews()
                contact.contactDetails?.forEach { detail ->
                    val detailView = LayoutInflater.from(itemView.context)
                        .inflate(R.layout.item_contact_detail, detailsContainer, false)

                    val typeText = detailView.findViewById<TextView>(R.id.textViewDetailType)
                    val valueText = detailView.findViewById<TextView>(R.id.textViewDetailValue)

                    typeText.text = detail.contactType?.uppercase() ?: "UNKNOWN"
                    valueText.text = detail.contactValue ?: "N/A"
                    val copyButton = detailView.findViewById<ImageButton>(R.id.buttonCopyDetail)


                    if (!detail.contactMethodAltName.isNullOrEmpty()) {
                        typeText.text = "${typeText.text} (${detail.contactMethodAltName})"
                    }

                    copyButton.setOnClickListener {
                        val value = detail.contactValue
                        if (!value.isNullOrEmpty()) {
                            copyToClipboard(itemView.context, value)
                            Snackbar.make(itemView, "Copied: $value", Snackbar.LENGTH_SHORT).show()
                        }
                    }

                    detailsContainer.addView(detailView)
                }

                if (contact.contactDetails.isNullOrEmpty()) {
                    val emptyView = TextView(itemView.context).apply {
                        text = "No contact details available"
                        setPadding(16, 8, 16, 8)
                    }
                    detailsContainer.addView(emptyView)
                }
            }
        }
    }


}

class DiffCallback : DiffUtil.ItemCallback<ExternalContact>() {
    override fun areItemsTheSame(oldItem: ExternalContact, newItem: ExternalContact): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ExternalContact, newItem: ExternalContact): Boolean {
        return oldItem == newItem
    }
}
package info.proteo.cupcake.ui.contact

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.R
import info.proteo.cupcake.data.remote.model.instrument.ExternalContact

class ExternalContactAdapter(
    private val onRemoveClick: (contactId: Int) -> Unit,
    private val onEditClick: (contact: ExternalContact) -> Unit
) : ListAdapter<ExternalContact, ExternalContactAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_external_contact, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = getItem(position)
        holder.bind(contact, onRemoveClick, onEditClick)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val contactNameTextView: TextView = itemView.findViewById(R.id.textViewContactName)
        private val editButton: ImageButton = itemView.findViewById(R.id.buttonEditContact)
        private val removeButton: ImageButton = itemView.findViewById(R.id.buttonRemoveContact)

        fun bind(
            contact: ExternalContact,
            onRemoveClick: (contactId: Int) -> Unit,
            onEditClick: (contact: ExternalContact) -> Unit
        ) {
            contactNameTextView.text = contact.contactName ?: "N/A"
            removeButton.setOnClickListener { contact.id?.let { p1 -> onRemoveClick(p1) } }
            editButton.setOnClickListener { onEditClick(contact) }
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
}
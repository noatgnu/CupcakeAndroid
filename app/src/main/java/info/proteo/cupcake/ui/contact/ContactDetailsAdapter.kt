package info.proteo.cupcake.ui.contact

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.shared.data.model.instrument.ExternalContactDetails
import info.proteo.cupcake.databinding.ItemContactDetailEditableBinding
import kotlin.collections.get

class ContactDetailsAdapter(
    private val onRemoveClick: (position: Int) -> Unit,
    private val onDetailChange: (position: Int, updatedDetail: ExternalContactDetails) -> Unit
) : ListAdapter<ExternalContactDetails, ContactDetailsAdapter.DetailViewHolder>(DetailDiffCallback()) {

    private val contactTypes = listOf("email", "phone", "website", "other", "address")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailViewHolder {
        val binding = ItemContactDetailEditableBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        val spinnerAdapter = ArrayAdapter(parent.context, android.R.layout.simple_spinner_item, contactTypes)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDetailContactType.adapter = spinnerAdapter
        return DetailViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DetailViewHolder, position: Int) {
        val detail = getItem(position)
        holder.bind(detail, position)
    }

    inner class DetailViewHolder(private val binding: ItemContactDetailEditableBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var currentDetail: ExternalContactDetails? = null
        private var currentPosition: Int = -1
        private var isBinding: Boolean = false

        private val spinnerListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (!isBinding) {
                    reportChange()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        init {
            // Only use focus listeners - no TextWatchers
            binding.editTextDetailLabel.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus && !isBinding) {
                    reportChange()
                }
            }

            binding.editTextDetailValue.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus && !isBinding) {
                    reportChange()
                }
            }

            binding.spinnerDetailContactType.onItemSelectedListener = spinnerListener
            binding.buttonRemoveDetail.setOnClickListener { onRemoveClick(adapterPosition) }
        }

        fun bind(detail: ExternalContactDetails, position: Int) {
            isBinding = true
            currentDetail = detail
            currentPosition = position

            binding.editTextDetailLabel.setText(detail.contactMethodAltName ?: "")
            binding.editTextDetailValue.setText(detail.contactValue ?: "")

            val contactTypeIndex = if (detail.contactType != null) {
                contactTypes.indexOf(detail.contactType).takeIf { it >= 0 } ?: 0
            } else 0
            binding.spinnerDetailContactType.setSelection(contactTypeIndex)

            isBinding = false
        }

        fun reportChange() {
            currentDetail?.let {
                val updatedLabel = binding.editTextDetailLabel.text.toString()
                val updatedType = binding.spinnerDetailContactType.selectedItem.toString()
                val updatedValue = binding.editTextDetailValue.text.toString()

                val updatedDetail = it.copy(
                    contactMethodAltName = updatedLabel,
                    contactType = updatedType,
                    contactValue = updatedValue
                )

                if (updatedDetail != it) {
                    onDetailChange(currentPosition, updatedDetail)
                }
            }
        }
    }

    fun commitAllEdits(recyclerView: RecyclerView) {
        for (i in 0 until itemCount) {
            val viewHolder = recyclerView.findViewHolderForAdapterPosition(i) as? DetailViewHolder
            viewHolder?.reportChange()
        }
    }

    class DetailDiffCallback : DiffUtil.ItemCallback<ExternalContactDetails>() {
        override fun areItemsTheSame(oldItem: ExternalContactDetails, newItem: ExternalContactDetails): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ExternalContactDetails, newItem: ExternalContactDetails): Boolean {
            return oldItem == newItem
        }
    }
}
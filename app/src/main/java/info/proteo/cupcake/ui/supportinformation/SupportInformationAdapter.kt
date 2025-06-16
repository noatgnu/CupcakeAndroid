package info.proteo.cupcake.ui.instrument

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.shared.data.model.instrument.SupportInformation
import info.proteo.cupcake.shared.data.model.storage.StorageObject
import info.proteo.cupcake.data.repository.StorageRepository
import info.proteo.cupcake.data.repository.SupportInformationRepository
import info.proteo.cupcake.databinding.DialogEditSupportInformationBinding
import info.proteo.cupcake.databinding.ItemStorageObjectSuggestionBinding
import info.proteo.cupcake.databinding.ItemSupportInformationBinding
import info.proteo.cupcake.ui.storage.StorageObjectSuggestionAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SupportInformationAdapter(
    private val onItemClick: (SupportInformation) -> Unit,
    private val supportInformationRepository: SupportInformationRepository,
    private val storageObjectRepository: StorageRepository
) : ListAdapter<SupportInformation, SupportInformationAdapter.SupportInfoViewHolder>(SupportInfoDiffCallback()) {

    inner class SupportInfoViewHolder(
        private val binding: ItemSupportInformationBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        private val viewHolderScope = CoroutineScope(Dispatchers.Main + Job())

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }

            binding.buttonEditSupportInfo.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    showEditDialog(getItem(position))
                }
            }
        }

        fun bind(supportInfo: SupportInformation) {
            binding.apply {
                root.setOnClickListener {
                    onItemClick(supportInfo)
                }
                textVendorName.text = supportInfo.vendorName ?: "N/A"
                textManufacturerName.text = supportInfo.manufacturerName ?: "N/A"
                textSerialNumber.text = "S/N: ${supportInfo.serialNumber ?: "N/A"}"

                val warrantyStart = supportInfo.warrantyStartDate ?: "N/A"
                val warrantyEnd = supportInfo.warrantyEndDate ?: "N/A"
                textWarrantyDates.text = "Warranty: $warrantyStart to $warrantyEnd"

                textMaintenanceFrequency.text = "Maintenance: Every ${supportInfo.maintenanceFrequencyDays ?: "N/A"} days"

                textLocation.text = "Location: Loading..."

                supportInfo.location?.let { location ->
                    val locationId = location.id
                    viewHolderScope.launch {
                        try {
                            val pathResult = withContext(Dispatchers.IO) {
                                storageObjectRepository.getPathToRoot(locationId)
                            }

                            pathResult.fold(
                                onSuccess = { pathItems ->
                                    val pathString = if (pathItems.isNotEmpty()) {
                                        pathItems.joinToString(" > ") { it.name }
                                    } else {
                                        supportInfo.location?.objectName ?: "Unknown location"
                                    }
                                    textLocation.text = "Location: $pathString"
                                },
                                onFailure = {
                                    // If path fetch fails, display basic location name if available
                                    textLocation.text = "Location: ${supportInfo.location?.objectName ?: "Unknown"}"
                                    Log.e("SupportInfoAdapter", "Failed to fetch location path", it)
                                }
                            )
                        } catch (e: Exception) {
                            textLocation.text = "Location: ${supportInfo.location?.objectName ?: "Unknown"}"
                            Log.e("SupportInfoAdapter", "Error fetching location path", e)
                        }
                    }
                } ?: run {
                    // No location ID, so no location is set
                    textLocation.text = "Location: Not set"
                }
            }
        }

        fun onDetached() {
            viewHolderScope.cancel()
        }

        private fun showEditDialog(supportInfo: SupportInformation) {
            val context = binding.root.context
            val dialogBinding = DialogEditSupportInformationBinding.inflate(LayoutInflater.from(context))
            val dialog = AlertDialog.Builder(context)
                .setTitle("Edit Support Information")
                .setView(dialogBinding.root)
                .create()

            dialogBinding.editTextVendorName.setText(supportInfo.vendorName)
            dialogBinding.editTextManufacturerName.setText(supportInfo.manufacturerName)
            dialogBinding.editTextSerialNumber.setText(supportInfo.serialNumber)
            dialogBinding.editTextMaintenanceFrequency.setText(supportInfo.maintenanceFrequencyDays?.toString())
            dialogBinding.editTextWarrantyStart.setText(supportInfo.warrantyStartDate)
            dialogBinding.editTextWarrantyEnd.setText(supportInfo.warrantyEndDate)

            supportInfo.location?.let {
                dialogBinding.editTextLocation.setText(it.objectName)
            }

            val locationAdapter = StorageObjectSuggestionAdapter { selectedLocation ->
                dialogBinding.editTextLocation.setText(selectedLocation.objectName)
                dialogBinding.recyclerViewLocationSuggestions.visibility = View.GONE
                dialogBinding.editTextLocation.tag = selectedLocation.id
            }

            dialogBinding.recyclerViewLocationSuggestions.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = locationAdapter
            }

            val searchScope = CoroutineScope(Dispatchers.Main)
            var searchJob: Job? = null

            dialogBinding.editTextLocation.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    val query = s.toString().trim()
                    searchJob?.cancel()

                    if (query.length >= 2) {
                        searchJob = searchScope.launch {
                            delay(300)
                            try {
                                val result = withContext(Dispatchers.IO) {
                                    storageObjectRepository.searchStorageObjects(query, 0, 10)
                                }

                                result.fold(
                                    onSuccess = { response ->
                                        locationAdapter.submitList(response.results)
                                        dialogBinding.recyclerViewLocationSuggestions.visibility =
                                            if (response.results.isNotEmpty()) View.VISIBLE else View.GONE
                                    },
                                    onFailure = {
                                        dialogBinding.recyclerViewLocationSuggestions.visibility = View.GONE
                                    }
                                )
                            } catch (e: Exception) {
                                dialogBinding.recyclerViewLocationSuggestions.visibility = View.GONE
                            }
                        }
                    } else {
                        dialogBinding.recyclerViewLocationSuggestions.visibility = View.GONE
                    }
                }
            })

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val calendar = Calendar.getInstance()

            dialogBinding.editTextWarrantyStart.setOnClickListener {
                DatePickerDialog(context, { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    dialogBinding.editTextWarrantyStart.setText(dateFormat.format(calendar.time))
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
            }

            dialogBinding.editTextWarrantyEnd.setOnClickListener {
                DatePickerDialog(context, { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    dialogBinding.editTextWarrantyEnd.setText(dateFormat.format(calendar.time))
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
            }

            dialogBinding.buttonSave.setOnClickListener {
                val updatedSupportInfo = supportInfo.copy(
                    vendorName = dialogBinding.editTextVendorName.text.toString().takeIf { it.isNotBlank() },
                    manufacturerName = dialogBinding.editTextManufacturerName.text.toString().takeIf { it.isNotBlank() },
                    serialNumber = dialogBinding.editTextSerialNumber.text.toString().takeIf { it.isNotBlank() },
                    maintenanceFrequencyDays = dialogBinding.editTextMaintenanceFrequency.text.toString().toIntOrNull(),
                    locationId = dialogBinding.editTextLocation.tag as? Int,
                    warrantyStartDate = dialogBinding.editTextWarrantyStart.text.toString().takeIf { it.isNotBlank() },
                    warrantyEndDate = dialogBinding.editTextWarrantyEnd.text.toString().takeIf { it.isNotBlank() }
                )

                searchScope.launch {
                    try {
                        val result = withContext(Dispatchers.IO) {
                            supportInformationRepository.updateSupportInformation(
                                supportInfo.id,
                                updatedSupportInfo
                            )
                        }

                        result.fold(
                            onSuccess = { updatedInfo ->
                                val currentList = currentList.toMutableList()
                                val position = currentList.indexOfFirst { it.id == updatedInfo.id }
                                if (position != -1) {
                                    currentList[position] = updatedInfo
                                    submitList(currentList)
                                }
                            },
                            onFailure = {
                                Log.e("SupportInfoAdapter", "Failed to update support information", it)
                            }
                        )
                    } catch (e: Exception) {
                    }

                    dialog.dismiss()
                }
            }

            dialogBinding.buttonCancel.setOnClickListener {
                dialog.dismiss()
                searchScope.cancel()
            }

            dialog.setOnDismissListener {
                searchScope.cancel()
            }

            dialog.show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SupportInfoViewHolder {
        val binding = ItemSupportInformationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SupportInfoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SupportInfoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class SupportInfoDiffCallback : DiffUtil.ItemCallback<SupportInformation>() {
        override fun areItemsTheSame(oldItem: SupportInformation, newItem: SupportInformation): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SupportInformation, newItem: SupportInformation): Boolean {
            return oldItem == newItem
        }
    }

    override fun onViewRecycled(holder: SupportInfoViewHolder) {
        super.onViewRecycled(holder)
        holder.onDetached()
    }
}

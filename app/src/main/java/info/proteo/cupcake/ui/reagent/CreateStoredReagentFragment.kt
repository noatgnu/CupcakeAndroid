package info.proteo.cupcake.ui.reagent

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.R
import info.proteo.cupcake.databinding.FragmentCreateStoredReagentBinding
import info.proteo.cupcake.shared.data.model.reagent.Reagent
import info.proteo.cupcake.shared.data.model.reagent.StoredReagentCreateRequest
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CreateStoredReagentFragment : Fragment() {

    private val TAG = "CreateStoredReagentFrg"
    private var _binding: FragmentCreateStoredReagentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CreateStoredReagentViewModel by viewModels()

    private var storageObjectId: Int = 0
    private var storageName: String = ""

    private lateinit var reagentAdapter: ReagentAdapter
    private var isCreatingNewReagent = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateStoredReagentBinding.inflate(inflater, container, false)

        // Get arguments from bundle instead of using navArgs
        arguments?.let {
            storageObjectId = it.getInt("storageObjectId", 0)
            storageName = it.getString("storageName", "")
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUi()
        observeViewModel()
        setupListeners()
    }

    private fun setupUi() {
        // Set storage field if we have a storage location from arguments
        if (storageObjectId > 0) {
            binding.storageAutocomplete.setText(storageName)
        }

        // Create custom adapter that doesn't filter locally (server does filtering)
        reagentAdapter = ReagentAdapter(requireContext())
        binding.reagentAutocomplete.setAdapter(reagentAdapter)

        // Use threshold of 2 characters to start showing suggestions
        binding.reagentAutocomplete.threshold = 2

        // Handle reagent selection
        binding.reagentAutocomplete.setOnItemClickListener { _, _, position, _ ->
            val selectedReagent = reagentAdapter.getItem(position)
            Log.d(TAG, "Selected reagent: ${selectedReagent.name} (${selectedReagent.unit})")
            viewModel.selectReagent(selectedReagent)
            binding.unitAutocomplete.setText(selectedReagent.unit)
        }

        val unitAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            resources.getStringArray(R.array.reagent_units)
        )
        binding.unitAutocomplete.setAdapter(unitAdapter)
    }

    private fun observeViewModel() {
        // Observe search results
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.searchResults.collectLatest { reagents ->
                Log.d(TAG, "Received ${reagents.size} search results in fragment")
                reagentAdapter.updateReagents(reagents)

                // Force dropdown to show if we have results and input has focus
                if (reagents.isNotEmpty() && binding.reagentAutocomplete.hasFocus()) {
                    binding.reagentAutocomplete.post {
                        if (!binding.reagentAutocomplete.isPopupShowing) {
                            binding.reagentAutocomplete.showDropDown()
                        }
                    }
                }
            }
        }

        // Observe loading state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.progressIndicator.isVisible = isLoading
            }
        }

        // Observe error messages
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collectLatest { errorMessage ->
                errorMessage?.let {
                    Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }

        // Observe selected reagent
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedReagent.collectLatest { reagent ->
                reagent?.let {
                    // Update the UI with selected reagent
                    binding.reagentAutocomplete.setText(it.name, false)
                    binding.unitAutocomplete.setText(it.unit, false)
                }
            }
        }

        // Export metadata observer (no changes needed)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.exportedMetadata.collectLatest { metadata ->
                // Check if we have metadata to show
                if (metadata != null && metadata.isNotEmpty()) {
                    val sessionId = viewModel.getMetadataSessionId()
                    val protocolId = viewModel.getMetadataProtocolId()
                    val stepId = viewModel.getMetadataStepId()

                    // Build metadata display text
                    val metadataText = "From Protocol: ${protocolId ?: "Unknown"}\n" +
                        "Session: ${sessionId ?: "Unknown"}\n" +
                        "Step: ${stepId ?: "Unknown"}"

                    // This would be shown in a card/info view if we had one
                }
            }
        }
    }

    private fun setupListeners() {
        // Search query handling - this is the critical part for autocomplete
        binding.reagentAutocomplete.doAfterTextChanged { text ->
            val query = text?.toString() ?: ""
            Log.d(TAG, "Text changed: '$query'")
            if (query.isNotEmpty() && query.length >= 2) {
                viewModel.updateSearchQuery(query)
            }
        }

        // Create stored reagent
        binding.saveButton.setOnClickListener {
            if (validateForm()) {
                // Check if we have metadata and should confirm including it
                if (viewModel.exportedMetadata.value != null &&
                    viewModel.exportedMetadata.value?.isNotEmpty() == true) {
                    showMetadataConfirmationDialog()
                } else {
                    createStoredReagent(false)
                }
            }
        }
    }

    private fun showMetadataConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Include Metadata?")
            .setMessage("Do you want to include the protocol metadata with this reagent?")
            .setPositiveButton("Yes") { _, _ -> createStoredReagent(true) }
            .setNegativeButton("No") { _, _ -> createStoredReagent(false) }
            .show()
    }

    private fun validateForm(): Boolean {
        var isValid = true

        // Check quantity
        val quantityStr = binding.quantityInput.text.toString()
        if (quantityStr.isEmpty() || quantityStr.toFloatOrNull() == null || quantityStr.toFloat() <= 0) {
            binding.quantityLayout.error = "Please enter a valid quantity"
            isValid = false
        } else {
            binding.quantityLayout.error = null
        }

        // Check reagent selection
        if (binding.reagentAutocomplete.text.isNullOrEmpty()) {
            binding.reagentLayout.error = "Please select a reagent"
            isValid = false
        } else {
            binding.reagentLayout.error = null
        }

        return isValid
    }

    private fun createStoredReagent(withMetadata: Boolean) {
        val quantity = binding.quantityInput.text.toString().toFloat()
        val notes = binding.notesInput.text.toString()
        val barcode = binding.barcodeInput.text.toString().takeIf { it.isNotEmpty() }
        val shareable = binding.shareableSwitch.isChecked

        val selectedReagent = viewModel.selectedReagent.value

        if (selectedReagent == null) {
            Toast.makeText(requireContext(), "Please select a valid reagent", Toast.LENGTH_SHORT).show()
            return
        }

        val request = StoredReagentCreateRequest(
            name = selectedReagent.name,
            unit = selectedReagent.unit,
            storageObject = storageObjectId,
            quantity = quantity,
            notes = notes,
            barcode = barcode,
            shareable = shareable,
            createdByProtocol = if (withMetadata) viewModel.getMetadataProtocolId() else null,
            createdBySession = if (withMetadata) viewModel.getMetadataSessionId() else null,
            createdByStep = if (withMetadata) viewModel.getMetadataStepId() else null,
            createdByProject = null
        )

        viewLifecycleOwner.lifecycleScope.launch {
            val result = viewModel.createStoredReagent(request)
            result.observe(viewLifecycleOwner) { res ->
                res.onSuccess {
                    Toast.makeText(requireContext(), "Reagent stored successfully", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }.onFailure { error ->
                    Toast.makeText(requireContext(), "Failed to store reagent: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Custom adapter that doesn't filter locally since filtering is done on the server
     */
    private inner class ReagentAdapter(context: android.content.Context) :
            ArrayAdapter<Reagent>(context, android.R.layout.simple_dropdown_item_1line), Filterable {

        private val reagents = mutableListOf<Reagent>()

        override fun getCount(): Int = reagents.size

        override fun getItem(position: Int): Reagent = reagents[position]

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(android.R.layout.simple_dropdown_item_1line, parent, false)

            val reagent = getItem(position)
            (view as TextView).text = "${reagent.name} (${reagent.unit})"
            return view
        }

        fun updateReagents(newReagents: List<Reagent>) {
            reagents.clear()
            reagents.addAll(newReagents)
            notifyDataSetChanged()
        }

        override fun getFilter(): Filter {
            return object : Filter() {
                override fun performFiltering(constraint: CharSequence?): FilterResults {
                    // Return all items since filtering is done by the server
                    val results = FilterResults()
                    results.values = reagents
                    results.count = reagents.size
                    return results
                }

                override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                    notifyDataSetChanged()
                }
            }
        }
    }
}
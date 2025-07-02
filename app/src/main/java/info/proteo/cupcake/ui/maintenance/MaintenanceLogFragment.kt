package info.proteo.cupcake.ui.maintenance

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.MaterialDatePicker
import androidx.core.widget.addTextChangedListener
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.R
import info.proteo.cupcake.databinding.DialogCreateMaintenanceLogBinding
import info.proteo.cupcake.databinding.DialogFilterMaintenanceLogsBinding
import info.proteo.cupcake.databinding.FragmentMaintenanceLogBinding
import info.proteo.cupcake.shared.data.model.maintenance.MaintenanceConstants
import info.proteo.cupcake.shared.data.model.maintenance.MaintenanceLog
import info.proteo.cupcake.shared.data.model.maintenance.MaintenanceLogRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class MaintenanceLogFragment : Fragment() {

    private var _binding: FragmentMaintenanceLogBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MaintenanceLogViewModel by viewModels()
    private lateinit var maintenanceLogAdapter: MaintenanceLogAdapter
    
    private val args: MaintenanceLogFragmentArgs by navArgs()
    private val instrumentId: Long get() = args.instrumentId

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMaintenanceLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMenu()
        setupRecyclerView()
        setupSwipeRefresh()
        setupFilterControls()
        observeViewModel()
        Log.d("MaintenanceLogFragment", "Instrument ID: $instrumentId")
        if (instrumentId != -1L) {
            viewModel.setInstrumentId(instrumentId)
        }
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_maintenance_log, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_add_maintenance_log -> {
                        showCreateMaintenanceLogDialog()
                        true
                    }
                    R.id.action_filter -> {
                        showFilterDialog()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupRecyclerView() {
        maintenanceLogAdapter = MaintenanceLogAdapter(
            onItemClick = { maintenanceLog ->
                showMaintenanceLogDetails(maintenanceLog)
            },
            onStatusChange = { maintenanceLog, newStatus ->
                viewModel.updateMaintenanceLogStatus(maintenanceLog.id, newStatus)
            }
        )

        binding.recyclerViewMaintenanceLogs.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = maintenanceLogAdapter
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItem = layoutManager.findFirstVisibleItemPosition()

                    if ((visibleItemCount + firstVisibleItem) >= totalItemCount - 5
                        && firstVisibleItem >= 0 && dy > 0 && viewModel.hasMoreData) {
                        viewModel.loadMoreMaintenanceLogs()
                    }
                }
            })
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadMaintenanceLogs(refresh = true)
        }
    }

    private fun setupFilterControls() {
        binding.chipFilterTemplates.setOnCheckedChangeListener { _, _ ->
            viewModel.toggleTemplatesOnly()
        }
        
        // Setup search functionality
        binding.searchView?.addTextChangedListener { text ->
            viewModel.searchMaintenanceLogs(text?.toString())
        }
    }

    private fun observeViewModel() {
        viewModel.maintenanceLogs.observe(viewLifecycleOwner) { result ->
            binding.swipeRefreshLayout.isRefreshing = false

            result.onSuccess { response ->
                maintenanceLogAdapter.submitList(response.results)
                binding.emptyView.isVisible = response.results.isEmpty()
                binding.recyclerViewMaintenanceLogs.isVisible = response.results.isNotEmpty()
            }.onFailure { error ->
                Toast.makeText(
                    requireContext(),
                    "Error loading maintenance logs: ${error.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
                binding.emptyView.isVisible = true
                binding.recyclerViewMaintenanceLogs.isVisible = false
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }

        viewModel.createResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                it.onSuccess {
                    Toast.makeText(requireContext(), "Maintenance log created successfully", Toast.LENGTH_SHORT).show()
                }.onFailure { error ->
                    Toast.makeText(requireContext(), "Error creating maintenance log: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                }
                viewModel.clearResults()
            }
        }

        viewModel.updateResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                it.onSuccess {
                    Toast.makeText(requireContext(), "Maintenance log updated successfully", Toast.LENGTH_SHORT).show()
                }.onFailure { error ->
                    Toast.makeText(requireContext(), "Error updating maintenance log: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                }
                viewModel.clearResults()
            }
        }

        viewModel.deleteResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                it.onSuccess {
                    Toast.makeText(requireContext(), "Maintenance log deleted successfully", Toast.LENGTH_SHORT).show()
                }.onFailure { error ->
                    Toast.makeText(requireContext(), "Error deleting maintenance log: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                }
                viewModel.clearResults()
            }
        }
    }

    private fun showCreateMaintenanceLogDialog() {
        val dialogBinding = DialogCreateMaintenanceLogBinding.inflate(layoutInflater)

        // Setup maintenance type dropdown
        val typeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            listOf("Routine", "Emergency", "Other")
        )
        dialogBinding.autoCompleteMaintenanceType.setAdapter(typeAdapter)
        dialogBinding.autoCompleteMaintenanceType.setText("Routine", false)

        // Setup status dropdown
        val statusAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            listOf("Pending", "In Progress", "Completed", "Requested", "Cancelled")
        )
        dialogBinding.autoCompleteStatus.setAdapter(statusAdapter)
        dialogBinding.autoCompleteStatus.setText("Pending", false)

        // Setup date picker
        dialogBinding.editTextMaintenanceDate.setOnClickListener {
            showDatePicker { selectedDate ->
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                dialogBinding.editTextMaintenanceDate.setText(dateFormat.format(selectedDate))
            }
        }

        // Set default date to today
        val today = Date()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        dialogBinding.editTextMaintenanceDate.setText(dateFormat.format(today))

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Create Maintenance Log")
            .setView(dialogBinding.root)
            .setPositiveButton("Create", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val createButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            createButton.setOnClickListener {
                createMaintenanceLog(dialogBinding, dialog)
            }
        }

        dialog.show()
    }

    private fun createMaintenanceLog(dialogBinding: DialogCreateMaintenanceLogBinding, dialog: AlertDialog) {
        val maintenanceDate = dialogBinding.editTextMaintenanceDate.text.toString()
        val maintenanceType = when (dialogBinding.autoCompleteMaintenanceType.text.toString()) {
            "Routine" -> MaintenanceConstants.Types.ROUTINE
            "Emergency" -> MaintenanceConstants.Types.EMERGENCY
            "Other" -> MaintenanceConstants.Types.OTHER
            else -> MaintenanceConstants.Types.ROUTINE
        }
        val status = when (dialogBinding.autoCompleteStatus.text.toString()) {
            "Pending" -> MaintenanceConstants.Statuses.PENDING
            "In Progress" -> MaintenanceConstants.Statuses.IN_PROGRESS
            "Completed" -> MaintenanceConstants.Statuses.COMPLETED
            "Requested" -> MaintenanceConstants.Statuses.REQUESTED
            "Cancelled" -> MaintenanceConstants.Statuses.CANCELLED
            else -> MaintenanceConstants.Statuses.PENDING
        }
        val description = dialogBinding.editTextDescription.text.toString()
        val notes = dialogBinding.editTextNotes.text.toString()
        val isTemplate = dialogBinding.checkBoxTemplate.isChecked

        if (maintenanceDate.isBlank()) {
            dialogBinding.textInputLayoutMaintenanceDate.error = "Date is required"
            return
        }

        if (description.isBlank()) {
            dialogBinding.textInputLayoutDescription.error = "Description is required"
            return
        }

        val request = MaintenanceLogRequest(
            instrumentId = instrumentId,
            maintenanceDate = "${maintenanceDate}T00:00:00",
            maintenanceType = maintenanceType,
            maintenanceDescription = description,
            maintenanceNotes = notes.ifBlank { null },
            status = status,
            isTemplate = isTemplate
        )

        viewModel.createMaintenanceLog(request)
        dialog.dismiss()
    }

    private fun showDatePicker(onDateSelected: (Date) -> Unit) {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select maintenance date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            onDateSelected(Date(selection))
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun showMaintenanceLogDetails(maintenanceLog: MaintenanceLog) {
        val action = MaintenanceLogFragmentDirections
            .actionMaintenanceLogFragmentToMaintenanceLogDetailFragment(maintenanceLog.id)
        findNavController().navigate(action)
    }

    private fun showDeleteConfirmationDialog(maintenanceLog: MaintenanceLog) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Maintenance Log")
            .setMessage("Are you sure you want to delete this maintenance log? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteMaintenanceLog(maintenanceLog.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFilterDialog() {
        val dialogBinding = DialogFilterMaintenanceLogsBinding.inflate(layoutInflater)
        
        // Setup maintenance type dropdown
        viewModel.maintenanceTypes.value?.getOrNull()?.let { types ->
            val typeNames = listOf("All") + types.map { it.label }
            val typeAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                typeNames
            )
            dialogBinding.autoCompleteMaintenanceType.setAdapter(typeAdapter)
            dialogBinding.autoCompleteMaintenanceType.setText("All", false)
        }
        
        // Setup status dropdown
        viewModel.maintenanceStatuses.value?.getOrNull()?.let { statuses ->
            val statusNames = listOf("All") + statuses.map { it.label }
            val statusAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                statusNames
            )
            dialogBinding.autoCompleteStatus.setAdapter(statusAdapter)
            dialogBinding.autoCompleteStatus.setText("All", false)
        }
        
        // Setup date pickers
        dialogBinding.editTextDateFrom.setOnClickListener {
            showDatePicker { selectedDate ->
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                dialogBinding.editTextDateFrom.setText(dateFormat.format(selectedDate))
            }
        }
        
        dialogBinding.editTextDateTo.setOnClickListener {
            showDatePicker { selectedDate ->
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                dialogBinding.editTextDateTo.setText(dateFormat.format(selectedDate))
            }
        }
        
        // Set current filter values
        dialogBinding.editTextSearch.setText(viewModel.getCurrentSearchQuery() ?: "")
        dialogBinding.checkBoxTemplatesOnly.isChecked = viewModel.getShowTemplatesOnly()
        
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Filter Maintenance Logs")
            .setView(dialogBinding.root)
            .setPositiveButton("Apply", null)
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Clear All", null)
            .create()
            
        dialog.setOnShowListener {
            val applyButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val clearButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            
            applyButton.setOnClickListener {
                applyFilters(dialogBinding)
                dialog.dismiss()
            }
            
            clearButton.setOnClickListener {
                clearAllFilters(dialogBinding)
            }
        }
        
        dialog.show()
    }
    
    private fun applyFilters(dialogBinding: DialogFilterMaintenanceLogsBinding) {
        val searchQuery = dialogBinding.editTextSearch.text.toString().takeIf { it.isNotBlank() }
        val maintenanceType = dialogBinding.autoCompleteMaintenanceType.text.toString().takeIf { it != "All" }
        val status = dialogBinding.autoCompleteStatus.text.toString().takeIf { it != "All" }
        val dateFrom = dialogBinding.editTextDateFrom.text.toString().takeIf { it.isNotBlank() }
        val dateTo = dialogBinding.editTextDateTo.text.toString().takeIf { it.isNotBlank() }
        val templatesOnly = dialogBinding.checkBoxTemplatesOnly.isChecked
        val includeCompleted = dialogBinding.checkBoxIncludeCompleted.isChecked
        
        // Convert display names back to API values
        val apiMaintenanceType = when (maintenanceType) {
            "Routine" -> MaintenanceConstants.Types.ROUTINE
            "Emergency" -> MaintenanceConstants.Types.EMERGENCY
            "Other" -> MaintenanceConstants.Types.OTHER
            else -> null
        }
        
        val apiStatus = when (status) {
            "Pending" -> MaintenanceConstants.Statuses.PENDING
            "In Progress" -> MaintenanceConstants.Statuses.IN_PROGRESS
            "Completed" -> MaintenanceConstants.Statuses.COMPLETED
            "Requested" -> MaintenanceConstants.Statuses.REQUESTED
            "Cancelled" -> MaintenanceConstants.Statuses.CANCELLED
            else -> null
        }
        
        viewModel.applyFilters(
            searchQuery = searchQuery,
            maintenanceType = apiMaintenanceType,
            status = apiStatus,
            dateFrom = dateFrom,
            dateTo = dateTo,
            templatesOnly = templatesOnly,
            includeCompleted = includeCompleted
        )
    }
    
    private fun clearAllFilters(dialogBinding: DialogFilterMaintenanceLogsBinding) {
        dialogBinding.editTextSearch.setText("")
        dialogBinding.autoCompleteMaintenanceType.setText("All", false)
        dialogBinding.autoCompleteStatus.setText("All", false)
        dialogBinding.editTextDateFrom.setText("")
        dialogBinding.editTextDateTo.setText("")
        dialogBinding.checkBoxTemplatesOnly.isChecked = false
        dialogBinding.checkBoxIncludeCompleted.isChecked = true
        
        viewModel.clearAllFilters()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerViewMaintenanceLogs.adapter = null
        _binding = null
    }
}
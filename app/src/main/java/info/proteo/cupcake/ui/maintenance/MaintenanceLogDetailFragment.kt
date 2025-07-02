package info.proteo.cupcake.ui.maintenance

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.R
import info.proteo.cupcake.databinding.DialogCreateMaintenanceLogBinding
import info.proteo.cupcake.databinding.FragmentMaintenanceLogDetailBinding
import info.proteo.cupcake.shared.data.model.annotation.Annotation
import info.proteo.cupcake.shared.data.model.maintenance.MaintenanceConstants
import info.proteo.cupcake.shared.data.model.maintenance.MaintenanceLog
import info.proteo.cupcake.shared.data.model.maintenance.MaintenanceLogRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class MaintenanceLogDetailFragment : Fragment() {

    private var _binding: FragmentMaintenanceLogDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MaintenanceLogDetailViewModel by viewModels()
    
    private val maintenanceLogId: Long by lazy {
        // Check if we have the argument in our bundle (works for both Safe Args and manual args)
        arguments?.getLong("maintenanceLogId") ?: -1L
    }

    private lateinit var annotationAdapter: MaintenanceLogAnnotationAdapter
    private var currentMaintenanceLog: MaintenanceLog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMaintenanceLogDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMenu()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()

        // Load the maintenance log
        if (maintenanceLogId != -1L) {
            viewModel.loadMaintenanceLog(maintenanceLogId)
        }
    }


    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_maintenance_log_detail, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_refresh -> {
                        viewModel.refreshData()
                        true
                    }
                    R.id.action_delete -> {
                        showDeleteConfirmationDialog()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupRecyclerView() {
        annotationAdapter = MaintenanceLogAnnotationAdapter(
            onItemClick = { annotation ->
                openAnnotation(annotation)
            },
            onDeleteClick = { annotation ->
                showDeleteAnnotationConfirmationDialog(annotation)
            }
        )

        binding.recyclerViewAnnotations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = annotationAdapter
        }
    }

    private fun setupClickListeners() {
        binding.buttonEditStatus.setOnClickListener {
            showUpdateStatusDialog()
        }

        binding.buttonEdit.setOnClickListener {
            showEditMaintenanceLogDialog()
        }

        binding.buttonAddAnnotation.setOnClickListener {
            // TODO: Navigate to annotation creation screen
            Toast.makeText(requireContext(), "Add annotation functionality coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.fabQuickAction.setOnClickListener {
            showEditMaintenanceLogDialog()
        }
    }

    private fun observeViewModel() {
        viewModel.maintenanceLog.observe(viewLifecycleOwner) { result ->
            result.onSuccess { maintenanceLog ->
                currentMaintenanceLog = maintenanceLog
                displayMaintenanceLog(maintenanceLog)
            }.onFailure { error ->
                Toast.makeText(
                    requireContext(),
                    "Error loading maintenance log: ${error.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        viewModel.annotations.observe(viewLifecycleOwner) { result ->
            result.onSuccess { annotations ->
                displayAnnotations(annotations)
            }.onFailure { error ->
                Toast.makeText(
                    requireContext(),
                    "Error loading annotations: ${error.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
                displayAnnotations(emptyList())
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
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
                    findNavController().navigateUp()
                }.onFailure { error ->
                    Toast.makeText(requireContext(), "Error deleting maintenance log: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                }
                viewModel.clearResults()
            }
        }
    }

    private fun displayMaintenanceLog(maintenanceLog: MaintenanceLog) {
        binding.apply {
            // Status chip
            chipStatus.text = getStatusDisplayName(maintenanceLog.status)
            chipStatus.setChipBackgroundColorResource(getStatusColor(maintenanceLog.status))

            // Basic information
            textViewDate.text = formatDate(maintenanceLog.maintenanceDate)
            textViewType.text = getTypeDisplayName(maintenanceLog.maintenanceType)
            textViewCreatedBy.text = maintenanceLog.createdByUser?.fullName ?: "Unknown"
            textViewCreatedAt.text = formatDateTime(maintenanceLog.createdAt)
            textViewDescription.text = maintenanceLog.maintenanceDescription ?: "No description"

            // Notes (show only if present)
            if (maintenanceLog.maintenanceNotes?.isNotBlank() == true) {
                labelNotes.isVisible = true
                textViewNotes.isVisible = true
                textViewNotes.text = maintenanceLog.maintenanceNotes
            } else {
                labelNotes.isVisible = false
                textViewNotes.isVisible = false
            }

            // Show annotations card if there's an annotation folder
            cardAnnotations.isVisible = maintenanceLog.annotationFolder != null
        }
    }

    private fun displayAnnotations(annotations: List<Annotation>) {
        annotationAdapter.submitList(annotations)
        binding.textViewNoAnnotations.isVisible = annotations.isEmpty()
        binding.recyclerViewAnnotations.isVisible = annotations.isNotEmpty()
    }

    private fun showUpdateStatusDialog() {
        val currentLog = currentMaintenanceLog ?: return

        val statusOptions = listOf(
            MaintenanceConstants.Statuses.PENDING to "Pending",
            MaintenanceConstants.Statuses.IN_PROGRESS to "In Progress",
            MaintenanceConstants.Statuses.COMPLETED to "Completed",
            MaintenanceConstants.Statuses.REQUESTED to "Requested",
            MaintenanceConstants.Statuses.CANCELLED to "Cancelled"
        )

        val statusNames = statusOptions.map { it.second }.toTypedArray()
        val currentIndex = statusOptions.indexOfFirst { it.first == currentLog.status }

        AlertDialog.Builder(requireContext())
            .setTitle("Update Status")
            .setSingleChoiceItems(statusNames, currentIndex) { dialog, which ->
                val newStatus = statusOptions[which].first
                viewModel.updateMaintenanceLogStatus(newStatus)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditMaintenanceLogDialog() {
        val currentLog = currentMaintenanceLog ?: return
        val dialogBinding = DialogCreateMaintenanceLogBinding.inflate(layoutInflater)

        // Pre-populate fields
        dialogBinding.editTextMaintenanceDate.setText(formatDate(currentLog.maintenanceDate))
        dialogBinding.editTextDescription.setText(currentLog.maintenanceDescription)
        dialogBinding.editTextNotes.setText(currentLog.maintenanceNotes)
        dialogBinding.checkBoxTemplate.isChecked = currentLog.isTemplate

        // Setup dropdowns
        setupMaintenanceTypeDropdown(dialogBinding.autoCompleteMaintenanceType, currentLog.maintenanceType)
        setupStatusDropdown(dialogBinding.autoCompleteStatus, currentLog.status)

        // Setup date picker
        dialogBinding.editTextMaintenanceDate.setOnClickListener {
            showDatePicker { selectedDate ->
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                dialogBinding.editTextMaintenanceDate.setText(dateFormat.format(selectedDate))
            }
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Edit Maintenance Log")
            .setView(dialogBinding.root)
            .setPositiveButton("Update", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val updateButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            updateButton.setOnClickListener {
                updateMaintenanceLog(dialogBinding, dialog)
            }
        }

        dialog.show()
    }

    private fun setupMaintenanceTypeDropdown(dropdown: MaterialAutoCompleteTextView, currentType: String) {
        val typeOptions = listOf("Routine", "Emergency", "Other")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, typeOptions)
        dropdown.setAdapter(adapter)
        dropdown.setText(getTypeDisplayName(currentType), false)
    }

    private fun setupStatusDropdown(dropdown: MaterialAutoCompleteTextView, currentStatus: String) {
        val statusOptions = listOf("Pending", "In Progress", "Completed", "Requested", "Cancelled")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, statusOptions)
        dropdown.setAdapter(adapter)
        dropdown.setText(getStatusDisplayName(currentStatus), false)
    }

    private fun updateMaintenanceLog(dialogBinding: DialogCreateMaintenanceLogBinding, dialog: AlertDialog) {
        val currentLog = currentMaintenanceLog ?: return

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
            instrumentId = currentLog.instrumentId,
            maintenanceDate = "${maintenanceDate}T00:00:00",
            maintenanceType = maintenanceType,
            maintenanceDescription = description,
            maintenanceNotes = notes.ifBlank { null },
            status = status,
            isTemplate = isTemplate
        )

        viewModel.updateMaintenanceLog(request)
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

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Maintenance Log")
            .setMessage("Are you sure you want to delete this maintenance log? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteMaintenanceLog()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteAnnotationConfirmationDialog(annotation: Annotation) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Attachment")
            .setMessage("Are you sure you want to delete this attachment?")
            .setPositiveButton("Delete") { _, _ ->
                // TODO: Implement annotation deletion
                Toast.makeText(requireContext(), "Delete annotation functionality coming soon", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openAnnotation(annotation: Annotation) {
        when {
            annotation.annotation?.isNotBlank() == true -> {
                // Show text annotation in dialog
                AlertDialog.Builder(requireContext())
                    .setTitle(annotation.annotationName ?: "Text Note")
                    .setMessage(annotation.annotation)
                    .setPositiveButton("Close", null)
                    .show()
            }
            annotation.file?.isNotBlank() == true -> {
                // Try to open file with external app
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse(annotation.file)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Unable to open file", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                Toast.makeText(requireContext(), "No content available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getStatusDisplayName(status: String): String {
        return when (status) {
            MaintenanceConstants.Statuses.PENDING -> "Pending"
            MaintenanceConstants.Statuses.IN_PROGRESS -> "In Progress"
            MaintenanceConstants.Statuses.COMPLETED -> "Completed"
            MaintenanceConstants.Statuses.REQUESTED -> "Requested"
            MaintenanceConstants.Statuses.CANCELLED -> "Cancelled"
            else -> status.replaceFirstChar { it.uppercase() }
        }
    }

    private fun getTypeDisplayName(type: String): String {
        return when (type) {
            MaintenanceConstants.Types.ROUTINE -> "Routine"
            MaintenanceConstants.Types.EMERGENCY -> "Emergency"
            MaintenanceConstants.Types.OTHER -> "Other"
            else -> type.replaceFirstChar { it.uppercase() }
        }
    }

    private fun getStatusColor(status: String): Int {
        return when (status) {
            MaintenanceConstants.Statuses.PENDING -> R.color.status_pending
            MaintenanceConstants.Statuses.IN_PROGRESS -> R.color.status_in_progress
            MaintenanceConstants.Statuses.COMPLETED -> R.color.status_completed
            MaintenanceConstants.Statuses.REQUESTED -> R.color.status_requested
            MaintenanceConstants.Statuses.CANCELLED -> R.color.status_cancelled
            else -> R.color.status_pending
        }
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            date?.let { outputFormat.format(it) } ?: dateString
        } catch (e: Exception) {
            dateString.split("T").firstOrNull() ?: dateString
        }
    }

    private fun formatDateTime(dateString: String?): String {
        if (dateString.isNullOrBlank()) return "Unknown"
        
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            date?.let { outputFormat.format(it) } ?: dateString
        } catch (e: Exception) {
            dateString
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
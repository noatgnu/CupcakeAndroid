package info.proteo.cupcake.ui.labgroup

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import info.proteo.cupcake.databinding.DialogCreateEditLabGroupBinding
import info.proteo.cupcake.shared.data.model.user.LabGroup

class CreateEditLabGroupDialog : DialogFragment() {

    private var _binding: DialogCreateEditLabGroupBinding? = null
    private val binding get() = _binding!!

    private var labGroup: LabGroup? = null
    private var onResult: ((String, String, Boolean, Int?) -> Unit)? = null
    private var selectedStorageId: Int? = null

    companion object {
        fun newInstance(
            labGroup: LabGroup? = null,
            onResult: (String, String, Boolean, Int?) -> Unit
        ): CreateEditLabGroupDialog {
            return CreateEditLabGroupDialog().apply {
                this.labGroup = labGroup
                this.onResult = onResult
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCreateEditLabGroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupClickListeners()
        populateFields()
    }

    private fun setupUI() {
        // Set dialog title
        binding.tvDialogTitle.text = if (labGroup == null) "Create Lab Group" else "Edit Lab Group"
        binding.btnSave.text = if (labGroup == null) "Create" else "Update"

        // Professional switch listener
        binding.switchProfessional.setOnCheckedChangeListener { _, isChecked ->
            binding.cardServiceStorage.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) {
                selectedStorageId = null
                binding.tvSelectedStorage.visibility = View.GONE
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            saveLabGroup()
        }

        binding.btnSelectStorage.setOnClickListener {
            selectStorageObject()
        }
    }

    private fun populateFields() {
        labGroup?.let { group ->
            binding.etLabGroupName.setText(group.name)
            binding.etLabGroupDescription.setText(group.description ?: "")
            binding.switchProfessional.isChecked = group.isProfessional
            
            if (group.isProfessional) {
                binding.cardServiceStorage.visibility = View.VISIBLE
                group.serviceStorage?.let { storage ->
                    selectedStorageId = storage.id
                    binding.tvSelectedStorage.text = "${storage.objectName} (${storage.objectType})"
                    binding.tvSelectedStorage.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun saveLabGroup() {
        val name = binding.etLabGroupName.text.toString().trim()
        val description = binding.etLabGroupDescription.text.toString().trim()
        val isProfessional = binding.switchProfessional.isChecked

        // Validation
        if (name.isEmpty()) {
            binding.etLabGroupName.error = "Lab group name is required"
            return
        }

        if (isProfessional && selectedStorageId == null) {
            Toast.makeText(requireContext(), "Service storage object is required for professional lab groups", Toast.LENGTH_LONG).show()
            return
        }

        // Call result callback
        onResult?.invoke(name, description, isProfessional, selectedStorageId)
        dismiss()
    }

    private fun selectStorageObject() {
        // TODO: Implement storage object selection dialog
        // For now, show a placeholder message
        Toast.makeText(requireContext(), "Storage object selection not yet implemented", Toast.LENGTH_SHORT).show()
        
        // Placeholder: simulate selecting a storage object
        selectedStorageId = 1
        binding.tvSelectedStorage.text = "Default Storage (Container)"
        binding.tvSelectedStorage.visibility = View.VISIBLE
    }

    override fun onStart() {
        super.onStart()
        // Make dialog width match parent with some margin
        val width = (resources.displayMetrics.widthPixels * 0.9).toInt()
        dialog?.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
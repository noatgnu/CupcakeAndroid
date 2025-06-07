package info.proteo.cupcake.ui.contact

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
// import androidx.fragment.app.setFragmentResult // No longer needed
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.R
import info.proteo.cupcake.databinding.DialogAddEditContactBinding

@AndroidEntryPoint
class AddEditContactDialogFragment : DialogFragment() {

    private var _binding: DialogAddEditContactBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddEditContactDialogViewModel by viewModels()
    private val parentViewModel: ExternalContactViewModel by viewModels({ requireParentFragment() })


    private var supportInfoIdArg: Int = -1
    private var contactIdToEditArg: Int? = null
    private lateinit var roleArg: String
    private var isEditingModeArg: Boolean = false

    private lateinit var detailsAdapter: ContactDetailsAdapter

    companion object {
        const val TAG = "AddEditContactDialog"
        private const val ARG_SUPPORT_INFO_ID = "arg_support_info_id"
        private const val ARG_CONTACT_ID = "arg_contact_id"
        private const val ARG_ROLE = "arg_role"
        private const val ARG_IS_EDITING = "arg_is_editing"

        // REQUEST_KEY_SAVE_CONTACT_RESULT and BUNDLE_KEYs are no longer needed here
        // as we directly call parentViewModel.loadSupportInformation(...)

        fun newInstance(
            supportInfoId: Int,
            contactId: Int?,
            role: String,
            isEditing: Boolean
        ): AddEditContactDialogFragment {
            return AddEditContactDialogFragment().apply {
                arguments = bundleOf(
                    ARG_SUPPORT_INFO_ID to supportInfoId,
                    ARG_CONTACT_ID to contactId, // Will be null if contactId is null
                    ARG_ROLE to role,
                    ARG_IS_EDITING to isEditing
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialogStyle)

        arguments?.let {
            supportInfoIdArg = it.getInt(ARG_SUPPORT_INFO_ID)
            contactIdToEditArg = if (it.containsKey(ARG_CONTACT_ID) && it.get(ARG_CONTACT_ID) is Int) {
                it.getInt(ARG_CONTACT_ID)
            } else {
                null
            }
            roleArg = it.getString(ARG_ROLE) ?: ""
            isEditingModeArg = it.getBoolean(ARG_IS_EDITING, false)
        }
        if (roleArg.isEmpty() || supportInfoIdArg == -1) {
            Log.e(TAG, "Essential arguments missing. Role: $roleArg, SupportInfoID: $supportInfoIdArg")
            Toast.makeText(context, "Error: Missing required information.", Toast.LENGTH_LONG).show()
            dismiss()
            return
        }
        viewModel.initialize(supportInfoIdArg, contactIdToEditArg, roleArg, isEditingModeArg)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAddEditContactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupDetailsRecyclerView()
        setupBindings()
        observeViewModel()

        binding.buttonAddDetail.setOnClickListener {
            viewModel.addContactDetail()
        }
    }

    private fun setupToolbar() {
        binding.toolbarAddEditContact.setNavigationOnClickListener { dismiss() }
        binding.toolbarAddEditContact.title = if (isEditingModeArg) getString(R.string.title_edit_contact) else getString(R.string.title_add_contact)
        binding.toolbarAddEditContact.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_save_contact -> {
                    collectDataAndSave()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupBindings() {
        binding.editTextContactName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.updateContactName(s.toString())
                // Clear error when user types
                if (binding.textInputLayoutContactName.error != null) {
                    binding.textInputLayoutContactName.error = null
                }
            }
        })
    }

    private fun setupDetailsRecyclerView() {
        detailsAdapter = ContactDetailsAdapter(
            onRemoveClick = { position ->
                viewModel.removeContactDetail(position)
            },
            onDetailChange = { position, updatedDetail ->
                viewModel.updateContactDetail(position, updatedDetail)
            }
        )
        binding.recyclerViewContactDetails.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = detailsAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.contactName.observe(viewLifecycleOwner) { name ->
            if (binding.editTextContactName.text.toString() != name) {
                binding.editTextContactName.setText(name)
            }
        }

        viewModel.contactDetails.observe(viewLifecycleOwner) { details ->
            detailsAdapter.submitList(details)
            binding.textViewNoDetails.isVisible = details.isEmpty()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBarAddEditContact.isVisible = isLoading
            binding.toolbarAddEditContact.menu.findItem(R.id.action_save_contact)?.isEnabled = !isLoading
        }

        viewModel.operationResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                it.fold(
                    onSuccess = { message ->
                        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                        if (supportInfoIdArg != -1) {
                            parentViewModel.loadSupportInformation(supportInfoIdArg)
                            Log.d(TAG, "Successfully saved. Requested parent refresh for supportInfoId: $supportInfoIdArg")
                        } else {
                            Log.e(TAG, "Save successful but cannot refresh parent: supportInfoIdArg is invalid (-1).")
                            Snackbar.make(binding.root, "Save successful, but list refresh might be delayed.", Snackbar.LENGTH_LONG).show()
                        }
                        viewModel.clearOperationResult()
                        dismiss()
                    },
                    onFailure = { error ->
                        Snackbar.make(binding.root, "Error: ${error.message}", Snackbar.LENGTH_LONG).show()
                        viewModel.clearOperationResult()
                    }
                )
            }
        }
    }

    private fun collectDataAndSave() {
        binding.root.requestFocus()

        (binding.recyclerViewContactDetails.adapter as? ContactDetailsAdapter)?.let { adapter ->
            adapter.commitAllEdits(binding.recyclerViewContactDetails)
        }

        val contactName = binding.editTextContactName.text.toString().trim()
        if (contactName.isEmpty()) {
            binding.textInputLayoutContactName.error = getString(R.string.error_contact_name_empty)
            return
        } else {
            binding.textInputLayoutContactName.error = null
        }

        viewModel.saveContact(contactName)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
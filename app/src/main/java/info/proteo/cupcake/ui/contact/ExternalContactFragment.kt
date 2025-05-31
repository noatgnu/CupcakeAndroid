package info.proteo.cupcake.ui.contact

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast.LENGTH_LONG
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.R
import info.proteo.cupcake.databinding.FragmentExternalContactBinding

@AndroidEntryPoint
class ExternalContactFragment : Fragment() {
    private var _binding: FragmentExternalContactBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ExternalContactViewModel by viewModels()
    private var supportInfoId: Int = -1
    private lateinit var pagerAdapter: ExternalContactPagerAdapter
    private val TAG = "ExternalContactFragment"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { supportInfoId = it.getInt(ARG_SUPPORT_INFO_ID, -1) }
        if (supportInfoId == -1) Log.e(TAG, "Missing supportInfoId")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExternalContactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupViewPagerAndTabs()
        observeViewModel()
        if (supportInfoId != -1) viewModel.loadSupportInformation(supportInfoId)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }

    private fun setupViewPagerAndTabs() {
        pagerAdapter = ExternalContactPagerAdapter(this, supportInfoId)
        binding.viewPager.adapter = pagerAdapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, pos ->
            tab.text = if (pos == 0) getString(R.string.tab_vendor_contacts)
            else getString(R.string.tab_manufacturer_contacts)
        }.attach()
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }

        viewModel.supportInfo.observe(viewLifecycleOwner) { result ->
            // This observer already exists implicitly in ContactListFragment,
            // but we can add it here to log or handle any specific fragment-level UI updates
            result.fold(
                onSuccess = {
                    Log.d(TAG, "Support info loaded successfully: ${it.id}")
                    // You could update fragment-level UI elements here if needed
                },
                onFailure = { error ->
                    Log.e(TAG, "Error loading support info: ${error.message}")
                    Snackbar.make(binding.root,
                        "Error loading contact information: ${error.message}",
                        Snackbar.LENGTH_LONG).show()
                }
            )
        }

        viewModel.contactOperationStatus.observe(viewLifecycleOwner) { result ->
            result?.fold(
                onSuccess = { msg ->
                    Snackbar.make(binding.root, msg, LENGTH_LONG).show()
                },
                onFailure = { e ->
                    Snackbar.make(binding.root, "Error: ${e.message}", LENGTH_LONG).show()
                }
            )
            viewModel.clearContactOperationStatus()
        }
    }

    fun handleAddContact(role: String) {
        Log.d(TAG, "Adding $role contact")
        showAddEditContactDialog(null, role, false)
    }

    fun handleEditContact(id: Int, role: String) {
        Log.d(TAG, "Editing contact #$id ($role)")
        showAddEditContactDialog(id, role, true)
    }

    fun handleRemoveContact(id: Int, role: String) {
        Log.d(TAG, "Removing contact #$id ($role)")
        if (!isAdded || context == null) {
            Log.e(TAG, "Fragment not attached, cannot show remove dialog.")
            return
        }

        // Check if already processing a removal
        if (viewModel.isLoading.value == true) {
            Log.d(TAG, "Already processing a request, ignoring additional removal")
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_title_remove_contact)
            .setMessage(R.string.dialog_message_remove_contact_confirmation)
            .setPositiveButton(R.string.action_remove) { dialog, _ ->
                if (supportInfoId == -1) {
                    Snackbar.make(binding.root, "Error: Missing support information ID.", Snackbar.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                (dialog as? AlertDialog)?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
                (dialog as? AlertDialog)?.getButton(AlertDialog.BUTTON_NEGATIVE)?.isEnabled = false

                Snackbar.make(binding.root, "Removing contact...", Snackbar.LENGTH_SHORT).show()

                val handler = android.os.Handler(android.os.Looper.getMainLooper())
                handler.postDelayed({
                    if (viewModel.isLoading.value == true) {
                        dialog.dismiss()
                        Snackbar.make(binding.root, "Operation is taking longer than expected.", Snackbar.LENGTH_LONG).show()
                    }
                }, 5000)

                viewModel.removeContactLink(supportInfoId, id, role)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showAddEditContactDialog(
        contactId: Int?,
        role: String,
        isEditing: Boolean
    ) {
        if (supportInfoId == -1) {
            Snackbar.make(binding.root,
                "Cannot add/edit contact: missing SupportInfo ID.",
                LENGTH_LONG).show()
            return
        }

        AddEditContactDialogFragment.newInstance(
            supportInfoId, contactId, role, isEditing
        ).show(childFragmentManager, AddEditContactDialogFragment.TAG)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_SUPPORT_INFO_ID = "arg_support_info_id"

        fun newInstance(supportInfoId: Int) = ExternalContactFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_SUPPORT_INFO_ID, supportInfoId)
            }
        }
    }
}
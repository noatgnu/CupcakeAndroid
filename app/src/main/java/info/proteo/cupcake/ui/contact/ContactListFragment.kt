package info.proteo.cupcake.ui.contact

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import info.proteo.cupcake.databinding.FragmentContactListBinding

class ContactListFragment : Fragment() {

    private var _binding: FragmentContactListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExternalContactViewModel by viewModels({requireParentFragment()})
    private lateinit var contactAdapter: ExternalContactAdapter

    private var supportInfoId: Int = -1
    private lateinit var contactType: String // "vendor" or "manufacturer"
    private val TAG = "ContactListFragment"

    companion object {
        const val ARG_SUPPORT_INFO_ID = "support_info_id"
        const val ARG_CONTACT_TYPE = "contact_type"

        fun newInstance(supportInfoId: Int, contactType: String): ContactListFragment {
            return ContactListFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SUPPORT_INFO_ID, supportInfoId)
                    putString(ARG_CONTACT_TYPE, contactType)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            supportInfoId = it.getInt(ARG_SUPPORT_INFO_ID)
            contactType = it.getString(ARG_CONTACT_TYPE) ?: ""
        }
        if (contactType.isEmpty()) {
            Log.e(TAG, "ContactType is essential and was not provided.")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContactListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupAddButton()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        contactAdapter = ExternalContactAdapter(
            onRemoveClick = { contactId ->
                findExternalContactParent()?.handleRemoveContact(contactId, contactType)
            },
            onEditClick = { contact ->
                contact.id?.let { findExternalContactParent()?.handleEditContact(it, contactType) }
            }
        )
        binding.recyclerViewSpecificContacts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = contactAdapter
        }
    }

    private fun setupAddButton() {
        binding.buttonAddSpecificContact.setOnClickListener {
            Log.d(TAG, "Add button clicked, supportInfoId=$supportInfoId, contactType=$contactType")
            findExternalContactParent()?.handleAddContact(contactType)
        }
    }

    private fun findExternalContactParent(): ExternalContactFragment? {
        var currentFragment: Fragment? = this
        while (currentFragment != null) {
            currentFragment = currentFragment.parentFragment
            if (currentFragment is ExternalContactFragment) {
                return currentFragment
            }
        }
        Log.e(TAG, "Parent ExternalContactFragment not found")
        Snackbar.make(binding.root, "Error: Could not find parent fragment", Snackbar.LENGTH_LONG).show()
        return null
    }

    private fun observeViewModel() {
        viewModel.supportInfo.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = { supportInformation ->
                    val contacts = if (contactType.equals("vendor", ignoreCase = true)) {
                        supportInformation.vendorContacts
                    } else {
                        supportInformation.manufacturerContacts
                    } ?: emptyList()
                    contactAdapter.submitList(contacts)
                    binding.emptyViewSpecific.isVisible = contacts.isEmpty()
                    binding.recyclerViewSpecificContacts.isVisible = contacts.isNotEmpty()
                },
                onFailure = {
                    contactAdapter.submitList(emptyList())
                    binding.emptyViewSpecific.isVisible = true
                    binding.recyclerViewSpecificContacts.isVisible = false
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
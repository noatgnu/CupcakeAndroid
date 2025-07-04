package info.proteo.cupcake.ui.labgroup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.R
import info.proteo.cupcake.databinding.FragmentLabGroupDetailBinding
import info.proteo.cupcake.shared.data.model.user.LabGroup
import info.proteo.cupcake.ui.labgroup.adapter.LabGroupMembersPagerAdapter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class LabGroupDetailFragment : Fragment() {

    private var _binding: FragmentLabGroupDetailBinding? = null
    private val binding get() = _binding!!

    private var labGroupId: Int = -1
    private val viewModel: LabGroupDetailViewModel by viewModels()
    private lateinit var membersPagerAdapter: LabGroupMembersPagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLabGroupDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get lab group ID from arguments
        labGroupId = arguments?.getInt("labGroupId", -1) ?: -1
        
        if (labGroupId == -1) {
            // Handle error - maybe show error state or finish activity
            return
        }

        setupMembersPager()
        observeUiState()
        loadLabGroupDetail()
    }

    private fun setupMembersPager() {
        membersPagerAdapter = LabGroupMembersPagerAdapter(this)
        binding.viewPagerMembers.adapter = membersPagerAdapter

        TabLayoutMediator(binding.tabLayoutMembers, binding.viewPagerMembers) { tab, position ->
            when (position) {
                0 -> tab.text = "All Members"
                1 -> tab.text = "Managers"
            }
        }.attach()
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUi(state)
                }
            }
        }
    }

    private fun updateUi(state: LabGroupDetailUiState) {
        // Loading state
        binding.progressLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE

        // Lab group details
        state.labGroup?.let { labGroup ->
            updateLabGroupHeader(labGroup)
            updateStorageInformation(labGroup)
        }

        // Members
        updateMembersSection(state)

        // Error handling
        state.error?.let { error ->
            android.util.Log.e("LabGroupDetail", "Lab group error: $error")
            // Show error toast
            android.widget.Toast.makeText(context, "Error: $error", android.widget.Toast.LENGTH_LONG).show()
        }
        
        state.membersError?.let { error ->
            android.util.Log.e("LabGroupDetail", "Members error: $error")
            // Show error toast
            android.widget.Toast.makeText(context, "Members error: $error", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun updateLabGroupHeader(labGroup: LabGroup) {
        binding.tvLabGroupName.text = labGroup.name
        binding.tvLabGroupId.text = "ID: ${labGroup.id}"

        // Professional badge
        binding.chipProfessional.visibility = if (labGroup.isProfessional) View.VISIBLE else View.GONE

        // Description
        if (!labGroup.description.isNullOrBlank()) {
            binding.tvLabGroupDescription.text = labGroup.description
            binding.tvLabGroupDescription.visibility = View.VISIBLE
        } else {
            binding.tvLabGroupDescription.visibility = View.GONE
        }

        // Dates
        binding.tvCreatedDate.text = formatDate("Created: ", labGroup.createdAt)
        binding.tvUpdatedDate.text = formatDate("Last updated: ", labGroup.updatedAt)
    }

    private fun updateStorageInformation(labGroup: LabGroup) {
        val hasStorageInfo = labGroup.defaultStorage != null || labGroup.serviceStorage != null
        binding.cardStorageInfo.visibility = if (hasStorageInfo) View.VISIBLE else View.GONE

        if (hasStorageInfo) {
            // Default storage
            if (labGroup.defaultStorage != null) {
                binding.layoutDefaultStorage.visibility = View.VISIBLE
                binding.tvDefaultStorage.text = "${labGroup.defaultStorage!!.objectName} (${labGroup.defaultStorage!!.objectType})"
            } else {
                binding.layoutDefaultStorage.visibility = View.GONE
            }

            // Service storage
            if (labGroup.serviceStorage != null) {
                binding.layoutServiceStorage.visibility = View.VISIBLE
                binding.tvServiceStorage.text = "${labGroup.serviceStorage!!.objectName} (${labGroup.serviceStorage!!.objectType})"
            } else {
                binding.layoutServiceStorage.visibility = View.GONE
            }

            // Storage missing warning for professional groups
            val showWarning = labGroup.isProfessional && labGroup.serviceStorage == null
            binding.layoutStorageMissing.visibility = if (showWarning) View.VISIBLE else View.GONE
        }
    }

    private fun updateMembersSection(state: LabGroupDetailUiState) {
        android.util.Log.d("LabGroupDetail", "updateMembersSection - members: ${state.allMembers.size}, managers: ${state.managers.size}, loading: ${state.isMembersLoading}")
        
        // Members loading state
        binding.loadingMembersState.visibility = if (state.isMembersLoading) View.VISIBLE else View.GONE
        binding.viewPagerMembers.visibility = if (state.isMembersLoading) View.GONE else View.VISIBLE

        // Member count
        binding.tvMemberCount.text = state.allMembers.size.toString()

        // Empty state
        val isEmpty = state.allMembers.isEmpty() && !state.isMembersLoading
        binding.emptyMembersState.visibility = if (isEmpty) View.VISIBLE else View.GONE

        // Update pager adapter
        membersPagerAdapter.updateMembers(state.allMembers, state.managers)
    }

    private fun formatDate(prefix: String, dateString: String?): String {
        return try {
            if (dateString.isNullOrBlank()) return ""
            
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.US)
            val outputFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.US)
            val date = inputFormat.parse(dateString)
            "$prefix${outputFormat.format(date ?: Date())}"
        } catch (e: Exception) {
            "$prefix$dateString"
        }
    }

    private fun loadLabGroupDetail() {
        viewModel.loadLabGroupDetail(labGroupId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
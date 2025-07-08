package info.proteo.cupcake.ui.labgroup.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import info.proteo.cupcake.databinding.FragmentLabGroupMembersListBinding
import info.proteo.cupcake.shared.data.model.user.User
import info.proteo.cupcake.ui.labgroup.LabGroupDetailActivity
import info.proteo.cupcake.ui.labgroup.adapter.LabGroupMemberAdapter

class LabGroupMembersListFragment : Fragment() {

    private var _binding: FragmentLabGroupMembersListBinding? = null
    private val binding get() = _binding!!

    private lateinit var memberAdapter: LabGroupMemberAdapter
    private var members: List<User> = emptyList()
    private var managers: List<User> = emptyList()
    private var listType: String = ""

    companion object {
        fun newInstance(members: List<User>, managers: List<User>, listType: String): LabGroupMembersListFragment {
            val fragment = LabGroupMembersListFragment()
            fragment.members = members
            fragment.managers = managers
            fragment.listType = listType
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLabGroupMembersListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        updateMembers()
    }

    private fun setupRecyclerView() {
        val activity = requireActivity()
        val canManage = if (activity is LabGroupDetailActivity) {
            activity.canUserManageGroup()
        } else false

        memberAdapter = LabGroupMemberAdapter(
            onMemberClick = { user ->
                // Just show basic member info or do nothing
            },
            onRemoveMember = { user ->
                showRemoveConfirmation(user)
            },
            onAddManager = { user ->
                showAddManagerConfirmation(user)
            },
            onRemoveManager = { user ->
                showRemoveManagerConfirmation(user)
            },
            canManageMembers = canManage,
            managers = managers
        )

        binding.recyclerViewMembers.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = memberAdapter
        }
    }

    private fun updateMembers() {
        memberAdapter.submitList(members)
        
        // Show/hide empty state
        binding.emptyState.visibility = if (members.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerViewMembers.visibility = if (members.isEmpty()) View.GONE else View.VISIBLE
    }

    fun updateMembers(newMembers: List<User>, newManagers: List<User> = managers) {
        android.util.Log.d("LabGroupMembersListFragment", "updateMembers called - type: $listType, members: ${newMembers.size}, managers: ${newManagers.size}")
        this.members = newMembers
        this.managers = newManagers
        if (::memberAdapter.isInitialized) {
            android.util.Log.d("LabGroupMembersListFragment", "Updating adapter - type: $listType")
            memberAdapter.updateManagers(newManagers)
            memberAdapter.submitList(newMembers.toList()) // Create new list to force DiffUtil update
            memberAdapter.notifyDataSetChanged() // Force full reload
            
            // Update permissions
            val activity = requireActivity()
            val canManage = if (activity is LabGroupDetailActivity) {
                activity.canUserManageGroup()
            } else false
            memberAdapter.updateManagementPermissions(canManage)
            
            // Update empty state
            binding.emptyState.visibility = if (newMembers.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerViewMembers.visibility = if (newMembers.isEmpty()) View.GONE else View.VISIBLE
            android.util.Log.d("LabGroupMembersListFragment", "Adapter update completed - type: $listType")
        }
    }

    private fun showRemoveConfirmation(user: User) {
        // Get the parent activity to handle member removal
        val activity = requireActivity()
        if (activity is LabGroupDetailActivity) {
            activity.removeMemberFromGroup(user)
        }
    }

    private fun showAddManagerConfirmation(user: User) {
        // Get the parent activity to handle adding manager
        val activity = requireActivity()
        if (activity is LabGroupDetailActivity) {
            activity.addManagerToGroup(user)
        }
    }

    private fun showRemoveManagerConfirmation(user: User) {
        // Get the parent activity to handle removing manager
        val activity = requireActivity()
        if (activity is LabGroupDetailActivity) {
            activity.removeManagerFromGroup(user)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
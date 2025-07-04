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
    private var listType: String = ""

    companion object {
        fun newInstance(members: List<User>, listType: String): LabGroupMembersListFragment {
            val fragment = LabGroupMembersListFragment()
            fragment.members = members
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
            canManageMembers = canManage
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

    fun updateMembers(newMembers: List<User>) {
        this.members = newMembers
        if (::memberAdapter.isInitialized) {
            updateMembers()
            // Also update permissions in case they changed
            val activity = requireActivity()
            val canManage = if (activity is LabGroupDetailActivity) {
                activity.canUserManageGroup()
            } else false
            memberAdapter.updateManagementPermissions(canManage)
        }
    }

    private fun showRemoveConfirmation(user: User) {
        // Get the parent activity to handle member removal
        val activity = requireActivity()
        if (activity is LabGroupDetailActivity) {
            activity.removeMemberFromGroup(user)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
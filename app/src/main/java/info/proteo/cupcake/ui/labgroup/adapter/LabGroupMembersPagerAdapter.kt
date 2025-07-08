package info.proteo.cupcake.ui.labgroup.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import info.proteo.cupcake.shared.data.model.user.User
import info.proteo.cupcake.ui.labgroup.fragment.LabGroupMembersListFragment

class LabGroupMembersPagerAdapter(private val parentFragment: Fragment) : FragmentStateAdapter(parentFragment) {

    private var allMembers: List<User> = emptyList()
    private var managers: List<User> = emptyList()
    private var allMembersFragment: LabGroupMembersListFragment? = null
    private var managersFragment: LabGroupMembersListFragment? = null

    fun updateMembers(allMembers: List<User>, managers: List<User>) {
        this.allMembers = allMembers
        this.managers = managers
        
        // Update existing fragments if they exist
        allMembersFragment?.updateMembers(allMembers, managers)
        managersFragment?.updateMembers(managers, managers)
        
        // Force reload all fragments
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                allMembersFragment = LabGroupMembersListFragment.newInstance(allMembers, managers, "All Members")
                allMembersFragment!!
            }
            1 -> {
                managersFragment = LabGroupMembersListFragment.newInstance(managers, managers, "Managers")
                managersFragment!!
            }
            else -> throw IllegalStateException("Invalid position: $position")
        }
    }
}
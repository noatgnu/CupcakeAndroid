package info.proteo.cupcake.ui.labgroup.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.R
import info.proteo.cupcake.databinding.ItemLabGroupMemberBinding
import info.proteo.cupcake.shared.data.model.user.User

class LabGroupMemberAdapter(
    private val onMemberClick: (User) -> Unit,
    private val onRemoveMember: ((User) -> Unit)? = null,
    private val onAddManager: ((User) -> Unit)? = null,
    private val onRemoveManager: ((User) -> Unit)? = null,
    private var canManageMembers: Boolean = false,
    private var managers: List<User> = emptyList()
) : ListAdapter<User, LabGroupMemberAdapter.MemberViewHolder>(UserDiffCallback()) {

    fun updateManagementPermissions(canManage: Boolean) {
        if (this.canManageMembers != canManage) {
            this.canManageMembers = canManage
            notifyDataSetChanged()
        }
    }

    fun updateManagers(managersList: List<User>) {
        this.managers = managersList
        notifyDataSetChanged() // Always force update
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = ItemLabGroupMemberBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MemberViewHolder(
        private val binding: ItemLabGroupMemberBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            android.util.Log.d("LabGroupMemberAdapter", "bind() called for user: ${user.id} - ${user.username}")
            
            binding.apply {
                // User name
                val fullName = listOfNotNull(user.firstName, user.lastName)
                    .joinToString(" ")
                    .ifBlank { user.username }
                tvMemberName.text = fullName

                // Username
                tvMemberUsername.text = "@${user.username}"

                // Email
                tvMemberEmail.text = user.email

                // Staff badge
                chipStaff.visibility = if (user.isStaff) android.view.View.VISIBLE else android.view.View.GONE

                // Manager badge
                val isManager = managers.any { it.id == user.id }
                android.util.Log.d("LabGroupMemberAdapter", "User ${user.id} isManager: $isManager, managers: ${managers.map { it.id }}")
                chipManager?.visibility = if (isManager) android.view.View.VISIBLE else android.view.View.GONE

                // Management actions button
                if (canManageMembers) {
                    btnMemberActions.visibility = android.view.View.VISIBLE
                    btnMemberActions.setOnClickListener { view ->
                        showActionsMenu(view, user, isManager)
                    }
                } else {
                    btnMemberActions.visibility = android.view.View.GONE
                }

                // Click listener for member card
                root.setOnClickListener {
                    onMemberClick(user)
                }
            }
        }
        
        private fun showActionsMenu(view: android.view.View, user: User, isManager: Boolean) {
            val popupMenu = PopupMenu(view.context, view)
            popupMenu.menuInflater.inflate(R.menu.menu_member_actions, popupMenu.menu)
            
            // Configure menu items based on manager status
            val makeManagerItem = popupMenu.menu.findItem(R.id.action_make_manager)
            val removeManagerItem = popupMenu.menu.findItem(R.id.action_remove_manager)
            
            makeManagerItem?.isVisible = !isManager
            removeManagerItem?.isVisible = isManager
            
            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_make_manager -> {
                        onAddManager?.invoke(user)
                        true
                    }
                    R.id.action_remove_manager -> {
                        onRemoveManager?.invoke(user)
                        true
                    }
                    R.id.action_remove_from_group -> {
                        onRemoveMember?.invoke(user)
                        true
                    }
                    else -> false
                }
            }
            
            popupMenu.show()
        }
    }

    private class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}
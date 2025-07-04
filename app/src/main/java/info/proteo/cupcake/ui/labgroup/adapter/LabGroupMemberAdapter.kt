package info.proteo.cupcake.ui.labgroup.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.databinding.ItemLabGroupMemberBinding
import info.proteo.cupcake.shared.data.model.user.User

class LabGroupMemberAdapter(
    private val onMemberClick: (User) -> Unit,
    private val onRemoveMember: ((User) -> Unit)? = null,
    private var canManageMembers: Boolean = false
) : ListAdapter<User, LabGroupMemberAdapter.MemberViewHolder>(UserDiffCallback()) {

    fun updateManagementPermissions(canManage: Boolean) {
        if (this.canManageMembers != canManage) {
            this.canManageMembers = canManage
            notifyDataSetChanged()
        }
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

                // Remove button visibility and click listener
                btnRemoveMember.visibility = if (canManageMembers) android.view.View.VISIBLE else android.view.View.GONE
                btnRemoveMember.setOnClickListener {
                    onRemoveMember?.invoke(user)
                }

                // Click listener for member card
                root.setOnClickListener {
                    onMemberClick(user)
                }
            }
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
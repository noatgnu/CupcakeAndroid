package info.proteo.cupcake.ui.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.data.local.entity.user.UserPreferencesEntity
import info.proteo.cupcake.databinding.ItemUserProfileBinding

class UserProfileAdapter(
    private val onProfileSelected: (userId: String, hostname: String) -> Unit
) : ListAdapter<UserPreferencesEntity, UserProfileAdapter.ViewHolder>(ProfileDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUserProfileBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemUserProfileBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(profile: UserPreferencesEntity) {
            binding.userName.text = profile.userId
            binding.hostname.text = profile.hostname
            binding.activeIndicator.isVisible = profile.isActive

            binding.root.setOnClickListener {
                onProfileSelected(profile.userId, profile.hostname)
            }
        }
    }

    class ProfileDiffCallback : DiffUtil.ItemCallback<UserPreferencesEntity>() {
        override fun areItemsTheSame(oldItem: UserPreferencesEntity, newItem: UserPreferencesEntity): Boolean {
            return oldItem.userId == newItem.userId && oldItem.hostname == newItem.hostname
        }

        override fun areContentsTheSame(oldItem: UserPreferencesEntity, newItem: UserPreferencesEntity): Boolean {
            return oldItem == newItem
        }
    }
}
package info.proteo.cupcake.ui.labgroup.adapter

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.databinding.ItemUserSelectionBinding
import info.proteo.cupcake.shared.data.model.user.User
import java.util.Locale

class UserSelectionAdapter(
    private val onUserClick: (User) -> Unit
) : ListAdapter<User, UserSelectionAdapter.UserViewHolder>(UserDiffCallback()) {
    
    private var searchQuery: String = ""
    
    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserSelectionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserViewHolder(
        private val binding: ItemUserSelectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.apply {
                // User name with highlighting
                val fullName = listOfNotNull(user.firstName, user.lastName)
                    .joinToString(" ")
                    .ifBlank { user.username }
                tvUserName.text = highlightText(fullName, searchQuery)

                // Username with highlighting
                val username = "@${user.username}"
                tvUsername.text = highlightText(username, searchQuery)

                // Email with highlighting
                val email = user.email ?: "No email"
                tvUserEmail.text = highlightText(email, searchQuery)

                // Staff badge
                chipStaff.visibility = if (user.isStaff) android.view.View.VISIBLE else android.view.View.GONE

                // Click listener
                root.setOnClickListener {
                    onUserClick(user)
                }
            }
        }
        
        private fun highlightText(text: String, query: String): CharSequence {
            if (query.isBlank() || query.length < 2) {
                return text
            }
            
            val spannable = SpannableString(text)
            val startIndex = text.lowercase(Locale.getDefault()).indexOf(query.lowercase(Locale.getDefault()))
            
            if (startIndex >= 0) {
                spannable.setSpan(
                    StyleSpan(Typeface.BOLD),
                    startIndex,
                    startIndex + query.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            
            return spannable
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
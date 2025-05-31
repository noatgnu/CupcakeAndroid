package info.proteo.cupcake.ui.user

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.R
import info.proteo.cupcake.data.remote.model.user.UserBasic

class UserSearchAdapter(
    private val onUserSelected: (UserBasic) -> Unit
) : ListAdapter<UserBasic, UserSearchAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_search_result, parent, false)
        return UserViewHolder(view, onUserSelected)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class UserViewHolder(
        itemView: View,
        private val onUserSelected: (UserBasic) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val textViewUsername: TextView = itemView.findViewById(R.id.textViewUsername)
        private val textViewFullName: TextView = itemView.findViewById(R.id.textViewFullName)

        fun bind(user: UserBasic) {
            textViewUsername.text = user.username
            textViewFullName.text = "${user.firstName} ${user.lastName}"

            itemView.setOnClickListener {
                onUserSelected(user)
            }
        }
    }

    private class UserDiffCallback : DiffUtil.ItemCallback<UserBasic>() {
        override fun areItemsTheSame(oldItem: UserBasic, newItem: UserBasic): Boolean {
            return oldItem.username == newItem.username
        }

        override fun areContentsTheSame(oldItem: UserBasic, newItem: UserBasic): Boolean {
            return oldItem == newItem
        }
    }
}
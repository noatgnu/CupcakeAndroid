package info.proteo.cupcake.ui.protocol

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.R
import info.proteo.cupcake.data.remote.model.user.User
import info.proteo.cupcake.data.remote.model.user.UserBasic

class ProtocolUserListSearchAdapter(
    private val users: List<User>,
    private val onRemoveClick: (User) -> Unit
) : RecyclerView.Adapter<ProtocolUserListSearchAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_with_remove, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        holder.bind(user)
    }

    override fun getItemCount() = users.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.userName)
        private val removeButton: View = itemView.findViewById(R.id.removeButton)

        fun bind(user: User) {
            val displayName = if (!user.firstName.isNullOrEmpty() || !user.lastName.isNullOrEmpty()) {
                "${user.firstName ?: ""} ${user.lastName ?: ""}"
            } else {
                user.username
            }
            nameText.text = displayName

            removeButton.setOnClickListener {
                onRemoveClick(user)
            }
        }
    }
}

class ProtocolUserSearchAdapter(
    private val users: List<UserBasic>,
    private val onAddClick: (UserBasic) -> Unit
) : RecyclerView.Adapter<ProtocolUserSearchAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_with_add, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        holder.bind(user)
    }

    override fun getItemCount() = users.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.userName)
        private val addButton: View = itemView.findViewById(R.id.addButton)

        fun bind(user: UserBasic) {
            nameText.text = user.username

            addButton.setOnClickListener {
                onAddClick(user)
            }
        }
    }
}
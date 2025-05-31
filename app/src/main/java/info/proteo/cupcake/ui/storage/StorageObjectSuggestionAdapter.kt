package info.proteo.cupcake.ui.storage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.R
import info.proteo.cupcake.data.remote.model.storage.StorageObject

class StorageObjectSuggestionAdapter(
    private val onItemSelected: (StorageObject) -> Unit
) : ListAdapter<StorageObject, StorageObjectSuggestionAdapter.ViewHolder>(StorageObjectDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_storage_object_suggestion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val storageObject = getItem(position)
        holder.bind(storageObject)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textStorageName: TextView = itemView.findViewById(R.id.textStorageName)
        private val textStoragePath: TextView = itemView.findViewById(R.id.textStoragePath)
        private val textStorageType: TextView = itemView.findViewById(R.id.textStorageType)

        fun bind(storageObject: StorageObject) {
            textStorageName.text = storageObject.objectName
            textStorageType.text = storageObject.objectType ?: "Unknown type"

            val path = if (!storageObject.pathToRoot.isNullOrEmpty()) {
                storageObject.pathToRoot.joinToString(" > ") { it.name }
            } else {
                "Root"
            }
            textStoragePath.text = path

            itemView.setOnClickListener {
                onItemSelected(storageObject)
            }
        }
    }

    private class StorageObjectDiffCallback : DiffUtil.ItemCallback<StorageObject>() {
        override fun areItemsTheSame(oldItem: StorageObject, newItem: StorageObject): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: StorageObject, newItem: StorageObject): Boolean {
            return oldItem == newItem
        }
    }
}
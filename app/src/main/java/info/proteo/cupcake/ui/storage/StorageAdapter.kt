package info.proteo.cupcake.ui.storage.adapter

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.shared.data.model.storage.StorageObject
import info.proteo.cupcake.databinding.ItemStorageObjectBinding

class StorageAdapter(
    private val onStorageObjectClicked: (StorageObject) -> Unit
) : ListAdapter<StorageObject, StorageAdapter.StorageViewHolder>(StorageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StorageViewHolder {
        val binding = ItemStorageObjectBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return StorageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StorageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class StorageViewHolder(private val binding: ItemStorageObjectBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(storageObject: StorageObject) {
            binding.apply {
                textViewName.text = storageObject.objectName
                textViewType.text = storageObject.objectType ?: ""

                if (storageObject.childCount > 0) {
                    textViewChildCount.visibility = View.VISIBLE
                    textViewChildCount.text = "${storageObject.childCount} storage objects"
                } else {
                    textViewChildCount.visibility = View.GONE
                }

                // Handle image
                if (!storageObject.pngBase64.isNullOrBlank()) {
                    try {
                        val imageBytes = android.util.Base64.decode(storageObject.pngBase64, android.util.Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        imageViewObject.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        // Fallback to default icon
                        setDefaultIcon(storageObject)
                    }
                } else {
                    setDefaultIcon(storageObject)
                }

                root.setOnClickListener { onStorageObjectClicked(storageObject) }
            }
        }

        private fun setDefaultIcon(storageObject: StorageObject) {

        }
    }

    class StorageDiffCallback : DiffUtil.ItemCallback<StorageObject>() {
        override fun areItemsTheSame(oldItem: StorageObject, newItem: StorageObject): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: StorageObject, newItem: StorageObject): Boolean {
            return oldItem == newItem
        }
    }
}
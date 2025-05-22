package info.proteo.cupcake.ui.reagent

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.data.remote.model.reagent.StoredReagent
import info.proteo.cupcake.databinding.ItemStoredReagentBinding
import java.text.SimpleDateFormat
import java.util.Locale

class StoredReagentAdapter(
    private val onItemClick: (StoredReagent) -> Unit
) : ListAdapter<StoredReagent, StoredReagentAdapter.ViewHolder>(StoredReagentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Log.d("StoredReagentAdapter", "Creating view holder")
        val binding = ItemStoredReagentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        Log.d("StoredReagentAdapter", "Binding item at position $position: $item")
        holder.bind(item)
    }

    inner class ViewHolder(private val binding: ItemStoredReagentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(storedReagent: StoredReagent) {
            binding.textViewReagentName.text = storedReagent.reagent.name
            binding.textViewQuantity.text = "${storedReagent.currentQuantity ?: 0} ${storedReagent.reagent.unit}"

            storedReagent.expirationDate?.let { expDate ->
                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                binding.textViewExpiry.text = "Expires: ${dateFormat.format(expDate)}"
            }

            if (!storedReagent.notes.isNullOrBlank()) {
                binding.textViewNotes.text = storedReagent.notes
            } else {
                binding.textViewNotes.text = "No notes"
            }

            binding.textViewAddedBy.text = "Added by: ${storedReagent.user.username}"

            storedReagent.pngBase64?.let { base64Image ->
                try {
                    val base64Content = if (base64Image.startsWith("data:image/png;base64,")) {
                        base64Image.substring("data:image/png;base64,".length)
                    } else {
                        base64Image
                    }

                    val imageBytes = Base64.decode(base64Content, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    binding.imageViewReagent.setImageBitmap(bitmap)
                    binding.imageViewReagent.visibility = View.VISIBLE
                } catch (e: Exception) {
                    binding.imageViewReagent.visibility = View.GONE
                }
            } ?: run {
                binding.imageViewReagent.visibility = View.GONE
            }
        }
    }

    private class StoredReagentDiffCallback : DiffUtil.ItemCallback<StoredReagent>() {
        override fun areItemsTheSame(oldItem: StoredReagent, newItem: StoredReagent): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: StoredReagent, newItem: StoredReagent): Boolean {
            return oldItem == newItem
        }
    }


}
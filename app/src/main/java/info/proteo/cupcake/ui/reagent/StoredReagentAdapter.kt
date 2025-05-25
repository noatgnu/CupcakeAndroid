package info.proteo.cupcake.ui.reagent

import android.graphics.BitmapFactory
import android.graphics.PorterDuff
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.R
import info.proteo.cupcake.data.remote.model.reagent.StoredReagent
import info.proteo.cupcake.databinding.ItemStoredReagentBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class StoredReagentAdapter(
    private val onItemClick: (StoredReagent) -> Unit
) : ListAdapter<StoredReagent, StoredReagentAdapter.ViewHolder>(StoredReagentDiffCallback()) {
    private var locationPaths: Map<Int, String> = mutableMapOf<Int, String>()

    fun updateLocationPaths(paths: Map<Int, String>) {
        locationPaths = paths
        notifyDataSetChanged()
    }


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
            if (storedReagent.shareable) {
                binding.imageViewShareable.visibility = View.VISIBLE
                binding.imageViewShareable.setColorFilter(
                    ContextCompat.getColor(itemView.context, R.color.success),
                    PorterDuff.Mode.SRC_IN
                )
            } else {
                binding.imageViewShareable.visibility = View.GONE
            }
            binding.textViewReagentName.text = storedReagent.reagent.name
            binding.textViewQuantity.text = buildString {
                append(storedReagent.currentQuantity)
                append(" ")
                append(storedReagent.reagent.unit)
            }

            storedReagent.storageObject.let { location ->
                val locationId = location.id
                if (locationId != null) {
                    val locationPath = locationPaths[locationId] ?: location.objectName
                    binding.textViewLocation.text = buildString {
                        append("Location: ")
                        append(locationPath)
                    }
                    binding.textViewLocation.visibility = View.VISIBLE
                } else {
                    binding.textViewLocation.text = buildString {
                        append("Location: ")
                        append(location.objectName)
                    }
                    binding.textViewLocation.visibility = View.VISIBLE
                }
            }

            storedReagent.expirationDate?.let { expDate ->

                binding.textViewExpiry.text = buildString {
                    append("Expires: ")
                    append(formatDate(expDate))
                }
            }

            if (!storedReagent.notes.isNullOrBlank()) {
                binding.textViewNotes.text = storedReagent.notes
            } else {
                binding.textViewNotes.text = buildString {
                    append("No notes")
                }
            }

            binding.textViewAddedBy.text = buildString {
                append("Added by: ")
                append(storedReagent.user.username)
            }

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

    private fun formatDate(expiryDateStr: String?): String {
        if (expiryDateStr.isNullOrEmpty()) return "No expiry date"

        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val expiryDate = dateFormat.parse(expiryDateStr) ?: return expiryDateStr
            val currentDate = Calendar.getInstance().time

            val diffMs = expiryDate.time - currentDate.time
            val diffDays = diffMs / (24 * 60 * 60 * 1000)

            return when {
                diffDays < 0 -> "Expired ${-diffDays} days ago"
                diffDays == 0L -> "Expires today"
                diffDays == 1L -> "Expires tomorrow"
                diffDays < 30 -> "Expires in $diffDays days"
                else -> "$expiryDateStr (${diffDays} days left)"
            }
        } catch (e: Exception) {
            return expiryDateStr
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
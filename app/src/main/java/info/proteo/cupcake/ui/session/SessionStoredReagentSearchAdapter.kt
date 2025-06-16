package info.proteo.cupcake.ui.session

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.R
import info.proteo.cupcake.shared.data.model.reagent.StoredReagent
import info.proteo.cupcake.shared.data.model.storage.StoragePathItem
import info.proteo.cupcake.data.repository.StorageRepository
import info.proteo.cupcake.databinding.ItemSessionStoredReagentSearchBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class SessionStoredReagentSearchAdapter(
    private val onItemClick: (StoredReagent) -> Unit,
    private val onLoadMore: () -> Unit,
    private val storageRepository: StorageRepository // Inject StorageRepository
) : ListAdapter<StoredReagent, SessionStoredReagentSearchAdapter.ViewHolder>(DiffCallback()) {

    private var selectedReagent: StoredReagent? = null
    private var isLoading = false
    private val job = SupervisorJob()
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main + job)

    companion object {
        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_LOADING = 1
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        job.cancel()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSessionStoredReagentSearchBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onItemClick, storageRepository, coroutineScope)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, item == selectedReagent)

        if (position == itemCount - 1 && !isLoading) {
            onLoadMore()
        }
    }

    fun getSelectedReagent(): StoredReagent? {
        return selectedReagent
    }

    fun setLoading(isLoading: Boolean) {
        this.isLoading = isLoading
        // You might want to notify the adapter if you add a loading view type
        // For now, this just controls the onLoadMore callback
    }

    fun appendItems(newItems: List<StoredReagent>) {
        val currentList = currentList.toMutableList()
        currentList.addAll(newItems)
        submitList(currentList)
    }


    class ViewHolder(
        private val binding: ItemSessionStoredReagentSearchBinding,
        private val onItemClick: (StoredReagent) -> Unit,
        private val storageRepository: StorageRepository, // Pass StorageRepository
        private val coroutineScope: CoroutineScope
    ) : RecyclerView.ViewHolder(binding.root) {

        private val expirationDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC") // Assuming expiration date is in UTC
        }
        private val displayDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        private fun formatExpirationDate(dateString: String?): String {
            if (dateString.isNullOrEmpty()) return "N/A"
            return try {
                val date = expirationDateFormat.parse(dateString)
                if (date != null) displayDateFormat.format(date) else "N/A"
            } catch (e: Exception) {
                Log.w("ReagentSearchVH", "Could not parse expiration date: $dateString", e)
                dateString // return original string if parsing fails
            }
        }

        fun bind(reagent: StoredReagent, isSelected: Boolean) {
            binding.textViewReagentName.text = reagent.reagent.name
            binding.textViewQuantity.text = reagent.currentQuantity.toString()
            binding.textViewUnit.text = reagent.reagent.unit
            binding.textViewStorage.text = ""

            reagent.storageObjectId?.let { storageId ->
                coroutineScope.launch {
                    try {
                        val pathResult = storageRepository.getPathToRoot(storageId)
                        if (pathResult.isSuccess) {
                            val pathItems = pathResult.getOrNull()
                            if (!pathItems.isNullOrEmpty()) {
                                val pathString = pathItems.joinToString(" > ") { it.name }
                                binding.textViewStorage.text = pathString
                            } else {
                                binding.textViewStorage.text = reagent.storageObject!!.objectName
                            }
                        } else {
                            Log.w("ReagentSearchVH", "Failed to get path for ${reagent.storageObject!!.objectName}: ${pathResult.exceptionOrNull()?.message}")
                            binding.textViewStorage.text = reagent.storageObject!!.objectName
                        }
                    } catch (e: Exception) {
                        Log.e("ReagentSearchVH", "Error fetching storage path for ${reagent.storageObject!!.objectName}", e)
                        binding.textViewStorage.text = reagent.storageObject!!.objectName
                    }
                }
            } ?: run {
                binding.textViewStorage.text = reagent.storageObject!!.objectName
            }


            if (!reagent.barcode.isNullOrEmpty()) {
                binding.layoutBarcode.visibility = View.VISIBLE
                binding.textViewBarcode.text = reagent.barcode
            } else {
                binding.layoutBarcode.visibility = View.GONE
            }

            if (!reagent.expirationDate.isNullOrEmpty()) {
                binding.layoutExpiration.visibility = View.VISIBLE
                binding.textViewExpirationDate.text = formatExpirationDate(reagent.expirationDate)
            } else {
                binding.layoutExpiration.visibility = View.GONE
            }

            if (!reagent.pngBase64.isNullOrEmpty()) {
                try {
                    val base64Content = reagent.pngBase64!!.replace("data:image/png;base64,", "")
                    val imageBytes = Base64.decode(base64Content, Base64.DEFAULT)
                    val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    binding.reagentImageView.setImageBitmap(decodedImage)
                    binding.reagentImageView.visibility = View.VISIBLE
                } catch (e: IllegalArgumentException) {
                    Log.e("ReagentSearchVH", "Base64 decoding failed for reagent image", e)
                    binding.reagentImageView.visibility = View.GONE
                }
            } else {
                binding.reagentImageView.visibility = View.GONE
            }

            itemView.isSelected = isSelected
            binding.root.strokeWidth = if (isSelected) 3 else 0
            binding.root.strokeColor = if (isSelected) itemView.context.getColor(R.color.secondary) else itemView.context.getColor(android.R.color.transparent)


            itemView.setOnClickListener {
                (bindingAdapter as? SessionStoredReagentSearchAdapter)?.let { adapter ->
                    val previouslySelected = adapter.selectedReagent
                    if (previouslySelected == reagent) { // Deselect if clicking the same item
                        adapter.selectedReagent = null
                    } else {
                        adapter.selectedReagent = reagent
                    }

                    previouslySelected?.let { prev ->
                        val prevPosition = adapter.currentList.indexOf(prev)
                        if (prevPosition != -1) adapter.notifyItemChanged(prevPosition)
                    }

                    val currentPosition = adapter.currentList.indexOf(reagent)
                    if (currentPosition != -1) adapter.notifyItemChanged(currentPosition)

                    adapter.onItemClick(reagent)
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<StoredReagent>() {
        override fun areItemsTheSame(oldItem: StoredReagent, newItem: StoredReagent): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: StoredReagent, newItem: StoredReagent): Boolean {
            return oldItem == newItem
        }
    }
}
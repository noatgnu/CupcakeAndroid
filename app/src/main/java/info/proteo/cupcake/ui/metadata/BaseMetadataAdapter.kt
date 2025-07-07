package info.proteo.cupcake.ui.metadata

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.R
import info.proteo.cupcake.databinding.ItemMetadataBinding

/**
 * Base adapter for metadata items that provides common functionality
 * and reduces code duplication across different metadata adapters.
 */
abstract class BaseMetadataAdapter<T : Any>(
    diffCallback: DiffUtil.ItemCallback<T>
) : ListAdapter<T, BaseMetadataAdapter.ViewHolder>(diffCallback) {

    class ViewHolder(val binding: ItemMetadataBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMetadataBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        bindItem(holder.binding, item)
        
        holder.binding.sdrfButton.setOnClickListener {
            val converter = SDRFConverter()
            val (columnName, sdrfValue) = convertToSdrf(converter, item)
            showSdrfDialog(holder.itemView.context, columnName, sdrfValue)
        }
    }

    /**
     * Bind the item data to the view elements.
     * Implementations should handle setting title, subtitle, and description,
     * and manage visibility of optional fields.
     */
    protected abstract fun bindItem(binding: ItemMetadataBinding, item: T)

    /**
     * Convert the item to SDRF format using the provided converter.
     */
    protected abstract fun convertToSdrf(converter: SDRFConverter, item: T): Pair<String, String>

    /**
     * Helper method to set text and manage visibility
     */
    protected fun setTextWithVisibility(textView: TextView, text: String?) {
        if (!text.isNullOrEmpty()) {
            textView.text = text
            textView.visibility = View.VISIBLE
        } else {
            textView.visibility = View.GONE
        }
    }

    private fun showSdrfDialog(context: Context, columnName: String, sdrfValue: String) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_sdrf, null)
        dialogView.findViewById<TextView>(R.id.sdrfColumnNameText).text = columnName
        dialogView.findViewById<TextView>(R.id.sdrfValueText).text = sdrfValue

        AlertDialog.Builder(context)
            .setTitle("SDRF Format")
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .show()
    }

    companion object {
        /**
         * Creates a generic DiffUtil.ItemCallback for entities with an 'id' property
         */
        inline fun <reified T : Any> createDiffCallback(
            crossinline getId: (T) -> Any
        ): DiffUtil.ItemCallback<T> {
            return object : DiffUtil.ItemCallback<T>() {
                override fun areItemsTheSame(oldItem: T, newItem: T): Boolean {
                    return getId(oldItem) == getId(newItem)
                }

                override fun areContentsTheSame(oldItem: T, newItem: T): Boolean {
                    return oldItem == newItem
                }
            }
        }
    }
}
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
import info.proteo.cupcake.data.local.entity.metadatacolumn.SubcellularLocationEntity

import info.proteo.cupcake.databinding.ItemMetadataBinding

class SubcellularLocationAdapter : ListAdapter<SubcellularLocationEntity, SubcellularLocationAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<SubcellularLocationEntity>() {
        override fun areItemsTheSame(oldItem: SubcellularLocationEntity, newItem: SubcellularLocationEntity): Boolean {
            return oldItem.locationIdentifier == newItem.locationIdentifier
        }

        override fun areContentsTheSame(oldItem: SubcellularLocationEntity, newItem: SubcellularLocationEntity): Boolean {
            return oldItem == newItem
        }
    }
) {
    class ViewHolder(val binding: ItemMetadataBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMetadataBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val location = getItem(position)

        with(holder.binding) {
            titleText.text = location.accession
            subtitleText.text = location.locationIdentifier
            descriptionText.text = location.definition

            sdrfButton.setOnClickListener {
                val converter = SDRFConverter()
                val (columnName, sdrfValue) = converter.convertSubcellularLocation(location)
                showSdrfDialog(holder.itemView.context, columnName, sdrfValue)
            }
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
}
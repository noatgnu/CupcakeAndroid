package info.proteo.cupcake.ui.metadata

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.R
import info.proteo.cupcake.data.local.entity.metadatacolumn.HumanDiseaseEntity
import info.proteo.cupcake.databinding.ItemMetadataBinding

class HumanDiseaseAdapter : ListAdapter<HumanDiseaseEntity, HumanDiseaseAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<HumanDiseaseEntity>() {
        override fun areItemsTheSame(oldItem: HumanDiseaseEntity, newItem: HumanDiseaseEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HumanDiseaseEntity, newItem: HumanDiseaseEntity): Boolean {
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
        val disease = getItem(position)

        with(holder.binding) {
            titleText.text = disease.identifier
            subtitleText.text = disease.acronym ?: ""
            descriptionText.text = disease.definition ?: disease.synonyms ?: ""

            sdrfButton.setOnClickListener {
                val converter = SDRFConverter()

                val (columnName, sdrfValue) = converter.convertHumanDisease(disease.identifier?:"")
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
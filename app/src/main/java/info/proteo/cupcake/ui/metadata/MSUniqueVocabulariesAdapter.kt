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
import info.proteo.cupcake.data.local.entity.metadatacolumn.MSUniqueVocabulariesEntity
import info.proteo.cupcake.databinding.ItemMetadataBinding

class MSUniqueVocabulariesAdapter : ListAdapter<MSUniqueVocabulariesEntity, MSUniqueVocabulariesAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<MSUniqueVocabulariesEntity>() {
        override fun areItemsTheSame(oldItem: MSUniqueVocabulariesEntity, newItem: MSUniqueVocabulariesEntity): Boolean {
            return oldItem.accession == newItem.accession
        }

        override fun areContentsTheSame(oldItem: MSUniqueVocabulariesEntity, newItem: MSUniqueVocabulariesEntity): Boolean {
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
        val vocab = getItem(position)

        with(holder.binding) {
            titleText.text = vocab.accession
            subtitleText.text = vocab.name ?: ""
            descriptionText.text = "${vocab.definition} (${vocab.termType})"

            sdrfButton.setOnClickListener {
                val converter = SDRFConverter()
                val columnName = getColumnNameFromTermType(vocab.termType)
                val (_, sdrfValue) = converter.convertMSUniqueVocabulary(vocab, columnName)
                showSdrfDialog(holder.itemView.context, columnName, sdrfValue)
            }
        }
    }

    private fun getColumnNameFromTermType(termType: String?): String {
        return when (termType) {
            "sample attribute" -> "label"
            "cleavage agent" -> "cleavage agent details"
            "instrument" -> "instrument"
            "dissociation method" -> "dissociation method"
            "mass analyzer type" -> "ms2 analyzer type"
            "enrichment process" -> "enrichment process"
            "fractionation method" -> "fractionation method"
            "proteomics data acquisition method" -> "proteomics data acquisition method"
            "reduction reagent" -> "reduction reagent"
            "alkylation reagent" -> "alkylation reagent"
            else -> "parameter value" // Default column name
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
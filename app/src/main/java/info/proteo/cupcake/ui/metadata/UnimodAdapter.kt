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
import info.proteo.cupcake.data.local.entity.metadatacolumn.UnimodEntity
import info.proteo.cupcake.databinding.ItemMetadataBinding
import org.json.JSONArray

data class UnimodSpec(
    val name: String,
    val position: String? = null,
    val aa: String? = null,
    val classification: String? = null,
    val monoMass: Double? = null,
    val modificationType: String? = null,
    val targetSite: String? = null
)

class UnimodAdapter : ListAdapter<UnimodEntity, UnimodAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<UnimodEntity>() {
        override fun areItemsTheSame(oldItem: UnimodEntity, newItem: UnimodEntity): Boolean {
            return oldItem.accession == newItem.accession
        }

        override fun areContentsTheSame(oldItem: UnimodEntity, newItem: UnimodEntity): Boolean {
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
        val unimod = getItem(position)

        with(holder.binding) {
            titleText.text = unimod.name
            subtitleText.text = "Accession: ${unimod.accession}"

            val descriptionBuilder = StringBuilder()

            // Add definition if available
            if (!unimod.definition.isNullOrBlank()) {
                descriptionBuilder.append("${unimod.definition}\n\n")
            }

            if (!unimod.additionalData.isNullOrBlank()) {
                try {
                    val (deltaMass, specs) = parseAdditionalData(unimod.additionalData)

                    // Add delta mass if available
                    deltaMass?.let {
                        descriptionBuilder.append("Delta Mono Mass: $it\n\n")
                    }

                    // Add specifications if available
                    if (specs.isNotEmpty()) {
                        descriptionBuilder.append("Specifications:\n")

                        specs.forEach { spec ->
                            descriptionBuilder.append("• ${spec.name.removePrefix("spec_")}:\n")

                            spec.aa?.let {
                                descriptionBuilder.append("  Target: $it\n")
                            }

                            spec.position?.let {
                                descriptionBuilder.append("  Position: $it\n")
                            }

                            spec.classification?.let {
                                descriptionBuilder.append("  Classification: $it\n")
                            }

                            spec.monoMass?.let {
                                descriptionBuilder.append("  Mono Mass: $it\n")
                            }

                            spec.targetSite?.let {
                                descriptionBuilder.append("  Target Site: $it\n")
                            }

                            descriptionBuilder.append("\n")
                        }
                    }
                } catch (e: Exception) {
                    descriptionBuilder.append("Error parsing additional data: ${e.message}")
                }
            }

            descriptionText.text = descriptionBuilder.toString()

            sdrfButton.setOnClickListener {
                val converter = SDRFConverter()

                // Parse the specs
                if (!unimod.additionalData.isNullOrBlank()) {
                    try {
                        val (deltaMass, parsedSpecs) = parseAdditionalData(unimod.additionalData)

                        // Enhance specs with data from main unimod entity
                        val enhancedSpecs = enhanceSpecsWithUnimodData(parsedSpecs, unimod, deltaMass)

                        if (enhancedSpecs.isNotEmpty()) {
                            // For Unimod, each spec can be its own SDRF
                            showMultipleSpecsDialog(holder.itemView.context, unimod, enhancedSpecs)
                        } else {
                            // If no specs, just convert the basic info
                            val (columnName, sdrfValue) = converter.convertUnimod(unimod)
                            showSdrfDialog(holder.itemView.context, columnName, sdrfValue)
                        }
                    } catch (e: Exception) {
                        // If parsing fails, just convert the basic info
                        val (columnName, sdrfValue) = converter.convertUnimod(unimod)
                        showSdrfDialog(holder.itemView.context, columnName, sdrfValue)
                    }
                } else {
                    // If no additional data, just convert the basic info
                    val (columnName, sdrfValue) = converter.convertUnimod(unimod)
                    showSdrfDialog(holder.itemView.context, columnName, sdrfValue)
                }
            }
        }
    }

    private fun enhanceSpecsWithUnimodData(specs: List<UnimodSpec>, unimod: UnimodEntity, deltaMass: Double?): List<UnimodSpec> {
        return specs.map { spec ->
            var enhancedSpec = spec

            // If mono mass is missing but delta mass is available
            if (enhancedSpec.monoMass == null && deltaMass != null) {
                enhancedSpec = enhancedSpec.copy(monoMass = deltaMass)
            }

            // If target site is missing but AA is available
            if (enhancedSpec.targetSite == null && !enhancedSpec.aa.isNullOrBlank()) {
                enhancedSpec = enhancedSpec.copy(targetSite = enhancedSpec.aa)
            }

            // If AA is missing but target site is available
            if (enhancedSpec.aa == null && !enhancedSpec.targetSite.isNullOrBlank()) {
                enhancedSpec = enhancedSpec.copy(aa = enhancedSpec.targetSite)
            }

            // If position is missing, use a default
            if (enhancedSpec.position == null) {
                // Try to extract position from definition
                val position = when {
                    unimod.definition?.contains("N-terminal", ignoreCase = true) == true -> "Protein N-term"
                    unimod.definition?.contains("C-terminal", ignoreCase = true) == true -> "Protein C-term"
                    unimod.definition?.contains("N-term", ignoreCase = true) == true -> "Any N-term"
                    unimod.definition?.contains("C-term", ignoreCase = true) == true -> "Any C-term"
                    else -> "Anywhere"
                }
                enhancedSpec = enhancedSpec.copy(position = position)
            }

            // Default to Variable modification if none specified
            if (enhancedSpec.modificationType == null) {
                enhancedSpec = enhancedSpec.copy(modificationType = "Variable")
            }

            // If classification is missing, try to extract from definition
            if (enhancedSpec.classification == null && !unimod.definition.isNullOrBlank()) {
                val lowerDef = unimod.definition.lowercase()
                val classification = when {
                    lowerDef.contains("post-translational") -> "Post-translational"
                    lowerDef.contains("artifact") || lowerDef.contains("artefact") -> "Artifact"
                    lowerDef.contains("isotopic") -> "Isotopic label"
                    lowerDef.contains("chemical") -> "Chemical derivative"
                    else -> null
                }

                if (classification != null) {
                    enhancedSpec = enhancedSpec.copy(classification = classification)
                }
            }

            enhancedSpec
        }
    }

    private fun showMultipleSpecsDialog(context: Context, unimod: UnimodEntity, specs: List<UnimodSpec>) {
        // Create a dialog with a list of specs to choose from
        val items = specs.map { spec ->
            val title = spec.name.removePrefix("spec_")
            val details = listOfNotNull(
                spec.modificationType?.let { "Type: $it" },
                spec.aa?.let { "AA: $it" },
                spec.position?.let { "Position: $it" },
                spec.monoMass?.let { "Mass: $it" },
                spec.classification?.let { "Class: $it" }
            ).joinToString(", ")

            "$title ($details)"
        }.toTypedArray()

        AlertDialog.Builder(context)
            .setTitle("Choose Specification")
            .setItems(items) { dialog, which ->
                val selectedSpec = specs[which]
                showModificationTypeDialog(context, unimod, selectedSpec)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showModificationTypeDialog(context: Context, unimod: UnimodEntity, spec: UnimodSpec) {
        // If the spec already has a modification type, use it directly
        if (spec.modificationType != null) {
            val converter = SDRFConverter()
            val (columnName, sdrfValue) = converter.convertUnimodSpec(unimod, spec)
            showSdrfDialog(context, columnName, sdrfValue)
            return
        }

        // Otherwise, show dialog to select modification type
        val modTypes = arrayOf("Fixed", "Variable", "Annotated")

        AlertDialog.Builder(context)
            .setTitle("Select Modification Type")
            .setItems(modTypes) { dialog, which ->
                val modType = modTypes[which]
                val specWithModType = spec.copy(modificationType = modType)
                val converter = SDRFConverter()
                val (columnName, sdrfValue) = converter.convertUnimodSpec(unimod, specWithModType)
                showSdrfDialog(context, columnName, sdrfValue)
            }
            .setNegativeButton("Cancel", null)
            .show()
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

    private fun parseAdditionalData(jsonString: String): Pair<Double?, List<UnimodSpec>> {
        val specs = mutableMapOf<String, UnimodSpec>()
        var deltaMass: Double? = null

        try {
            val jsonArray = JSONArray(jsonString)

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val id = item.getString("id")
                val description = item.getString("description")

                // Extract delta mono mass
                if (id == "delta_mono_mass") {
                    deltaMass = description.toDoubleOrNull()
                    continue
                }

                // Process specifications
                if (id.startsWith("spec_")) {
                    val nameParts = id.split("_")
                    if (nameParts.size >= 2) {
                        val specName = "spec_${nameParts[1]}"

                        // Get or create spec object
                        val spec = specs.getOrPut(specName) {
                            UnimodSpec(name = specName)
                        }

                        // Update properties based on suffix
                        when {
                            id.endsWith("position") -> {
                                if (description.contains("Anywhere") ||
                                    description.contains("N-term") ||
                                    description.contains("C-term")) {
                                    specs[specName] = spec.copy(
                                        position = description.split(",")[0]
                                    )
                                }
                            }
                            id.endsWith("site") -> {
                                specs[specName] = spec.copy(aa = description)
                            }
                            id.endsWith("classification") -> {
                                specs[specName] = spec.copy(
                                    classification = description.split(",")[0]
                                )
                            }
                            id.endsWith("mono_mass") -> {
                                val mass = description.split(",")[0].toDoubleOrNull()
                                if (mass != null && mass > 0) {
                                    specs[specName] = spec.copy(monoMass = mass)
                                }
                            }
                            id.endsWith("target_site") -> {
                                specs[specName] = spec.copy(targetSite = description)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            throw e
        }

        return Pair(deltaMass, specs.values.toList())
    }
}
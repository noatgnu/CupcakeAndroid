package info.proteo.cupcake.ui.metadata

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import info.proteo.cupcake.R
import info.proteo.cupcake.data.local.entity.metadatacolumn.UnimodEntity
import org.json.JSONArray

data class UnimodSpec(
    val name: String,
    val position: String? = null,
    val aa: String? = null,
    val classification: String? = null,
    val monoMass: Double? = null
)

class UnimodAdapter : ListAdapter<UnimodEntity, UnimodAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_metadata, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.titleText)
        private val subtitle: TextView = view.findViewById(R.id.subtitleText)
        private val description: TextView = view.findViewById(R.id.descriptionText)

        fun bind(item: UnimodEntity) {
            title.text = item.name
            subtitle.text = "Accession: ${item.accession}"

            val descriptionBuilder = StringBuilder()

            // Add definition if available
            if (!item.definition.isNullOrBlank()) {
                descriptionBuilder.append("${item.definition}\n\n")
            }
            if (!item.additionalData.isNullOrBlank()) {
                try {
                    val (deltaMass, specs) = parseAdditionalData(item.additionalData)

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

                            descriptionBuilder.append("\n")
                        }
                    }
                } catch (e: Exception) {
                    descriptionBuilder.append("Error parsing additional data: ${e.message}")
                }
            }

            description.text = descriptionBuilder.toString()
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

    class DiffCallback : DiffUtil.ItemCallback<UnimodEntity>() {
        override fun areItemsTheSame(oldItem: UnimodEntity, newItem: UnimodEntity): Boolean {
            return oldItem.accession == newItem.accession
        }

        override fun areContentsTheSame(oldItem: UnimodEntity, newItem: UnimodEntity): Boolean {
            return oldItem == newItem
        }
    }
}
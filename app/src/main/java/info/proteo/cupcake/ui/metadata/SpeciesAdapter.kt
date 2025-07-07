package info.proteo.cupcake.ui.metadata

import info.proteo.cupcake.data.local.entity.metadatacolumn.SpeciesEntity
import info.proteo.cupcake.databinding.ItemMetadataBinding

class SpeciesAdapter : BaseMetadataAdapter<SpeciesEntity>(
    createDiffCallback { it.id }
) {
    
    override fun bindItem(binding: ItemMetadataBinding, item: SpeciesEntity) {
        with(binding) {
            titleText.text = item.code ?: item.taxon ?: "Unknown"
            setTextWithVisibility(subtitleText, item.officialName ?: item.commonName)
            setTextWithVisibility(descriptionText, item.synonym)
        }
    }

    override fun convertToSdrf(converter: SDRFConverter, item: SpeciesEntity): Pair<String, String> {
        return converter.convertSpecies(item)
    }
}
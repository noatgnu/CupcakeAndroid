package info.proteo.cupcake.ui.metadata

import info.proteo.cupcake.data.local.entity.metadatacolumn.HumanDiseaseEntity
import info.proteo.cupcake.databinding.ItemMetadataBinding

class HumanDiseaseAdapter : BaseMetadataAdapter<HumanDiseaseEntity>(
    createDiffCallback { it.id }
) {
    
    override fun bindItem(binding: ItemMetadataBinding, item: HumanDiseaseEntity) {
        with(binding) {
            titleText.text = item.identifier
            setTextWithVisibility(subtitleText, item.acronym)
            setTextWithVisibility(descriptionText, item.definition ?: item.synonyms)
        }
    }

    override fun convertToSdrf(converter: SDRFConverter, item: HumanDiseaseEntity): Pair<String, String> {
        return converter.convertHumanDisease(item.identifier ?: "")
    }
}
package info.proteo.cupcake.ui.metadata

import info.proteo.cupcake.data.local.entity.metadatacolumn.MSUniqueVocabulariesEntity
import info.proteo.cupcake.data.local.entity.metadatacolumn.SpeciesEntity
import info.proteo.cupcake.data.local.entity.metadatacolumn.SubcellularLocationEntity
import info.proteo.cupcake.data.local.entity.metadatacolumn.TissueEntity
import info.proteo.cupcake.data.local.entity.metadatacolumn.UnimodEntity


class SDRFConverter {
    fun convertSubcellularLocation(location: SubcellularLocationEntity): Pair<String, String> {
        val columnName = "subcellular location"
        val identifier = location.locationIdentifier
        val accession = location.accession

        val value = if (identifier!!.contains("AC=")) {
            "NT=$identifier"
        } else {
            "NT=$identifier;AC=$accession"
        }

        return Pair(columnName, value)
    }

    fun convertSpecies(species: SpeciesEntity): Pair<String, String> {
        val columnName = "organism"
        val officialName = species.officialName

        return Pair(columnName, officialName!!)
    }

    fun convertMSUniqueVocabulary(vocab: MSUniqueVocabulariesEntity, columnName: String): Pair<String, String> {
        val name = vocab.name
        val accession = vocab.accession

        val value = if (name!!.contains("AC=")) {
            "NT=$name"
        } else {
            "NT=$name;AC=$accession"
        }

        return Pair(columnName, value)
    }

    fun convertUnimod(unimod: UnimodEntity): Pair<String, String> {
        val columnName = "modification parameters"
        val name = unimod.name
        val accession = unimod.accession

        val value = if (name!!.contains("AC=") || name!!.contains("ac=")) {
            "NT=$name"
        } else {
            "AC=$accession;NT=$name"
        }

        return Pair(columnName, value)
    }

    fun convertHumanDisease(disease: String): Pair<String, String> {
        val columnName = "disease"
        val value = "NT=$disease"
        return Pair(columnName, value)
    }

    fun convertTissue(tissue: TissueEntity) : Pair<String, String> {
        val columnName = "organism part"
        val name = tissue.identifier
        val value = if (name!!.contains("AC=")) {
            "NT=$name"
        } else {
            "NT=$name;AC=${tissue.accession}"
        }
        return Pair(columnName, value)
    }


}
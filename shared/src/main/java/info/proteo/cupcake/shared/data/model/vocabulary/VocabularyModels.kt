package info.proteo.cupcake.shared.data.model.vocabulary

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Tissue(
    val identifier: String,
    val accession: String,
    val synonyms: String?,
    @Json(name = "cross_references") val crossReferences: String?
)

@JsonClass(generateAdapter = true)
data class HumanDisease(
    val identifier: String,
    val acronym: String?,
    val accession: String,
    val synonyms: String?,
    @Json(name = "cross_references") val crossReferences: String?,
    val definition: String?,
    val keywords: String?
)

@JsonClass(generateAdapter = true)
data class Species(
    val id: Int,
    val code: String,
    val taxon: String?,
    @Json(name = "common_name") val commonName: String?,
    @Json(name = "official_name") val officialName: String?,
    val synonym: String?
)

@JsonClass(generateAdapter = true)
data class SubcellularLocation(
    @Json(name = "location_identifier") val locationIdentifier: String,
    @Json(name = "topology_identifier") val topologyIdentifier: String?,
    @Json(name = "orientation_identifier") val orientationIdentifier: String?,
    val accession: String,
    val definition: String?,
    val synonyms: String?,
    val content: String?,
    @Json(name = "is_a") val isA: String?,
    @Json(name = "part_of") val partOf: String?,
    val keyword: String?,
    @Json(name = "gene_ontology") val geneOntology: String?,
    val annotation: String?,
    val references: String?,
    val links: String?
)

@JsonClass(generateAdapter = true)
data class MSUniqueVocabularies(
    val accession: String,
    val name: String,
    val definition: String?,
    @Json(name = "term_type") val termType: String
)

@JsonClass(generateAdapter = true)
data class Unimod(
    val accession: String,
    val name: String,
    val definition: String?,
    @Json(name = "additional_data") val additionalData: String?
)
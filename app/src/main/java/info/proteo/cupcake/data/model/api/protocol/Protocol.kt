package info.proteo.cupcake.data.model.api.protocol

import com.squareup.moshi.Json
import info.proteo.cupcake.data.model.api.annotation.Annotation
import info.proteo.cupcake.data.model.api.metadatacolumn.MetadataColumn
import info.proteo.cupcake.data.model.api.reagent.Reagent
import info.proteo.cupcake.data.model.api.tag.Tag

data class ProtocolModel(
    val id: Int,
    @Json(name = "protocol_id") val protocolId: String?,
    @Json(name = "protocol_created_on") val protocolCreatedOn: String?,
    @Json(name = "protocol_doi") val protocolDoi: String?,
    @Json(name = "protocol_title") val protocolTitle: String?,
    @Json(name = "protocol_description") val protocolDescription: String?,
    @Json(name = "protocol_url") val protocolUrl: String?,
    @Json(name = "protocol_version_uri") val protocolVersionUri: String?,
    val steps: List<ProtocolStep>,
    val sections: List<ProtocolSection>,
    val enabled: Boolean,
    @Json(name = "complexity_rating") val complexityRating: Float,
    @Json(name = "duration_rating") val durationRating: Float,
    val reagents: List<ProtocolReagent>,
    val tags: List<ProtocolTag>,
    @Json(name = "metadata_columns") val metadataColumns: List<MetadataColumn>
)

data class ProtocolStep(
    val id: Int,
    val protocol: Int,
    @Json(name = "step_id") val stepId: String?,
    @Json(name = "step_description") val stepDescription: String?,
    @Json(name = "step_section") val stepSection: String?,
    @Json(name = "step_duration") val stepDuration: String?,
    @Json(name = "next_step") val nextStep: Int?,
    val annotations: List<Annotation>,
    val variations: List<StepVariation>,
    @Json(name = "previous_step") val previousStep: Int?,
    val reagents: List<StepReagent>,
    val tags: List<StepTag>,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?
)

data class ProtocolSection(
    val id: Int,
    val protocol: Int,
    @Json(name = "section_description") val sectionDescription: String?,
    @Json(name = "section_duration") val sectionDuration: String?,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?
)

data class ProtocolRating(
    val id: Int,
    val protocol: Int,
    val user: Int,
    @Json(name = "complexity_rating") val complexityRating: Int,
    @Json(name = "duration_rating") val durationRating: Int,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?
)

data class ProtocolReagent(
    val id: Int,
    val protocol: Int,
    val reagent: Reagent,
    val quantity: Float,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?
)

data class ProtocolTag(
    val id: Int,
    val protocol: Int,
    val tag: Tag,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?
)
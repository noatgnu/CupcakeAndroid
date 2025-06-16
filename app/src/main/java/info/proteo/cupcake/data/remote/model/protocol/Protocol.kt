package info.proteo.cupcake.data.remote.model.protocol

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import info.proteo.cupcake.shared.data.model.annotation.Annotation
import info.proteo.cupcake.shared.data.model.metadatacolumn.MetadataColumn
import info.proteo.cupcake.shared.data.model.reagent.Reagent
import info.proteo.cupcake.shared.data.model.tag.Tag

@JsonClass(generateAdapter = true)
data class ProtocolModel(
    val id: Int,
    @Json(name = "protocol_id") val protocolId: Long?,
    @Json(name = "protocol_created_on") val protocolCreatedOn: String?,
    @Json(name = "protocol_doi") val protocolDoi: String?,
    @Json(name = "protocol_title") val protocolTitle: String?,
    @Json(name = "protocol_description") val protocolDescription: String?,
    @Json(name = "protocol_url") val protocolUrl: String?,
    @Json(name = "protocol_version_uri") val protocolVersionUri: String?,
    val steps: List<ProtocolStep>? = emptyList<ProtocolStep>(),
    val sections: List<ProtocolSection>? = emptyList<ProtocolSection>(),
    val enabled: Boolean,
    @Json(name = "complexity_rating") val complexityRating: Float = 0f,
    @Json(name = "duration_rating") val durationRating: Float = 0f,
    val reagents: List<ProtocolReagent>? = emptyList<ProtocolReagent>(),
    val tags: List<ProtocolTag>? = emptyList<ProtocolTag>(),
    @Json(name = "metadata_columns") val metadataColumns: List<MetadataColumn>? = emptyList<MetadataColumn>(),
)

@JsonClass(generateAdapter = true)
data class ProtocolStep(
    val id: Int,
    val protocol: Int,
    @Json(name = "step_id") val stepId: Int?,
    @Json(name = "step_description") val stepDescription: String?,
    @Json(name = "step_section") val stepSection: Int?,
    @Json(name = "step_duration") val stepDuration: Int?,
    @Json(name = "next_step") val nextStep: List<Int>?,
    val annotations: List<Annotation>? = emptyList<Annotation>(),
    val variations: List<StepVariation>? = emptyList<StepVariation>(),
    @Json(name = "previous_step") val previousStep: Int?,
    val reagents: List<StepReagent>? = emptyList<StepReagent>(),
    val tags: List<StepTag>? = emptyList<StepTag>(),
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?
)

@JsonClass(generateAdapter = true)
data class ProtocolSection(
    val id: Int,
    val protocol: Int,
    @Json(name = "section_description") val sectionDescription: String?,
    @Json(name = "section_duration") val sectionDuration: Long?,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?
)

@JsonClass(generateAdapter = true)
data class ProtocolRating(
    val id: Int,
    val protocol: Int,
    val user: Int,
    @Json(name = "complexity_rating") val complexityRating: Int,
    @Json(name = "duration_rating") val durationRating: Int,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?
)

@JsonClass(generateAdapter = true)
data class ProtocolReagent(
    val id: Int,
    val protocol: Int,
    val reagent: Reagent,
    val quantity: Float,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?
)

@JsonClass(generateAdapter = true)
data class ProtocolTag(
    val id: Int,
    val protocol: Int,
    val tag: Tag,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?
)
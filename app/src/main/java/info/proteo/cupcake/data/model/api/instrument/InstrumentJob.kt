package info.proteo.cupcake.data.model.api.instrument

import com.squareup.moshi.Json
import info.proteo.cupcake.data.model.api.metadatacolumn.MetadataColumn
import info.proteo.cupcake.data.model.api.metadatacolumn.MetadataTableTemplate
import info.proteo.cupcake.data.model.api.project.Project
import info.proteo.cupcake.data.model.api.protocol.ProtocolModel
import info.proteo.cupcake.data.model.api.protocol.Session
import info.proteo.cupcake.data.model.api.reagent.StoredReagent
import info.proteo.cupcake.data.model.api.user.LabGroupBasic
import info.proteo.cupcake.data.model.api.user.UserBasic

data class InstrumentJob(
    val id: Int,
    val instrument: Int,
    val user: UserBasic?,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?,
    val project: Project?,
    val session: Session?,
    val protocol: ProtocolModel?,
    @Json(name = "job_type") val jobType: String?,
    @Json(name = "user_annotations") val userAnnotations: List<Annotation>?,
    @Json(name = "staff_annotations") val staffAnnotations: List<Annotation>?,
    val assigned: Boolean?,
    val staff: List<UserBasic>?,
    @Json(name = "instrument_usage") val instrumentUsage: InstrumentUsage?,
    @Json(name = "job_name") val jobName: String?,
    @Json(name = "user_metadata") val userMetadata: List<MetadataColumn>?,
    @Json(name = "staff_metadata") val staffMetadata: List<MetadataColumn>?,
    @Json(name = "sample_number") val sampleNumber: Int?,
    @Json(name = "sample_type") val sampleType: String?,
    val funder: String?,
    @Json(name = "cost_center") val costCenter: String?,
    @Json(name = "stored_reagent") val storedReagent: StoredReagent?,
    @Json(name = "injection_volume") val injectionVolume: Float?,
    @Json(name = "injection_unit") val injectionUnit: String?,
    @Json(name = "search_engine") val searchEngine: String?,
    @Json(name = "search_engine_version") val searchEngineVersion: String?,
    @Json(name = "search_details") val searchDetails: String?,
    val location: String?,
    val status: String?,
    @Json(name = "service_lab_group") val serviceLabGroup: LabGroupBasic?,
    @Json(name = "selected_template") val selectedTemplate: MetadataTableTemplate?,
    @Json(name = "submitted_at") val submittedAt: String?,
    @Json(name = "completed_at") val completedAt: String?
)
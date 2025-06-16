package info.proteo.cupcake.shared.data.model.instrument

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class InstrumentUsage(
    val id: Int,
    val instrument: Int,
    val annotation: Int?,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?,
    @Json(name = "time_started") val timeStarted: String?,
    @Json(name = "time_ended") val timeEnded: String?,
    val user: String?,
    val description: String?,
    val approved: Boolean?,
    val maintenance: Boolean?,
    @Json(name = "approved_by") val approvedBy: Int?
)

@JsonClass(generateAdapter = true)
data class CreateInstrumentUsageRequest(
    val instrument: Int,
    @Json(name = "time_started") val timeStarted: String,
    @Json(name = "time_ended") val timeEnded: String,
    val description: String,
    val repeat: Int? = null,
    @Json(name = "repeat_until") val repeatUntil: String? = null,
    val maintenance: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class UpdateInstrumentUsageRequest(
    @Json(name = "time_started") val timeStarted: String? = null,
    @Json(name = "time_ended") val timeEnded: String? = null,
    val approved: Boolean? = null,
    val maintenance: Boolean? = null,
    val description: String? = null
)

@JsonClass(generateAdapter = true)
data class PatchInstrumentUsageRequest(
    val instrument: Int? = null,
    val annotation: Int? = null,
    @Json(name = "time_started") val timeStarted: String? = null,
    @Json(name = "time_ended") val timeEnded: String? = null,
    val description: String? = null,
    val approved: Boolean? = null,
    val maintenance: Boolean? = null
)


@JsonClass(generateAdapter = true)
data class ExportUsageRequest(
    val instruments: List<Int>? = null,
    @Json(name = "lab_group") val labGroup: List<Int>? = null,
    val user: List<Int>? = null,
    @Json(name = "time_started") val timeStarted: String? = null,
    @Json(name = "time_ended") val timeEnded: String? = null,
    val mode: String? = null,
    @Json(name = "file_format") val fileFormat: String? = null,
    @Json(name = "calculate_duration_with_cutoff") val calculateDurationWithCutoff: Boolean? = null,
    @Json(name = "instance_id") val instanceId: String? = null,
    @Json(name = "includes_maintenance") val includesMaintenance: Boolean? = null,
    @Json(name = "approved_only") val approvedOnly: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class ExportUsageResponse(
    @Json(name = "job_id") val jobId: String
)
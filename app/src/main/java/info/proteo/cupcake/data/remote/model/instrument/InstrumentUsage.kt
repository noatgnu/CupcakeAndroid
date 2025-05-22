package info.proteo.cupcake.data.remote.model.instrument

import com.squareup.moshi.Json

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
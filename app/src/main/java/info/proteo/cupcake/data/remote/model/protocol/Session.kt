package info.proteo.cupcake.data.remote.model.protocol

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Session(
    val id: Int,
    val user: Int,
    @Json(name = "unique_id") val uniqueId: String,
    val enabled: Boolean,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?,
    val protocols: List<Int>,
    val name: String?,
    @Json(name = "time_keeper") val timeKeeper: List<TimeKeeper>,
    @Json(name = "started_at") val startedAt: String?,
    @Json(name = "ended_at") val endedAt: String?,
    val projects: List<Int>
)
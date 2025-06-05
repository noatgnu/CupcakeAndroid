package info.proteo.cupcake.data.remote.model.protocol

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Session(
    val id: Int,
    val user: Int,
    @Json(name = "unique_id") val uniqueId: String,
    val enabled: Boolean,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    val protocols: List<Int> = emptyList(),
    val name: String? = null,
    @Json(name = "time_keeper") val timeKeeper: List<TimeKeeper> = emptyList(),
    @Json(name = "started_at") val startedAt: String? = null,
    @Json(name = "ended_at") val endedAt: String? = null,
    val projects: List<Int> = emptyList(),
)
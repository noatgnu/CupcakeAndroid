package info.proteo.cupcake.shared.data.model.protocol

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TimeKeeper(
    val id: Int,
    @Json(name = "start_time") val startTime: String?,
    val session: Int?,
    val step: Int?,
    val started: Boolean,
    @Json(name = "current_duration") val currentDuration: Int?
)
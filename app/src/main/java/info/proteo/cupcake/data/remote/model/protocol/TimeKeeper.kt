package info.proteo.cupcake.data.remote.model.protocol

import com.squareup.moshi.Json

data class TimeKeeper(
    val id: Int,
    @Json(name = "start_time") val startTime: String?,
    val session: Int?,
    val step: Int?,
    val started: Boolean,
    @Json(name = "current_duration") val currentDuration: Float?
)
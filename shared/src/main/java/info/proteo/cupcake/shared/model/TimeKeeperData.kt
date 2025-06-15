package info.proteo.cupcake.shared.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Shared TimeKeeper data model that can be synchronized between mobile and wearable
 * Matches the structure of the existing TimeKeeper model from the main app
 */
@JsonClass(generateAdapter = true)
data class TimeKeeperData(
    val id: Int,
    @Json(name = "start_time") val startTime: String?,
    val session: Int?,
    val step: Int?,
    val started: Boolean,
    @Json(name = "current_duration") val currentDuration: Int?,
    @Json(name = "session_name") val sessionName: String? = null,
    @Json(name = "step_name") val stepName: String? = null
)

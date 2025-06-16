package info.proteo.cupcake.shared.data.model.instrument

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DelayUsageRequest(
    @Json(name = "start_date") val startDate: String? = null,
    val days: Int
)

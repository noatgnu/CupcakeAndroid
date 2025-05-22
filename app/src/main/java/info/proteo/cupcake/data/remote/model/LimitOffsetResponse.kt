package info.proteo.cupcake.data.remote.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LimitOffsetResponse<T>(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<T>
)

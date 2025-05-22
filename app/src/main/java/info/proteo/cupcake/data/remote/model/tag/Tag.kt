package info.proteo.cupcake.data.remote.model.tag

import com.squareup.moshi.Json

data class Tag(
    val id: Int,
    val tag: String,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?
)
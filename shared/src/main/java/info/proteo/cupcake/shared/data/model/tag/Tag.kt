package info.proteo.cupcake.shared.data.model.tag

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Tag(
    val id: Int,
    val tag: String,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?
)
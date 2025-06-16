package info.proteo.cupcake.shared.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DataPermission(
    val edit: Boolean = false,
    val delete: Boolean = false,
    val view: Boolean = false,
    val use: Boolean = false
)